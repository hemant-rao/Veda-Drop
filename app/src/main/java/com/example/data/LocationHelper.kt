package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Device location for VedaDrop.
 *
 * Ported from the Early Rover app's hardened acquisition chain — the previous
 * version made a single FusedLocation BALANCED_POWER request and gave up the
 * moment it returned null (very common on a cold start / indoors), which read to
 * users as "can't set my location". This version walks a robust fallback chain
 * and is guaranteed to resolve exactly once within a bounded time:
 *
 *   1. FusedLocation getCurrentLocation(PRIORITY_HIGH_ACCURACY)  — a fresh, precise fix
 *   2. FusedLocation lastLocation                                — recent cached fix
 *   3. system LocationManager getLastKnownLocation(GPS → Network)
 *   4. overall 12s timeout → resolve with whatever we have, else null
 *
 * Permission-aware (returns null when not granted) and never throws — callers
 * fall back to manual address entry / un-sorted discovery. We deliberately drop
 * Early Rover's timezone + reverse-geocode work: VedaDrop only needs (lat, lon),
 * and turns it into an address via the backend geo gateway (geoReverse).
 */
object LocationHelper {

    private const val OVERALL_TIMEOUT_MS = 12_000L

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Best-effort current location. Returns (lat, lon) or null if permission is
     * missing / location is off / every provider returns nothing.
     */
    suspend fun current(context: Context): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            acquire(context.applicationContext) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }

    /**
     * Drives the fallback chain. [onResult] is invoked exactly once (guarded by an
     * AtomicBoolean) with the first fix found, or null when the chain is exhausted
     * / the overall timeout fires.
     */
    @SuppressLint("MissingPermission")
    private fun acquire(context: Context, onResult: (Pair<Double, Double>?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        val completed = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        fun finish(loc: Location?) {
            if (!completed.compareAndSet(false, true)) return
            mainHandler.removeCallbacksAndMessages(null)
            onResult(loc?.let { it.latitude to it.longitude })
        }

        // Hard timeout so the caller's spinner can never stick forever even if a
        // provider neither completes nor fails (e.g. a wedged Play Services call).
        mainHandler.postDelayed({ finish(systemLastKnown(context)) }, OVERALL_TIMEOUT_MS)

        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { fresh ->
                    if (fresh != null) {
                        finish(fresh)
                    } else {
                        // No fresh fix → try the recent cached one, then the system providers.
                        fused.lastLocation
                            .addOnSuccessListener { last -> finish(last ?: systemLastKnown(context)) }
                            .addOnFailureListener { finish(systemLastKnown(context)) }
                    }
                }
                .addOnFailureListener { finish(systemLastKnown(context)) }
        } catch (_: Exception) {
            finish(systemLastKnown(context))
        }
    }

    /**
     * §698 — continuous location stream for live tracking (the partner travelling to
     * the customer). Mirrors Early Rover's travel-service cadence: FusedLocation
     * PRIORITY_HIGH_ACCURACY at a ~5s interval. Returns a [LocationUpdates] handle whose
     * [LocationUpdates.stop] MUST be called to release the listener (else GPS keeps
     * draining the battery). Returns null when permission is missing.
     */
    @SuppressLint("MissingPermission")
    fun startUpdates(
        context: Context,
        intervalMs: Long = 5_000L,
        onUpdate: (Double, Double) -> Unit,
    ): LocationUpdates? {
        if (!hasPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val req = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, intervalMs
        )
            .setMinUpdateIntervalMillis((intervalMs / 2).coerceAtLeast(2_000L))
            .setWaitForAccurateLocation(false)
            .build()
        val cb = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation ?: return
                onUpdate(loc.latitude, loc.longitude)
            }
        }
        return try {
            client.requestLocationUpdates(req, cb, Looper.getMainLooper())
            LocationUpdates(client, cb)
        } catch (_: Exception) {
            null
        }
    }

    /** Native LocationManager last-known fix: GPS first (more precise), then Network. */
    @SuppressLint("MissingPermission")
    private fun systemLastKnown(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val gps = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            gps ?: if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
        } catch (_: Exception) {
            null
        }
    }
}

/** Handle for an active [LocationHelper.startUpdates] stream; call [stop] to release it. */
class LocationUpdates(
    private val client: com.google.android.gms.location.FusedLocationProviderClient,
    private val cb: com.google.android.gms.location.LocationCallback,
) {
    fun stop() {
        try {
            client.removeLocationUpdates(cb)
        } catch (_: Exception) {
        }
    }
}
