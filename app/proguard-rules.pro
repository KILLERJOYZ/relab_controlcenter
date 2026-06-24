# Add project specific ProGuard rules here.

# ── Preserve line numbers for crash stack traces ──────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Compose runtime & annotations ─────────────────────────────────────────────
# The Compose compiler embeds metadata that the runtime reads at startup.
# Stripping these causes crashes in minified release builds.
-keep class androidx.compose.** { *; }
-keepclassmembers class * { @androidx.compose.runtime.Composable *; }

# Preserve Compose UI stability annotations used by strong-skipping mode
-keepclassmembers class * {
    @androidx.compose.runtime.Stable *;
    @androidx.compose.runtime.Immutable *;
}

# ── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ── Kotlinx serialization (if used transitively) ──────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Room & WorkManager Database ────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * implements androidx.startup.Initializer {
    <init>();
}

# ── JNI / Native code ──────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.example.relab_tool.benchmark.domain.engine.BenchmarkNativeBridge { *; }

# ── AndroidX Tracing & Test Runner ────────────────────────────────────────────
-keep class androidx.tracing.** { *; }
-keep class androidx.test.** { *; }
-dontwarn androidx.test.**

# ── Kotlin Stdlib ─────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Benchmark Engines & Scoring ───────────────────────────────────────────────
-keep class com.example.relab_tool.benchmark.** { *; }



