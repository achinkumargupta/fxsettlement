package net.corda.fxsettlement.contracts;


import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.fxsettlement.TestUtils;
import net.corda.fxsettlement.states.RecordedTradeState;
import net.corda.testing.node.MockServices;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Currency;
import java.util.Date;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class TradeSettleTests {

    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.fxsettlement.states", "net.corda.fxsettlement.contracts",
                    "net.corda.finance.contracts.asset")
    );

    private Cash.State createCashState(AbstractParty owner, Amount<Currency> amount) {
        OpaqueBytes defaultBytes = new OpaqueBytes(new byte[1]);
        PartyAndReference partyAndReference = new PartyAndReference(owner, defaultBytes);
        return new Cash.State(partyAndReference, amount, owner);
    }

    @Test
    public void mustIncludeSettleCommand() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);
        Cash.State inputCash = createCashState(TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
        OwnableState outputCash = inputCash.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState();

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                return tx.failsWith("Contract Verification Failed");
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(TestUtils.BOB.getPublicKey(), new Commands.DummyCommand());
                return tx.failsWith("Contract verification failed");
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash);
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                return tx.verifies();
            });
            return null;
        });
    }


    @Test
    public void mustBeOneGroupOfIOUs() {
        RecordedTradeState iouONE = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        RecordedTradeState iouTWO = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(100000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(1000, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);
        
        Cash.State inputCash = createCashState(TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
        CommandAndState outputCash = inputCash.withNewOwner(TestUtils.ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iouONE);
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iouTWO);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouONE.pay(Currencies.DOLLARS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash.getOwnableState());
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.failsWith("List has more than one element.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iouONE);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouONE.pay(Currencies.DOLLARS(5)));
                tx.input(Cash.class.getName(), inputCash);
                tx.output(Cash.class.getName(), outputCash.getOwnableState());
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void mustHaveOneInputIOU() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(1000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        RecordedTradeState iouOne = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(1000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(1000, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        Cash.State tenPounds = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(10));
        Cash.State fivePounds = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(5));

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.failsWith("There must be one input IOU.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), fivePounds);
                tx.output(Cash.class.getName(), fivePounds.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iouOne);
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.input(Cash.class.getName(), tenPounds);
                tx.output(Cash.class.getName(), tenPounds.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.verifies();
                return null;
            });
            return  null;
        });

    }


    @Test
    public void mustBeCashOutputStatesPresent() {

        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(1000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);
        Cash.State cash = createCashState(TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
        CommandAndState cashPayment = cash.withNewOwner(TestUtils.ALICE.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.failsWith("There must be output cash.");
                return null;
            });
            l.transaction(tx -> {
                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
                tx.input(Cash.class.getName(), cash);
                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.BOB.getPublicKey()), new RecordedTradeContract.Commands.Settle());
                tx.verifies();
                return null;
            });
            return null;
        });

    }

//    @Test
//    public void mustBeCashOutputStatesWithRecipientAsOwner() {
//        RecordedTradeState iou = new RecordedTradeState(new Date(),
//                new Date(),
//                new Amount(1000, Currency.getInstance("USD")),
//                Currency.getInstance("USD"),
//                TestUtils.ALICE.getParty(),
//                new Amount(100, Currency.getInstance("EUR")),
//                Currency.getInstance("EUR"),
//                TestUtils.BOB.getParty(),
//                RecordedTradeState.TradeStatus.NEW);
//
//        Cash.State cash = createCashState(TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
//        CommandAndState invalidCashPayment = cash.withNewOwner(TestUtils.CHARLIE.getParty());
//        CommandAndState validCashPayment = cash.withNewOwner(TestUtils.ALICE.getParty());
//
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), cash);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.output(Cash.class.getName(), invalidCashPayment.getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), invalidCashPayment.getCommand());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("There must be output cash paid to the counterparty.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), cash);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.output(Cash.class.getName(), validCashPayment.getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), validCashPayment.getCommand());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            return null;
//        });
//
//    }


//    @Test
//    public void cashSettlementAmountMustBeLessThanRemainingIOUAmount() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Cash.State elevenDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(11));
//        Cash.State tenDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(10));
//        Cash.State fiveDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
//
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), elevenDollars);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(11)));
//                tx.output(Cash.class.getName(), elevenDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("The amount settled cannot be more than the amount outstanding.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), tenDollars);
//                tx.output(Cash.class.getName(), tenDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            return null;
//        });
//    }
//

