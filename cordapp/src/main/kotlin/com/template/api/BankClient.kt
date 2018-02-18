package com.dgkrajnik.bank.api

import com.template.AlokIssueRequest
import com.template.AlokMoveRequest
import com.template.AlokState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow

/**
 * Interface for communicating with the Bank of Daniel node.
 */
object BankOfAlokClientApi {
    /**
     * Requests a Daniel issuance via RPC.
     *
     * @return the issuing transaction.
     */
    fun requestRPCIssue(rpcAddress: NetworkHostAndPort, thought: String, issuer: CordaX500Name): SignedTransaction {
        val client = CordaRPCClient(rpcAddress)
        client.start("user1", "test").use { connection ->
            val rpc = connection.proxy
            rpc.waitUntilNetworkReady().getOrThrow()

            val issuerID = rpc.wellKnownPartyFromX500Name(issuer) ?: throw IllegalArgumentException("Could not find the issuer node '${issuer}'.")

            return rpc.startFlow(::AlokIssueRequest, thought, issuerID)
                    .returnValue.getOrThrow()
        }
    }

    /**
     * Requests a Daniel transfer via RPC
     *
     * @return the move transaction
     */
    fun requestRPCMove(rpcAddress: NetworkHostAndPort, daniel: StateAndRef<AlokState>, newOwner: CordaX500Name): SignedTransaction {
        val client = CordaRPCClient(rpcAddress)
        client.start("user1", "test").use { connection ->
            val rpc = connection.proxy
            rpc.waitUntilNetworkReady().getOrThrow()

            val ownerID = rpc.wellKnownPartyFromX500Name(newOwner) ?: throw IllegalArgumentException("Could not find the new owner node '${newOwner}'.")

            return rpc.startFlow(::AlokMoveRequest, daniel, ownerID)
                    .returnValue.getOrThrow()
        }
    }
}