package com.ntdpz.pie.feature.pet.overlay

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import com.ntdpz.pie.core.designsystem.component.CompanionAnimation
import com.ntdpz.pie.core.designsystem.component.CompanionSpriteView
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetPreferences
import com.ntdpz.pie.core.domain.pet.PetRepository
import com.ntdpz.pie.core.domain.pet.PetReactions
import com.ntdpz.pie.core.domain.pet.PetBehavior
import com.ntdpz.pie.feature.pet.animation
import com.ntdpz.pie.feature.pet.label
import com.ntdpz.pie.feature.pet.PetNotificationImages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/** Feature-owned runtime; the app subclass supplies dependencies only. Never reads diary data. */
abstract class PetOverlayService : Service(), Application.ActivityLifecycleCallbacks {
    protected abstract val petRepository: PetRepository
    protected abstract val preferenceWriteScope: CoroutineScope
    protected abstract val overlayStatus: PetOverlayStatus
    protected abstract val notificationImages: PetNotificationImages
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private var sprite: CompanionSpriteView? = null
    private var bitmap: Bitmap? = null
    private var reactionBitmap: Bitmap? = null
    private var preferences = PetPreferences()
    private var ready = false
    private var attached = false
    private var foreground = true
    private val started = mutableSetOf<Activity>()
    private var lastInteraction = SystemClock.elapsedRealtime()
    private val reactions = PetReactions()
    private var dragging = false
    private var receiverRegistered = false
    private val params by lazy {
        WindowManager.LayoutParams(
            (128 * resources.displayMetrics.density).roundToInt(),
            (128 * resources.displayMetrics.density).roundToInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; title = "PIE companion" }
    }
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshVisibility() }
    }

    override fun onCreate() {
        super.onCreate()
        application.registerActivityLifecycleCallbacks(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screen, filter)
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForeground(731, notification())
        } catch (_: Exception) {
            fail("เปิดบริการ PIE ไม่สำเร็จ")
            return START_NOT_STICKY
        }
        // A startForegroundService request must be acknowledged even if permission
        // was revoked between the UI check and this callback.
        if (!Settings.canDrawOverlays(this)) {
            overlayStatus.failed("กรุณาอนุญาตให้ PIE แสดงทับแอปอื่นแล้วลองอีกครั้ง")
            stopSelf()
            return START_NOT_STICKY
        }
        if (bitmap == null && sprite == null) {
            overlayStatus.starting()
            load()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("pie_companion", "PIE นอกแอป", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 731, Intent(this, javaClass).setAction("stop"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 732, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return Notification.Builder(this, "pie_companion")
            .setSmallIcon(notificationImages.smallIcon)
            .setLargeIcon(notificationImages.head())
            .setColor(notificationImages.color)
            .setContentTitle("PIE นอกแอปเปิดอยู่")
            .setContentText("น้องจะปรากฏเมื่อออกจาก PIE · กดค้างที่น้องเพื่อปิด")
            .setStyle(Notification.BigTextStyle().bigText("น้องจะปรากฏเมื่อออกจาก PIE · กดค้างที่น้องเพื่อปิด"))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "หยุด PIE", stop).build())
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).build()
    }

    private var loading = false
    private fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                val (atlas, reactionAtlas) = withContext(Dispatchers.IO) {
                    val options = BitmapFactory.Options().apply { inScaled = false }
                    requireNotNull(BitmapFactory.decodeResource(resources, com.ntdpz.pie.core.designsystem.R.drawable.pie_companion, options)) to
                        requireNotNull(BitmapFactory.decodeResource(resources, com.ntdpz.pie.core.designsystem.R.drawable.pie_reactions, options))
                }
                bitmap = atlas
                reactionBitmap = reactionAtlas
                sprite = CompanionSpriteView(this@PetOverlayService, atlas, reactionAtlas).apply {
                    contentDescription = "PIE นอกแอป แตะเพื่อทักทาย กดค้างเพื่อปิด"
                    setOnClickListener { reactToTouch() }
                    setOnLongClickListener { stopSelf(); true }
                }
                installDragging()
                launch {
                    try {
                        petRepository.preferences.collect {
                            preferences = it
                            ready = true
                            sprite?.reducedMotion = it.reducedMotion
                            if (!dragging) place(it.position)
                            refreshVisibility()
                            if (overlayStatus.state.value.phase != PetOverlayPhase.Failed) overlayStatus.running()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        fail("เปิดการตั้งค่า PIE ไม่สำเร็จ กรุณาลองใหม่")
                    }
                }
                while (true) {
                    refreshVisibility()
                    if (attached) {
                        val now = SystemClock.elapsedRealtime()
                        var selected = reactions.selection?.takeIf { it.isActive(now) }
                        if (selected == null && !preferences.reducedMotion && now - lastInteraction < PetBehavior.RestAfterMillis) {
                            selected = reactions.quiet(now)
                        }
                        val animation = when {
                            selected != null -> selected.action.animation()
                            now - lastInteraction >= PetBehavior.RestAfterMillis -> CompanionAnimation.Rest
                            else -> CompanionAnimation.Idle
                        }
                        sprite?.animation = animation
                        val description = when {
                            selected != null -> "ทักทาย ${selected.action.label()}"
                            animation == CompanionAnimation.Rest -> "พัก"
                            else -> "อยู่เป็นเพื่อน"
                        }
                        sprite?.contentDescription = "PIE นอกแอป · $description · กดค้างเพื่อปิด"
                    }
                    delay(500)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                fail("เปิด PIE นอกแอปไม่สำเร็จ ลองใหม่จากการตั้งค่า PIE")
            }
        }
    }

    private fun refreshVisibility() {
        if (!Settings.canDrawOverlays(this)) {
            overlayStatus.failed("กรุณาอนุญาตให้ PIE แสดงทับแอปอื่นแล้วลองอีกครั้ง")
            stopSelf()
            return
        }
        val interactive = getSystemService(PowerManager::class.java).isInteractive
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val show = ready && !foreground && interactive && !locked
        val view = sprite ?: return
        try {
            if (show && !attached) {
                place(preferences.position)
                windows.addView(view, params)
                attached = true
                lastInteraction = SystemClock.elapsedRealtime()
                reactions.reset(lastInteraction)
            } else if (!show && attached) {
                windows.removeViewImmediate(view)
                attached = false
                dragging = false
            }
        } catch (_: Exception) { fail("แสดง PIE ไม่สำเร็จ กรุณาตรวจสิทธิ์แสดงทับแอปอื่น") }
    }

    private fun reactToTouch() {
        lastInteraction = SystemClock.elapsedRealtime()
        val selected = reactions.touch(lastInteraction)
        sprite?.apply {
            contentDescription = "PIE นอกแอป · ทักทาย ${selected.action.label()} · กดค้างเพื่อปิด"
            playReaction(selected.action.animation())
        }
    }

    @Suppress("DEPRECATION")
    private fun bounds(): Pair<Int, Int> {
        val size = android.graphics.Point()
        windows.defaultDisplay.getSize(size)
        return (size.x - params.width).coerceAtLeast(0) to (size.y - params.height).coerceAtLeast(0)
    }

    private fun place(position: PetPosition) {
        val (maxX, maxY) = bounds()
        params.x = (position.x * maxX).roundToInt()
        params.y = (position.y * maxY).roundToInt()
        if (attached) windows.updateViewLayout(sprite, params)
    }

    @Suppress("ClickableViewAccessibility")
    private fun installDragging() {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var downAt = 0L
        var longPressed = false
        val longPress = Runnable {
            if (!dragging) {
                longPressed = true
                sprite?.performLongClick()
            }
        }
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        sprite?.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                // Only a start-of-touch signal. Do not read/store its coordinates,
                // inspect the underlying app, or intercept that app's gesture.
                MotionEvent.ACTION_OUTSIDE -> reactToTouch()
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    downAt = SystemClock.elapsedRealtime()
                    dragging = false
                    longPressed = false
                    view.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) {
                        dragging = true
                        view.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        val (maxX, maxY) = bounds()
                        params.x = (startX + dx).roundToInt().coerceIn(0, maxX)
                        params.y = (startY + dy).roundToInt().coerceIn(0, maxY)
                        try { windows.updateViewLayout(view, params) } catch (_: Exception) { stopSelf() }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPress)
                    lastInteraction = SystemClock.elapsedRealtime()
                    if (dragging) {
                        val (maxX, maxY) = bounds()
                        val position = PetPosition(if (maxX > 0) params.x.toFloat() / maxX else 0f, if (maxY > 0) params.y.toFloat() / maxY else 0f)
                        preferences = preferences.copy(position = position)
                        reactions.reset(lastInteraction)
                        preferenceWriteScope.launch {
                            try { petRepository.setPosition(position) }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { Toast.makeText(applicationContext, "จำตำแหน่ง PIE ไม่สำเร็จ", Toast.LENGTH_SHORT).show() }
                        }
                    } else if (!longPressed) {
                        if (lastInteraction - downAt >= ViewConfiguration.getLongPressTimeout()) view.performLongClick()
                        else view.performClick()
                    }
                    dragging = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    dragging = false
                    place(preferences.position)
                }
            }
            true
        }
    }

    private fun fail(message: String) {
        overlayStatus.failed(message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        stopSelf()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dragging = false
        place(preferences.position)
    }

    override fun onActivityStarted(activity: Activity) { started.add(activity); foreground = true; refreshVisibility() }
    override fun onActivityStopped(activity: Activity) { started.remove(activity); foreground = started.isNotEmpty(); refreshVisibility() }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) { started.remove(activity) }
    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        application.unregisterActivityLifecycleCallbacks(this)
        if (receiverRegistered) unregisterReceiver(screen)
        scope.cancel()
        if (attached) { windows.removeViewImmediate(sprite); attached = false }
        sprite = null
        bitmap?.recycle()
        bitmap = null
        reactionBitmap?.recycle()
        reactionBitmap = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        overlayStatus.stopped()
        super.onDestroy()
    }
}
