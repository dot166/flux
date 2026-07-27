package com.nononsenseapps.feeder.ui.compose.utils

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gsa.overlay.NexusOverlay

val LocalWindowSizeMetrics: ProvidableCompositionLocal<WindowSizeClass> =
    compositionLocalOf { error("Missing WindowSize container!") }

val LocalWindowSize: ProvidableCompositionLocal<DpSize> =
    compositionLocalOf { error("Missing WindowMetrics container!") }

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun Activity.withWindowSize(content: @Composable () -> Unit) {
    val windowSizeclass = calculateWindowSizeClass(activity = this)

    CompositionLocalProvider(LocalWindowSizeMetrics provides windowSizeclass) {
        content()
    }
}

@Composable
fun Activity.withWindowMetrics(content: @Composable () -> Unit) {
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
    val size =
        with(density) {
            metrics.bounds
                .toComposeRect()
                .size
                .toDpSize()
        }
    CompositionLocalProvider(LocalWindowSize provides size) {
        content()
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NexusOverlay.withWindowSize(content: @Composable () -> Unit) {
    val windowSizeclass = calculateWindowSizeClass(overlay = this)

    CompositionLocalProvider(LocalWindowSizeMetrics provides windowSizeclass) {
        content()
    }
}

@ExperimentalMaterial3WindowSizeClassApi
@Composable
fun calculateWindowSizeClass(overlay: NexusOverlay): WindowSizeClass {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use overlay#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = overlay.windowManager?.currentWindowMetrics
    val size = with(density) { metrics?.bounds?.toComposeRect()?.size?.toDpSize() }
    return WindowSizeClass.calculateFromSize(size ?: DpSize.Zero)
}

@Composable
fun NexusOverlay.withWindowMetrics(content: @Composable () -> Unit) {
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = windowManager?.currentWindowMetrics
    val size =
        with(density) {
            metrics?.bounds
                ?.toComposeRect()
                ?.size
                ?.toDpSize()
        } ?: DpSize.Zero
    CompositionLocalProvider(LocalWindowSize provides size) {
        content()
    }
}

@Composable
fun WithPreviewWindowSize(
    windowSizeclass: WindowSizeClass,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWindowSizeMetrics provides windowSizeclass) {
        content()
    }
}

@Composable
fun isCompactLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        LocalWindowSizeMetrics.current.heightSizeClass == WindowHeightSizeClass.Compact

@Composable
fun isCompactDevice(): Boolean {
    val windowSize = LocalWindowSizeMetrics.current
    return windowSize.heightSizeClass == WindowHeightSizeClass.Compact ||
        windowSize.widthSizeClass == WindowWidthSizeClass.Compact
}

enum class ScreenType {
    DUAL,
    SINGLE,
}

fun getScreenType(windowSize: WindowSizeClass) =
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> ScreenType.SINGLE
        else -> ScreenType.DUAL
    }
