package com.kuaida.core.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuaida.core.common.ui.theme.QaTheme
import com.kuaida.core.common.ui.theme.QuickAnswerTone
import com.kuaida.core.common.ui.theme.quickAnswerContentColor
import com.kuaida.core.common.ui.theme.quickAnswerSurfaceColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAnswerScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QaTheme.backgroundBrush),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                if (snackbarHostState != null) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        QuickAnswerTopBarTitle(
                            title = title,
                            subtitle = subtitle,
                        )
                    },
                    navigationIcon = { navigationIcon?.invoke() },
                    actions = actions,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            bottomBar = {
                bottomBar?.invoke()
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

@Composable
fun QuickAnswerTopBarTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun QuickAnswerSectionCard(
    modifier: Modifier = Modifier,
    tone: QuickAnswerTone = QuickAnswerTone.Surface,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedContainerColor = containerColor ?: quickAnswerSurfaceColor(tone)
    val resolvedContentColor = contentColor ?: quickAnswerContentColor(tone)
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(QaTheme.spacing.large),
            content = content,
        )
    }
}

@Composable
fun QuickAnswerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(QaTheme.spacing.small),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionColor,
            )
        }
    }
}

@Composable
fun QuickAnswerPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: QuickAnswerTone = QuickAnswerTone.Tonal,
) {
    val containerColor = quickAnswerSurfaceColor(tone)
    val contentColor = quickAnswerContentColor(tone)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun QuickAnswerMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: QuickAnswerTone = QuickAnswerTone.Tonal,
) {
    val containerColor = quickAnswerSurfaceColor(tone)
    val contentColor = quickAnswerContentColor(tone)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.78f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun QuickAnswerInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
fun QuickAnswerEmptyStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    tone: QuickAnswerTone = QuickAnswerTone.Tonal,
) {
    QuickAnswerSectionCard(
        modifier = modifier,
        tone = tone,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun QuickAnswerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val labelContent: (@Composable () -> Unit)? = if (label != null) {
        { Text(label) }
    } else {
        null
    }
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = labelContent,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
fun QuickAnswerToneDot(
    tone: QuickAnswerTone,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(quickAnswerSurfaceColor(tone)),
    )
}
