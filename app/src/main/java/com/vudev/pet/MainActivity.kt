package com.vudev.pet

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.vudev.pet.core.Behavior
import com.vudev.pet.core.Interaction
import com.vudev.pet.core.Mood
import com.vudev.pet.core.PetEngine
import com.vudev.pet.core.PetState
import com.vudev.pet.core.SpeechContext
import com.vudev.pet.core.SpeechSelector
import com.vudev.pet.data.PetRepository
import com.vudev.pet.data.PetSettings
import com.vudev.pet.render.Effect
import com.vudev.pet.render.PetLooks
import com.vudev.pet.render.PetView
import com.vudev.pet.service.PetOverlayService
import com.vudev.pet.speech.PetSpeech

/**
 * Màn hình chính: xem trước thú, chăm sóc, bật/tắt overlay, vào cài đặt.
 * Không dùng AndroidX/Compose để giữ APK nhỏ và build nhẹ trên máy 4 GB.
 */
class MainActivity : Activity() {

    private lateinit var settings: PetSettings
    private lateinit var repo: PetRepository
    private lateinit var engine: PetEngine
    private lateinit var petView: PetView
    private lateinit var nameView: TextView
    private lateinit var moodView: TextView
    private lateinit var bubbleView: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var overlayButton: Button
    private lateinit var permissionStatus: TextView
    private lateinit var speech: PetSpeech
    private val selector = SpeechSelector()
    private val handler = Handler(Looper.getMainLooper())
    private val bars = LinkedHashMap<String, ProgressBar>()
    private val values = LinkedHashMap<String, TextView>()
    private var pendingOverlayStart = false
    private var lastAutoSpeechAt = 0L
    private var greeted = false
    private var lastAppliedLookId = ""
    private var lastAppliedSize = 0

