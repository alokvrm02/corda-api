package com.template

import net.corda.core.contracts.*
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder

// *****************
// * Contract Code *
// *****************
val ALOK_CONTRACT_ID = "com.template.AlokContract"

open class AlokContract : Contract {

    interface Commands : CommandData{
        class Issue :Commands
        class Move :Commands
    }
    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        val groups =tx.groupStates(AlokState::withoutOwner)

        val command = tx.commands.requireSingleCommand<AlokContract.Commands>()
        val timeWindow : TimeWindow?=tx.timeWindow


        for((inputs, outputs, _) in groups){
            when(command.value){
                is AlokContract.Commands.Move ->{
                    val input = inputs.single()

                    requireThat {
                        "The transaction is signed by the owner of the Alok." using (input.owner.owningKey in command.signers)
                        "The state is propagated." using (outputs.size == 1)
                    }
                }

                is AlokContract.Commands.Issue->{
                    val output = outputs.single()
                    timeWindow?.untilTime ?: throw IllegalArgumentException("Instances must be timestamped !!")

                    requireThat {
                        "Output states are signed by command Signer." using (output.issuer.owningKey in command.signers)
                        "Output contains a thought" using (!output.thought.equals(""))
                        "Cant reissue an existing State." using inputs.isEmpty()
                    }
                }
                else -> throw IllegalArgumentException("Command not found [E/1]!!")
            }

        }
    }

    companion object {
        fun generateIssue(thought: String, issuer : AbstractParty, owner: AbstractParty, notary: Party): TransactionBuilder{
            val state = AlokState(thought,issuer,owner)
            val stateAndContract = StateAndContract(state, ALOK_CONTRACT_ID)
            return TransactionBuilder(notary = notary).withItems(stateAndContract, Command(Commands.Issue(), issuer.owningKey))
        }

        fun generateMove(tx : TransactionBuilder,alok : StateAndRef<AlokState>, newOwner: AbstractParty){
            tx.addInputState(alok)
            val outputState = alok.state.data.withOwner(newOwner)
            tx.addOutputState(outputState, ALOK_CONTRACT_ID)
            tx.addCommand(Command(Commands.Move(),alok.state.data.owner.owningKey))

        }
    }

}

// *********
// * State *
// *********
data class TemplateState(val data: String) : ContractState {
    override val participants: List<AbstractParty> get() = listOf()
}

// *********
// * Custom State *
// *********
data class AlokState(val thought: String, val issuer : AbstractParty, val owner : AbstractParty): ContractState{
    override val participants: List<AbstractParty> get() = listOf(owner,issuer)

    fun withoutOwner() = copy(owner = AnonymousParty(NullKeys.NullPublicKey))

    fun withOwner(newOwner : AbstractParty): AlokState{
        return copy(owner=newOwner)
    }
}