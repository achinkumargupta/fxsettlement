package net.corda.fxsettlement.states;


import net.corda.core.contracts.*;
import net.corda.core.identity.Party;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import net.corda.finance.Currencies;
import net.corda.fxsettlement.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.Part;
import java.util.*;
import static org.junit.Assert.*;

public class RecordedTradeStateTests {

    @Test
    public void hasIOUAmountFieldOfCorrectType() throws NoSuchFieldException {
        Field tradedAssetAmount = RecordedTradeState.class.getDeclaredField("tradedAssetAmount");
        assertTrue(tradedAssetAmount.getType().isAssignableFrom(Amount.class));

        Field counterAssetAmount = RecordedTradeState.class.getDeclaredField("counterAssetAmount");
        assertTrue(counterAssetAmount.getType().isAssignableFrom(Amount.class));
    }

    @Test
    public void hasPartyFieldOfCorrectType() throws NoSuchFieldException {
        Field tradingParty = RecordedTradeState.class.getDeclaredField("tradingParty");
        assertTrue(tradingParty.getType().isAssignableFrom(Party.class));

        Field counterParty = RecordedTradeState.class.getDeclaredField("counterParty");
        assertTrue(counterParty.getType().isAssignableFrom(Party.class));
    }

    @Test
    public void hasCurrencyFieldOfCorrectType() throws NoSuchFieldException {
        Field tradedAssetType = RecordedTradeState.class.getDeclaredField("tradedAssetType");
        assertTrue(tradedAssetType.getType().isAssignableFrom(Currency.class));

        Field counterAssetType = RecordedTradeState.class.getDeclaredField("counterAssetType");
        assertTrue(counterAssetType.getType().isAssignableFrom(Currency.class));
    }

    @Test
    public void hasDateFieldOfCorrectType() throws NoSuchFieldException {
        Field valueDate = RecordedTradeState.class.getDeclaredField("valueDate");
        assertTrue(valueDate.getType().isAssignableFrom(Date.class));

        Field tradeTime = RecordedTradeState.class.getDeclaredField("tradeTime");
        assertTrue(tradeTime.getType().isAssignableFrom(Date.class));
    }


    @Test
    public void partiesAreParticipants() {
        RecordedTradeState recordedTradeState = new RecordedTradeState(new Date(), new Date(), new Amount(0, Currency.getInstance("USD")),
                Currency.getInstance("USD"), TestUtils.ALICE.getParty(),
                new Amount(0, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"), TestUtils.BOB.getParty(), RecordedTradeState.TradeStatus.NEW);

        assertNotEquals(recordedTradeState.getParticipants().indexOf(TestUtils.BOB.getParty()), -1);
        assertNotEquals(recordedTradeState.getParticipants().indexOf(TestUtils.ALICE.getParty()), -1);
    }

    @Test
    public void isLinearState() {
        assert(LinearState.class.isAssignableFrom(RecordedTradeState.class));
    }

    @Test
    public void hasLinearIdFieldOfCorrectType() throws NoSuchFieldException {
        // Does the linearId field exist?
        Field linearIdField = RecordedTradeState.class.getDeclaredField("linearId");

        // Is the linearId field of the correct type?
        assertTrue(linearIdField.getType().isAssignableFrom(UniqueIdentifier.class));
    }

    @Test
    public void checkIOUStateParameterOrdering() throws NoSuchFieldException {

        List<Field> fields = Arrays.asList(RecordedTradeState.class.getDeclaredFields());

        int tradeTimeIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("tradeTime"));
        int valueDateIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("valueDate"));
        int tradingPartyIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("tradingParty"));
        int counterPartyIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("counterParty"));
        int tradedAssetAmountIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("tradedAssetAmount"));
        int counterAssetAmountIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("counterAssetAmount"));
        int tradedAssetTypeIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("tradedAssetType"));
        int counterAssetTypeIdx = fields.indexOf(RecordedTradeState.class.getDeclaredField("counterAssetType"));

        assertTrue(tradeTimeIdx < valueDateIdx);
        assertTrue(valueDateIdx < tradingPartyIdx);
        assertTrue(tradingPartyIdx < counterPartyIdx);
        assertTrue(counterPartyIdx < tradedAssetAmountIdx);

        assertTrue(tradedAssetAmountIdx < counterAssetAmountIdx);
        assertTrue(counterAssetAmountIdx < tradedAssetTypeIdx);
        assertTrue(tradedAssetTypeIdx < counterAssetTypeIdx);
    }

    @Test
    public void checkPayHelperMethod() {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        assertEquals(new Amount(9500, Currency.getInstance("USD")),
                iou.pay(new Amount(500, Currency.getInstance("USD"))).getTradedAssetAmount());
    }

    @Test
    public void correctConstructorsExist() {
        try {
            Constructor<RecordedTradeState> contructor =
                    RecordedTradeState.class.getConstructor(Date.class, Date.class, Amount.class,
                            Currency.class, Party.class, Amount.class, Currency.class, Party.class,
                            RecordedTradeState.TradeStatus.class);
        } catch( NoSuchMethodException nsme ) {
            fail("The correct public constructor does not exist!");
        }
    }

    @Test
    public void copyFunctionWorks() throws InterruptedException {
        RecordedTradeState iou = new RecordedTradeState(new Date(),
                new Date(),
                new Amount(10000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.ALICE.getParty(),
                new Amount(100, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.BOB.getParty(),
                RecordedTradeState.TradeStatus.NEW);

        // Just to let the time move
        Thread.sleep(1000);

        RecordedTradeState iouCopy = iou.copy(new Date(),
                new Date(),
                new Amount(5000, Currency.getInstance("USD")),
                Currency.getInstance("USD"),
                TestUtils.BOB.getParty(),
                new Amount(5000, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"),
                TestUtils.ALICE.getParty(),
                RecordedTradeState.TradeStatus.CANCELLED);


        assertEquals(iou.getLinearId(), iouCopy.getLinearId());
        assertNotEquals(iou.getTradeTime(), iouCopy.getTradeTime());
        assertNotEquals(iou.getValueDate(), iouCopy.getValueDate());
        assertNotEquals(iou.getTradedAssetAmount(), iouCopy.getTradedAssetAmount());
        assertNotEquals(iou.getCounterAssetAmount(), iouCopy.getCounterAssetAmount());
        assertEquals(iou.getTradedAssetType(), iouCopy.getTradedAssetType());
        assertEquals(iou.getCounterAssetType(), iouCopy.getCounterAssetType());
        assertNotEquals(iou.getTradingParty(), iouCopy.getTradingParty());
        assertNotEquals(iou.getCounterParty(), iouCopy.getCounterParty());
        assertNotEquals(iou.getTradeStatus(), iouCopy.getTradeStatus());
    }
}
