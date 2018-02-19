package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream the contents of the node's vault.
 */
fun main(args: Array<String>) {
    AlokClient().main(args)
}



    private class AlokClient {
        companion object {
            val logger: Logger = loggerFor<AlokClient>()
            private fun logState(state: StateAndRef<AlokState>) = logger.info("{}", state.state.data)
        }

        fun main(args: Array<String>) {
            require(args.size == 2) { "Usage: AlokClient <node address> <thought>`" }
            val nodeAddress = parse(args[0])
            val client = CordaRPCClient(nodeAddress)

            val proxy = client.start("user1", "test").proxy

            val (snapshot, updates) = proxy.vaultTrack(AlokState::class.java)

            proxy.waitUntilNetworkReady().getOrThrow()

            val issuerID = proxy.wellKnownPartyFromX500Name(BOD_NAME) ?: throw IllegalArgumentException("Could not find the issuer node '${BOD_NAME}'.")

            logger.info("=============came here===============")
            proxy.startFlow(::AlokIssueRequest, args[1], issuerID)
                    .returnValue.getOrThrow()

            snapshot.states.forEach { logState(it) }
            updates.subscribe { update ->
                update.produced.forEach { logState(it) }
            }
        }
    }