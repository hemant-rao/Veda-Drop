@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LocationHelper
import com.example.data.remote.GeoSuggestionDto
import com.example.ui.theme.VedaDropRose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * §725 — a single chosen location, fully resolved (coords + reverse-geocoded
 * address/city/pincode). Returned by [LocationSearchOverlay] so every caller —
 * home "Deliver To", booking, partner KYC, partner business location — gets the
 * same shape and decides for itself what to do with it.
 */
data class PickedLocation(
    val lat: Double,
    val lon: Double,
    val address: String,
    val city: String = "",
    val pincode: String = "",
    val title: String = "",
    val subtitle: String = "",
)

/**
 * §725 — the ONE ride-app-style location picker used everywhere a location is set.
 *
 * Full-screen overlay: a search input pinned to the TOP, a prominent "Use my
 * current location" row (GPS, most accurate), and search-as-you-type results
 * filling the rest of the screen. Replaces the old small centered dialog + the
 * five duplicated inline pickers. Reverse-geocodes the chosen point and hands a
 * fully-resolved [PickedLocation] to [onPicked], then dismisses.
 */
@Composable
fun LocationSearchOverlay(
    viewModel: VedaDropViewModel,
    title: String = "Set location",
    onDismiss: () -> Unit,
    onPicked: (PickedLocation) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LocationSearchPanel(
                viewModel = viewModel,
                title = title,
                onClose = onDismiss,
                onPicked = { p ->
                    onPicked(p)
                    onDismiss()
                },
            )
        }
    }
}

/** The full-height inner content of the picker (top input + GPS row + results). */
@Composable
fun LocationSearchPanel(
    viewModel: VedaDropViewModel,
    title: String,
    onClose: () -> Unit,
    onPicked: (PickedLocation) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<GeoSuggestionDto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }      // GPS detect in flight
    var searching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val detect: () -> Unit = {
        busy = true
        viewModel.captureDeviceLocation(notifyOnFail = true) { loc ->
            if (loc == null) {
                busy = false
            } else {
                scope.launch {
                    val rev = viewModel.reverseGeocode(loc.first, loc.second)
                    busy = false
                    onPicked(
                        PickedLocation(
                            lat = loc.first,
                            lon = loc.second,
                            address = rev?.address ?: "%.5f, %.5f".format(loc.first, loc.second),
                            city = rev?.city ?: "",
                            pincode = rev?.pincode ?: "",
                            title = "Current location",
                        )
                    )
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) detect()
        else viewModel.notify("Location permission denied. Search your area below instead.", isError = true)
    }

    LaunchedEffect(query) {
        if (query.trim().length >= 3) {
            delay(300)
            searching = true
            suggestions = viewModel.searchPlaces(query.trim())
            searching = false
        } else {
            suggestions = emptyList()
            searching = false
        }
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + search input — pinned to the TOP (ride-app style).
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search area, street, building or city…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("location_search_input"),
                )
            }
        }

        // Prominent "use my current location" action.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy) {
                    if (LocationHelper.hasPermission(ctx)) detect()
                    else permLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) {
                CircularProgressIndicator(color = VedaDropRose, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Use my current location", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Most accurate — uses your device GPS", fontSize = 12.sp, color = Color.Gray)
            }
        }
        HorizontalDivider()

        // Search status — never leave the list silently blank.
        val statusModifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        when {
            query.isBlank() -> Text(
                "Tap “Use my current location”, or type to search any address",
                fontSize = 12.sp, color = Color.Gray, modifier = statusModifier,
            )
            query.trim().length in 1..2 -> Text(
                "Type at least 3 letters to search", fontSize = 12.sp, color = Color.Gray, modifier = statusModifier,
            )
            searching -> Row(modifier = statusModifier, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = VedaDropRose)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching…", fontSize = 12.sp, color = Color.Gray)
            }
            suggestions.isEmpty() -> Text(
                "No matches found. Try a different spelling, or use your current location.",
                fontSize = 12.sp, color = Color.Gray, modifier = statusModifier,
            )
            else -> Text(
                "${suggestions.size} ${if (suggestions.size == 1) "match" else "matches"} found",
                fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium, modifier = statusModifier,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(suggestions.take(20)) { sug ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val lat = sug.lat
                                val lon = sug.lon
                                if (lat == null || lon == null) {
                                    viewModel.notify("That place has no coordinates — pick another.", isError = true)
                                    return@launch
                                }
                                val rev = viewModel.reverseGeocode(lat, lon)
                                onPicked(
                                    PickedLocation(
                                        lat = lat,
                                        lon = lon,
                                        address = rev?.address
                                            ?: listOfNotNull(sug.title, sug.subtitle).joinToString(", "),
                                        city = rev?.city ?: "",
                                        pincode = rev?.pincode ?: "",
                                        title = sug.title ?: "",
                                        subtitle = sug.subtitle ?: "",
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(sug.title ?: "", fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!sug.subtitle.isNullOrBlank()) {
                            Text(sug.subtitle!!, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
