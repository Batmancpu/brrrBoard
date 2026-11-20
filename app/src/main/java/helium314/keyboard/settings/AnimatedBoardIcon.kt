// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R

/**
 * Welcome-only, bounded reveal of the exact full-color MangoLoads mark.
 *
 * The image is an immutable resource: the ten keys, shield-only refraction,
 * mango and italic brr never morph or redraw. A single 780ms draw transform
 * settles permanently; no background coroutine runs in the actual keyboard.
 */
@Composable
fun AnimatedBoardIcon(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true,
) {
    val progress = remember { Animatable(if (isAnimating) 0f else 1f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    val fraction = progress.value
    Image(
        painter = painterResource(id = R.drawable.mangoloads_brand_full),
        contentDescription = null, // The adjacent "Welcome to brrBoard" already names it.
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            alpha = 0.7f + 0.3f * fraction
            val scale = 0.965f + 0.035f * fraction
            scaleX = scale
            scaleY = scale
            translationY = (1f - fraction) * 6.dp.toPx()
        },
    )
}
