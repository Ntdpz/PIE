package com.ntdpz.pie.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.ntdpz.pie.core.designsystem.motion.piePressScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PiePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: PieGlyph? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.Confirm); onClick() },
        interactionSource = interactions,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.piePressScale(interactions).fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (icon != null) PieIcon(
                icon,
                modifier = Modifier.align(Alignment.CenterStart),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
            )
            Text(label, modifier = if (icon != null) Modifier.padding(horizontal = 24.dp) else Modifier)
        }
    }
}
