plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.glamgo.pxrjty"
    // §706 — the app uses java.time (LocalDate/DateTimeFormatter/Instant) across
    // several screens, which requires API 26+. minSdk was 24, so on API 24-25 it
    // crashed at runtime (NoClassDefFoundError). Raising to 26 makes java.time
    // legal everywhere (covers ~99% of active devices). ALTERNATIVE if API 24-25
    // support is needed: keep minSdk 24 and enable core-library desugaring
    // (isCoreLibraryDesugaringEnabled = true + coreLibraryDesugaring(desugar_jdk_libs)).
    minSdk = 26
    targetSdk = 36
    // §812 — first real version bump (was stuck at 1/"1.0" through §700-§805;
    // Play rejects an upload whose versionCode isn't greater than the last one).
    versionCode = 2
    versionName = "1.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)  // §687 — runtime location permission helper
  implementation(libs.androidx.activity.compose)
  // §725 — CameraX (preview + frame analysis) + ML Kit Face Detection drive the
  // guided 3-photo face KYC (FaceCaptureFlow): head-pose (Euler-Y) detection
  // auto-captures Front/Left/Right views with a built-in liveness check.
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.face.detection)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.messaging)  // §710 P0-5 — FCM push (see FCM_PAYMENTS_SETUP.md for google-services.json + plugin)
  implementation(libs.firebase.analytics)  // §750 — Google Analytics (auto-inits from google-services.json; see VedaDropAnalytics)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)  // §687 — FusedLocationProviderClient (device GPS)
  // §778 — AdMob removed: Veda Drop is intentionally AD-FREE (founder directive).
  // Ads live only in the OdioBook utility apps (Early Rover / Dig Deep / Xello Mind).
  // §770 — Phone Number Hint API (one-tap SIM-number chooser, NO permission). Used for
  // first-time, OTP-free phone verification: confirms the number being registered is the
  // SIM physically in this device. Direct coordinate (like the ads/Razorpay lines above)
  // to avoid a version-catalog edit; bump if Gradle can't resolve it.
  implementation("com.google.android.gms:play-services-auth:21.3.0")
  implementation(libs.maplibre.android)  // §690 — MapLibre GL engine + OpenFreeMap free vector tiles (live map; no proprietary tile SDK)
  implementation(libs.retrofit)
  // §746 — Razorpay Checkout Android SDK: the partner ₹99/mo listing subscription is
  // the app's ONLY in-app payment. Reuses OdioBook's Razorpay merchant account — the
  // server (/partner/subscription/checkout) returns the order_id + key_id, so NO key
  // is hard-coded here. Bump the version if Gradle can't resolve it.
  implementation("com.razorpay:checkout:1.6.39")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// §732 — FCM closed-app push. The client (VedaDropMessagingService), token
// registration, and the backend sender are ALL already in place; the only missing
// piece is Firebase credentials. We apply the Google Services plugin ONLY when
// google-services.json is present, so the project keeps building before Firebase is
// configured (this `if` is false today → no-op, build unchanged). To enable push:
//   1. Add app/google-services.json from the Firebase console (package
//      com.aistudio.glamgo.pxrjty).
//   2. Put the plugin on the build classpath — in gradle/libs.versions.toml [plugins]:
//        google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
//      and in the ROOT build.gradle.kts plugins { }:
//        alias(libs.plugins.google.services) apply false
//   3. Set the backend FCM service-account (GLAMGO_FCM_CREDENTIALS) + turn on the
//      `fcm_push` flag, then rebuild. Full steps in FCM_SETUP.md.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

tasks.register("revertButtons") {
    doLast {
        val uiDir = file("src/main/java/com/example/ui")
        uiDir.walk().filter { it.extension == "kt" }.forEach { file ->
            var text = file.readText()
            
            val newText = text.replace("shape = Shapes.medium, ", "")
                              .replace("shape = Shapes.medium,", "")
                              .replace("import com.example.ui.theme.Shapes\n", "")
            
            if (text != newText) {
                file.writeText(newText)
            }
        }
    }
}
