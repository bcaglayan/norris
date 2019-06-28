package io.dotanuki.norris.facts

import io.dotanuki.norris.architecture.StateMachine
import io.dotanuki.norris.architecture.UnsupportedUserInteraction
import io.dotanuki.norris.architecture.UserInteraction
import io.dotanuki.norris.architecture.UserInteraction.OpenedScreen
import io.dotanuki.norris.architecture.UserInteraction.RequestedFreshContent
import io.dotanuki.norris.domain.FetchFacts

class FactsViewModel(
    private val usecase: FetchFacts,
    private val machine: StateMachine<FactsPresentation>
) {

    fun bind() = machine.states()

    fun handle(interaction: UserInteraction) =
        interpret(interaction)
            .let { task ->
                machine.forward(task)
            }

    private fun interpret(interaction: UserInteraction) =
        when (interaction) {
            OpenedScreen, RequestedFreshContent -> ::showRandomFacts
            else -> throw UnsupportedUserInteraction
        }

    private suspend fun showRandomFacts() =
        usecase
            .randomFacts()
            .map { FactDisplayRow(it) }
            .let { rows ->
                FactsPresentation(rows)
            }
}