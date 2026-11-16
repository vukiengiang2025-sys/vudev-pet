package com.vudev.pet.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.vudev.pet.MainActivity
import com.vudev.pet.R
import com.vudev.pet.SettingsActivity
import com.vudev.pet.core.Behavior
import com.vudev.pet.core.Interaction
import com.vudev.pet.core.PetClock
import com.vudev.pet.core.PetEngine
import com.vudev.pet.core.PetState
import com.vudev.pet.core.ReminderKind
import com.vudev.pet.core.ReminderPolicy
import com.vudev.pet.core.SpeechContext
import com.vudev.pet.core.SpeechSelector
import com.vudev.pet.data.PetRepository
import com.vudev.pet.data.PetSettings
import com.vudev.pet.render.Effect
import com.vudev.pet.render.PetLooks
import com.vudev.pet.render.PetView
import com.vudev.pet.speech.PetSpeech
import com.vudev.pet.util.PetNotifications
import kotlin.math.abs
import kotlin.math.sign

/**
 * Foreground service sở hữu cửa sổ overlay của thú cưng.
 *
 * Trách nhiệm: vòng tick trạng thái, di chuyển, nhận touch, bong bóng thoại, menu vòng,
 * TTS, lưu trạng thái, và tạm dừng khi màn hình tắt. Không đọc nội dung ứng dụng khác,
 * không dùng Accessibility.
 */
class PetOverlayService : Service() {

    private lateinit var settings: PetSettings
    private lateinit var repo: PetRepository
    private lateinit var engine: PetEngine
    private lateinit var speech: PetSpeech
    private val selector = SpeechSelector()
    private val handler = Handler(Looper.getMainLooper())

    private var wm: WindowManager? = null
    private var petView: PetView? = null
    private var petParams: WindowManager.LayoutParams? = null
    private var bubble: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menu: LinearLayout? = null
    private var menuParams: WindowManager.LayoutParams? = null

    private var screenW = 0
    private var screenH = 0
    private var sizePx = 0
    private var petX = 0f
    private var petY = 0f
    private var targetX: Float? = null
    private var screenOn = true
    private var isSuspended = false
    private var lastSaveAt = 0L
    private var lastAutoSayAt = 0L
    private val reminders = ReminderPolicy()
    private var lastMood: Int = -1
    private var lastWalking = false

