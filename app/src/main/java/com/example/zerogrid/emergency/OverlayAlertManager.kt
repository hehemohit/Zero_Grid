package com.example.zerogrid.emergency

import android.content.Context
import android.graphics.PixelFormat
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.zerogrid.location.HazardAlert

/**
 * Manages the system-level draw-over-app overlay for hazard warnings.
 *
 * Uses [WindowManager.addView] with [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * so the alert appears on top of ANY app or the lock screen (API 26+).
 *
 * Alarm ringtone + vibration fire immediately on [show] and stop on [dismiss].
 */
object OverlayAlertManager {

    private const val TAG = "OverlayAlertManager"

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var ringtone: Ringtone? = null
    private var currentAlert: HazardAlert? = null

    /**
     * Returns true if [Settings.canDrawOverlays] is granted.
     */
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Shows the hazard overlay on top of all apps.
     * No-op if permission is not granted or alert is already showing.
     */
    fun show(context: Context, alert: HazardAlert) {
        if (!canDrawOverlays(context)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted — skipping overlay.")
            return
        }
        if (overlayView != null) {
            Log.d(TAG, "Overlay already visible — ignoring duplicate alert.")
            return
        }
        currentAlert = alert

        playAlarmSound(context)
        vibrate(context)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        // ComposeView needs a LifecycleOwner attached to the window to work correctly.
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                HazardOverlayContent(
                    alert      = alert,
                    onDismiss  = { dismiss() }
                )
            }
        }
        overlayView = composeView
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        wm.addView(composeView, params)
        Log.i(TAG, "Overlay shown for hazard: ${alert.eventId}")
    }

    /**
     * Dismisses the overlay, stops the alarm, and cancels vibration.
     */
    fun dismiss() {
        try {
            ringtone?.stop()
            ringtone = null

            overlayView?.let { windowManager?.removeView(it) }
            overlayView = null
            windowManager = null
            currentAlert  = null
            Log.i(TAG, "Overlay dismissed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay: ${e.message}")
        }
    }

    // ── Sound & Haptics ───────────────────────────────────────────────────────

    private fun playAlarmSound(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play alarm: ${e.message}")
        }
    }

    private fun vibrate(context: Context) {
        try {
            // Long-short-short SOS-style pattern
            val pattern = longArrayOf(0, 600, 200, 200, 200, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not vibrate: ${e.message}")
        }
    }
}

// ── Minimal LifecycleOwner for the ComposeView inside WindowManager ───────────

/**
 * A self-contained [LifecycleOwner] / [ViewModelStoreOwner] / [SavedStateRegistryOwner]
 * that can be attached to a [ComposeView] hosted in a [WindowManager] overlay
 * (which has no Activity/Fragment lifecycle by default).
 */
class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    fun onStart()   = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume()  = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onPause()   = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onStop()    = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onDestroy() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
}
