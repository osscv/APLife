package net.dkly.aplife.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import net.dkly.aplife.ui.theme.BrandButtonContainer
import net.dkly.aplife.ui.theme.BrandButtonContent
import net.dkly.aplife.ui.theme.BrandButtonDisabledContainer
import net.dkly.aplife.ui.theme.BrandButtonDisabledContent
import net.dkly.aplife.ui.theme.SuccessGreen
import net.dkly.aplife.ui.theme.DangerRed

/**
 * Stays-the-same-in-dark-mode brand button: lavender container + dark navy text.
 * Used for primary CTAs ("Continue", "Get started", "Finish & sync") so they always pop.
 */
@Composable
internal fun brandButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = BrandButtonContainer,
    contentColor = BrandButtonContent,
    disabledContainerColor = BrandButtonDisabledContainer,
    disabledContentColor = BrandButtonDisabledContent,
)

internal val ScreenHorizontalPadding = 16.dp
internal val SectionGap = 12.dp
internal val CardCorner = RoundedCornerShape(20.dp)
internal val ChipCorner = RoundedCornerShape(999.dp)

@Composable
internal fun SectionHeader(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    ) {
        if (leadingIcon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
internal fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = Modifier
        .fillMaxWidth()
        .clip(CardCorner)
    Card(
        modifier = base.then(modifier).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = CardCorner,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 2.dp,
        ),
    ) {
        Box(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
internal fun StatusBadge(
    text: String,
    positive: Boolean,
) {
    val bg = if (positive) SuccessGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
    val fg = if (positive) SuccessGreen else DangerRed
    Box(
        modifier = Modifier
            .clip(ChipCorner)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun ColorDot(color: Color, size: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
internal fun MetaRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ScreenScaffold(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
    ) { content() }
}

// ---- Intake-code field with autocomplete -------------------------------

@Composable
internal fun IntakeAutocompleteField(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onSuggestionPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val showDropdown = focused && value.isNotBlank() && suggestions.isNotEmpty()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.uppercase()) },
            label = { Text("Intake code (e.g. UCDF2509IRS)") },
            singleLine = true,
            placeholder = { Text("Start typing…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        if (showDropdown) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                ) {
                    items(suggestions, key = { it }) { code ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionPicked(code) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            HighlightedText(code = code, query = value)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(code: String, query: String) {
    val q = query.uppercase()
    val idx = if (q.isBlank()) -1 else code.indexOf(q)
    if (idx < 0) {
        Text(code, style = MaterialTheme.typography.bodyMedium)
    } else {
        val before = code.substring(0, idx)
        val match = code.substring(idx, idx + q.length)
        val after = code.substring(idx + q.length)
        Row {
            if (before.isNotEmpty()) Text(before, style = MaterialTheme.typography.bodyMedium)
            Text(
                match,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (after.isNotEmpty()) Text(after, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
