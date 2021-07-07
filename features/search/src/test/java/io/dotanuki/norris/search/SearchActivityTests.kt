package io.dotanuki.norris.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.dotanuki.norris.persistance.LocalStorage
import io.dotanuki.norris.search.di.searchModule
import io.dotanuki.norris.search.presentation.SearchScreenState.Content
import io.dotanuki.norris.search.presentation.SearchScreenState.Done
import io.dotanuki.norris.search.presentation.SearchScreenState.Idle
import io.dotanuki.norris.search.presentation.SearchScreenState.Loading
import io.dotanuki.norris.search.ui.SearchActivity
import io.dotanuki.norris.search.util.FakeSearchScreen
import io.dotanuki.norris.search.util.FakeSearchScreen.Companion.searchScreen
import io.dotanuki.norris.search.util.searchTestModule
import io.dotanuki.testing.app.TestApplication
import io.dotanuki.testing.app.awaitPendingExecutions
import io.dotanuki.testing.app.whenActivityResumed
import io.dotanuki.testing.rest.RestDataBuilder
import io.dotanuki.testing.rest.RestInfrastructureRule
import io.dotanuki.testing.rest.RestInfrastructureTestModule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchActivityTests {

    private lateinit var localStorage: LocalStorage
    private lateinit var screen: FakeSearchScreen

    @get:Rule val restInfrastructure = RestInfrastructureRule()

    private val suggestions = RestDataBuilder.suggestionsPayload(
        listOf("career", "celebrity", "dev")
    )

    @Before fun `before each test`() {
        val restTestModule = RestInfrastructureTestModule(restInfrastructure.server)
        val testApplication = TestApplication.setupWith(
            searchModule,
            searchTestModule,
            restTestModule
        )

        restInfrastructure.restScenario(200, suggestions)
        localStorage = testApplication.localStorage
        screen = testApplication.searchScreen()
    }

    @Test fun `at first lunch, should display only suggestions`() {
        whenActivityResumed<SearchActivity> {

            assertThat(screen.isLinked).isTrue()

            val content = Content(
                suggestions = listOf("career", "celebrity", "dev"),
                history = emptyList()
            )

            val expectedStates = listOf(Idle, Loading, content)
            assertThat(screen.trackedStates).isEqualTo(expectedStates)
        }
    }

    @Test fun `should proceed saving term chosen from suggestions`() {
        localStorage.registerNewSearch("code")

        whenActivityResumed<SearchActivity> {
            screen.delegate.onChipClicked("dev")

            awaitPendingExecutions()

            val savedSearches = runBlocking { localStorage.lastSearches() }
            val expectedSearches = listOf("code", "dev")
            assertThat(savedSearches).isEqualTo(expectedSearches)

            val content = Content(
                suggestions = listOf("career", "celebrity", "dev"),
                history = listOf("code")
            )

            val expectedStates = listOf(Idle, Loading, content, Loading, Done)
            assertThat(screen.trackedStates).isEqualTo(expectedStates)
        }
    }

    @Test fun `should proceed saving new search term`() {
        localStorage.registerNewSearch("code")
        localStorage.registerNewSearch("dev")

        whenActivityResumed<SearchActivity> {

            screen.delegate.onNewSearch("kotlin")
            awaitPendingExecutions()

            val savedSearches = runBlocking { localStorage.lastSearches() }
            val expectedSearches = listOf("code", "dev", "kotlin")
            assertThat(savedSearches).isEqualTo(expectedSearches)

            val content = Content(
                suggestions = listOf("career", "celebrity", "dev"),
                history = listOf("code", "dev")
            )

            val expectedStates = listOf(Idle, Loading, content, Loading, Done)
            assertThat(screen.trackedStates).isEqualTo(expectedStates)
        }
    }
}