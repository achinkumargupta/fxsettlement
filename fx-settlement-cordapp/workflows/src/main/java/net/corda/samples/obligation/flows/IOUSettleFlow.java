package net.corda.samples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.lang.IllegalArgumentException;
import java.security.PublicKey;
import java.util.*;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

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

        private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        public InitiatorFlow(UniqueIdentifier stateLinearId) {
            this.stateLinearId = stateLinearId;
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            List<UUID> listOfLinearIds = Arrays.asList(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToSettle = (IOUState) ((StateAndRef) results.getStates().get(0)).getState().getData();
            Date valueDate = inputStateToSettle.valueDate;
            Party counterparty = inputStateToSettle.counterParty;
            Party tradingParty = inputStateToSettle.tradingParty;
            Amount<Currency> tradedAssetAmount = inputStateToSettle.tradedAssetAmount;
            Amount<Currency> counterAssetAmount = inputStateToSettle.counterAssetAmount;
            Currency tradedAssetType = inputStateToSettle.tradedAssetType;
            Currency counterAssetType = inputStateToSettle.counterAssetType;
            TradeStatus tradeStatus = inputStateToSettle.tradeStatus;
            Party notary = inputStateAndRefToSettle.getState().getNotary();
            TransactionBuilder tb = new TransactionBuilder(notary);
            generateCashInstructions(inputStateToSettle,tb);

            // Step 2. Create a transaction builder.
            // Obtain a reference to a notary we wish to use.



            System.out.println(df.format(valueDate) + ": :: " + df.format(new Date()));
            if (!df.format(valueDate).equals(df.format(new Date()))) {
                throw new IllegalArgumentException("Can only settle trades with Value date as today.");
            }

            // Step 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) tradedAssetAmount.getToken());
            if (cashBalance.getQuantity() < tradedAssetAmount.getQuantity()) {
                throw new IllegalArgumentException("Not enough cash in " + tradedAssetType + " to settle with the trade.");
            }

            // Step 6. Add the IOU input states and settle command to the transaction builder.
            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    Arrays.asList(counterparty.getOwningKey(),getOurIdentity().getOwningKey())
            );
            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // Step 8. Verify and sign the transaction.
            tb.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(tb, Arrays.asList(getOurIdentity().getOwningKey()));

            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            FlowSession session = initiateFlow(counterparty);
            new IdentitySyncFlow.Send(session, ptx.getTx());

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, session));
        }

        private void generateCashInstructions(IOUState inputStateToSettle, TransactionBuilder tb) {
            Vault.Page results = getServiceHub().getVaultService().queryBy(Cash.State.class);
            List<StateAndRef> inputStateAndRefToSettle = results.getStates();
            StateAndRef inputRef = null;
            for (StateAndRef srf : inputStateAndRefToSettle) {
                if (((Cash.State) srf.getState().getData()).getAmount().getToken()
                        .getProduct().equals(inputStateToSettle.getTradedAssetType())) {
                    inputRef = srf;
                }
            }

            if (inputRef == null) {
                throw new RuntimeException("Unable to find the Cash State associated with Asset Type :" + inputStateToSettle.getTradedAssetType());
            }
            Cash.State tokenCash = (Cash.State) inputRef.getState().getData();

            if (tokenCash.getAmount().getQuantity() < inputStateToSettle.getTradedAssetAmount().getQuantity()) {
                throw new IllegalArgumentException("Not enough cash in " + inputStateToSettle.getTradedAssetType() +
                        " to settle with the trade.");
            }

            Issued<Currency> issuedCurrency = new Issued<Currency>(tokenCash.getAmount().getToken().getIssuer(),
                    inputStateToSettle.getTradedAssetType());
            Amount<Issued<Currency>> tradedAssetAmount = new Amount<Issued<Currency>>(inputStateToSettle.getTradedAssetAmount().getQuantity(),
                    issuedCurrency);

            Cash.State tokenCashAfterTransfer = tokenCash.copy(tokenCash.getAmount().minus(tradedAssetAmount),
                    inputStateToSettle.getTradingParty());
            Cash.State tokenCashForPartyBAfterTransfer = tokenCash.copy(tradedAssetAmount,
                    inputStateToSettle.getCounterParty());

            tb.addInputState(inputRef);
            tb.addOutputState(tokenCashAfterTransfer);
            tb.addOutputState(tokenCashForPartyBAfterTransfer);

            tb.addCommand(new Cash.Commands.Move(),
                    Arrays.asList(getOurIdentity().getOwningKey(),
                    inputStateToSettle.getCounterParty().getOwningKey()));
        }

        private class CounterpartySpendHolder {
            private final StateAndRef inputStateAndRef;
            private final List<Cash.State> outputStates;
            private final CommandData command;

            public CounterpartySpendHolder(StateAndRef inputStateAndRef, List<Cash.State> outputStates, CommandData command) {
                this.inputStateAndRef = inputStateAndRef;
                this.outputStates = outputStates;
                this.command = command;
            }

            public StateAndRef getInputStateAndRef() {
                return inputStateAndRef;
            }

            public List<Cash.State> getOutputStates() {
                return outputStates;
            }

            public CommandData getCommand() {
                return command;
            }
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