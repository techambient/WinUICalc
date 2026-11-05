plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.winuicalc"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.ambient.winuicalc"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Retain only English string resources to strip unused localized strings
    androidResources {
      localeFilters += setOf("en")
    }

    // Strictly lock native binaries (.so files) to 64-bit ARM architecture (arm64-v8a) only
    ndk {
      abiFilters += setOf("arm64-v8a")
    }
  }

  signingConfigs {
    val debugKeystoreFile = file("${rootDir}/debug.keystore")
    if (debugKeystoreFile.exists()) {
      create("debugConfig") {
        storeFile = debugKeystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      // Maximum R8 code shrinking, obfuscation, and dead-code elimination
      isMinifyEnabled = true
      // Strips unused resources and raw layouts from dependencies
      isShrinkResources = true
      // Re-compresses PNG assets during build time
      isCrunchPngs = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      // Maximum R8 code shrinking and dead-code elimination for debug build as well
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (file("${rootDir}/debug.keystore").exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }

  // Aggressive packaging rules to strip non-essential library metadata, notices, and Kotlin modules
  packaging {
    resources {
      excludes += setOf(
        "META-INF/*.version",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*",
        "META-INF/*.kotlin_module",
        "META-INF/services/*",
        "META-INF/com.android.tools/*",
        "META-INF/androidx/*",
        "**.readme",
        "**.proto",
        "DebugProGuardRules.bin",
        "**/*.kotlin_builtins",
        "**/*.kotlin_metadata"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    buildConfig = false
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
}

// Strip OpenCensus telemetry and tracker modules completely across all configurations
configurations.all {
  exclude(group = "io.opencensus", module = "opencensus-api")
  exclude(group = "io.opencensus", module = "opencensus-proto")
}