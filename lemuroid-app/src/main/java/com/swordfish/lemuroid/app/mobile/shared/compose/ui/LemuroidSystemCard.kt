package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo

/**
 * NeoStation-style system card: bordered surface with a soft shadow, square artwork on top,
 * an uppercase footer with the system name and game count, and a signature primary-color
 * glow that fades in while pressed.
 */
@Composable
fun LemuroidSystemCard(
    modifier: Modifier = Modifier,
    system: MetaSystemInfo,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    val title =
        remember(system.metaSystem.titleResId) {
            system.getName(context)
        }

    val subtitle =
        remember(system.metaSystem.titleResId) {
            context.getString(
                R.string.system_grid_details,
                system.count,
            )
        }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val borderColor by animateColorAsState(
        targetValue =
            if (pressed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        animationSpec = tween(durationMillis = 200),
        label = "systemCardBorder",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "systemCardGlow",
    )

    Card(
        modifier =
            modifier
                .shadow(2.dp, LemuroidCardShape),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = LemuroidCardShape,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Box {
                LemuroidSystemImage(system)
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .alpha(glowAlpha)
                            .background(
                                Brush.verticalGradient(
                                    0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    0.35f to MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    1f to Color.Transparent,
                                ),
                            ),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
