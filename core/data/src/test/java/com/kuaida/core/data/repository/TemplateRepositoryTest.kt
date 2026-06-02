package com.kuaida.core.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryTest {
    private val repository = TemplateRepository()

    @Test
    fun observeTemplates_filtersByKeywordAcrossFields() = runTest {
        repository.observeTemplates("前端").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("frontend", items.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFavorite_updatesMatchingTemplateOnly() = runTest {
        repository.toggleFavorite("intro")

        repository.observeTemplates("").test {
            val items = awaitItem()
            val intro = items.first { it.id == "intro" }
            val whyUs = items.first { it.id == "why-us" }

            assertTrue(intro.favorite)
            assertTrue(whyUs.favorite)
            assertFalse(items.first { it.id == "metric" }.favorite)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
