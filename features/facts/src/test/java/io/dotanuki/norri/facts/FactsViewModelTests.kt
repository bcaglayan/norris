package io.dotanuki.norri.facts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.dotanuki.coroutines.testutils.CoroutinesTestHelper
import io.dotanuki.coroutines.testutils.CoroutinesTestHelper.Companion.runWithTestScope
import io.dotanuki.coroutines.testutils.collectForTesting
import io.dotanuki.norris.architecture.StateContainer
import io.dotanuki.norris.architecture.StateMachine
import io.dotanuki.norris.architecture.TaskExecutor
import io.dotanuki.norris.architecture.UserInteraction
import io.dotanuki.norris.architecture.ViewState
import io.dotanuki.norris.architecture.ViewState.Failed
import io.dotanuki.norris.architecture.ViewState.Loading
import io.dotanuki.norris.facts.FactPresentation
import io.dotanuki.norris.facts.FactsViewModel
import io.dotanuki.norris.rest.FetchFacts
import io.dotanuki.norris.rest.errors.RemoteServiceIntegrationError.UnexpectedResponse
import io.dotanuki.norris.rest.model.ChuckNorrisFact
import io.dotanuki.norris.rest.model.RelatedCategory
import io.dotanuki.norris.rest.services.RemoteFactsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class FactsViewModelTests {

    @get:Rule val helper = CoroutinesTestHelper()

    private val factsService = mock<RemoteFactsService>()
    private lateinit var viewModel: FactsViewModel

    @Before fun `before each test`() {
        val stateMachine = StateMachine<List<FactPresentation>>(
            executor = TaskExecutor.Synchronous(helper.scope),
            container = StateContainer.Unbounded()
        )

        val usecase = FetchFacts(factsService)
        viewModel = FactsViewModel(usecase, stateMachine)
    }

    @Test fun `should report failure when fetching from remote`() {
        runWithTestScope(helper.scope) {

            // Given
            val emissions = viewModel.bind().collectForTesting()

            // When
            whenever(factsService.availableCategories())
                .thenAnswer { throw UnexpectedResponse }

            // And
            viewModel.handle(UserInteraction.OpenedScreen).join()

            // Then
            val viewStates = listOf(
                Loading.FromEmpty,
                Failed(UnexpectedResponse)
            )

            assertThat(emissions).isEqualTo(viewStates)
        }
    }

    @Test fun `should fetch article from remote data source with success`() {
        runWithTestScope(helper.scope) {

            // Given
            val emissions = viewModel.bind().collectForTesting()
            val categories = listOf(
                RelatedCategory.Available("dev")
            )

            val facts = listOf(
                ChuckNorrisFact(
                    id = "2wzginmks8azrbaxnamxdw",
                    shareableUrl = "https://api.chucknorris.io/jokes/2wzginmks8azrbaxnamxdw",
                    textual = "Chuck Norris commits before Git repo even exits",
                    category = RelatedCategory.Available("dev")
                )
            )

            // When
            whenever(factsService.availableCategories()).thenReturn(categories)
            whenever(factsService.fetchFacts(anyString())).thenReturn(facts)

            // And
            viewModel.handle(UserInteraction.OpenedScreen).join()

            // And
            val presentation = listOf(
                FactPresentation(
                    tag = RelatedCategory.Available("dev"),
                    url = "https://api.chucknorris.io/jokes/2wzginmks8azrbaxnamxdw",
                    fact = "Chuck Norris commits before Git repo even exits",
                    displayWithSmallerFontSize = false
                )
            )

            // Then
            val viewStates = listOf(
                Loading.FromEmpty,
                ViewState.Success(presentation)
            )

            assertThat(emissions).isEqualTo(viewStates)
        }
    }
}