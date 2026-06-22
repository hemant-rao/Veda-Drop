package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.VedaDropGold
import com.example.ui.theme.VedaDropRose

/**
 * §709 — shared, theme-aware loading primitives. One home for every "something is
 * loading" affordance so the app reads consistently (Google-Blue brand, dark navy
 * surfaces). Three tiers:
 *   - [VedaDropTopLoadingBar]   thin global bar — "the app is doing something" (non-blocking)
 *   - [VedaDropFullScreenLoader] full-page loader — a fresh screen with nothing to show yet
 *   - [VedaDropInlineLoader]     centered content loader — a section/list still fetching
 */

/** The brand spinner: two counter-rotating arcs in the brand blue + gold accent. */
@Composable
fun VedaDropBrandSpinner(size: Int = 48) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.dp).rotate(angle),
            color = VedaDropRose, strokeWidth = (size / 12).coerceAtLeast(3).dp,
        )
        CircularProgressIndicator(
            modifier = Modifier.size((size * 0.55f).dp).rotate(-angle),
            color = VedaDropGold, strokeWidth = (size / 16).coerceAtLeast(2).dp,
        )
    }
}

/**
 * Full-page themed loader. Use as the entire body of a screen while its first data
 * fetch is in flight (so the user sees a branded loading state, never a blank box).
 */
@Composable
fun VedaDropFullScreenLoader(message: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VedaDropBrandSpinner(56)
            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    modifier = Modifier.padding(top = 18.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Blocking, dimmed overlay loader — drop on top of a screen (in a Box) for an action
 * that should freeze interaction until it completes (e.g. confirming a booking).
 * Swallows touches while [visible].
 */
@Composable
fun VedaDropLoadingScrim(visible: Boolean, message: String? = null) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) {},   // eat all gestures while loading
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VedaDropBrandSpinner(52)
                if (!message.isNullOrBlank()) {
                    Text(
                        message,
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Inline, centered content loader for a section/list that is still fetching. */
@Composable
fun VedaDropInlineLoader(modifier: Modifier = Modifier, message: String? = null) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VedaDropBrandSpinner(40)
            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Thin, non-blocking global progress bar pinned to the top of the content area —
 * the "the app is fetching something" signal (Swiggy/Zomato style). Animates in/out
 * so a quick call doesn't flash harshly.
 */
@Composable
fun VedaDropTopLoadingBar(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = VedaDropRose,
            trackColor = VedaDropRose.copy(alpha = 0.18f),
        )
    }
}

/**
 * A Jetpack Compose equivalent of React Error Boundary to prevent full-app crashes
 * and provide a user-friendly fallback UI when an unexpected error occurs.
 */
@Composable
fun ComposeErrorBoundary(
    onGoHome: () -> Unit,
    content: @Composable () -> Unit
) {
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Throwable?>(null) }
    
    if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Error Occurred",
                    tint = VedaDropRose,
                    modifier = Modifier.size(64.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Something went wrong",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error?.localizedMessage ?: "An unexpected error occurred during rendering this screen.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        error = null
                        onGoHome()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                ) {
                    Text("Return to Safety", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        content()
    }
}

/**
 * Pulse loading animation modifier to create high-quality skeleton loaders.
 * Gently oscillates open background items to signify active fetching state.
 */
fun Modifier.pulseLoading(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    this.alpha(alpha)
}

/**
 * Skeleton loading screen for a single Service Card to represent fetching state.
 */
@Composable
fun ServiceCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pulseLoading(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = DeepPlum)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Placeholder
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    // Rating Placeholder
                    Box(
                        modifier = Modifier
                            .size(32.dp, 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description line 1 Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Description line 2 Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price Label Placeholder
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    // "View Info" Button Placeholder
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading screen representing a list of Service Cards.
 */
@Composable
fun ServiceListSkeleton(count: Int = 3) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            ServiceCardSkeleton()
        }
    }
}

/**
 * Skeleton loading screen for a single Appointment Card.
 */
@Composable
fun AppointmentCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pulseLoading(),
        colors = CardDefaults.cardColors(containerColor = DeepPlum.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service Name Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
                // Price Placeholder
                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Professional / Specialist Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar / Slot row placeholder
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer row with statuses and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Chip Placeholder
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )
                
                // Action Button Placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading screen representing a list of Appointment Cards.
 */
@Composable
fun AppointmentListSkeleton(count: Int = 4) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            AppointmentCardSkeleton()
        }
    }
}


