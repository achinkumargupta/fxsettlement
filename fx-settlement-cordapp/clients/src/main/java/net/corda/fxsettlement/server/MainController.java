package net.corda.fxsettlement.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.fxsettlement.flows.*;
import net.corda.fxsettlement.states.RecordedTradeState;
import net.corda.fxsettlement.states.RecordedTradeState.TradeStatus;
import net.corda.core.node.services.Vault;
import java.text.SimpleDateFormat;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/iou") // The paths for HTTP requests are relative to this base path.
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public MainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/supported-currencies", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getSupportedCurrencies() {
        HashMap<String, List<String>> myMap = new HashMap<>();
        List<String> nodeNames = Arrays.asList("USD", "EUR", "GBP", "AUD");
        myMap.put("supportedCurrencies", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    @GetMapping(value = "/myorg",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whichismyorg(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.getOrganisation().toString());
        return myMap;
    }

    @GetMapping(value = "/ious",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<RecordedTradeState>> getIOUs() {
        // Filter by states type: IOU.
        return proxy.vaultQuery(RecordedTradeState.class).getStates();
    }

    @GetMapping(value = "/settled-trades",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<RecordedTradeState>> getSettledTrades() {
        // Filter by states type: IOU.
        QueryCriteria consumedStatusCriteria = new VaultQueryCriteria(Vault.StateStatus.CONSUMED);
        List<StateAndRef<RecordedTradeState>> consumedStatusTrades =
                proxy.vaultQueryByCriteria(consumedStatusCriteria, RecordedTradeState.class).getStates();
        List<UniqueIdentifier> settledIds = new ArrayList<UniqueIdentifier>();

        List<StateAndRef<RecordedTradeState>> activeTrades = proxy.vaultQuery(RecordedTradeState.class).getStates();
        List<UniqueIdentifier> activeIds = activeTrades.stream()
                .map(elt -> elt.getState().getData().getLinearId()).collect(Collectors.toList());

        List<StateAndRef<RecordedTradeState>> finalSettledTrades = new ArrayList<StateAndRef<RecordedTradeState>>();
        for (StateAndRef<RecordedTradeState> s : consumedStatusTrades) {
            boolean found = false;
            for (UniqueIdentifier activeOrSettledId : Stream.concat(activeIds.stream(), settledIds.stream())
                    .collect(Collectors.toList())) {
                if (s.getState().getData().getLinearId().equals(activeOrSettledId)) {
                    found = true;
                }
            }
            if (!found) {
                finalSettledTrades.add(s);
                settledIds.add(s.getState().getData().getLinearId());
            }
        }

        return finalSettledTrades;
    }

    @GetMapping(value = "/cash",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<Cash.State>> getCash() {
        // Filter by states type: Cash.
        return proxy.vaultQuery(Cash.State.class).getStates();
    }

    @GetMapping(value = "/cash-balances",produces = APPLICATION_JSON_VALUE)
    public Map<Currency,Amount<Currency>> cashBalances(){
        return getCashBalances(proxy);
    }

    @GetMapping(value = "/net-cash-balances",produces = APPLICATION_JSON_VALUE)
    public List<NetCashView.CashViewRecords> netCashBalances() {
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        List<Party> peersList = new ArrayList<>();
        List<Currency> currencies = new ArrayList<>();
        for (String counterparty : getPeers().get("peers")) {
            peersList.add(Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterparty))).orElseThrow(() -> new IllegalArgumentException("Unknown party name.")));
        }
        for (String cur : getSupportedCurrencies().get("supportedCurrencies")) {
            currencies.add(Currency.getInstance(cur));
        }
        NetCashView netCashView = new NetCashView(me,
                proxy.vaultQuery(RecordedTradeState.class).getStates(),
                peersList,
                currencies);

        return netCashView.getNetCashView();
    }

    @PutMapping(value =  "/issue-iou" , produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> issueIOU(@RequestParam(value = "valueDate") String valueDate,
                                           @RequestParam(value = "counterparty") String counterparty,
                                           @RequestParam(value = "tradedAmount") long tradedAmount,
                                           @RequestParam(value = "tradedCurrency") String tradedCurrency,
                                           @RequestParam(value = "counterAmount") long counterAmount,
                                           @RequestParam(value = "counterCurrency") String counterCurrency) throws IllegalArgumentException {
        // Get party objects for myself and the counterparty.
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        Party lender = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(counterparty))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));

        // Create a new IOU states using the parameters given.
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            RecordedTradeState state = new RecordedTradeState(
                    new Date(),
                    df.parse(valueDate),
                    new Amount<>((long) tradedAmount * 100, Currency.getInstance(tradedCurrency)),
                    Currency.getInstance(tradedCurrency),
                    me,
                    new Amount<>((long) counterAmount * 100, Currency.getInstance(counterCurrency)),
                    Currency.getInstance(counterCurrency),
                    lender,
                    TradeStatus.NEW);
            // Start the IOUIssueFlow. We block and waits for the flows to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(TradeIssueFlow.InitiatorFlow.class, state).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PutMapping(value =  "/net-trades" , produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> netTrades(@RequestParam(value = "currencyA") String currencyA,
                                            @RequestParam(value = "currencyB") String currencyB,
                                            @RequestParam(value = "party") String party) throws IllegalArgumentException {
        // Get party objects for myself and the counterparty.
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        Party lender = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));
        try {
            // Start the IOUNetTradesFlow. We block and waits for the flows to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(NetTradesFlow.InitiatorFlow.class, Currency.getInstance(currencyA), Currency.getInstance(currencyB), lender).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping(value =  "transfer-iou" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> transferIOU(@RequestParam(value = "id") String id,
                                              @RequestParam(value = "party") String party) {
        UniqueIdentifier linearId = new UniqueIdentifier(null,UUID.fromString(id));
        Party newLender = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party));
        try {
            proxy.startTrackedFlowDynamic(TradeTransferFlow.InitiatorFlow.class, linearId, newLender).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("IOU "+linearId.toString()+" transferred to "+party+".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/settle-iou?id=705dc5c5-44da-4006-a55b-e29f78955089&amount=98&currency=USD'
     */
    @GetMapping(value =  "settle-iou" , produces = TEXT_PLAIN_VALUE )
    public  ResponseEntity<String> settleIOU(@RequestParam(value = "id") String id) {

        UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(id));
        try {
            proxy.startTrackedFlowDynamic(TradeSettleFlow.InitiatorFlow.class, linearId).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Trade " + linearId.toString()+" is successfully settled.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Helper end-point to issue some cash to ourselves.
     * Example request:
     * curl -X GET 'http://localhost:10009/api/iou/self-issue-cash?amount=100&currency=USD'
     */
    @GetMapping(value =  "self-issue-cash" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> selfIssueCash(@RequestParam(value = "amount") int amount,
                      @RequestParam(value = "currency") String currency) {

        try {
            Cash.State cashState = proxy.startTrackedFlowDynamic(SelfIssueCashFlow.class,
                    new Amount<>((long) amount * 100, Currency.getInstance(currency))).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
