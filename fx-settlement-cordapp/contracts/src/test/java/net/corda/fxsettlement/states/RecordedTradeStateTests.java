package net.corda.fxsettlement.states;


import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import java.lang.reflect.Field;

import net.corda.fxsettlement.TestUtils;
import org.junit.Test;
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
//
//    @Test
//    public void checkPayHelperMethod() {
//        IOUState iou = new IOUState(new Date(), new Date(), new Amount(10000, Currency.getInstance("USD")),
//                Currency.getInstance("USD"), TestUtils.ALICE.getParty(),
//                new Amount(100, Currency.getInstance("EUR")),
//                Currency.getInstance("EUR"), TestUtils.BOB.getParty(), IOUState.TradeStatus.NEW);
//
//        assertEquals(Currencies.DOLLARS(95), iou.pay(Currencies.DOLLARS(500)).getTradedAssetAmount());
//        assertEquals(Currencies.DOLLARS(97), iou.pay(Currencies.DOLLARS(100)).pay(Currencies.DOLLARS(200)).getTradedAssetAmount());
//        assertEquals(Currencies.DOLLARS(90), iou.pay(Currencies.DOLLARS(500)).pay(Currencies.DOLLARS(300)).pay(Currencies.DOLLARS(200)).getTradedAssetAmount());
//    }

//    @Test
//    public void checkWithNewLenderHelperMethod() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Assert.assertEquals(TestUtils.MINICORP.getParty(), iou.withNewLender(TestUtils.MINICORP.getParty()).getLender());
//        Assert.assertEquals(TestUtils.MEGACORP.getParty(), iou.withNewLender(TestUtils.MEGACORP.getParty()).getLender());
//    }
//
//    @Test
//    public void correctConstructorsExist() {
//        // Public constructor for new states
//        try {
//            Constructor<IOUState> contructor = IOUState.class.getConstructor(Amount.class, Party.class, Party.class);
//        } catch( NoSuchMethodException nsme ) {
//            fail("The correct public constructor does not exist!");
//        }
//        // Private constructor for updating states
//        try {
//            Constructor<IOUState> contructor = IOUState.class.getDeclaredConstructor(Amount.class, Party.class, Party.class, Amount.class, UniqueIdentifier.class);
//        } catch( NoSuchMethodException nsme ) {
//            fail("The correct private copy constructor does not exist!");
//        }
//    }
}