//    @Test
//    public void cashSettlementMustBeInTheCorrectCurrency() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Cash.State tenDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(10));
//        Cash.State tenPounds = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(10));
//
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), tenPounds);
//                tx.output(Cash.class.getName(), tenPounds.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("Token mismatch: GBP vs USD");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), tenDollars);
//                tx.output(Cash.class.getName(), tenDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            return null;
//        });
//    }
//
//    @Test
//    public void mustOnlyHaveOutputIOUIfNotFullySettling() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Cash.State tenDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(10));
//        Cash.State fiveDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("There must be one output IOU.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), tenDollars);
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(10)));
//                tx.output(Cash.class.getName(), tenDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("There must be no output IOU as it has been fully settled.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), tenDollars);
//                tx.output(Cash.class.getName(), tenDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//            return null;
//        });
//    }
//
//    @Test
//    public void onlyPaidPropertyMayChange() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Cash.State fiveDollars = createCashState( TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
//
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                IOUState iouCopy = iou.copy(iou.amount, iou.lender, TestUtils.CHARLIE.getParty(), iou.paid).pay(Currencies.DOLLARS(5));
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouCopy);
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("The borrower may not change when settling.");
//                return null;
//            });
//
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                IOUState iouCopy = iou.copy(Currencies.DOLLARS(0), iou.lender, TestUtils.CHARLIE.getParty(), iou.paid).pay(Currencies.DOLLARS(5));
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouCopy);
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("The amount may not change when settling.");
//                return null;
//            });
//
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                IOUState iouCopy = iou.copy(iou.amount, TestUtils.CHARLIE.getParty(), iou.borrower, iou.paid).pay(Currencies.DOLLARS(5));
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouCopy);
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("The lender may not change when settling.");
//                return null;
//            });
//
//            l.transaction(tx -> {
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.input(Cash.class.getName(), fiveDollars);
//                tx.output(Cash.class.getName(), fiveDollars.withNewOwner(TestUtils.ALICE.getParty()).getOwnableState());
//                IOUState iouCopy = iou.copy(iou.amount, iou.lender, iou.borrower, iou.paid).pay(Currencies.DOLLARS(5));
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iouCopy);
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.verifies();
//                return null;
//            });
//
//            return null;
//        });
//
//    }


//    public void mustBeSignedByAllParticipants() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Cash.State cash = createCashState(TestUtils.BOB.getParty(), Currencies.DOLLARS(5));
//        CommandAndState cashPayment = cash.withNewOwner(TestUtils.ALICE.getParty());
//
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(Cash.class.getName(), cash);
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.ALICE.getPublicKey(), TestUtils.CHARLIE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(Cash.class.getName(), cash);
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(TestUtils.BOB.getPublicKey(), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
//                return null;
//            });
//            l.transaction(tx -> {
//                tx.input(Cash.class.getName(), cash);
//                tx.input(RecordedTradeContract.TRADE_CONTRACT_ID, iou);
//                tx.output(Cash.class.getName(), cashPayment.getOwnableState());
//                tx.output(RecordedTradeContract.TRADE_CONTRACT_ID, iou.pay(Currencies.DOLLARS(5)));
//                tx.command(TestUtils.BOB.getPublicKey(), new Cash.Commands.Move());
//                tx.command(Arrays.asList(TestUtils.BOB.getPublicKey(), TestUtils.ALICE.getPublicKey()), new RecordedTradeContract.Commands.Settle());
//                tx.failsWith("Both lender and borrower together only must sign IOU settle transaction.");
//                return null;
//            });
//            return null;
//        });
//
//    }
}
