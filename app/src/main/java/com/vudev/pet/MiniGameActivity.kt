package com.vudev.pet

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import com.vudev.pet.core.Interaction
import com.vudev.pet.core.PetEngine
import com.vudev.pet.core.PetState
import com.vudev.pet.data.PetRepository
import com.vudev.pet.widget.PetWidgetProvider
import kotlin.math.abs
import kotlin.random.Random

/**
 * Mini-game 1 phút: hứng bánh. Bấm/giữ để di chuyển thú, hứng càng nhiều bánh càng vui.
 * Kết thúc thì cộng thưởng qua PetEngine.interact(PLAY) và lưu trạng thái.
 */
class MiniGameActivity : Activity() {

    private var rewarded = false

    override fun onResume() {
        super.onResume()
        startService(android.content.Intent(this, com.vudev.pet.service.PetOverlayService::class.java)
            .setAction(com.vudev.pet.service.PetOverlayService.ACTION_SUSPEND))
    }

    override fun onPause() {
        super.onPause()
        startService(android.content.Intent(this, com.vudev.pet.service.PetOverlayService::class.java)
            .setAction(com.vudev.pet.service.PetOverlayService.ACTION_RESUME))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = CatchGameView(this) { score -> finishGame(score) }
        setContentView(view)
    }

    private fun finishGame(score: Int) {
        if (rewarded) return
        rewarded = true
        val now = System.currentTimeMillis()
        val repo = PetRepository(this)
        val saved = repo.load() ?: PetState()
        val engine = PetEngine(saved)
        engine.restore(saved, now)
        engine.interact(Interaction.PLAY, now)
        repo.save(engine.snapshot(now))
        PetWidgetProvider.refresh(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.minigame_result_title)
            .setMessage(getString(R.string.minigame_result_body, score))
            .setPositiveButton(R.string.minigame_ok) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}

private class CatchGameView(
    context: Context,
    private val onFinished: (Int) -> Unit,
) : View(context) {

    private val petPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16233F") }
    private val snackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D9A441") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#16233F")
        textSize = 46f
        isFakeBoldText = true
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5B6B8C")
        textSize = 36f
    }
    private val snackRect = RectF()

    private val snacks = ArrayList<FloatArray>() // x, y
    private var petX = 0f
    private var score = 0
    private var remainingMs = 45_000L
    private var lastMs = 0L
    private var spawnAt = 0L
    private var running = true
    private val random = Random(System.currentTimeMillis())

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.currentTimeMillis()
            val dt = if (lastMs == 0L) 16L else (now - lastMs).coerceAtMost(80L)
            lastMs = now
            remainingMs -= dt
            if (remainingMs <= 0L) {
                running = false
                onFinished(score)
                return
            }
            if (now >= spawnAt) {
                spawnAt = now + 650L
                snacks.add(floatArrayOf(random.nextFloat() * (width - 80f) + 40f, -40f))
            }
            val speed = 0.20f + (45_000L - remainingMs) / 45_000f * 0.25f
            val iterator = snacks.iterator()
            while (iterator.hasNext()) {
                val s = iterator.next()
                s[1] += speed * dt
                if (s[1] > height + 40f) {
                    iterator.remove()
                } else if (abs(s[0] - petX) < 80f && s[1] > height - 190f) {
                    score++
                    iterator.remove()
                }
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (petX <= 0f) petX = w / 2f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (width > 0) petX = width / 2f
        lastMs = 0L
        spawnAt = 0L
        postOnAnimation(loop)
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(loop)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                petX = event.x.coerceIn(80f, width - 80f)
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F7F8FE"))
        val groundY = height - 120f
        // Thú: bóng gradient đơn giản với mắt và miệng.
        val r = 70f
        canonicalGradient(canvas, petX, groundY, r)
        eyePaint.color = Color.WHITE
        canvas.drawCircle(petX - 26f, groundY - 14f, 18f, eyePaint)
        canvas.drawCircle(petX + 26f, groundY - 14f, 18f, eyePaint)
        canvas.drawCircle(petX - 24f, groundY - 12f, 8f, pupilPaint)
        canvas.drawCircle(petX + 28f, groundY - 12f, 8f, pupilPaint)
        val mouth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#16233F")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawArc(petX - 26f, groundY + 2f, petX + 26f, groundY + 40f, 10f, 160f, false, mouth)
        // Bánh rơi.
        for (s in snacks) {
            snackRect.set(s[0] - 22f, s[1] - 22f, s[0] + 22f, s[1] + 22f)
            canvas.drawOval(snackRect, snackPaint)
        }
        canvas.drawText(getContext().getString(R.string.minigame_score) + ": " + score, 40f, 80f, textPaint)
        val seconds = (remainingMs / 1000L).toInt()
        canvas.drawText(getContext().getString(R.string.minigame_time) + ": " + seconds + "s", 40f, 140f, smallPaint)
        canvas.drawText(getContext().getString(R.string.minigame_hint), 40f, height - 30f, smallPaint)
    }

    private fun canonicalGradient(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        bodyPaint.shader = android.graphics.LinearGradient(
            cx, cy - r, cx, cy + r,
            Color.parseColor("#8FB0FF"), Color.parseColor("#4C6FFF"),
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, bodyPaint)
        petPaint.color = Color.parseColor("#3550C8")
        petPaint.style = Paint.Style.STROKE
        petPaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, r, petPaint)
    }
}
