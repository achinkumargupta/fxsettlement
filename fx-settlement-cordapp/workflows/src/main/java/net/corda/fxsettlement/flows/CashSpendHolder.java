package net.corda.fxsettlement.flows;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.List;

@CordaSerializable
public class CashSpendHolder {
    private final StateAndRef<Cash.State> inputStateAndRef;
    private final List<Cash.State> outputStates;
    private final CommandData command;
    private final List<PublicKey> keys;
    private final String error;

    public CashSpendHolder(String error) {
        this(null, null, null, null, error);
    }

    public CashSpendHolder(StateAndRef inputStateAndRef, List<Cash.State> outputStates, CommandData command, List<PublicKey> keys) {
        this(inputStateAndRef, outputStates, command, keys, null);
    }

    @ConstructorForDeserialization
    public CashSpendHolder(StateAndRef inputStateAndRef, List<Cash.State> outputStates, CommandData command, List<PublicKey> keys, String error) {
        this.inputStateAndRef = inputStateAndRef;
        this.outputStates = outputStates;
        this.command = command;
        this.keys = keys;
        this.error = error;
    }

    public StateAndRef<Cash.State> getInputStateAndRef() {
        return inputStateAndRef;
    }

    public List<Cash.State> getOutputStates() {
        return outputStates;
    }

    public CommandData getCommand() {
        return command;
    }

    public List<PublicKey> getKeys() {
        return keys;
    }

    public String getError() {
        return error;
    }
}