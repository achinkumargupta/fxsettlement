package net.corda.samples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import java.util.List;
import java.util.stream.Collectors;

import net.corda.confidential.IdentitySyncFlow;
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
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.finance.workflows.asset.CashUtils;

import java.util.*;
import java.security.PublicKey;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

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
        private final Currency currencyA;
        private final Currency currencyB;

        public InitiatorFlow(Currency currencyA, Currency currencyB, Party netAgainstParty) {
            this.currencyA = currencyA;
            this.currencyB = currencyB;
            this.netAgainstParty = netAgainstParty;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            QueryCriteria stateStatusCriteria = new VaultQueryCriteria(Vault.StateStatus.CONSUMED);
            System.out.println("IOUNetTradesFlow - Currency Pair " + currencyA + " :: " + currencyB + " Party " + netAgainstParty);

//            for ( StateAndRef<IOUState> s : getServiceHub().getVaultService().queryBy(IOUState.class, stateStatusCriteria).getStates()) {
//                System.out.println("CONSUMED Vault" + s);
//            }

            // Code to get all states
//            Vault.Page allResults = getServiceHub().getVaultService().queryBy(IOUState.class);
//            List<StateAndRef> validInputStatesToSettle = new ArrayList<StateAndRef>();
//            List<PublicKey> listOfRequiredSigners = new ArrayList<PublicKey>();
//            Amount<Currency> totalAmount = new Amount<Currency>(0, currencyA);
//            for (Object stateToSettle : allResults.getStates()) {
//                IOUState inputStateToSettle = (IOUState) ((StateAndRef) stateToSettle).getState().getData();
//                if (inputStateToSettle.getCounterParty().getOwningKey().equals(netAgainstParty.getOwningKey())) {
//                    // Pick the matching input states
//                    totalAmount = totalAmount.plus(getTradedAssetAmount());
//                    listOfRequiredSigners.addAll(inputStateToSettle.getParticipants()
//                            .stream().map(AbstractParty::getOwningKey)
//                            .collect(Collectors.toList()));
//
//                    validInputStatesToSettle.add((StateAndRef) stateToSettle);
//                    System.out.println("Matched state " + inputStateToSettle.getLender() + " :: " + netAgainstParty.getOwningKey());
//                }
//            }
//
//            System.out.println("Total Amount: " + totalAmount);
//            // Step 1. Get a reference to the notary service on our network and our key pair.
//            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
//            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
//
//            // Step 2. Check the party running this flows is the borrower.
//            if (validInputStatesToSettle.isEmpty()) {
//                throw new IllegalArgumentException("There are no trades to settle for party " + netAgainstParty);
//            }
//
//            System.out.println("List of signers: " + listOfRequiredSigners.stream().distinct().collect(Collectors.toList()));
//            // Step 3. Create a new TransactionBuilder object.
//            final TransactionBuilder builder = new TransactionBuilder(notary);
//
//            // Step 4. Check we have enough cash to settle the requested amount.
//            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), currency);
//            if (cashBalance.getQuantity() < totalAmount.getQuantity()) {
//                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
//            }
//            System.out.println("Cash balance: " + cashBalance);
//
//            // Step 5. Get some cash from the vault and add a spend to our transaction builder.
//            // Vault might contain states "owned" by anonymous parties. This is one of techniques to anonymize transactions
//            // generateSpend returns all public keys which have to be used to sign transaction
//            List<PublicKey> keyList = CashUtils.generateSpend(getServiceHub(), builder, totalAmount, getOurIdentityAndCert(), netAgainstParty).getSecond();
//
//            // Step 6. Add the IOU input states and settle command to the transaction builder.
//            for (StateAndRef validInputStateToSettle : validInputStatesToSettle) {
//                Command<NetTrades> command = new Command<>(
//                        new NetTrades(),
//                        listOfRequiredSigners.stream().distinct().collect(Collectors.toList())
//                );
//                builder.addCommand(command);
//                builder.addInputState(validInputStateToSettle);
//            }
//            // Step 8. Verify and sign the transaction.
//            builder.verify(getServiceHub());
//            keyList.addAll(Arrays.asList(getOurIdentity().getOwningKey()));
//            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, keyList);
//
//            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
//            FlowSession session = initiateFlow(netAgainstParty);
//            new IdentitySyncFlow.Send(session, ptx.getTx());
//
//            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));
//
//            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
//             *     and the causes it to be persisted to the vault of appropriate nodes.
//             */
//            return subFlow(new FinalityFlow(fullySignedTransaction, session));
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
//
//            class SignTxFlow extends SignTransactionFlow {
//
//                private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
//                    super(flowSession, progressTracker);
//                }
//
//                @Override
//                protected void checkTransaction(SignedTransaction stx) {
//                    // Once the transaction has verified, initialize txWeJustSignedID variable.
//                    txWeJustSigned = stx.getId();
//                }
//            }
//
//            // Create a sign transaction flows
//            SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());
//
//            // Run the sign transaction flows to sign the transaction
//            subFlow(signTxFlow);
//
//            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
//            return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));
            return null;
        }
    }
}
