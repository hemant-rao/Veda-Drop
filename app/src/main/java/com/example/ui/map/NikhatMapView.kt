package com.example.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * §690/§692 — Nikhat Glow live tracking map. MapLibre GL engine + FREE
 * OpenStreetMap vector tiles (OpenFreeMap). Renders the customer + partner as
 * markers and the route between them — MUTUAL live tracking. No API key and no
 * Ola .aar SDK: the map renders straight from the MapLibre style URL handed back
 * by the geo gateway (/api/geo/app-config → tile_style_url).
 */

data class GeoPoint(val latitude: Double, val longitude: Double)

object NikhatMaps {
    const val DEFAULT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /** Decode an OSRM/Google precision-5 encoded polyline into points. */
    fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            poly.add(GeoPoint(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}

private const val SRC_ROUTE = "ng-route-src"
private const val LYR_ROUTE = "ng-route-lyr"
private const val SRC_PARTNER = "ng-partner-src"
private const val LYR_PARTNER = "ng-partner-lyr"
private const val SRC_CUSTOMER = "ng-customer-src"
private const val LYR_CUSTOMER_HALO = "ng-customer-halo-lyr"
private const val LYR_CUSTOMER = "ng-customer-lyr"

private const val COLOR_PARTNER = "#1A73E8"   // Google Blue (partner / brand)
private const val COLOR_CUSTOMER = "#1E8E3E"  // green (you) — distinct from brand blue
private const val COLOR_ROUTE = "#1A73E8"      // brand blue route line

@Volatile private var httpConfigured = false

/** A plain OkHttp client with a User-Agent (some free tile hosts require one). No
 *  API key is injected — OpenFreeMap tiles are open. */
private fun ensureMapHttp() {
    if (httpConfigured) return
    val client = OkHttpClient.Builder().addInterceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", "NikhatGlow/1.0 (Android)")
            .build()
        chain.proceed(req)
    }.build()
    HttpRequestUtil.setOkHttpClient(client)
    httpConfigured = true
}

@SuppressLint("MissingPermission")
@Composable
fun NikhatMapView(
    styleUrl: String,
    modifier: Modifier = Modifier,
    customer: GeoPoint? = null,
    partner: GeoPoint? = null,
    route: List<GeoPoint>? = null,
    followCurrent: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val mapView = remember {
        ensureMapHttp()
        MapLibre.getInstance(context.applicationContext)
        val options = MapLibreMapOptions()
        MapView(context, options)
    }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { _ ->
            runCatching {
                mapView.getMapAsync { map ->
                    mapRef.value = map
                    val uri = styleUrl.trim().ifBlank { NikhatMaps.DEFAULT_STYLE_URL }
                    map.setStyle(Style.Builder().fromUri(uri)) { style ->
                        runCatching {
                            styleRef.value = style
                            initLayers(style)
                            applyData(map, style, customer, partner, route, followCurrent, firstFit = true)
                        }
                    }
                }
            }
            mapView
        },
        update = { _ ->
            val map = mapRef.value
            val style = styleRef.value
            if (map != null && style != null && style.isFullyLoaded) {
                applyData(map, style, customer, partner, route, followCurrent, firstFit = false)
            }
        },
    )
}

private fun initLayers(style: Style) {
    if (style.getSource(SRC_ROUTE) != null) return
    style.addSource(GeoJsonSource(SRC_ROUTE))
    style.addSource(GeoJsonSource(SRC_PARTNER))
    style.addSource(GeoJsonSource(SRC_CUSTOMER))

    style.addLayer(
        LineLayer(LYR_ROUTE, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(COLOR_ROUTE),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineOpacity(0.85f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    style.addLayer(
        CircleLayer(LYR_PARTNER, SRC_PARTNER).withProperties(
            PropertyFactory.circleColor(COLOR_PARTNER),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
    style.addLayer(
        CircleLayer(LYR_CUSTOMER_HALO, SRC_CUSTOMER).withProperties(
            PropertyFactory.circleColor(COLOR_CUSTOMER),
            PropertyFactory.circleRadius(16f),
            PropertyFactory.circleOpacity(0.18f),
        )
    )
    style.addLayer(
        CircleLayer(LYR_CUSTOMER, SRC_CUSTOMER).withProperties(
            PropertyFactory.circleColor(COLOR_CUSTOMER),
            PropertyFactory.circleRadius(6f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
}

private fun applyData(
    map: MapLibreMap,
    style: Style,
    customer: GeoPoint?,
    partner: GeoPoint?,
    route: List<GeoPoint>?,
    followCurrent: Boolean,
    firstFit: Boolean,
) {
    (style.getSource(SRC_PARTNER) as? GeoJsonSource)?.setGeoJson(pointFeature(partner))
    (style.getSource(SRC_CUSTOMER) as? GeoJsonSource)?.setGeoJson(pointFeature(customer))
    (style.getSource(SRC_ROUTE) as? GeoJsonSource)?.setGeoJson(lineFeature(route))

    // §710 — guard the camera math. `LatLngBounds.Builder().build()` throws
    // InvalidLatLngBoundsException when every included point is identical (e.g. the
    // customer and partner are at the same coords, or only one real point), which
    // crashed the booking-detail screen the moment a customer opened an ongoing job.
    // Dedup the points and never build bounds from <2 DISTINCT ones; wrap the whole
    // camera move so any MapLibre failure degrades to "map doesn't recentre" — never a crash.
    val all = listOfNotNull(customer, partner) + (route ?: emptyList())
    val distinct = all.distinctBy { it.latitude to it.longitude }
    runCatching {
        when {
            followCurrent && customer != null ->
                map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(customer.latitude, customer.longitude)))
            firstFit && distinct.size >= 2 -> {
                val bounds = LatLngBounds.Builder().apply {
                    distinct.forEach { include(LatLng(it.latitude, it.longitude)) }
                }.build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            }
            firstFit && distinct.isNotEmpty() -> {
                val p = distinct.first()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 13.0))
            }
        }
    }
}

private fun pointFeature(p: GeoPoint?): FeatureCollection =
    if (p == null) FeatureCollection.fromFeatures(emptyArray<Feature>())
    else FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)))

private fun lineFeature(pts: List<GeoPoint>?): FeatureCollection {
    if (pts.isNullOrEmpty()) return FeatureCollection.fromFeatures(emptyArray<Feature>())
    val line = LineString.fromLngLats(pts.map { Point.fromLngLat(it.longitude, it.latitude) })
    return FeatureCollection.fromFeature(Feature.fromGeometry(line))
}
