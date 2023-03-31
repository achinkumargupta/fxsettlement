package net.corda.fxsettlement.contracts;

import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.serialization.SerializationWhitelist;
import net.corda.core.node.ServiceHub;

import java.util.*;
import java.lang.Class;

public class NewSerializationWhitelist implements SerializationWhitelist {

    @Override
    public List<Class<?>> getWhitelist() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(TransactionBuilder.class);
        list.add(ServiceHub.class);
        return list;
    }
}
