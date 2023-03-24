package net.corda.samples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.samples.obligation.contracts.IOUContract;
import net.corda.samples.obligation.states.IOUState;
import net.corda.samples.obligation.states.IOUState.TradeStatus;
import java.util.stream.Collectors;
import java.lang.IllegalArgumentException;
import java.security.PublicKey;
import java.util.*;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.identity.PartyAndCertificate;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.*;

public class IOUSettleFlow {

    /**
     * This is the flows which handles the settlement (partial or complete) of existing IOUs on the ledger.
     * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
     * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
     * The flows returns the [SignedTransaction] that was committed to the ledger.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier stateLinearId;

        public InitiatorFlow(UniqueIdentifier stateLinearId) {
            this.stateLinearId = stateLinearId;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException, TransactionResolutionException {
            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            List<UUID> listOfLinearIds = Arrays.asList(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToSettle = (IOUState) ((StateAndRef) results.getStates().get(0)).getState().getData();
            Party counterparty = inputStateToSettle.counterParty;
            Party tradingParty = inputStateToSettle.tradingParty;
            Amount<Currency> tradedAssetAmount = inputStateToSettle.tradedAssetAmount;
            Amount<Currency> counterAssetAmount = inputStateToSettle.counterAssetAmount;
            Currency tradedAssetType = inputStateToSettle.tradedAssetType;
            Currency counterAssetType = inputStateToSettle.counterAssetType;
            TradeStatus tradeStatus = inputStateToSettle.tradeStatus;

            // Step 2. Create a transaction builder.
            // Obtain a reference to a notary we wish to use.
            Party notary = inputStateAndRefToSettle.getState().getNotary();
            TransactionBuilder tb = new TransactionBuilder(notary);

            // Step 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) tradedAssetAmount.getToken());
            if (cashBalance.getQuantity() < tradedAssetAmount.getQuantity()) {
                throw new IllegalArgumentException("Not enough cash in " + tradedAssetType + " to settle with the trade.");
            }

            // Step 5. Get some cash from the vault and add a spend to our transaction builder.
            // Vault might contain states "owned" by anonymous parties. This is one of techniques to anonymize transactions
            // generateSpend returns all public keys which have to be used to sign transaction
            List<PublicKey> keyList = CashUtils.generateSpend(getServiceHub(), tb, tradedAssetAmount,
                    getOurIdentityAndCert(), counterparty).getSecond();

            // Step 6. Add the IOU input states and settle command to the transaction builder.
            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    Arrays.asList(counterparty.getOwningKey(),getOurIdentity().getOwningKey())
            );
            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // Step 8. Verify the transaction.
            tb.verify(getServiceHub());
            FlowSession session = initiateFlow(counterparty);

            // Sync the StateAndRefs between the nodes
            subFlow(new SendStateAndRefFlow(session, tb.inputStates().stream().map(t-> {
                                                try {
                                                    return getServiceHub().toStateAndRef(t);
                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }}).collect(Collectors.toList())));

            // Sync confidential identities between nodes
            List<AbstractParty> allInputParties = tb.inputStates().stream().flatMap(t-> {
                                                try {
                                                    return getServiceHub().toStateAndRef(t).getState().getData().getParticipants().stream();
                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }}).collect(Collectors.toList());

            List<AbstractParty> allOutputParties = tb.outputStates().stream()
                                                                    .flatMap(t -> t.getData().getParticipants().stream())
                                                                    .collect(Collectors.toList());

            Set<AbstractParty> uniqueParties = Stream.concat(allInputParties.stream(),
                                                             allOutputParties.stream()).collect(Collectors.toSet());
            Set<AbstractParty> confidentialParties = uniqueParties.stream().filter(t ->
                    getServiceHub().getNetworkMapCache().getNodesByLegalIdentityKey(t.getOwningKey()).isEmpty())
                    .collect(Collectors.toSet());

            Map<AbstractParty, PartyAndCertificate> partyToCertificateMap = new HashMap<AbstractParty, PartyAndCertificate>();
//            for (AbstractParty prty : uniqueParties) {
//                partyToCertificateMap.put(prty, getServiceHub().getNetworkMapCache().identityService
//                        .certificateFromKey(prty.getOwningKey()));
//            }

            System.out.println("CONFIDENTIAL : " + Arrays.toString(confidentialParties.toArray()));
            session.sendAndReceive(Object.class, confidentialParties).unwrap(
                    req -> {System.out.println(" WHAT I GOT " + req);return req;});

//                    UntrustworthyData<Boolean> packet2 = counterpartySession.sendAndReceive(Boolean.class, "You can send and receive any class!");
//                Boolean bool = packet2.unwrap(data -> {
//                    // Perform checking on the object received.
//                    // T O D O: Check the received object.
//                    // Return the object.
//                    return data;
//                });


//            progressTracker.currentStep = SENDING_TRANSACTION_PROPOSAL
//            session.send(tx)


            //getServiceHub().toStateAndRef(tb.inputStates().get(0))
            //getServiceHub().toStateAndRef(tb.inputStates().get(0)).getState().getData().getParticipants()
            //tb.outputStates().get(0).getData().getParticipants()
            //tb.outputStates().get(0).getData().getParticipants().get(0).getOwningKey()
            //getServiceHub().getNetworkMapCache().getNodesByLegalIdentityKey(tb.outputStates().get(1).getData().getParticipants().get(0).getOwningKey())
            //getServiceHub().getNetworkMapCache().identityService.certificateFromKey(tb.outputStates().get(0).getData().getParticipants().get(0).getOwningKey())
            //getServiceHub().getNetworkMapCache().identityService.certificateFromKey(getServiceHub().toStateAndRef(tb.inputStates().get(0)).getState().getData().getParticipants().get(0).getOwningKey())
            //List<PartyAndCertificate?>





            keyList.addAll(Arrays.asList(getOurIdentity().getOwningKey()));
            SignedTransaction ptx = getServiceHub().signInitialTransaction(tb, keyList);

            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            new IdentitySyncFlow.Send(session, ptx.getTx());

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, session));
        }

    }

    /**
     * This is the flows which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;
        private SecureHash txWeJustSignedId;

        public Responder(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSignedId = stx.getId();
                }
            }

            // Recieve and Sync the StateAndRefs between the nodes
            subFlow(new ReceiveStateAndRefFlow<ContractState>(otherPartyFlow));

            // Create a sign transaction flows
            SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flows to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSignedId));
        }
    }

    /**
     * Self issues the calling node an amount of cash in the desired currency.
     * Only used for demo/sample/training purposes!
     */

    @InitiatingFlow
    @StartableByRPC
    public static class SelfIssueCashFlow extends FlowLogic<Cash.State> {

        Amount<Currency> amount;

        SelfIssueCashFlow(Amount<Currency> amount) {
            this.amount = amount;
        }

        @Suspendable
        @Override
        public Cash.State call() throws FlowException {
            // Create the cash issue command.
            OpaqueBytes issueRef = OpaqueBytes.of(new byte[0]);

            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            // Create the cash issuance transaction.
            AbstractCashFlow.Result cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary));
            return (Cash.State) cashIssueTransaction.getStx().getTx().getOutput(0);
        }

    }

}
