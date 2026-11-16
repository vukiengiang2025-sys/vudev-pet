package com.vudev.pet.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.vudev.pet.core.Mood
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Vẽ thú cưng bằng Canvas: thân bóng gradient, tai, mắt có con ngươi, miệng theo cảm xúc,
 * hiệu ứng hạt. Renderer không biết needs/hành vi; app truyền [PetVisual].
 *
 * Quy ước chống giật: mọi Paint/RectF/Path dùng lại; shader chỉ tạo khi look hoặc kích
 * thước đổi; không cấp phát trong onDraw ngoài hạt (có giới hạn).
 */
class PetRenderer {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val earPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val bodyRect = RectF()
    private val eyeRect = RectF()
    private val cheekRect = RectF()
    private val shadowRect = RectF()
    private val mouthPath = Path()
    private val browPath = Path()

    private var gradient: Shader? = null
    private var gradientKey: String = ""

    private val particles = ArrayList<Particle>(48)
    private val random = Random(7L)

    fun burst(effect: Effect, radius: Float) {
        if (effect == Effect.NONE) return
        val count = when (effect) {
            Effect.HEARTS -> 6
            Effect.ZZZ -> 3
            Effect.SPARKLES -> 8
            Effect.CRUMBS -> 7
            Effect.BUBBLES -> 8
            Effect.ANGER -> 4
            Effect.NONE -> 0
        }
        repeat(count) {
            if (particles.size >= 48) return@repeat
            val angle = random.nextFloat() * 6.28318f
            val speed = radius * (0.25f + random.nextFloat() * 0.5f)
            particles.add(
                Particle(
                    x = 0f,
                    y = -radius * 0.1f,
                    vx = cos(angle) * speed * 0.25f,
                    vy = -speed * (0.6f + random.nextFloat() * 0.5f),
                    life = 0.7f + random.nextFloat() * 0.7f,
                    maxLife = 1.4f,
                    size = radius * (0.10f + random.nextFloat() * 0.10f),
                    type = effect,
                    rotation = random.nextFloat() * 360f,
                ),
            )
        }
    }

