package net.corda.fxsettlement.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.util.*;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.fxsettlement.contracts.RecordedTradeContract;
import net.corda.core.serialization.CordaSerializable;
import java.text.SimpleDateFormat;
/**
 * The State object, with the following properties:
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */

@BelongsToContract(RecordedTradeContract.class)
public class RecordedTradeState implements ContractState, LinearState {
    public final Date tradeTime;
    public final Date valueDate;
    public final Party tradingParty;
    public final Party counterParty;
    public final Amount<Currency> tradedAssetAmount;
    public final Amount<Currency> counterAssetAmount;
    public final Currency tradedAssetType;
    public final Currency counterAssetType;

    public final TradeStatus tradeStatus;

    private final UniqueIdentifier linearId;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    @CordaSerializable
    public enum TradeStatus {
        NEW,
        CONFIRMED,
        CANCELLED,
        SETTLED;
    }

    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    private RecordedTradeState(Date tradeTime,
                               Date valueDate,
                               Amount<Currency> tradedAssetAmount,
                               Currency tradedAssetType,
                               Party tradingParty,
                               Amount<Currency> counterAssetAmount,
                               Currency counterAssetType,
                               Party counterParty,
                               TradeStatus tradeStatus,
                               UniqueIdentifier linearId){
        this.tradeTime = tradeTime;
        this.valueDate = valueDate;
        this.tradedAssetAmount = tradedAssetAmount;
        this.tradedAssetType = tradedAssetType;
        this.tradingParty = tradingParty;
        this.counterAssetAmount = counterAssetAmount;
        this.counterAssetType = counterAssetType;
        this.counterParty = counterParty;
        this.tradeStatus = tradeStatus;
        this.linearId = linearId;
    }

    public RecordedTradeState(Date tradeTime,
                              Date valueDate,
                              Amount<Currency> tradedAssetAmount,
                              Currency tradedAssetType,
                              Party tradingParty,
                              Amount<Currency> counterAssetAmount,
                              Currency counterAssetType,
                              Party counterParty,
                              TradeStatus tradeStatus){
        this(tradeTime, valueDate, tradedAssetAmount, tradedAssetType, tradingParty, counterAssetAmount, counterAssetType,
                counterParty, tradeStatus, new UniqueIdentifier());
    }


    public Date getTradeTime() {
        return tradeTime;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public Party getTradingParty() {
        return tradingParty;
    }

    public Party getCounterParty() {
        return counterParty;
    }

    public Amount<Currency> getTradedAssetAmount() {
        return tradedAssetAmount;
    }

    public Amount<Currency> getCounterAssetAmount() {
        return counterAssetAmount;
    }

    public Currency getTradedAssetType() {
        return tradedAssetType;
    }

    public Currency getCounterAssetType() {
        return counterAssetType;
    }

    public RecordedTradeState.TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /**
     *  This method will return a list of the nodes which can "use" this states in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(tradingParty, counterParty);
    }

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current states with a newly specified lender. For use when transferring.
     * - [copy] creates a copy of the states using the internal copy constructor ensuring the LinearId is preserved.
     */
    public RecordedTradeState pay(Amount<Currency> amountToPay) {
        Amount<Currency> newAmountPaid = this.tradedAssetAmount.minus(amountToPay);
        return new RecordedTradeState(tradeTime, valueDate, newAmountPaid, tradedAssetType, tradingParty, counterAssetAmount,
                counterAssetType, counterParty, tradeStatus, this.getLinearId());
    }

    public RecordedTradeState copy(
            Date tradeTime,
            Date valueDate,
            Amount<Currency> tradedAssetAmount,
            Currency tradedAssetType,
            Party tradingParty,
            Amount<Currency> counterAssetAmount,
            Currency counterAssetType,
            Party counterParty,
            TradeStatus tradeStatus) {
        return new RecordedTradeState(tradeTime, valueDate, tradedAssetAmount,
                tradedAssetType, tradingParty, counterAssetAmount,
                counterAssetType, counterParty, tradeStatus, this.getLinearId());
    }

}
