package net.corda.fxsettlement.flows;

import co.paralleluniverse.fibers.Suspendable;
import java.util.List;
import java.util.stream.Collectors;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.utilities.ProgressTracker;

import net.corda.fxsettlement.contracts.RecordedTradeContract;
import net.corda.fxsettlement.states.RecordedTradeState;
import net.corda.core.identity.CordaX500Name;

import java.text.*;
import java.util.*;
import java.text.SimpleDateFormat;
/**
 * This is the flows which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flows returns the [SignedTransaction] that was committed to the ledger.
 */
public class TradeIssueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final RecordedTradeState inputState;
        private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        private final String valueDate;
        private final long tradedAmount;
        private final String tradedCurrency;
        private final long counterAmount;
        private final String counterCurrency;
        private final String myParty;
        private final String counterparty;

        public InitiatorFlow(String valueDate, long tradedAmount, String tradedCurrency,
                             long counterAmount, String counterCurrency, String myParty, String counterparty) {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.valueDate = valueDate;
            this.tradedAmount = tradedAmount;
            this.tradedCurrency = tradedCurrency;
            this.counterCurrency = counterCurrency;
            this.counterAmount = counterAmount;
            this.myParty = myParty;
            this.counterparty = counterparty;
            this.inputState = null;
        }

        public InitiatorFlow(RecordedTradeState inputState) {
            this.inputState = inputState;
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.valueDate = null;
            this.tradedAmount = 0;
            this.tradedCurrency = null;
            this.counterCurrency = null;
            this.counterAmount = 0;
            this.myParty = null;
            this.counterparty = null;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            RecordedTradeState state = inputState;
            try {
                if (state == null) {
                    state = new RecordedTradeState(new Date(),
                        df.parse(valueDate),
                        new Amount(tradedAmount, Currency.getInstance(tradedCurrency)),
                        Currency.getInstance(tradedCurrency),
                        getServiceHub().getIdentityService().wellKnownPartyFromX500Name(CordaX500Name.parse(myParty)),
                        new Amount(counterAmount, Currency.getInstance(counterCurrency)),
                        Currency.getInstance(counterCurrency),
                        getServiceHub().getIdentityService().wellKnownPartyFromX500Name(CordaX500Name.parse(counterparty)),
                        RecordedTradeState.TradeStatus.NEW);
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't parse the input parameters.");
            }

            // Create a new IOU states using the parameters given.
            try {
                if (state.getValueDate().compareTo(df.parse(df.format(new Date()))) < 0) {
                    throw new IllegalArgumentException("Can't record trade with value date that is prior to today");
                }
            } catch (ParseException e) {}

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            // Step 2. Create a new issue command.
            // Remember that a command is a CommandData object and a list of CompositeKeys
            final Command<RecordedTradeContract.Commands.Issue> issueCommand = new Command<>(
                    new RecordedTradeContract.Commands.Issue(), state.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList()));

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output states, as well as a command to the transaction builder.
            builder.addOutputState(state, RecordedTradeContract.IOU_CONTRACT_ID);
            builder.addCommand(issueCommand);

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            List<Party> otherParties = state.getParticipants()
                    .stream().map(el -> (Party)el)
                    .collect(Collectors.toList());

            otherParties.remove(getOurIdentity());

            List<FlowSession> sessions = otherParties
                    .stream().map(el -> initiateFlow(el))
                    .collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    /**
     * This is the flows which signs IOU issuances.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(TradeIssueFlow.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {

        private final FlowSession flowSession;
        private SecureHash txWeJustSigned;

        public ResponderFlow(FlowSession flowSession){
            this.flowSession = flowSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends SignTransactionFlow {

                private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
                    super(flowSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(req -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        req.using("This must be an IOU transaction", output instanceof RecordedTradeState);
                        return null;
                    });
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSigned = stx.getId();
                }
            }

            flowSession.getCounterpartyFlowInfo().getFlowVersion();

            // Create a sign transaction flows
            SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flows to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));

        }
    }
}
