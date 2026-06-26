package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.LocationHelper
import com.example.ui.VedaDropViewModel
import com.example.ui.VedaDropMainShell
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity(), com.razorpay.PaymentResultWithDataListener {
  companion object {
    /** FCM notification channel id. Any code posting a push notification (e.g. a
     *  FirebaseMessagingService.onMessageReceived) MUST target this same id, and
     *  the channel must exist before the first notification is posted on API 26+. */
    const val FCM_CHANNEL_ID = "nikhatglow_default"

    /** §725 Batch-B — dedicated HIGH-importance channel for URGENT job alerts. Uses an
     *  ALARM-stream sound, bypasses DND and vibrates, so a partner cannot miss a job a
     *  customer needs right now. Targeted by [UrgentAlarmService]'s full-screen notification. */
    const val URGENT_CHANNEL_ID = "vedadrop_urgent"
  }

  /** §725 Batch-B — the live VM (set during composition) so onNewIntent can route a
   *  warm-start notification tap. Null before first composition / after teardown. */
  private var activeViewModel: VedaDropViewModel? = null

  override fun onNewIntent(newIntent: android.content.Intent) {
    super.onNewIntent(newIntent)
    setIntent(newIntent)
    if (newIntent.getBooleanExtra("open_urgent_offers", false)) {
      activeViewModel?.openUrgentOffers()
    }
    newIntent.getStringExtra("notif_booking_id")?.let { activeViewModel?.openBookingFromPush(it) }
  }

  // §746 — Razorpay Checkout result callbacks. Checkout.open(activity, …) routes the
  // payment outcome back to the Activity that launched it; we forward to the live VM,
  // which verifies the signature server-side and unlocks the ₹99 listing subscription.
  override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: com.razorpay.PaymentData?) {
    activeViewModel?.onRazorpayResult(
      orderId = paymentData?.orderId,
      paymentId = razorpayPaymentId ?: paymentData?.paymentId,
      signature = paymentData?.signature,
      error = null,
    )
  }

  override fun onPaymentError(code: Int, response: String?, paymentData: com.razorpay.PaymentData?) {
    activeViewModel?.onRazorpayResult(
      orderId = null, paymentId = null, signature = null,
      error = response ?: "Payment was cancelled or could not be completed.",
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // §746 — warm up Razorpay Checkout so the first open is snappy (no-op-safe).
    try { com.razorpay.Checkout.preload(applicationContext) } catch (_: Exception) {}
    // §750 — Firebase Analytics (Google Analytics). No-ops until google-services.json
    // is added (see app/build.gradle.kts §732/§750 notes); records app opens once ready.
    com.example.analytics.VedaDropAnalytics.init(applicationContext)
    com.example.analytics.VedaDropAnalytics.event("app_open")
    // Create the default notification channel up-front. On Android 8+ (the app's
    // minSdk is 26) a channel must exist before any notification can be shown,
    // otherwise FCM-delivered notifications are silently dropped.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(FCM_CHANNEL_ID) == null) {
        manager.createNotificationChannel(
          NotificationChannel(
            FCM_CHANNEL_ID,
            "Veda Drop",
            NotificationManager.IMPORTANCE_HIGH
          ).apply {
            description = "Booking updates, chat messages and reminders."
          }
        )
      }
      // §725 Batch-B — urgent-job channel: ALARM sound, max importance, bypass DND,
      // vibrate. A channel's sound/importance are LOCKED after creation, so this must
      // be configured here once (uninstall/reinstall to change).
      if (manager.getNotificationChannel(URGENT_CHANNEL_ID) == null) {
        val alarmUri = android.media.RingtoneManager
          .getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
          ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val alarmAttrs = android.media.AudioAttributes.Builder()
          .setUsage(android.media.AudioAttributes.USAGE_ALARM)
          .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        manager.createNotificationChannel(
          NotificationChannel(
            URGENT_CHANNEL_ID,
            "Urgent jobs",
            NotificationManager.IMPORTANCE_HIGH
          ).apply {
            description = "Loud alert when a customer needs a professional right away."
            if (alarmUri != null) setSound(alarmUri, alarmAttrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
          }
        )
      }
    }
    enableEdgeToEdge()
    setContent {
      // §738 — the ViewModel is created OUTSIDE the theme so the user-selected
      // light/dark/system mode (persisted in prefs, exposed as a StateFlow) can be
      // read and fed into MyApplicationTheme. Flipping the mode recomposes the whole
      // tree with the new colorScheme + palette.
      val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<VedaDropViewModel>()
      // §725 Batch-B — keep a handle so a warm-start onNewIntent (tapping the
      // urgent full-screen notification while the app is alive) can route too.
      activeViewModel = viewModel
      val themeMode by viewModel.themeMode.collectAsState()
      MyApplicationTheme(themeMode = themeMode) {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          // §687 — request location once on launch; on grant (or if already
          // granted) capture the device fix so "near me" discovery engages. The
          // app works fine if the user denies — discovery just isn't distance-sorted
          // and addresses fall back to manual entry.
          val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
          ) { granted ->
            if (granted.values.any { it }) viewModel.captureDeviceLocation()
          }
          LaunchedEffect(Unit) {
            // §703 — pull the admin-controlled app config on launch so feature
            // gates + role-based nav + policy copy reflect the server immediately.
            viewModel.loadAppConfig()
            // §710 P0-5 — register this device's FCM token (no-op if logged out) +
            // deep-link a cold-start that came from tapping a push.
            viewModel.registerFcmToken()
            intent?.getStringExtra("notif_booking_id")?.let { viewModel.openBookingFromPush(it) }
            // §725 Batch-B — a tapped urgent full-screen notification cold-starts here.
            if (intent?.getBooleanExtra("open_urgent_offers", false) == true) {
              viewModel.openUrgentOffers()
            }
            if (LocationHelper.hasPermission(this@MainActivity)) {
              viewModel.captureDeviceLocation()
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
              }
            } else {
              val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS)
              else
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
              permLauncher.launch(perms)
            }
          }
          VedaDropMainShell(viewModel = viewModel)
        }
      }
    }
  }
}
