package com.template.api

import com.template.AlokIssueRequest
import com.template.AlokMoveRequest
import com.template.AlokState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.time.LocalDateTime
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.IllegalArgumentException


@Path("bank")
class BankWebApi(private val rpc: CordaRPCOps){
    companion object {
        val logger: Logger = loggerFor<BankWebApi>()
    }

    @GET
    @Path("date")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCurrentDate(): Any {
        return mapOf("date" to LocalDateTime.now().toLocalDate())
    }

    @POST
    @Path("issue-asset-request")
    @Consumes(MediaType.APPLICATION_JSON)
    fun issueAssetRequest(thought: String, issuer: CordaX500Name): Response{
        return try {
            val issuerID = rpc.wellKnownPartyFromX500Name(issuer) ?: throw IllegalArgumentException("Could not find the issuer node '${issuer}'.")
            rpc.startFlow(::AlokIssueRequest, thought, issuerID).returnValue.getOrThrow()
            logger.info("Issue request completed successfully: $thought")
            Response.status(Response.Status.CREATED).build()
        } catch (e: Exception) {
            logger.error("Issue request failed", e)
            Response.status(Response.Status.FORBIDDEN).build()
        }
    }


    @POST
    @Path("issue-move-request")
    @Consumes(MediaType.APPLICATION_JSON)
    fun issueMoveRequest(alok: StateAndRef<AlokState>, newOwner: CordaX500Name): Response {
        return try {
            val issuerID = rpc.wellKnownPartyFromX500Name(newOwner) ?: throw IllegalArgumentException("Could not find the new owner node '${newOwner}'.")
            rpc.startFlow(::AlokMoveRequest, alok, issuerID).returnValue.getOrThrow()
            logger.info("Movement request completed successfully")
            Response.status(Response.Status.CREATED).build()
        } catch (e: Exception) {
            logger.error("Movement request failed", e)
            Response.status(Response.Status.FORBIDDEN).build()
        }
    }

}