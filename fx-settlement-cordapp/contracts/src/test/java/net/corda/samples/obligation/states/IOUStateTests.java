package net.corda.samples.obligation.states;


import net.corda.finance.*;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import net.corda.samples.obligation.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class IOUStateTests {

    @Test
    public void hasIOUAmountFieldOfCorrectType() throws NoSuchFieldException {
        Field tradedAssetAmount = IOUState.class.getDeclaredField("tradedAssetAmount");
        assertTrue(tradedAssetAmount.getType().isAssignableFrom(Amount.class));

        Field counterAssetAmount = IOUState.class.getDeclaredField("counterAssetAmount");
        assertTrue(counterAssetAmount.getType().isAssignableFrom(Amount.class));
    }

    @Test
    public void hasPartyFieldOfCorrectType() throws NoSuchFieldException {
        Field tradingParty = IOUState.class.getDeclaredField("tradingParty");
        assertTrue(tradingParty.getType().isAssignableFrom(Party.class));

        Field counterParty = IOUState.class.getDeclaredField("counterParty");
        assertTrue(counterParty.getType().isAssignableFrom(Party.class));
    }

    @Test
    public void hasCurrencyFieldOfCorrectType() throws NoSuchFieldException {
        Field tradedAssetType = IOUState.class.getDeclaredField("tradedAssetType");
        assertTrue(tradedAssetType.getType().isAssignableFrom(Currency.class));

        Field counterAssetType = IOUState.class.getDeclaredField("counterAssetType");
        assertTrue(counterAssetType.getType().isAssignableFrom(Currency.class));
    }

    @Test
    public void hasDateFieldOfCorrectType() throws NoSuchFieldException {
        Field valueDate = IOUState.class.getDeclaredField("valueDate");
        assertTrue(valueDate.getType().isAssignableFrom(Date.class));

        Field tradeTime = IOUState.class.getDeclaredField("tradeTime");
        assertTrue(tradeTime.getType().isAssignableFrom(Date.class));
    }


    @Test
    public void partiesAreParticipants() {
        IOUState iouState = new IOUState(new Date(), new Date(), new Amount(0, Currency.getInstance("USD")),
                Currency.getInstance("USD"), TestUtils.ALICE.getParty(),
                new Amount(0, Currency.getInstance("EUR")),
                Currency.getInstance("EUR"), TestUtils.BOB.getParty(), IOUState.TradeStatus.NEW);

        assertNotEquals(iouState.getParticipants().indexOf(TestUtils.BOB.getParty()), -1);
        assertNotEquals(iouState.getParticipants().indexOf(TestUtils.ALICE.getParty()), -1);
    }

    @Test
    public void isLinearState() {
        assert(LinearState.class.isAssignableFrom(IOUState.class));
    }

    @Test
    public void hasLinearIdFieldOfCorrectType() throws NoSuchFieldException {
        // Does the linearId field exist?
        Field linearIdField = IOUState.class.getDeclaredField("linearId");

        // Is the linearId field of the correct type?
        assertTrue(linearIdField.getType().isAssignableFrom(UniqueIdentifier.class));
    }

    @Test
    public void checkIOUStateParameterOrdering() throws NoSuchFieldException {

        List<Field> fields = Arrays.asList(IOUState.class.getDeclaredFields());

        int tradeTimeIdx = fields.indexOf(IOUState.class.getDeclaredField("tradeTime"));
        int valueDateIdx = fields.indexOf(IOUState.class.getDeclaredField("valueDate"));
        int tradingPartyIdx = fields.indexOf(IOUState.class.getDeclaredField("tradingParty"));
        int counterPartyIdx = fields.indexOf(IOUState.class.getDeclaredField("counterParty"));
        int tradedAssetAmountIdx = fields.indexOf(IOUState.class.getDeclaredField("tradedAssetAmount"));
        int counterAssetAmountIdx = fields.indexOf(IOUState.class.getDeclaredField("counterAssetAmount"));
        int tradedAssetTypeIdx = fields.indexOf(IOUState.class.getDeclaredField("tradedAssetType"));
        int counterAssetTypeIdx = fields.indexOf(IOUState.class.getDeclaredField("counterAssetType"));

        assertTrue(tradeTimeIdx < valueDateIdx);
        assertTrue(valueDateIdx < tradingPartyIdx);
        assertTrue(tradingPartyIdx < counterPartyIdx);
        assertTrue(counterPartyIdx < tradedAssetAmountIdx);

        assertTrue(tradedAssetAmountIdx < counterAssetAmountIdx);
        assertTrue(counterAssetAmountIdx < tradedAssetTypeIdx);
        assertTrue(tradedAssetTypeIdx < counterAssetTypeIdx);
    }

//    @Test
//    public void checkPayHelperMethod() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        assertEquals(Currencies.DOLLARS(5), iou.pay(Currencies.DOLLARS(5)).getPaid());
//        assertEquals(Currencies.DOLLARS(3), iou.pay(Currencies.DOLLARS(1)).pay(Currencies.DOLLARS(2)).getPaid());
//        assertEquals(Currencies.DOLLARS(10), iou.pay(Currencies.DOLLARS(5)).pay(Currencies.DOLLARS(3)).pay(Currencies.DOLLARS(2)).getPaid());
//    }

//    /**
//     * Task 11.
//     * TODO: Add a helper method called [withNewLender] that can be called from an {@link }IOUState} to change the IOU's lender.
//     * - This will also utilize the copy constructor.
//     */
//    @Test
//    public void checkWithNewLenderHelperMethod() {
//        IOUState iou = new IOUState(Currencies.DOLLARS(10), TestUtils.ALICE.getParty(), TestUtils.BOB.getParty());
//        Assert.assertEquals(TestUtils.MINICORP.getParty(), iou.withNewLender(TestUtils.MINICORP.getParty()).getLender());
//        Assert.assertEquals(TestUtils.MEGACORP.getParty(), iou.withNewLender(TestUtils.MEGACORP.getParty()).getLender());
//    }
//
//    /**
//     * Task 12.
//     * TODO: Ensure constructors are overloaded correctly.
//     * This test serves as a sanity check that the two constructors have been implemented properly. If it fails, refer to the instructions of Tasks 8 and 10.
//     */
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
