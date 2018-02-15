package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream the contents of the node's vault.
 */
fun main(args: Array<String>) {
    TemplateClient().main(args)
}

private class AlokClient {
    companion object {
        val logger: Logger = loggerFor<AlokClient>()
        private fun logState(state: StateAndRef<AlokState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: AlokClient <node address> <thought>" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all existing TemplateStates and all future TemplateStates.
        val (snapshot, updates) = proxy.vaultTrack(AlokState::class.java)

        proxy.waitUntilNetworkReady().getOrThrow()
        val issuerID = proxy.wellKnownPartyFromX500Name(BOD_NAME) ?: throw IllegalArgumentException("Could not find the issuer node '${BOD_NAME}'.")

        proxy.startFlow(::AlokIssueRequest, args[1], issuerID)
                .returnValue.getOrThrow()
        // Log the existing TemplateStates and listen for new ones.
        snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
