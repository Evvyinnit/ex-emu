package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Caps content width and centers it on wide screens so phone-first layouts stay
 * readable in landscape and on tablets instead of stretching edge to edge.
 */
@Composable
fun MaxContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Int = 720,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth.dp)) {
            content()
        }
    }
}
