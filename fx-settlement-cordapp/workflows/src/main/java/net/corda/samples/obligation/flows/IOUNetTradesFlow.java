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
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.workflows.asset.CashUtils;

import java.util.*;
import java.security.PublicKey;
import java.text.SimpleDateFormat;

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

        private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        public InitiatorFlow(Currency currencyA, Currency currencyB, Party netAgainstParty) {
            this.currencyA = currencyA;
            this.currencyB = currencyB;
            this.netAgainstParty = netAgainstParty;
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            System.out.println("IOUNetTradesFlow - Currency Pair " + currencyA + " :: " + currencyB + " Party " + netAgainstParty);

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            Vault.Page allResults = getServiceHub().getVaultService().queryBy(IOUState.class);
            List<StateAndRef> validInputStatesToSettle = new ArrayList<StateAndRef>();
            List<PublicKey> listOfRequiredSigners = new ArrayList<PublicKey>();

            List<Long> spends = getNetSpends(allResults, validInputStatesToSettle, listOfRequiredSigners);
            // Step 2. Check the party running this flows is the borrower.
            if (validInputStatesToSettle.isEmpty()) {
                throw new IllegalArgumentException("There are no trades with value date as today to settle for party " + netAgainstParty);
            }

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);
            FlowSession session = initiateFlow(netAgainstParty);

            // Get net spends and create spend commands
            long netSpendForCurrencyA = spends.get(0);
            long netSpendForCurrencyB = spends.get(1);

            // Notify the other party of the spends