    private val ticker = object : Runnable {
        override fun run() {
            val tick = engine.tick(System.currentTimeMillis())
            applyVisual()
            if (tick.behaviorChanged) maybeAutoSpeak()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = PetSettings(this)
        repo = PetRepository(this)
        speech = PetSpeech(this)
        speech.init()
        val saved = repo.load()
        val initial = saved ?: PetState(name = settings.petName, personality = settings.personality)
        engine = PetEngine(initial)
        engine.restore(initial.copy(name = settings.petName, personality = settings.personality), System.currentTimeMillis())
        buildUi()
        applyVisual()
        requestNotificationPermissionIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(22))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTextColor(Color.parseColor("#16233F"))
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.tagline)
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            setPadding(0, dp(2), 0, dp(14))
        })

        // Thẻ xem trước.
        val previewCard = card()
        val previewBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        petView = PetView(this).apply {
            look = PetLooks.byId(settings.lookId)
            lastAppliedLookId = look.id
        }
        previewBox.addView(petView, LinearLayout.LayoutParams(dp(230), dp(230)))
        nameView = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.parseColor("#16233F"))
            gravity = Gravity.CENTER
        }
        previewBox.addView(nameView)
        moodView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        }
        previewBox.addView(moodView)
        bubbleView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#16233F"))
            gravity = Gravity.CENTER
            background = bubbleBackground()
            setPadding(dp(12), dp(8), dp(12), dp(8))
            visibility = View.INVISIBLE
        }
        previewBox.addView(bubbleView)
        previewCard.addView(previewBox)
        root.addView(previewCard, topMargin(dp(4)))

        // Nhu cầu.
        val needsCard = card()
        needsCard.addView(sectionTitle(getString(R.string.needs_title)))
        addNeedRow(needsCard, "hunger", getString(R.string.need_hunger), "#FF8A65")
        addNeedRow(needsCard, "happiness", getString(R.string.need_happiness), "#FFC94D")
        addNeedRow(needsCard, "energy", getString(R.string.need_energy), "#4C6FFF")
        addNeedRow(needsCard, "cleanliness", getString(R.string.need_cleanliness), "#2FBFA0")
        addNeedRow(needsCard, "affection", getString(R.string.need_affection), "#FF6F91")
        root.addView(needsCard, topMargin(dp(14)))

        // Hành động.
        val actionsCard = card()
        actionsCard.addView(sectionTitle(getString(R.string.action_status)))
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(actionButton(getString(R.string.action_feed)) { interact(Interaction.FEED) }, weight())
        row1.addView(actionButton(getString(R.string.action_pet)) { interact(Interaction.PET) }, weight())
        actionsCard.addView(row1)
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(actionButton(getString(R.string.action_play)) { interact(Interaction.PLAY) }, weight())
        row2.addView(actionButton(getString(R.string.action_bathe)) { interact(Interaction.BATHE) }, weight())
        actionsCard.addView(row2)
        root.addView(actionsCard, topMargin(dp(14)))

        root.addView(Button(this).apply {
            text = getString(R.string.action_minigame)
            setOnClickListener { startActivity(Intent(this@MainActivity, MiniGameActivity::class.java)) }
        }, topMargin(dp(10)))

        // Overlay.
        val overlayCard = card()
        overlayCard.addView(sectionTitle(getString(R.string.overlay_permission_title)))
        overlayCard.addView(TextView(this).apply {
            text = getString(R.string.overlay_permission_body)
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            setPadding(0, dp(4), 0, dp(10))
        })
        overlayStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#3550C8"))
        }
        overlayCard.addView(overlayStatus)
        overlayButton = Button(this).apply {
            text = getString(R.string.btn_start_overlay)
            setOnClickListener { toggleOverlay() }
        }
        overlayCard.addView(overlayButton, topMargin(dp(8)))
        root.addView(overlayCard, topMargin(dp(14)))

        val healthCard = card()
        healthCard.addView(sectionTitle(getString(R.string.health_check_title)))
        permissionStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            setPadding(0, dp(2), 0, dp(10))
        }
        healthCard.addView(permissionStatus)
        healthCard.addView(Button(this).apply {
            text = getString(R.string.btn_disable_battery_opt)
            setOnClickListener { requestIgnoreBatteryOptimizations() }
        })
        healthCard.addView(Button(this).apply {
            text = getString(R.string.hyperos_guide_button)
            setOnClickListener { showHyperosGuide() }
        })
        root.addView(healthCard, topMargin(dp(14)))

        val settingsButton = Button(this).apply {
            text = getString(R.string.action_settings)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        root.addView(settingsButton, topMargin(dp(14)))

        setContentView(scroll)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(1), Color.parseColor("#E4E9F7"))
        }
        setPadding(dp(16), dp(14), dp(16), dp(16))
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(Color.parseColor("#16233F"))
        setPadding(0, 0, 0, dp(6))
    }

    private fun addNeedRow(parent: LinearLayout, key: String, label: String, color: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, dp(3))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            width = dp(96)
        })
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
        }
        row.addView(bar, LinearLayout.LayoutParams(0, dp(10), 1f).apply { marginEnd = dp(10) })
        val value = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#5B6B8C"))
            width = dp(40)
            gravity = Gravity.END
        }
        row.addView(value)
        parent.addView(row)
        bars[key] = bar
        values[key] = value
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 14f
        setOnClickListener { onClick() }
    }

    private fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun topMargin(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = value
        }

    private fun interact(interaction: Interaction) {
        val result = engine.interact(interaction, System.currentTimeMillis())
        repo.save(engine.snapshot(System.currentTimeMillis()))
        applyVisual()
        burstFor(interaction)
        showBubble(result.speech, force = true)
    }

    private fun burstFor(interaction: Interaction) {
        when (interaction) {
            Interaction.PET -> petView.burst(Effect.HEARTS)
            Interaction.FEED -> petView.burst(Effect.CRUMBS)
            Interaction.BATHE -> petView.burst(Effect.BUBBLES)
            Interaction.PLAY -> petView.burst(Effect.SPARKLES)
            else -> Unit
        }
    }

    private fun applyVisual() {
        val state = engine.state
        petView.setMood(state.mood)
        petView.setWalking(state.behavior == Behavior.WANDER || state.behavior == Behavior.PLAY)
        if (petView.look.id != settings.lookId) {
            petView.look = PetLooks.byId(settings.lookId)
        }
        nameView.text = state.name
        moodView.text = moodLabel(state.mood)
        bars["hunger"]?.progress = state.needs.hunger.toInt()
        bars["happiness"]?.progress = state.needs.happiness.toInt()
        bars["energy"]?.progress = state.needs.energy.toInt()
        bars["cleanliness"]?.progress = state.needs.cleanliness.toInt()
        bars["affection"]?.progress = state.needs.affection.toInt()
        values["hunger"]?.text = state.needs.hunger.toInt().toString()
        values["happiness"]?.text = state.needs.happiness.toInt().toString()
        values["energy"]?.text = state.needs.energy.toInt().toString()
        values["cleanliness"]?.text = state.needs.cleanliness.toInt().toString()
        values["affection"]?.text = state.needs.affection.toInt().toString()
        updateOverlayUi()
    }

    private fun updateOverlayUi() {
        val running = PetOverlayService.isRunning
        overlayStatus.text = getString(if (running) R.string.service_running else R.string.service_not_running)
        overlayButton.text = getString(if (running) R.string.btn_stop_overlay else R.string.btn_start_overlay)
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val notifOk = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val batteryOk = runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        }.getOrDefault(false)
        val yes = getString(R.string.status_granted)
        val no = getString(R.string.status_missing)
        permissionStatus.text = getString(R.string.health_overlay) + ": " + (if (overlayOk) yes else no) +
            "\n" + getString(R.string.health_notifications) + ": " + (if (notifOk) yes else no) +
            "\n" + getString(R.string.health_battery) + ": " + (if (batteryOk) yes else no)
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, R.string.health_battery_done, Toast.LENGTH_SHORT).show()
            updatePermissionStatus()
            return
        }
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + packageName))
        if (runCatching { startActivity(request) }.isFailure) {
            runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    private fun showHyperosGuide() {
        AlertDialog.Builder(this)
            .setTitle(R.string.hyperos_guide_title)
            .setMessage(R.string.hyperos_guide_body)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun maybeAutoSpeak() {
        if (PetOverlayService.isRunning) return
        val state = engine.state
        if (state.behavior != Behavior.TALK) return
        val now = System.currentTimeMillis()
        if (now - lastAutoSpeechAt < 60_000L) return
        if (settings.isQuietNow(com.vudev.pet.core.PetClock.hourOfDay(now))) return
        lastAutoSpeechAt = now
        showBubble(SpeechContext.IDLE_CHAT, force = false)
    }

    private fun greetOnce() {
        if (greeted) return
        greeted = true
        val now = System.currentTimeMillis()
        val context = engine.greetingContext(now, 0.0)
        showBubble(context, force = false)
    }

    private fun showBubble(context: SpeechContext, force: Boolean) {
        if (!force && !settings.bubbleOn) return
        val now = System.currentTimeMillis()
        val phrase = selector.pick(context, engine.state.personality, now) ?: return
        bubbleView.text = getString(R.string.speech_prefix, engine.state.name, phrase)
        bubbleView.visibility = View.VISIBLE
        handler.removeCallbacks(hideBubble)
        handler.postDelayed(hideBubble, 4200L)
        if (!PetOverlayService.isRunning && settings.soundOn &&
            !settings.isQuietNow(com.vudev.pet.core.PetClock.hourOfDay(now))
        ) {
            speech.speak(phrase)
        }
    }

    private val hideBubble = Runnable { bubbleView.visibility = View.INVISIBLE }

    private fun moodLabel(mood: Mood): String = when (mood) {
        Mood.CALM -> getString(R.string.mood_calm)
        Mood.HAPPY -> getString(R.string.mood_happy)
        Mood.EXCITED -> getString(R.string.mood_excited)
        Mood.CURIOUS -> getString(R.string.mood_curious)
        Mood.SAD -> getString(R.string.mood_sad)
        Mood.SLEEPY -> getString(R.string.mood_sleepy)
        Mood.ANGRY -> getString(R.string.mood_angry)
        Mood.SURPRISED -> getString(R.string.mood_surprised)
        Mood.SULK -> getString(R.string.mood_sulk)
    }

    private fun toggleOverlay() {
        if (PetOverlayService.isRunning) {
            startService(Intent(this, PetOverlayService::class.java).setAction(PetOverlayService.ACTION_STOP))
            settings.overlayEnabled = false
            handler.postDelayed({ updateOverlayUi() }, 300L)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            pendingOverlayStart = true
            Toast.makeText(this, R.string.overlay_permission_denied, Toast.LENGTH_LONG).show()
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
            }
            return
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        android.util.Log.i("PetMain", "starting overlay service, canDraw=" + Settings.canDrawOverlays(this))
        requestNotificationPermissionIfNeeded()
        val intent = Intent(this, PetOverlayService::class.java).setAction(PetOverlayService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        settings.overlayEnabled = true
        handler.postDelayed({ updateOverlayUi() }, 500L)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    override fun onResume() {
        super.onResume()
        // Nạp lại trạng thái mới nhất (mini-game, service hoặc tiến trình khác có thể vừa ghi).
        repo.load()?.let { engine.restore(it, System.currentTimeMillis()) }
        // Quay lại từ màn hình xin quyền overlay: nếu đã được cấp thì bật luôn,
        // không bắt người dùng bấm lại lần nữa.
        if (pendingOverlayStart) {
            pendingOverlayStart = false
            if (Settings.canDrawOverlays(this)) startOverlayService()
        } else if (settings.overlayEnabled && !PetOverlayService.isRunning && Settings.canDrawOverlays(this)) {
            // Tự phục hồi overlay nếu tiến trình từng bị hệ thống kill (M5).
            startOverlayService()
        }
        engine.setName(settings.petName)
        engine.setPersonality(settings.personality, System.currentTimeMillis())
        petView.look = PetLooks.byId(settings.lookId)
        applyVisual()
        handler.post(ticker)
        handler.postDelayed({ greetOnce() }, 700L)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(hideBubble)
        repo.save(engine.snapshot(System.currentTimeMillis()))
        com.vudev.pet.widget.PetWidgetProvider.refresh(this)
        speech.stop()
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun bubbleBackground(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.WHITE)
        setStroke(dp(1), Color.parseColor("#DCE3F5"))
    }
}
