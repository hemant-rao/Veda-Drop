@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VedaDropRose
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * §725 — guided 3-photo face KYC.
 *
 * A full-screen CameraX (front camera) + ML Kit Face Detection flow that walks the
 * partner through three poses — FRONT, LEFT, RIGHT — and AUTO-CAPTURES each one only
 * after the head is actually held in that pose. ML Kit's `headEulerAngleY` (yaw)
 * tells us which way the head is turned; requiring the head to move from front →
 * left → right is a lightweight liveness check (a single still photo can't satisfy
 * all three steps). Returns the three captured bitmaps via [onComplete].
 *
 * No FileProvider / ImageCapture wiring: we grab the live preview frame with
 * `PreviewView.bitmap` at the moment the pose is confirmed, mirroring the existing
 * §706 selfie-thumbnail approach. The caller compresses each to a base64 data URL.
 */
private enum class FaceStep(val title: String, val instruction: String) {
    FRONT("Front", "Look straight at the camera and hold still"),
    LEFT("Left", "Slowly turn your head to your LEFT"),
    RIGHT("Right", "Slowly turn your head to your RIGHT"),
}

// Pose thresholds (degrees). FRONT needs a near-centred head; LEFT/RIGHT need a
// clear side turn. ML Kit reports yaw on the (display-mirrored) front-camera frame.
// If on a real device the LEFT/RIGHT prompts feel swapped, flip YAW_SIGN to -1 —
// that's the single knob that controls direction mapping.
private const val YAW_SIGN = 1
private const val FRONT_YAW_MAX = 12f
private const val FRONT_PITCH_MAX = 15f
private const val SIDE_YAW_MIN = 25f
private const val HOLD_FRAMES = 5   // consecutive good frames before auto-capture

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun FaceCaptureFlow(
    onComplete: (front: Bitmap, left: Bitmap, right: Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    var front by remember { mutableStateOf<Bitmap?>(null) }
    var left by remember { mutableStateOf<Bitmap?>(null) }
    var right by remember { mutableStateOf<Bitmap?>(null) }
    // Explicit MutableState so the background analyzer can read the live step.
    val stepState = remember { mutableStateOf(FaceStep.FRONT) }
    var detected by remember { mutableStateOf(false) }
    var poseGood by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Position your face inside the circle") }
    var pendingCapture by remember { mutableStateOf<FaceStep?>(null) }
    var flash by remember { mutableStateOf(false) }
    var camError by remember { mutableStateOf<String?>(null) }
    // Plain holder mutated from the analyzer thread (no recomposition needed).
    val holdCount = remember { intArrayOf(0) }

    // Confirmed pose → grab the current preview frame ON THE MAIN THREAD, flash, advance.
    LaunchedEffect(pendingCapture) {
        val s = pendingCapture ?: return@LaunchedEffect
        val bmp = previewView.bitmap
        if (bmp != null) {
            when (s) {
                FaceStep.FRONT -> front = bmp
                FaceStep.LEFT -> left = bmp
                FaceStep.RIGHT -> right = bmp
            }
            flash = true
            delay(350)
            flash = false
            stepState.value = when (s) {
                FaceStep.FRONT -> FaceStep.LEFT
                FaceStep.LEFT -> FaceStep.RIGHT
                FaceStep.RIGHT -> FaceStep.RIGHT
            }
        }
        holdCount[0] = 0
        pendingCapture = null
    }

    // All three captured → hand back to the caller.
    LaunchedEffect(front, left, right) {
        val f = front; val l = left; val r = right
        if (f != null && l != null && r != null) {
            delay(250)
            onComplete(f, l, r)
        }
    }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val preview = CameraXPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    detector.process(input)
                        .addOnSuccessListener { faces ->
                            val face = faces.maxByOrNull {
                                it.boundingBox.width() * it.boundingBox.height()
                            }
                            if (face == null) {
                                detected = false
                                poseGood = false
                                status = "No face detected — center your face"
                                holdCount[0] = 0
                            } else {
                                detected = true
                                val yaw = face.headEulerAngleY
                                val pitch = face.headEulerAngleX
                                val st = stepState.value
                                val good = when (st) {
                                    FaceStep.FRONT ->
                                        abs(yaw) <= FRONT_YAW_MAX && abs(pitch) <= FRONT_PITCH_MAX
                                    FaceStep.LEFT -> yaw * YAW_SIGN >= SIDE_YAW_MIN
                                    FaceStep.RIGHT -> yaw * YAW_SIGN <= -SIDE_YAW_MIN
                                }
                                poseGood = good
                                status = if (good) "Hold still…" else when (st) {
                                    FaceStep.FRONT -> "Look straight at the camera"
                                    FaceStep.LEFT -> "Turn your head to your LEFT"
                                    FaceStep.RIGHT -> "Turn your head to your RIGHT"
                                }
                                if (good && pendingCapture == null) {
                                    holdCount[0]++
                                    if (holdCount[0] >= HOLD_FRAMES) pendingCapture = st
                                } else if (!good) {
                                    holdCount[0] = 0
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                camError = e.message ?: "Camera unavailable on this device"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try { provider?.unbindAll() } catch (_: Exception) {}
            try { analysisExecutor.shutdown() } catch (_: Exception) {}
            try { detector.close() } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Face guide ring — turns green when the pose is good.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(0.8f)
                        .border(
                            width = 3.dp,
                            color = if (poseGood) SuccessGreen else Color.White.copy(alpha = 0.7f),
                            shape = CircleShape,
                        )
                )
            }

            if (flash) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
            }

            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("face_capture_cancel_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text("Face verification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FaceStep.values().forEach { s ->
                                val done = when (s) {
                                    FaceStep.FRONT -> front != null
                                    FaceStep.LEFT -> left != null
                                    FaceStep.RIGHT -> right != null
                                }
                                val active = stepState.value == s && !done
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = when {
                                        done -> SuccessGreen
                                        active -> VedaDropRose
                                        else -> Color.White.copy(alpha = 0.25f)
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (done) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        Text(s.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (!detected && camError == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Looking for your face…", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Text(
                            camError ?: status,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stepState.value.instruction,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * §728 (parity C1) — SINGLE-SHOT live start-selfie capture for the job-start flow.
 *
 * A lightweight sibling of [FaceCaptureFlow]: front camera + ML Kit face detection,
 * AUTO-CAPTURES one clear front-facing frame after a short hold (on-device
 * face-detected liveness — NOT server-side 1:1 face recognition, which would need a
 * face-match service). The partner captures this when starting a job; the caller
 * compresses the bitmap to a base64 data URL and sends it with the start-OTP.
 *
 * Reuses the exact CameraX + PreviewView.bitmap grab approach as the 3-pose flow.
 * [onCancel] aborts (the caller must NOT start the job).
 */
@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun SelfieCaptureFlow(
    onCapture: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    var detected by remember { mutableStateOf(false) }
    var poseGood by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Position your face inside the circle") }
    var capture by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var camError by remember { mutableStateOf<String?>(null) }
    val holdCount = remember { intArrayOf(0) }

    // Good pose held long enough → grab the live frame on the main thread, flash, return.
    LaunchedEffect(capture) {
        if (!capture || captured) return@LaunchedEffect
        val bmp = previewView.bitmap
        if (bmp != null) {
            captured = true
            flash = true
            delay(300)
            flash = false
            onCapture(bmp)
        } else {
            // No frame yet — let the analyzer try again on the next good hold.
            holdCount[0] = 0
            capture = false
        }
    }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val preview = CameraXPreview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    detector.process(input)
                        .addOnSuccessListener { faces ->
                            val face = faces.maxByOrNull {
                                it.boundingBox.width() * it.boundingBox.height()
                            }
                            if (face == null) {
                                detected = false
                                poseGood = false
                                status = "No face detected — center your face"
                                holdCount[0] = 0
                            } else {
                                detected = true
                                val good = abs(face.headEulerAngleY) <= FRONT_YAW_MAX &&
                                    abs(face.headEulerAngleX) <= FRONT_PITCH_MAX
                                poseGood = good
                                status = if (good) "Hold still…" else "Look straight at the camera"
                                if (good && !capture && !captured) {
                                    holdCount[0]++
                                    if (holdCount[0] >= HOLD_FRAMES) capture = true
                                } else if (!good) {
                                    holdCount[0] = 0
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                camError = e.message ?: "Camera unavailable on this device"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try { provider?.unbindAll() } catch (_: Exception) {}
            try { analysisExecutor.shutdown() } catch (_: Exception) {}
            try { detector.close() } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(0.8f)
                        .border(
                            width = 3.dp,
                            color = if (poseGood) SuccessGreen else Color.White.copy(alpha = 0.7f),
                            shape = CircleShape,
                        )
                )
            }

            if (flash) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
            }

            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("selfie_capture_cancel_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text("Start selfie", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!detected && camError == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Looking for your face…", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        Text(
                            camError ?: status,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "A live selfie is taken to confirm you've arrived for this job.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
