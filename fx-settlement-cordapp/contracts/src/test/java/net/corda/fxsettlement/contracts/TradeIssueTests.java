package net.corda.fxsettlement.contracts;


import net.corda.core.contracts.*;
import net.corda.fxsettlement.TestUtils;

import static net.corda.testing.node.NodeTestUtils.ledger;

import net.corda.fxsettlement.states.RecordedTradeState;
import net.corda.testing.node.MockServices;
import org.junit.*;

import java.util.Arrays;
import java.util.Currency;
import java.util.Date;

public class TradeIssueTests {
    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.fxsettlement.states", "net.corda.fxsettlement.contracts")
    );

    @Test
    public void mustIncludeIssueCommand() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(),
                        TestUtils.BOB.getPublicKey()),
                        new Commands.DummyCommand()); // Wrong type.
                return tx.failsWith("Invalid command - Trade Contract verification failed");
            });
            l.transaction(tx -> {
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue()); // Correct type.
                return tx.verifies();
            });
            return null;
        });
    }


    @Test
    public void issueTransactionMustHaveNoInputs() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("No inputs should be consumed when recording a trade.");
            });
            l.transaction(tx -> {
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                return tx.verifies(); // As there are no input sates
            });
            return null;
        });
    }

    @Test
    public void issueTransactionMustHaveOneOutput() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou); // Two outputs fails.
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Only one output states should be created when recording a trade.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou); // One output passes.
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void cannotCreateZeroValueIOUs() {
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID,
                        new RecordedTradeState(new Date(),
                        new Date(),
                        new Amount(0, Currency.getInstance("USD")),
                        Currency.getInstance("USD"),
                        TestUtils.ALICE.getParty(),
                        new Amount(100, Currency.getInstance("EUR")),
                        Currency.getInstance("EUR"),
                        TestUtils.BOB.getParty(),
                        RecordedTradeState.TradeStatus.NEW)); // Zero amount fails.
                return tx.failsWith("A newly issued trade must have a positive traded asset amount.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID,
                        new RecordedTradeState(new Date(),
                                new Date(),
                                new Amount(10000, Currency.getInstance("USD")),
                                Currency.getInstance("USD"),
                                TestUtils.ALICE.getParty(),
                                new Amount(100, Currency.getInstance("EUR")),
                                Currency.getInstance("EUR"),
                                TestUtils.BOB.getParty(),
                                RecordedTradeState.TradeStatus.NEW));
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void bothPartiesCannotBeTheSame() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.ALICE.getParty(),
                RecordedTradeState.TradeStatus.NEW);

       ledger(ledgerServices, l-> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties cannot have the same identity.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()),
                        new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID,
                        new RecordedTradeState(new Date(),
                                new Date(),
                                new Amount(10000, Currency.getInstance("USD")),
                                Currency.getInstance("USD"),
                                TestUtils.ALICE.getParty(),
                                new Amount(100, Currency.getInstance("EUR")),
                                Currency.getInstance("EUR"),
                                TestUtils.BOB.getParty(),
                                RecordedTradeState.TradeStatus.NEW));
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void lenderAndBorrowerMustSignIssueTransaction() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        ledger(ledgerServices, l->{
            l.transaction(tx-> {
                tx.command(TestUtils.DUMMY.getPublicKey(),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(TestUtils.ALICE.getPublicKey(),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(TestUtils.BOB.getPublicKey(),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.BOB.getPublicKey(), TestUtils.BOB.getPublicKey()),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.BOB.getPublicKey(), TestUtils.MINICORP.getPublicKey(), TestUtils.ALICE.getPublicKey()),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.failsWith("Both parties together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.BOB.getPublicKey(), TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()),  new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.verifies();
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Issue());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                return tx.verifies();
            });
            return null;
        });
    }
}
