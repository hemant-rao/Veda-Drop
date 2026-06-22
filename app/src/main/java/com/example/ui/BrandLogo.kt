package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Reusable, centralized Brand Logo component.
 * Ensures the official Veda Drop brand image (light_logo_with_bg)
 * is uniformly rendered as a perfect circle (50% border-radius/CircleShape)
 * to avoid any edge or white corner glitches across the entire application interface.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Veda Drop Logo",
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent
) {
    val imageModifier = modifier
        .clip(CircleShape)
        .let {
            if (borderWidth > 0.dp) {
                it.border(borderWidth, borderColor, CircleShape)
            } else {
                it
            }
        }

    Image(
        painter = painterResource(id = R.drawable.light_logo_with_bg),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = imageModifier
    )
}