    fun update(dtSeconds: Float) {
        if (dtSeconds <= 0f) return
        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.x += p.vx * dtSeconds
            p.y += p.vy * dtSeconds
            p.vy -= p.size * 0.25f * dtSeconds
            p.rotation += 60f * dtSeconds
            p.life -= dtSeconds
            if (p.life <= 0f) particles.removeAt(i)
        }
    }

    fun clearParticles() = particles.clear()

    fun hasParticles(): Boolean = particles.isNotEmpty()

    fun draw(canvas: Canvas, width: Int, height: Int, look: PetLook, visual: PetVisual, blink: Float) {
        val radius = minOf(width, height) * 0.40f
        val cx = width / 2f
        val cy = height / 2f + visual.bobPx
        val alpha = 255

        // Bóng đổ dưới chân.
        shadowRect.set(cx - radius * 0.75f, cy + radius * 0.78f, cx + radius * 0.75f, cy + radius * 1.0f)
        shadowPaint.alpha = 60
        canvas.drawOval(shadowRect, shadowPaint)

        canvas.save()
        canvas.rotate(visual.tiltDeg, cx, cy)

        val halfW = radius * (1f + (1f - visual.squash) * 0.35f)
        val halfH = radius * visual.squash

        // Tai (vẽ trước để nằm sau thân).
        earPaint.color = look.ear
        val earR = radius * 0.22f
        canvas.drawCircle(cx - halfW * 0.62f, cy - halfH * 0.72f, earR, earPaint)
        canvas.drawCircle(cx + halfW * 0.62f, cy - halfH * 0.72f, earR, earPaint)

        // Thân.
        bodyRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        val key = look.id + ":" + width + ":" + height
        if (key != gradientKey) {
            gradient = LinearGradient(
                cx, cy - halfH, cx, cy + halfH,
                look.bodyTop, look.bodyBottom, Shader.TileMode.CLAMP,
            )
            gradientKey = key
        }
        bodyPaint.shader = gradient
        bodyPaint.alpha = alpha
        canvas.drawOval(bodyRect, bodyPaint)

        // Viền.
        rimPaint.color = look.rim
        rimPaint.strokeWidth = radius * 0.035f
        rimPaint.alpha = 210
        canvas.drawOval(bodyRect, rimPaint)

        // Một chút sáng phía trên.
        bodyPaint.shader = null
        bodyPaint.color = 0x33FFFFFF
        bodyRect.set(cx - halfW * 0.55f, cy - halfH * 0.72f, cx + halfW * 0.1f, cy - halfH * 0.18f)
        canvas.drawOval(bodyRect, bodyPaint)

        // Má hồng.
        cheekPaint.color = look.cheek
        cheekRect.set(cx - halfW * 0.62f, cy + halfH * 0.10f, cx - halfW * 0.18f, cy + halfH * 0.34f)
        canvas.drawOval(cheekRect, cheekPaint)
        cheekRect.set(cx + halfW * 0.18f, cy + halfH * 0.10f, cx + halfW * 0.62f, cy + halfH * 0.34f)
        canvas.drawOval(cheekRect, cheekPaint)

        drawFace(canvas, cx, cy, halfW, halfH, radius, look, visual, blink)
        canvas.restore()

        drawParticles(canvas, cx, cy, radius)

        if (visual.touchGlow > 0.01f) {
            glowPaint.color = Color.WHITE
            glowPaint.alpha = (visual.touchGlow * 140).toInt().coerceIn(0, 255)
            glowPaint.strokeWidth = radius * 0.06f
            bodyRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
            canvas.drawOval(bodyRect, glowPaint)
        }
    }

    private fun drawFace(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        halfW: Float,
        halfH: Float,
        radius: Float,
        look: PetLook,
        visual: PetVisual,
        blink: Float,
    ) {
        val eyeDx = halfW * 0.36f
        val eyeCy = cy - halfH * 0.14f
        val eyeRx = radius * 0.235f
        val eyeRy = radius * 0.265f
        val closed = blink > 0.55f || visual.mood == Mood.SLEEPY

        if (closed) {
            linePaint.color = look.pupil
            linePaint.strokeWidth = radius * 0.045f
            for (side in intArrayOf(-1, 1)) {
                val ex = cx + side * eyeDx
                mouthPath.reset()
                mouthPath.moveTo(ex - eyeRx * 0.85f, eyeCy)
                mouthPath.quadTo(ex, eyeCy + eyeRy * 0.45f, ex + eyeRx * 0.85f, eyeCy)
                canvas.drawPath(mouthPath, linePaint)
            }
        } else {
            val eyeScaleY = if (blink > 0.05f) (1f - blink).coerceAtLeast(0.08f) else 1f
            for (side in intArrayOf(-1, 1)) {
                val ex = cx + side * eyeDx
                eyeRect.set(ex - eyeRx, eyeCy - eyeRy * eyeScaleY, ex + eyeRx, eyeCy + eyeRy * eyeScaleY)
                eyePaint.color = look.eyeWhite
                val big = visual.mood == Mood.SURPRISED
                val scale = if (big) 1.08f else 1f
                eyeRect.set(
                    ex - eyeRx * scale, eyeCy - eyeRy * eyeScaleY * scale,
                    ex + eyeRx * scale, eyeCy + eyeRy * eyeScaleY * scale,
                )
                canvas.drawOval(eyeRect, eyePaint)

                val lookX = visual.lookX * eyeRx * 0.30f
                val lookY = visual.lookY * eyeRy * 0.22f
                val pupilR = radius * if (big) 0.085f else 0.10f
                pupilPaint.color = look.pupil
                canvas.drawCircle(ex + lookX, eyeCy + lookY, pupilR, pupilPaint)
                canvas.drawCircle(
                    ex + lookX - pupilR * 0.35f,
                    eyeCy + lookY - pupilR * 0.40f,
                    pupilR * 0.32f,
                    highlightPaint,
                )
            }

            // Lông mày cho giận/buồn.
            linePaint.color = look.mouth
            linePaint.strokeWidth = radius * 0.035f
            when (visual.mood) {
                Mood.ANGRY -> {
                    for (side in intArrayOf(-1, 1)) {
                        val ex = cx + side * eyeDx
                        browPath.reset()
                        browPath.moveTo(ex - side * eyeRx * 0.9f, eyeCy - eyeRy * 1.15f)
                        browPath.lineTo(ex + side * eyeRx * 0.9f, eyeCy - eyeRy * 0.75f)
                        canvas.drawPath(browPath, linePaint)
                    }
                }
                Mood.SAD, Mood.SULK -> {
                    for (side in intArrayOf(-1, 1)) {
                        val ex = cx + side * eyeDx
                        browPath.reset()
                        browPath.moveTo(ex - side * eyeRx * 0.8f, eyeCy - eyeRy * 0.85f)
                        browPath.lineTo(ex + side * eyeRx * 0.8f, eyeCy - eyeRy * 1.25f)
                        canvas.drawPath(browPath, linePaint)
                    }
                }
                else -> Unit
            }
        }

        drawMouth(canvas, cx, cy, halfW, halfH, radius, look, visual)
    }

    private fun drawMouth(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        halfW: Float,
        halfH: Float,
        radius: Float,
        look: PetLook,
        visual: PetVisual,
    ) {
        val my = cy + halfH * 0.30f
        val mw = halfW * 0.30f
        mouthPath.reset()
        linePaint.color = look.mouth
        linePaint.strokeWidth = radius * 0.045f

        when (visual.mood) {
            Mood.HAPPY, Mood.EXCITED -> {
                mouthPath.moveTo(cx - mw, my - radius * 0.02f)
                mouthPath.quadTo(cx, my + radius * 0.20f, cx + mw, my - radius * 0.02f)
                canvas.drawPath(mouthPath, linePaint)
                if (visual.mood == Mood.EXCITED) {
                    linePaint.strokeWidth = radius * 0.035f
                    mouthPath.reset()
                    mouthPath.moveTo(cx - mw * 0.4f, my + radius * 0.19f)
                    mouthPath.quadTo(cx, my + radius * 0.30f, cx + mw * 0.4f, my + radius * 0.19f)
                    canvas.drawPath(mouthPath, linePaint)
                }
            }
            Mood.SAD, Mood.SULK -> {
                mouthPath.moveTo(cx - mw, my + radius * 0.09f)
                mouthPath.quadTo(cx, my - radius * 0.06f, cx + mw, my + radius * 0.09f)
                canvas.drawPath(mouthPath, linePaint)
            }
            Mood.SURPRISED -> {
                linePaint.style = Paint.Style.STROKE
                canvas.drawCircle(cx, my + radius * 0.02f, radius * 0.075f, linePaint)
            }
            Mood.ANGRY -> {
                mouthPath.moveTo(cx - mw * 0.7f, my + radius * 0.07f)
                mouthPath.quadTo(cx, my - radius * 0.02f, cx + mw * 0.7f, my + radius * 0.07f)
                canvas.drawPath(mouthPath, linePaint)
            }
            Mood.SLEEPY -> {
                canvas.drawLine(cx - mw * 0.5f, my, cx + mw * 0.5f, my, linePaint)
            }
            else -> {
                mouthPath.moveTo(cx - mw * 0.55f, my)
                mouthPath.quadTo(cx, my + radius * 0.10f, cx + mw * 0.55f, my)
                canvas.drawPath(mouthPath, linePaint)
            }
        }
    }

    private fun drawParticles(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        if (particles.isEmpty()) return
        for (p in particles) {
            val alpha = ((p.life / p.maxLife).coerceIn(0f, 1f) * 255f).toInt()
            when (p.type) {
                Effect.HEARTS -> {
                    bodyPaint.shader = null
                    bodyPaint.color = 0xFFFF6F91.toInt()
                    bodyPaint.alpha = alpha
                    drawHeart(canvas, cx + p.x, cy + p.y, p.size, bodyPaint)
                }
                Effect.ZZZ -> {
                    bodyPaint.color = 0xFF5B7CFF.toInt()
                    bodyPaint.alpha = alpha
                    bodyPaint.textSize = p.size * 2.2f
                    bodyPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    canvas.drawText("z", cx + p.x + radius * 0.5f, cy + p.y - radius * 0.3f, bodyPaint)
                }
                Effect.SPARKLES -> {
                    bodyPaint.color = 0xFFFFE066.toInt()
                    bodyPaint.alpha = alpha
                    drawStar(canvas, cx + p.x, cy + p.y, p.size, p.rotation, bodyPaint)
                }
                Effect.CRUMBS -> {
                    bodyPaint.color = 0xFFD9A441.toInt()
                    bodyPaint.alpha = alpha
                    canvas.drawCircle(cx + p.x, cy + p.y, p.size * 0.45f, bodyPaint)
                }
                Effect.BUBBLES -> {
                    linePaint.color = 0xFFB3E5FF.toInt()
                    linePaint.alpha = alpha
                    linePaint.strokeWidth = radius * 0.02f
                    canvas.drawCircle(cx + p.x, cy + p.y, p.size * 0.6f, linePaint)
                }
                Effect.ANGER -> {
                    linePaint.color = 0xFFE53935.toInt()
                    linePaint.alpha = alpha
                    linePaint.strokeWidth = radius * 0.03f
                    val s = p.size * 0.6f
                    canvas.drawLine(cx + radius * 0.55f + p.x, cy - radius * 0.55f + p.y, cx + radius * 0.55f + p.x + s, cy - radius * 0.55f + p.y + s, linePaint)
                    canvas.drawLine(cx + radius * 0.55f + p.x + s, cy - radius * 0.55f + p.y, cx + radius * 0.55f + p.x, cy - radius * 0.55f + p.y + s, linePaint)
                }
                Effect.NONE -> Unit
            }
        }
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        mouthPath.reset()
        mouthPath.moveTo(x, y + size * 0.6f)
        mouthPath.cubicTo(x - size * 1.4f, y - size * 0.5f, x - size * 0.6f, y - size * 1.4f, x, y - size * 0.5f)
        mouthPath.cubicTo(x + size * 0.6f, y - size * 1.4f, x + size * 1.4f, y - size * 0.5f, x, y + size * 0.6f)
        canvas.drawPath(mouthPath, paint)
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, paint: Paint) {
        mouthPath.reset()
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) size else size * 0.38f
            val a = Math.toRadians((rotation + i * 45f).toDouble())
            val px = x + cos(a).toFloat() * r
            val py = y + sin(a).toFloat() * r
            if (i == 0) mouthPath.moveTo(px, py) else mouthPath.lineTo(px, py)
        }
        mouthPath.close()
        canvas.drawPath(mouthPath, paint)
    }
}
