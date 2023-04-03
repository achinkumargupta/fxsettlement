package net.corda.fxsettlement.flows;

import co.paralleluniverse.fibers.Suspendable;
import java.util.List;
import java.util.stream.Collectors;

import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.UntrustworthyData;

import java.util.*;
import java.security.PublicKey;
import java.text.SimpleDateFormat;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

import net.corda.fxsettlement.contracts.RecordedTradeContract;
import net.corda.fxsettlement.states.RecordedTradeState;
import net.corda.core.identity.CordaX500Name;

/**
 * This is the flows which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flows returns the [SignedTransaction] that was committed to the ledger.
 */
public class NetTradesFlow {

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

            Vault.Page allResults = getServiceHub().getVaultService().queryBy(RecordedTradeState.class);
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
            session.send(new NetSpendHolder(currencyA, netSpendForCurrencyA,
                                            currencyB, netSpendForCurrencyB,
                                            getOurIdentity(), netAgainstParty));

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
                // Request for commands from the counterparty
                UntrustworthyData<CashSpendHolder> counterPartySpendHolder =
                        session.receive(CashSpendHolder.class);

                CashSpendHolder counterPartyCashCommands = counterPartySpendHolder.unwrap(data -> {
                    System.out.println("Received Initiator CounterParty Data:" + data);
                    return data;
                });
                CashSpendUtils.addCashCommandsToTransactionBuilder(counterPartyCashCommands, builder);
                subFlow(new ReceiveStateAndRefFlow(session));
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
                UntrustworthyData<CashSpendHolder> counterPartySpendHolder =
                        session.receive(CashSpendHolder.class);

                CashSpendHolder counterPartyCashCommands = counterPartySpendHolder.unwrap(data -> {
                    System.out.println("Received Initiator CounterParty Data:" + data);
                    return data;
                });
                CashSpendUtils.addCashCommandsToTransactionBuilder(counterPartyCashCommands, builder);
                subFlow(new ReceiveStateAndRefFlow(session));
            }

            // Step 6. Add the IOU input states and settle command to the transaction builder.
            for (StateAndRef validInputStateToSettle : validInputStatesToSettle) {
                Command<RecordedTradeContract.Commands.NetTrades> command = new Command<>(
                        new RecordedTradeContract.Commands.NetTrades(),
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
                RecordedTradeState inputStateToSettle = (RecordedTradeState) ((StateAndRef) stateToSettle).getState().getData();

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

    @CordaSerializable
    public static class NetSpendHolder {
        private final Currency currencyA;
        private final Long amountA;
        private final Currency currencyB;
        private final Long amountB;
        private final Party myself;
        private final Party netAgainstParty;

        @ConstructorForDeserialization
        public NetSpendHolder(Currency currencyA, Long amountA, Currency currencyB, Long amountB, Party myself, Party netAgainstParty) {
            this.currencyA = currencyA;
            this.amountA = amountA;
            this.currencyB = currencyB;
            this.amountB = amountB;
            this.myself = myself;
            this.netAgainstParty = netAgainstParty;
        }

        public Currency getCurrencyA() {
            return currencyA;
        }

        public Long getAmountA() {
            return amountA;
        }

        public Currency getCurrencyB() {
            return currencyB;
        }

        public Long getAmountB() {
            return amountB;
        }

        public Party getMyself() {
            return myself;
        }

        public Party getNetAgainstParty() {
            return netAgainstParty;
        }
    }

    /**
     * This is the flows which signs IOU issuances.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(NetTradesFlow.InitiatorFlow.class)
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

            UntrustworthyData<NetSpendHolder> spendHolderUntrustworthyData = flowSession.receive(NetSpendHolder.class);
            NetSpendHolder spendHolder = spendHolderUntrustworthyData.unwrap(data -> {return data;});
            System.out.println("Details " +
                    spendHolder.getCurrencyA() + " :: " + spendHolder.getAmountA() + " :: " +
                    spendHolder.getCurrencyB() + " :: " + spendHolder.getAmountB() + " :: " +
                    spendHolder.getMyself() + " :: " + spendHolder.getNetAgainstParty());
            if (spendHolder.getAmountA() < 0) {
                // We have to spend the money
                CashSpendHolder currencyACashCommands = CashSpendUtils.generateCashCommands(getServiceHub(),
                            spendHolder.getCurrencyA(),
                            new Amount<>(-spendHolder.getAmountA(), spendHolder.getCurrencyA()),
                            spendHolder.getNetAgainstParty(),
                            spendHolder.getMyself());
                flowSession.send(currencyACashCommands);

                subFlow(new SendStateAndRefFlow(flowSession,
                        Arrays.asList(currencyACashCommands.getInputStateAndRef())));
            }
            if (spendHolder.getAmountB() < 0) {
                // We have to spend the money
                CashSpendHolder currencyBCashCommands = CashSpendUtils.generateCashCommands(getServiceHub(),
                        spendHolder.getCurrencyB(),
                        new Amount<>(-spendHolder.getAmountB(), spendHolder.getCurrencyB()),
                        spendHolder.getNetAgainstParty(),
                        spendHolder.getMyself());
                flowSession.send(currencyBCashCommands);

                subFlow(new SendStateAndRefFlow(flowSession,
                        Arrays.asList(currencyBCashCommands.getInputStateAndRef())));
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
