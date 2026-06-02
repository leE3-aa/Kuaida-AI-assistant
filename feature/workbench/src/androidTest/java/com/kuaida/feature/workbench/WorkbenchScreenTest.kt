package com.kuaida.feature.workbench

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kuaida.core.model.AnswerStylePreset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkbenchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workbenchScreen_showsRecordingSheetAndStartAction() {
        composeRule.setContent {
            MaterialTheme {
                WorkbenchScreen(
                    state = WorkbenchUiState(
                        input = "",
                        selectedStyle = AnswerStylePreset.DETAILED,
                        recordingSheet = WorkbenchRecordingSheetUiState(
                            isVisible = true,
                            stage = RecordingStage.IDLE,
                        ),
                    ),
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    onInputChange = { _ -> },
                    onStyleChange = { _ -> },
                    onSubmit = {},
                    onOpenRecordingSheet = {},
                    onDismissRecordingSheet = {},
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onFinishRecording = {},
                    onRerecord = {},
                    onRetryRecordingTranscription = {},
                    onFollowUp = { _ -> },
                    onOpenTemplates = {},
                )
            }
        }

        composeRule.onNodeWithText("语音录音").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").assertIsDisplayed()
        composeRule.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun workbenchScreen_clickingTemplateButtonInvokesCallback() {
        var openTemplateCount = 0

        composeRule.setContent {
            MaterialTheme {
                WorkbenchScreen(
                    state = WorkbenchUiState(
                        input = "请介绍一次项目推进经历",
                        selectedStyle = AnswerStylePreset.SPOKEN,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    onInputChange = { _ -> },
                    onStyleChange = { _ -> },
                    onSubmit = {},
                    onOpenRecordingSheet = {},
                    onDismissRecordingSheet = {},
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onFinishRecording = {},
                    onRerecord = {},
                    onRetryRecordingTranscription = {},
                    onFollowUp = { _ -> },
                    onOpenTemplates = { openTemplateCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("模板").performClick()

        assertEquals(1, openTemplateCount)
        composeRule.onAllNodesWithText("Token / 费用").assertCountEquals(0)
    }
}
