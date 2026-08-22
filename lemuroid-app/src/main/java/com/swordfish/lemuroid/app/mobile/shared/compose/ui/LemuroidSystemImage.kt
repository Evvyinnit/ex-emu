package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo

/**
 * NeoStation-style system artwork: surface background tinted with the system color,
 * with the white console logo floating in the middle.
 */
@Composable
fun LemuroidSystemImage(system: MetaSystemInfo) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(system.metaSystem.color()).copy(alpha = 0.4f)),
        )
        Image(
            modifier = Modifier.fillMaxSize(0.62f),
            painter = painterResource(id = system.metaSystem.imageResId),
            contentDescription = stringResource(id = system.metaSystem.titleResId),
            contentScale = ContentScale.FillBounds,
        )
    }
}
