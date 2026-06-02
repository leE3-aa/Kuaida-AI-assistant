package com.kuaida.feature.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus
import com.kuaida.core.model.UsageCost
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historyScreen_deleteActionOpensDialog() {
        composeRule.setContent {
            MaterialTheme {
                HistoryScreen(
                    state = HistoryUiState(
                        sessions = listOf(
                            InterviewSession(
                                id = "session_1",
                                question = "请介绍一次项目推进经历",
                                transcript = null,
                                answer = AnswerBlock(
                                    conclusion = "先对齐目标，再拆解计划。",
                                    keyPoints = listOf("目标一致"),
                                    expansion = "随后明确节奏和依赖。",
                                ),
                                provider = ProviderType.QWEN,
                                modelId = "qwen-plus",
                                stylePreset = AnswerStylePreset.DETAILED,
                                sourceType = QuestionSourceType.TEXT,
                                usage = UsageCost(totalTokens = 32, estimatedCostUsd = 0.12),
                                status = SessionStatus.COMPLETED,
                                latencyMs = 900L,
                                createdAt = 1L,
                            ),
                        ),
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    snackbarHostState = SnackbarHostState(),
                    onSearchChange = { _ -> },
                    onDelete = { _ -> },
                    onAskAgain = { _ -> },
                )
            }
        }

        composeRule.onNodeWithText("删除").performClick()

        composeRule.onNodeWithText("删除历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("删除后将移除这条会话及其关联成本记录，确定删除“请介绍一次项目推进经历”吗？")
            .assertIsDisplayed()
    }
}