//            session.send(new HashMap<Currency, Long>() {{put(currencyA, netSpendForCurrencyA);
//            put(currencyB, netSpendForCurrencyB);}});

            if (netSpendForCurrencyA > 0) {
                Amount netSpendForCurrencyAAmount = new Amount<>(netSpendForCurrencyA, currencyA);

                // Generate Cash Transfer Commands
                CashSpendHolder mySpends = CashSpendUtils.generateCashCommands(getServiceHub(),
                        currencyA,
                        netSpendForCurrencyAAmount,
                        getOurIdentity(),
                        netAgainstParty);
                CashSpendUtils.addCashCommandsToTransactionBuilder(mySpends, builder);
            } else {
//                // Request for commands from the counterparty
//                UntrustworthyData<CashSpendHolder> counterPartySpendHolder =
//                        session.sendAndReceive(CashSpendHolder.class, inputStateToSettle);
//
//                CashSpendHolder counterPartyCashCommands = counterPartySpendHolder.unwrap(data -> {
//                    System.out.println("Received Initiator CounterParty Data:" + data);
//                    return data;
//                });
//                CashSpendUtils.addCashCommandsToTransactionBuilder(counterPartyCashCommands, builder);
//                subFlow(new ReceiveStateAndRefFlow(session));
            }

            if (netSpendForCurrencyB > 0) {
                Amount netSpendForCurrencyBAmount = new Amount<>(netSpendForCurrencyB, currencyB);
                // Generate Cash Transfer Commands
                CashSpendHolder mySpends = CashSpendUtils.generateCashCommands(getServiceHub(),
                        currencyB,
                        netSpendForCurrencyBAmount,
                        getOurIdentity(),
                        netAgainstParty);
                CashSpendUtils.addCashCommandsToTransactionBuilder(mySpends, builder);
            } else {
                // Request for commands from the counterparty
//                UntrustworthyData<CashSpendHolder> counterPartySpendHolder =
//                        session.sendAndReceive(CashSpendHolder.class, inputStateToSettle);
//
//                CashSpendHolder counterPartyCashCommands = counterPartySpendHolder.unwrap(data -> {
//                    System.out.println("Received Initiator CounterParty Data:" + data);
//                    return data;
//                });
//                CashSpendUtils.addCashCommandsToTransactionBuilder(counterPartyCashCommands, builder);
//                subFlow(new ReceiveStateAndRefFlow(session));
            }

            // Step 6. Add the IOU input states and settle command to the transaction builder.
            for (StateAndRef validInputStateToSettle : validInputStatesToSettle) {
                Command<NetTrades> command = new Command<>(
                        new NetTrades(),
                        listOfRequiredSigners.stream().distinct().collect(Collectors.toList())
                );
                builder.addCommand(command);
                builder.addInputState(validInputStateToSettle);
            }

            // Step 8. Verify and sign the transaction.
            builder.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder,
                    Arrays.asList(getOurIdentity().getOwningKey()));

            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            new IdentitySyncFlow.Send(session, ptx.getTx());

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, session));
        }

        private List<Long> getNetSpends(Vault.Page allResults,
                                        List<StateAndRef> validInputStatesToSettle,
                                        List<PublicKey> listOfRequiredSigners) {
            long netSpendForCurrencyA = 0;
            long netSpendForCurrencyB = 0;

            for (Object stateToSettle : allResults.getStates()) {
                IOUState inputStateToSettle = (IOUState) ((StateAndRef) stateToSettle).getState().getData();

                if (!df.format(inputStateToSettle.getValueDate()).equals(df.format(new Date()))) {
                    continue;
                }
                if (!inputStateToSettle.getCounterParty().getOwningKey().equals(netAgainstParty.getOwningKey()) &&
                        !inputStateToSettle.getTradingParty().getOwningKey().equals(netAgainstParty.getOwningKey())) {
                    continue;
                }
                if (!inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyA.getCurrencyCode()) &&
                        !inputStateToSettle.getCounterAssetType().getCurrencyCode().equals(currencyA.getCurrencyCode())) {
                    continue;
                }
                if (!inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyB.getCurrencyCode()) &&
                        !inputStateToSettle.getCounterAssetType().getCurrencyCode().equals(currencyB.getCurrencyCode())) {
                    continue;
                }

                // This means tradingAmount has to be reduced from our account
                if (inputStateToSettle.getCounterParty().getOwningKey().equals(netAgainstParty.getOwningKey())) {
                    // Pick the matching input states
                    if (inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyA.getCurrencyCode())) {
                        netSpendForCurrencyA = netSpendForCurrencyA + inputStateToSettle.getTradedAssetAmount().getQuantity();
                        netSpendForCurrencyB = netSpendForCurrencyB - inputStateToSettle.getCounterAssetAmount().getQuantity();
                    }
                    else if (inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyB.getCurrencyCode())) {
                        netSpendForCurrencyA = netSpendForCurrencyA - inputStateToSettle.getCounterAssetAmount().getQuantity();
                        netSpendForCurrencyB = netSpendForCurrencyB + inputStateToSettle.getTradedAssetAmount().getQuantity();
                    }

                    listOfRequiredSigners.addAll(inputStateToSettle.getParticipants()
                            .stream().map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList()));

                    validInputStatesToSettle.add((StateAndRef) stateToSettle);
                }

                if (inputStateToSettle.getTradingParty().getOwningKey().equals(netAgainstParty.getOwningKey())) {
                    // Pick the matching input states
                    if (inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyA.getCurrencyCode())) {
                        netSpendForCurrencyA = netSpendForCurrencyA - inputStateToSettle.getTradedAssetAmount().getQuantity();
                        netSpendForCurrencyB = netSpendForCurrencyB + inputStateToSettle.getCounterAssetAmount().getQuantity();
                    }
                    else if (inputStateToSettle.getTradedAssetType().getCurrencyCode().equals(currencyB.getCurrencyCode())) {
                        netSpendForCurrencyA = netSpendForCurrencyA + inputStateToSettle.getCounterAssetAmount().getQuantity();
                        netSpendForCurrencyB = netSpendForCurrencyB - inputStateToSettle.getTradedAssetAmount().getQuantity();
                    }

                    listOfRequiredSigners.addAll(inputStateToSettle.getParticipants()
                            .stream().map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList()));

                    validInputStatesToSettle.add((StateAndRef) stateToSettle);
                }
            }

            return Arrays.asList(netSpendForCurrencyA, netSpendForCurrencyB);
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
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSigned = stx.getId();
                }
            }

            // Create a sign transaction flows
            SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flows to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));

        }
    }
}