    private var dragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var pettingFlips = 0
    private var lastMoveDirection = 0
    private var gesture: GestureDetector? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (!screenOn) {
                handler.postDelayed(this, 2_000L)
                return
            }
            val now = System.currentTimeMillis()
            val tick = engine.tick(now)
            applyState(tick.behaviorChanged)
            if (tick.wokeUp) speechSay(SpeechContext.BACK, forceBubble = false)
            if (now - lastSaveAt > 20_000L) {
                save()
                lastSaveAt = now
            }
            maybeRemind(now)
            handler.postDelayed(this, 1_000L)
        }
    }

    private val mover = object : Runnable {
        override fun run() {
            if (!screenOn || petView == null) return
            val walking = engine.state.behavior == Behavior.WANDER || engine.state.behavior == Behavior.PLAY
            if (!walking) return
            val target = targetX
            if (target == null) {
                targetX = randomTarget()
            } else {
                val now = SystemClock.uptimeMillis()
                val dt = if (lastMoveAt == 0L) 0.033f else ((now - lastMoveAt).coerceAtMost(80L) / 1000f)
                lastMoveAt = now
                val dx = target - petX
                val step = WALK_SPEED_DP * resources.displayMetrics.density * dt
                if (abs(dx) <= step + 2f) {
                    petX = target
                    targetX = null
                } else {
                    petX += if (dx > 0f) step else -step
                    petView?.setFacingLeft(dx < 0f)
                }
                clampPosition()
                applyPetLayout()
                moveBubbleWithPet()
            }
            handler.postDelayed(this, 33L)
        }
    }

    private var lastMoveAt = 0L

    private val hideBubble = Runnable { bubble?.visibility = View.GONE }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    petView?.setPaused(true)
                    hideBubble.run()
                    hideMenu()
                    save()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    petView?.setPaused(false)
                    val now = System.currentTimeMillis()
                    engine.restore(engine.state, now)
                    applyState(true)
                    handler.removeCallbacks(ticker)
                    handler.post(ticker)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PetSettings(this)
        repo = PetRepository(this)
        speech = PetSpeech(this)
        speech.init()
        val saved = repo.load()
        val initial = saved ?: PetState(name = settings.petName, personality = settings.personality)
        engine = PetEngine(initial)
        engine.restore(initial.copy(name = settings.petName, personality = settings.personality), System.currentTimeMillis())

        startForeground(PetNotifications.ID_SERVICE, PetNotifications.foreground(this))
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        isRunning = true
        Log.i(TAG, "overlay service created; size=" + settings.sizeDp + "dp look=" + settings.lookId)
        createPetWindow()
        handler.post(ticker)
    }

    private fun createPetWindow(): Unit {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        this.wm = wm
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels
        sizePx = (settings.sizeDp * resources.displayMetrics.density).toInt()
        clampSize()

        val view = PetView(this).apply {
            look = PetLooks.byId(settings.lookId)
            alpha = settings.opacity
        }
        petView = view
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        petParams = params
        val savedPosition = settings.savedPosition()
        petX = if (savedPosition.first >= 0) savedPosition.first.toFloat() else (screenW - sizePx - dp(18)).toFloat()
        petY = if (savedPosition.second >= 0) savedPosition.second.toFloat() else (screenH - sizePx - dp(140)).toFloat()
        clampPosition()
        params.x = petX.toInt()
        params.y = petY.toInt()
        runCatching { wm.addView(view, params) }.onFailure {
            stopSelf()
            return
        }
        setupTouch(view)
        applyState(true)
        Log.i(TAG, "pet overlay window added at " + petX + "," + petY + " size=" + sizePx)
    }

    private fun setupTouch(view: PetView) {
        gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                interact(Interaction.TAP)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                speechSay(SpeechContext.IDLE_CHAT, forceBubble = true)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                showMenu()
            }
        })
        view.setOnTouchListener { _, event ->
            val g = gesture
            whenever(g) { it.onTouchEvent(event) }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Dừng đi lại khi ngón tay chạm vào thú để thú không "chạy trốn" khỏi tay.
                    handler.removeCallbacks(mover)
                    targetX = null
                    lastMoveAt = 0L
                    dragging = false
                    pettingFlips = 0
                    lastMoveDirection = 0
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dragStartX = petX
                    dragStartY = petY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val slop = dp(12).toFloat()
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        targetX = null
                        engine.interact(Interaction.DRAGGED, System.currentTimeMillis())
                        petView?.burst(Effect.SPARKLES)
                    }
                    if (dragging) {
                        petX = dragStartX + dx
                        petY = dragStartY + dy
                        clampPosition()
                        applyPetLayout()
                        moveBubbleWithPet()
                    } else {
                        val dir = sign(dx).toInt()
                        if (dir != 0 && lastMoveDirection != 0 && dir != lastMoveDirection) pettingFlips++
                        if (dir != 0) lastMoveDirection = dir
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        dragging = false
                        settings.savePosition(petX.toInt(), petY.toInt())
                        save()
                    } else if (pettingFlips >= 2) {
                        interact(Interaction.PET)
                    }
                    pettingFlips = 0
                    // Tiếp tục hành vi đi lại nếu thú đang trong trạng thái đi dạo/chơi.
                    applyState(false)
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    pettingFlips = 0
                }
            }
            true
        }
    }

    private inline fun whenever(value: GestureDetector?, block: (GestureDetector) -> Unit) {
        if (value != null) block(value)
    }

    private fun applyState(behaviorChanged: Boolean) {
        val view = petView ?: return
        val state = engine.state
        view.setMood(state.mood)
        val walking = state.behavior == Behavior.WANDER || state.behavior == Behavior.PLAY
        if (walking != lastWalking) {
            lastWalking = walking
            view.setWalking(walking)
        }
        if (walking) {
            if (behaviorChanged || targetX == null) targetX = randomTarget()
            handler.removeCallbacks(mover)
            lastMoveAt = 0L
            handler.post(mover)
        } else {
            targetX = null
            handler.removeCallbacks(mover)
            if (state.behavior == Behavior.SULK) view.burst(Effect.ANGER)
        }
        if (behaviorChanged) {
            val now = System.currentTimeMillis()
            // Tự nói tối đa mỗi 60 giây để không spam giọng đọc.
            if (now - lastAutoSayAt > 60_000L) {
                when (state.behavior) {
                    Behavior.TALK -> {
                        lastAutoSayAt = now
                        speechSay(SpeechContext.IDLE_CHAT, forceBubble = false)
                    }
                    Behavior.SULK -> {
                        lastAutoSayAt = now
                        speechSay(SpeechContext.SAD, forceBubble = false)
                    }
                    Behavior.SLEEP -> view.burst(Effect.ZZZ)
                    else -> Unit
                }
            } else if (state.behavior == Behavior.SLEEP) {
                view.burst(Effect.ZZZ)
            }
        }
    }

    private fun randomTarget(): Float {
        val margin = dp(10).toFloat()
        return margin + kotlin.random.Random.nextFloat() * (screenW - sizePx - margin * 2).coerceAtLeast(1f)
    }

    private fun clampPosition() {
        petX = petX.coerceIn(0f, (screenW - sizePx).coerceAtLeast(0).toFloat())
        petY = petY.coerceIn(0f, (screenH - sizePx).coerceAtLeast(0).toFloat())
    }

    private fun applyPetLayout() {
        val view = petView ?: return
        val params = petParams ?: return
        params.x = petX.toInt()
        params.y = petY.toInt()
        runCatching { wm?.updateViewLayout(view, params) }
        moveBubbleWithPet()
    }

    private fun moveBubbleWithPet() {
        val b = bubble ?: return
        val params = bubbleParams ?: return
        if (b.visibility != View.VISIBLE) return
        params.x = (petX + sizePx / 2f - dp(110)).toInt().coerceAtLeast(dp(4))
        params.y = (petY - dp(58)).toInt().coerceAtLeast(dp(4))
        runCatching { wm?.updateViewLayout(b, params) }
    }

    private fun interact(interaction: Interaction) {
        val result = engine.interact(interaction, System.currentTimeMillis())
        save()
        val view = petView
        view?.setMood(result.state.mood)
        when (interaction) {
            Interaction.PET -> view?.burst(Effect.HEARTS)
            Interaction.FEED -> view?.burst(Effect.CRUMBS)
            Interaction.PLAY -> view?.burst(Effect.SPARKLES)
            Interaction.BATHE -> view?.burst(Effect.BUBBLES)
            Interaction.TAP -> view?.burst(Effect.SPARKLES)
            else -> view?.burst(Effect.SPARKLES)
        }
        speechSay(result.speech, forceBubble = true)
    }

    private fun speechSay(context: SpeechContext, forceBubble: Boolean) {
        val now = System.currentTimeMillis()
        val hour = PetClock.hourOfDay(now)
        if (settings.isQuietNow(hour)) return
        val phrase = selector.pick(context, engine.state.personality, now) ?: return
        if (forceBubble || settings.bubbleOn) showBubble(phrase)
        if (settings.soundOn) {
            speech.speak(phrase)
            petView?.setTalking(true)
            handler.postDelayed({ petView?.setTalking(false) }, 2_500L)
        }
    }

    private fun showBubble(text: String) {
        val wm = wm ?: return
        var b = bubble
        if (b == null) {
            val view = TextView(this).apply {
                textSize = 14f
                setTextColor(Color.parseColor("#16233F"))
                background = bubbleBackground()
                setPadding(dp(12), dp(8), dp(12), dp(8))
                maxLines = 3
                gravity = Gravity.CENTER
            }
            val params = WindowManager.LayoutParams(
                dp(220),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            runCatching { wm.addView(view, params) }.onFailure { return }
            bubble = view
            bubbleParams = params
            b = view
        }
        b.text = text
        b.visibility = View.VISIBLE
        moveBubbleWithPet()
        handler.removeCallbacks(hideBubble)
        handler.postDelayed(hideBubble, 4_200L)
    }

    private fun showMenu() {
        if (menu != null) return
        val wm = wm ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = menuBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val items = listOf(
            getString(R.string.action_feed) to { interact(Interaction.FEED) },
            getString(R.string.action_play) to { interact(Interaction.PLAY) },
            getString(R.string.action_bathe) to { interact(Interaction.BATHE) },
            getString(R.string.action_pet) to { interact(Interaction.PET) },
            getString(R.string.action_status) to { openActivity(MainActivity::class.java) },
            getString(R.string.action_settings) to { openActivity(SettingsActivity::class.java) },
            getString(R.string.action_hide) to { stopFromUser() },
            getString(R.string.action_close) to { hideMenu() },
        )
        items.forEach { (label, action) ->
            panel.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.parseColor("#16233F"))
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isClickable = true
                setOnClickListener {
                    action()
                    if (label != getString(R.string.action_close)) hideMenu()
                }
            })
        }
        val params = WindowManager.LayoutParams(
            dp(220),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - dp(220) - dp(16)).coerceAtLeast(dp(8))
            y = (screenH / 2 - dp(220)).coerceAtLeast(dp(8))
        }
        runCatching { wm.addView(panel, params) }.onFailure { return }
        menu = panel
        menuParams = params
    }

    private fun hideMenu() {
        val view = menu ?: return
        menu = null
        runCatching { wm?.removeView(view) }
    }

    private fun openActivity(clazz: Class<*>) {
        runCatching {
            startActivity(Intent(this, clazz).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun stopFromUser() {
        settings.overlayEnabled = false
        stopSelf()
    }

    private fun maybeRemind(now: Long) {
        val hour = PetClock.hourOfDay(now)
        val quiet = settings.isQuietNow(hour)
        val name = engine.state.name
        when (reminders.poll(engine.state, now, quiet)) {
            ReminderKind.HUNGRY -> PetNotifications.hungry(this, name)
            ReminderKind.SAD -> PetNotifications.sad(this, name)
            ReminderKind.NEGLECTED -> PetNotifications.neglected(this, name)
            null -> Unit
        }
    }

    private fun save() {
        repo.save(engine.snapshot(System.currentTimeMillis()))
        com.vudev.pet.widget.PetWidgetProvider.refresh(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                settings.overlayEnabled = false
                stopSelf()
            }
            ACTION_CONFIG_CHANGED -> applyConfig()
            ACTION_SUSPEND -> suspendOverlay()
            ACTION_RESUME -> resumeOverlay()
            else -> Unit
        }
        return START_NOT_STICKY
    }

    /** Tạm ẩn thú khi mở mini-game để không che màn hình chơi; service vẫn foreground. */
    private fun suspendOverlay() {
        if (isSuspended) return
        isSuspended = true
        handler.removeCallbacksAndMessages(null)
        save()
        val view = petView
        petView = null
        if (view != null) runCatching { wm?.removeView(view) }
        val b = bubble
        bubble = null
        if (b != null) runCatching { wm?.removeView(b) }
        hideMenu()
        Log.i(TAG, "overlay suspended for mini-game")
    }

    private fun resumeOverlay() {
        if (!isSuspended) return
        isSuspended = false
        // Mini-game (hoặc màn hình khác) có thể vừa cộng thưởng và ghi đè trạng thái:
        // nạp lại từ đĩa để không ghi đè phần thưởng bằng trạng thái cũ trong RAM.
        repo.load()?.let { engine.restore(it, System.currentTimeMillis()) }
        handler.post(ticker)
        if (petView == null) createPetWindow()
        Log.i(TAG, "overlay resumed after mini-game")
    }

    private fun applyConfig() {
        val view = petView ?: return
        val saved = repo.load()
        if (saved != null) engine.restore(saved, System.currentTimeMillis())
        engine.setName(settings.petName)
        engine.setPersonality(settings.personality, System.currentTimeMillis())
        view.look = PetLooks.byId(settings.lookId)
        view.alpha = settings.opacity
        sizePx = (settings.sizeDp * resources.displayMetrics.density).toInt()
        clampSize()
        val params = petParams ?: return
        params.width = sizePx
        params.height = sizePx
        clampPosition()
        applyPetLayout()
        applyState(true)
    }

    private fun clampSize() {
        sizePx = sizePx.coerceIn(dp(80), screenW.coerceAtMost(screenH) * 2 / 3)
    }

    override fun onDestroy() {
        isRunning = false
        save()
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { petView?.let { wm?.removeView(it) } }
        runCatching { bubble?.let { wm?.removeView(it) } }
        runCatching { menu?.let { wm?.removeView(it) } }
        speech.shutdown()
        stopForeground(true)
        Log.i(TAG, "overlay service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun bubbleBackground(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(Color.WHITE)
        setStroke(dp(1), Color.parseColor("#DCE3F5"))
    }

    private fun menuBackground(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(20).toFloat()
        setColor(Color.WHITE)
        setStroke(dp(1), Color.parseColor("#DCE3F5"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "PetOverlay"
        const val ACTION_START = "com.vudev.pet.action.START"
        const val ACTION_STOP = "com.vudev.pet.action.STOP"
        const val ACTION_CONFIG_CHANGED = "com.vudev.pet.action.CONFIG_CHANGED"
        const val ACTION_SUSPEND = "com.vudev.pet.action.SUSPEND"
        const val ACTION_RESUME = "com.vudev.pet.action.RESUME"
        private const val WALK_SPEED_DP = 55f

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
