package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.webserver.services.WebServerPluginRegistry
import java.time.Duration
import java.time.Instant
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


val CORP_NAME = CordaX500Name(organisation = "BCS Learning", locality = "Sydney", country = "AU")
internal val NOTARY_NAME = CordaX500Name(organisation = "Turicum Notary Service", locality = "Zurich", country = "CH", commonName="corda.notary.validating")
internal val BOD_NAME = CordaX500Name(organisation = "Bank of Alok", locality = "Bloemfontein", country = "ZA")

// *****************
// * API Endpoints *
// *****************
@Path("template")
class TemplateApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/template/templateGetEndpoint.
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {
        return Response.ok("Template GET endpoint.").build()
    }
}

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        return Unit
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        return Unit
    }
}

@InitiatingFlow
@StartableByRPC
class AlokIssueRequest(val thought : String,val issuer : Party) : FlowLogic<SignedTransaction>(){
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.getNotary(NOTARY_NAME)?:throw FlowException("Notary not found !!")
        val selfID = serviceHub.myInfo.legalIdentities[0]

        val issueTxBuilder = AlokContract.generateIssue(thought,issuer,selfID, notary)

        val bankSession = initiateFlow(issuer)

        issueTxBuilder.setTimeWindow(TimeWindow.fromStartAndDuration(Instant.now(serviceHub.clock), Duration.ofMillis(10000)))

        issueTxBuilder.verify(serviceHub)   // verifying transaction

        val signedTx = serviceHub.signInitialTransaction(issueTxBuilder)    //signing transaction
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(bankSession), CollectSignaturesFlow.tracker()))


        return subFlow(FinalityFlow(fullySignedTx)) //Finalising the transaction
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

@InitiatedBy(AlokIssueRequest::class)
class AlokIssueResponse(val counterpartySession: FlowSession): FlowLogic<Unit>(){
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()){
            override fun checkTransaction(stx: SignedTransaction) = requireThat{
                val output = stx.tx.outputs.single().data
                "This must be Alok Transaction." using (output is AlokState)

                val alok = output as AlokState
                "The issuer of Alok must be the issuing node" using (alok.issuer.owningKey == ourIdentity.owningKey)
            }
        }

        subFlow(signTransactionFlow)
    }
}



@InitiatingFlow
@StartableByRPC
class AlokMoveRequest(val alok: StateAndRef<AlokState>, val newOwner : Party) : FlowLogic<SignedTransaction>(){
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.getNotary(NOTARY_NAME) ?: throw FlowException("Notary not found")

        val txBuilder = TransactionBuilder(notary=notary)
        AlokContract.generateMove(txBuilder, alok, newOwner)

        val moveSession = initiateFlow(newOwner)

        txBuilder.setTimeWindow(TimeWindow.fromStartAndDuration(Instant.now(serviceHub.clock), Duration.ofMillis(10000)))

        txBuilder.verify(serviceHub)    // Verifying the transaction.

        val signedTx = serviceHub.signInitialTransaction(txBuilder) // Signing the transaction.

        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(moveSession)))   // Obtaining the counterparty's signature.

        return subFlow(FinalityFlow(fullySignedTx))      // Finalising the transaction.
    }

}

@InitiatedBy(AlokMoveRequest::class)
class AlokMoveResponse(val counterpartySession: FlowSession) : FlowLogic<Unit>(){
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()){
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output =stx.tx.outputs.single().data
                "This must be a Alok transaction" using (output is AlokState)

                val alok = output as AlokState
                "The issuer of a Alok must be the issuing node" using (alok.issuer.owningKey == ourIdentity.owningKey)
            }
        }
        subFlow(signTransactionFlow)
    }
}

// ***************************************************
// **************** Extra code ***********************
// ***************************************************

// ***********
// * Plugins *
// ***********
//class TemplateWebPlugin : WebServerPluginRegistry {
//    // A list of classes that expose web JAX-RS REST APIs.
//    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::TemplateApi))
//    //A list of directories in the resources directory that will be served by Jetty under /web.
//    // This template's web frontend is accessible at /web/template.
//    override val staticServeDirs: Map<String, String> = mapOf(
//            // This will serve the templateWeb directory in resources to /web/template
//            "template" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
//    )
//}
//
//// Serialization whitelist.
//class TemplateSerializationWhitelist : SerializationWhitelist {
//    override val whitelist: List<Class<*>> = listOf(TemplateData::class.java)
//}
//
//// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
//// we want to send it to other nodes within a flow.
//data class TemplateData(val payload: String)
