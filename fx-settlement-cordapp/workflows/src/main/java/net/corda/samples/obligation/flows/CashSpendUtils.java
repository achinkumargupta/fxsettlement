package net.corda.samples.obligation.flows;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.finance.contracts.asset.Cash;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

public class CashSpendUtils {
    public static CashSpendHolder generateCashCommands (
            ServiceHub hub,
            Currency assetType,
            Amount<Currency> assetAmount,
            Party tradingParty,
            Party counterParty) {
        Vault.Page results = hub.getVaultService().queryBy(Cash.State.class);
        List<StateAndRef> inputStateAndRefToSettle = results.getStates();
        StateAndRef inputRef = null;
        for (StateAndRef srf : inputStateAndRefToSettle) {
            if (((Cash.State) srf.getState().getData()).getAmount().getToken()
                    .getProduct().equals(assetType)) {
                inputRef = srf;
            }
        }
        if (inputRef == null) {
            return new CashSpendHolder("Unable to find the any Cash for " + assetType +
                    " with party " + tradingParty.getName().getOrganisation());
        }

        Cash.State tokenCash = (Cash.State) inputRef.getState().getData();
        if (tokenCash.getAmount().getQuantity() < assetAmount.getQuantity()) {
            return new CashSpendHolder("Not enough cash in " + assetType +
                    " with party " + tradingParty.getName().getOrganisation() + " to settle with the trade.");
        }

        Issued<Currency> issuedCurrency = new Issued<Currency>(tokenCash.getAmount().getToken().getIssuer(),
                assetType);
        Amount<Issued<Currency>> tradedAssetIssuedAmount = new Amount<Issued<Currency>>(assetAmount.getQuantity(),
                issuedCurrency);

        Cash.State tokenCashAfterTransfer = tokenCash.copy(tokenCash.getAmount().minus(tradedAssetIssuedAmount),
                tradingParty);
        Cash.State tokenCashAfterTransferForCounterParty = tokenCash.copy(tradedAssetIssuedAmount,
                counterParty);

        return new CashSpendHolder(inputRef,
                Arrays.asList(tokenCashAfterTransfer, tokenCashAfterTransferForCounterParty),
                new Cash.Commands.Move(),
                Arrays.asList(tradingParty.getOwningKey(), counterParty.getOwningKey()));
    }
}
