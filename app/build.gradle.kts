plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.relab_tool"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.relab.controlcenter"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.4.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // Java 17 enables newer bytecode optimisations used by the Compose compiler
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    // Compose compiler settings (Kotlin 2.0+ plugin block)
    composeCompiler {
        // Strong-skipping lets the compiler skip recomposition even for unstable types
        // when inputs are pointer-equal — the single biggest easy win for recomposition cost
        enableStrongSkippingMode = true
        // Emit per-build reports so you can audit composable skippability
        reportsDestination = layout.buildDirectory.dir("compose_reports")
        metricsDestination = layout.buildDirectory.dir("compose_metrics")
        // Tell the compiler that your domain model types and ImmutableList are stable
        stabilityConfigurationFile = rootProject.file("stability_config.conf")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    // Core Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.animation)

    // Material 3 (M3 Expressive tokens included in BOM 2025.05+)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material.icons.extended)

    // Google Material (XML-side, for theming compatibility)
    implementation(libs.com.google.android.material)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1") // Changed line
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    // Navigation & DI
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // LiteRT (TensorFlow Lite Successor)
    implementation("com.google.ai.edge.litert:litert:2.1.0")

    // Hybrid View support
    implementation("androidx.fragment:fragment-compose:1.8.6")
    implementation("androidx.compose.ui:ui-viewbinding")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Image loading — Coil
    implementation(libs.coil.compose)

    // Networking
    implementation(libs.okhttp)

    // ── M3 Expressive & performance additions ────────────────────────────────────
    // Shape morphing (RoundedPolygon / Morph / MorphPolygonShape)
    implementation(libs.androidx.graphics.shapes)
    // JankStats — frame-timing regression detector wired to analytics
    implementation(libs.androidx.metrics.performance)
    // ImmutableList / ImmutableMap — stable collection types for Compose stability
    implementation(libs.kotlinx.collections.immutable)
    // Baseline Profile installer — pre-compiles hot paths, ~30-40% faster cold start
    implementation(libs.androidx.profileinstaller)

    // Graphics path (existing)
    implementation("androidx.graphics:graphics-path:1.0.1")

    // CIT Test dependencies
    val camerax_version = "1.4.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.biometric:biometric:1.1.0")

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug only — never leaks into release APK
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    releaseImplementation(libs.androidx.compose.ui.tooling.preview)
}