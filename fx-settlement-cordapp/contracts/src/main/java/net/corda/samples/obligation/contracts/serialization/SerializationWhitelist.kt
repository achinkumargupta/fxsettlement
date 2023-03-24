package net.corda.samples.obligation.contracts.serialization

import net.corda.core.transactions.TransactionBuilder
import net.corda.core.serialization.SerializationWhitelist

class SerializationWhitelist : SerializationWhitelist {
    override val whitelist = listOf(
        TransactionBuilder::class.java
    )
}