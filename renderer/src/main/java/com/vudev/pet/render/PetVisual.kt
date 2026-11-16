package com.vudev.pet.render

import com.vudev.pet.core.Mood

/** Hiệu ứng hạt ngắn quanh thú, sinh ra khi tương tác hoặc theo cảm xúc. */
enum class Effect { NONE, HEARTS, ZZZ, SPARKLES, CRUMBS, BUBBLES, ANGER }

/**
 * Tất cả những gì renderer cần để vẽ một khung hình. App chỉ đổi dữ liệu này,
 * renderer không biết gì về needs/hành vi.
 */
data class PetVisual(
    var mood: Mood = Mood.CALM,
    var walking: Boolean = false,
    var facingLeft: Boolean = false,
    var lookX: Float = 0f,
    var lookY: Float = 0f,
    var squash: Float = 1f,
    var tiltDeg: Float = 0f,
    var bobPx: Float = 0f,
    var talking: Boolean = false,
    var touchGlow: Float = 0f,
)

/** Một hạt đơn giản; toạ độ tương đối theo kích thước thú. */
class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var type: Effect,
    var rotation: Float = 0f,
)
