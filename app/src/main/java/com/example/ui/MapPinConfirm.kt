@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.map.GeoPoint
import com.example.ui.map.VedaDropMaps
import com.example.ui.map.VedaDropMapView
import com.example.ui.theme.VedaDropRose
import kotlinx.coroutines.launch

/**
 * §729 (parity C2) — MAP-PIN CONFIRM.
 *
 * After the customer picks / searches a location (via [LocationSearchOverlay]) this
 * full-screen step shows a MapLibre map (reusing [VedaDropMapView]) centred on the chosen
 * lat/lon with a fixed CENTRE PIN drawn as a Compose overlay, plus a "Confirm location"
 * button and a "Recentre to my GPS" action. This catches the common GPS-vs-typed mismatch
 * before the address is persisted.
 *
 * The map is best-effort: [VedaDropMapView] already degrades to a non-crashing fallback
 * view when MapLibre can't start, and a missing map style still lets the user confirm the
 * typed coordinates — so this step never BLOCKS the booking flow.
 *
 * NOTE: this is a CONFIRMATION of the already-chosen point, not a draggable-map geocoder
 * (the free OSM gateway has no per-pixel reverse cost budget for live drag). "Recentre to
 * my GPS" re-resolves the address from the device fix; the typed/searched point keeps its
 * resolved address otherwise.
 */
@Composable
fun MapPinConfirmDialog(
    viewModel: VedaDropViewModel,
    initial: PickedLocation,
    title: String = "Confirm location",
    onConfirm: (PickedLocation) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MapPinConfirmPanel(viewModel, initial, title, onConfirm, onDismiss)
        }
    }
}

@Composable
private fun MapPinConfirmPanel(
    viewModel: VedaDropViewModel,
    initial: PickedLocation,
    title: String,
    onConfirm: (PickedLocation) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // The currently-shown point; "Recentre to my GPS" replaces it with the device fix.
    var picked by remember { mutableStateOf(initial) }
    var recentring by remember { mutableStateOf(false) }

    // Map style comes from the geo gateway (free OSM); refresh it if not loaded yet.
    val geoConfig by viewModel.geoConfig.collectAsState()
    LaunchedEffect(Unit) { if (geoConfig == null) viewModel.refreshGeoConfig() }
    val styleUrl = geoConfig?.tileStyleUrl?.takeIf { it.isNotBlank() } ?: VedaDropMaps.DEFAULT_STYLE_URL

    Column(modifier = Modifier.fillMaxSize()) {
        // Header.
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
        }

        // Map + centre pin overlay.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            VedaDropMapView(
                styleUrl = styleUrl,
                // The chosen point is rendered as the "customer" marker; the camera fits/
                // follows it. followCurrent keeps it centred when we recentre to GPS.
                customer = GeoPoint(picked.lat, picked.lon),
                followCurrent = true,
                modifier = Modifier.fillMaxSize(),
            )
            // Fixed centre pin (Compose overlay — the map itself doesn't draw a centre pin).
            // Offset the icon up by half its height so the tip points at the centre.
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Selected location",
                tint = VedaDropRose,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 36.dp)
                    .size(40.dp)
                    .testTag("map_pin_marker"),
            )
        }

        // Address readout + actions.
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("SELECTED ADDRESS", fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 11.sp)
                Text(
                    picked.address.ifBlank { "%.5f, %.5f".format(picked.lat, picked.lon) },
                    fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                if (picked.city.isNotBlank() || picked.pincode.isNotBlank()) {
                    Text(
                        listOf(picked.city, picked.pincode).filter { it.isNotBlank() }.joinToString(" • "),
                        fontSize = 12.sp, color = Color.Gray,
                    )
                }

                OutlinedButton(
                    onClick = {
                        recentring = true
                        viewModel.captureDeviceLocation(notifyOnFail = true) { loc ->
                            if (loc == null) {
                                recentring = false
                            } else {
                                scope.launch {
                                    val rev = viewModel.reverseGeocode(loc.first, loc.second)
                                    picked = PickedLocation(
                                        lat = loc.first,
                                        lon = loc.second,
                                        address = rev?.address ?: "%.5f, %.5f".format(loc.first, loc.second),
                                        city = rev?.city ?: "",
                                        pincode = rev?.pincode ?: "",
                                        title = "Current location",
                                    )
                                    recentring = false
                                }
                            }
                        }
                    },
                    enabled = !recentring,
                    modifier = Modifier.fillMaxWidth().testTag("map_recenter_gps_btn"),
                ) {
                    if (recentring) {
                        CircularProgressIndicator(color = VedaDropRose, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recentre to my current location", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }

                Button(
                    onClick = { onConfirm(picked) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("map_confirm_location_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Confirm location", fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }
        }
    }
}

/**
 * §729 (parity C2) — convenience wrapper that chains the existing [LocationSearchOverlay]
 * (search / GPS) into the [MapPinConfirmDialog] (map-pin confirm). The caller gets a single
 * fully-resolved + map-CONFIRMED [PickedLocation] via [onConfirmed]. Drop-in replacement for
 * a bare LocationSearchOverlay where a confirm step is wanted.
 */
@Composable
fun LocationPickWithMapConfirm(
    viewModel: VedaDropViewModel,
    title: String = "Set location",
    onDismiss: () -> Unit,
    onConfirmed: (PickedLocation) -> Unit,
) {
    // Two-phase: first search/GPS pick, then map confirm. Holding the intermediate pick in
    // state lets us swap the overlay for the map step without unmounting the whole flow.
    var picked by remember { mutableStateOf<PickedLocation?>(null) }

    val current = picked
    if (current == null) {
        LocationSearchOverlay(
            viewModel = viewModel,
            title = title,
            // §729 fix — LocationSearchOverlay auto-fires onDismiss right after onPicked.
            // Only treat it as a real cancel (tear down the whole flow) when nothing was
            // picked yet; once a place is picked, keep the wrapper mounted so the map-pin
            // confirm step renders instead of silently closing.
            onDismiss = { if (picked == null) onDismiss() },
            onPicked = { picked = it },
        )
    } else {
        MapPinConfirmDialog(
            viewModel = viewModel,
            initial = current,
            onConfirm = { confirmed ->
                onConfirmed(confirmed)
                onDismiss()
            },
            // Back from the map returns to the search step (not all the way out).
            onDismiss = { picked = null },
        )
    }
}
