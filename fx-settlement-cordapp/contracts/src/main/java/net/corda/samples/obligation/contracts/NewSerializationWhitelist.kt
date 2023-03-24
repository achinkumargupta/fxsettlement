package net.corda.samples.obligation.contracts

import net.corda.core.transactions.TransactionBuilder
import net.corda.core.serialization.SerializationWhitelist

class NewSerializationWhitelist : SerializationWhitelist {
    override val whitelist = listOf(
        TransactionBuilder::class.java
    )
}