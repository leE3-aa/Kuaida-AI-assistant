package com.kuaida.feature.knowledge

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.KnowledgeDocument
import org.junit.Rule
import org.junit.Test

class KnowledgeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun knowledgeScreen_deleteActionShowsConfirmationDialog() {
        composeRule.setContent {
            MaterialTheme {
                KnowledgeScreen(
                    state = KnowledgeUiState(
                        documents = listOf(
                            KnowledgeDocument(
                                id = "doc_1",
                                displayName = "面试手册",
                                mimeType = "text/plain",
                                sourceUri = "content://doc_1",
                                sizeBytes = 1024L,
                                parseStatus = DocumentParseStatus.READY,
                                chunkCount = 3,
                                embeddingCount = 3,
                                isEnabled = true,
                                createdAt = 1L,
                                updatedAt = 2L,
                            ),
                        ),
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    snackbarHostState = SnackbarHostState(),
                    onSearchChange = { _ -> },
                    onImport = {},
                    onToggle = { _, _ -> },
                    onDelete = { _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("删除文件").performClick()

        composeRule.onNodeWithText("删除知识文件").assertIsDisplayed()
        composeRule.onNodeWithText("删除后会同时移除分片、embedding 和相关成本记录，确定删除《面试手册》吗？")
            .assertIsDisplayed()
    }
}
