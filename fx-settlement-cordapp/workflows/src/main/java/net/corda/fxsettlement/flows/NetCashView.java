package net.corda.fxsettlement.flows;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.fxsettlement.states.RecordedTradeState;

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class NetCashView {
    private final Party me;
    private final List<Party> otherParties;
    private final List<Currency> currencies;
    private final List<StateAndRef<RecordedTradeState>> states;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public NetCashView(Party me, List<StateAndRef<RecordedTradeState>> states, List<Party> otherParties, List<Currency> currencies) {
        this.me = me;
        this.states = states;
        this.otherParties = otherParties;
        this.currencies = currencies;
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public class CashViewRecords {
        private final Party me;
        private final Party other;
        private final Amount<Currency> payIn;
        private final Amount<Currency> payOut;

        public CashViewRecords(Party me, Party other, Amount<Currency> payOut, Amount<Currency> payIn) {
            this.me = me;
            this.other = other;
            this.payIn = payIn;
            this.payOut = payOut;
        }

        public Party getMe() {
            return me;
        }

        public Party getOther() {
            return other;
        }

        public Amount<Currency> getPayIn() {
            return payIn;
        }

        public Amount<Currency> getPayOut() {
            return payOut;
        }
    }

    public List<CashViewRecords> getNetCashView() {
        List<CashViewRecords> cashViewRecords = new ArrayList<>();

        for (Party netAgainstParty : otherParties) {
            List<Currency> traversedCurrencies = new ArrayList<>();
            for (Currency currencyA : currencies) {
                for (Currency currencyB : currencies) {
                    if (currencyA.equals(currencyB) || traversedCurrencies.contains(currencyB)) {
                        continue;
                    } else {
                        addNetSpendRecord(netAgainstParty, currencyA, currencyB, cashViewRecords);
                    }
                }
                traversedCurrencies.add(currencyA);
            }
        }
        return cashViewRecords;
    }

    private void addNetSpendRecord(Party netAgainstParty, Currency currencyA,
                                   Currency currencyB, List<CashViewRecords> cashViewRecords) {
        long netSpendForCurrencyA = 0;
        long netSpendForCurrencyB = 0;

        for (Object stateToSettle : states) {
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
            }
        }

        if (netSpendForCurrencyA > 0 && netSpendForCurrencyB < 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount(netSpendForCurrencyA, currencyA),
                    new Amount<>(-netSpendForCurrencyB, currencyB)));
        } else if (netSpendForCurrencyA < 0 && netSpendForCurrencyB > 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount<>(netSpendForCurrencyB, currencyB),
                    new Amount(-netSpendForCurrencyA, currencyA)));
        } else if (netSpendForCurrencyA > 0 && netSpendForCurrencyB > 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount<>(netSpendForCurrencyB, currencyB),
                    null));
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount<>(netSpendForCurrencyA, currencyA),
                    null));
        } else if (netSpendForCurrencyA < 0 && netSpendForCurrencyB < 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, null, new Amount<>(-netSpendForCurrencyB, currencyB)));
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, null, new Amount<>(-netSpendForCurrencyA, currencyA)));
        } else if (netSpendForCurrencyA > 0 && netSpendForCurrencyB == 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount<>(netSpendForCurrencyA, currencyA),
                    null));
        } else if (netSpendForCurrencyA < 0 && netSpendForCurrencyB == 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, null, new Amount<>(-netSpendForCurrencyA, currencyA)));
        } else if (netSpendForCurrencyB > 0 && netSpendForCurrencyA == 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, new Amount<>(netSpendForCurrencyB, currencyB),
                    null));
        } else if (netSpendForCurrencyB < 0 && netSpendForCurrencyA == 0) {
            cashViewRecords.add(new CashViewRecords(me, netAgainstParty, null, new Amount<>(netSpendForCurrencyB, currencyB)));
        }
    }
}
