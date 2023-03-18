package net.corda.samples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import java.util.List;
import java.util.stream.Collectors;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;

import net.corda.samples.obligation.contracts.IOUContract;
import net.corda.samples.obligation.states.IOUState;
import static net.corda.samples.obligation.contracts.IOUContract.Commands.*;
import net.corda.core.identity.CordaX500Name;

/**
 * This is the flows which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flows returns the [SignedTransaction] that was committed to the ledger.
 */
public class IOUNetTradesFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final Party netAgainstParty;
        private final Currency currency;

        public InitiatorFlow(Currency currency, Party netAgainstParty) {
            this.currency = currency;
            this.netAgainstParty = netAgainstParty;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            System.out.println("Currency " + currency + " Party " + netAgainstParty);
            // Code to get all states
            Vault.Page allResults = getServiceHub().getVaultService().queryBy(IOUState.class);
            List<IOUState> validInputStateToSettle = new ArrayList<IOUState>();
            for (Object stateToSettle : allResults.getStates()) {
                IOUState inputStateToSettle = (IOUState) ((StateAndRef) stateToSettle).getState().getData();
                //if (inputStateToSettle)

                System.out.println(inputStateToSettle);

            }


            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            // Step 2. Create a new issue command.
            // Remember that a command is a CommandData object and a list of CompositeKeys
//            final Command<NetTrades> netTradesCommand = new Command<>(
//                    new NetTrades(), Collections.emptyList()
//                    .stream().map(AbstractParty::getOwningKey)
//                    .collect(Collectors.toList()));
//
//            // Step 3. Create a new TransactionBuilder object.
//            final TransactionBuilder builder = new TransactionBuilder(notary);
//
//            // Step 4. Add the iou as an output states, as well as a command to the transaction builder.
//            builder.addOutputState(null, IOUContract.IOU_CONTRACT_ID);
//            builder.addCommand(netTradesCommand);
//
//
//            // Step 5. Verify and sign it with our KeyPair.
//            builder.verify(getServiceHub());
//            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);
//
//
//            // Step 6. Collect the other party's signature using the SignTransactionFlow.
//            List<Party> otherParties = null;
//
//            otherParties.remove(getOurIdentity());
//
//            List<FlowSession> sessions = otherParties
//                    .stream().map(el -> initiateFlow(el))
//                    .collect(Collectors.toList());
//
//            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));
//
//            // Step 7. Assuming no exceptions, we can now finalise the transaction
//            return subFlow(new FinalityFlow(stx, sessions));
            return null;
        }
    }

    /**
     * This is the flows which signs IOU issuances.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUNetTradesFlow.InitiatorFlow.class)
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
                        req.using("This must be an IOU transaction", output instanceof IOUState);
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
