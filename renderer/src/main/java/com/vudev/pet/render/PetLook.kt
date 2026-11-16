package com.vudev.pet.render

import android.graphics.Color

/**
 * Bảng màu của thú. MVP chỉ có bóng biểu cảm; thêm look mới chỉ cần thêm dữ liệu,
 * không sửa renderer hay petcore.
 */
data class PetLook(
    val id: String,
    val label: String,
    val bodyTop: Int,
    val bodyBottom: Int,
    val rim: Int,
    val ear: Int,
    val eyeWhite: Int = Color.WHITE,
    val pupil: Int = 0xFF16233F.toInt(),
    val cheek: Int = 0x66FF8FA3,
    val mouth: Int = 0xFF16233F.toInt(),
)

object PetLooks {

    val BONG = PetLook(
        id = "bong",
        label = "Bông xanh",
        bodyTop = 0xFF8FB0FF.toInt(),
        bodyBottom = 0xFF4C6FFF.toInt(),
        rim = 0xFF3550C8.toInt(),
        ear = 0xFF6F8DF5.toInt(),
    )

    val MINT = PetLook(
        id = "mint",
        label = "Bạc hà",
        bodyTop = 0xFF9AF0D8.toInt(),
        bodyBottom = 0xFF2FBFA0.toInt(),
        rim = 0xFF1E8C76.toInt(),
        ear = 0xFF57D6BC.toInt(),
        cheek = 0x55FF9AA2,
        pupil = 0xFF123B34.toInt(),
        mouth = 0xFF123B34.toInt(),
    )

    val PEACH = PetLook(
        id = "peach",
        label = "Đào",
        bodyTop = 0xFFFFC2A8.toInt(),
        bodyBottom = 0xFFFF8A65.toInt(),
        rim = 0xFFD9613F.toInt(),
        ear = 0xFFFFA98A.toInt(),
        cheek = 0x66E24A5A,
        pupil = 0xFF4A1F1A.toInt(),
        mouth = 0xFF4A1F1A.toInt(),
    )

    val LAVENDER = PetLook(
        id = "lavender",
        label = "Oải hương",
        bodyTop = 0xFFD8C2FF.toInt(),
        bodyBottom = 0xFF9B72F2.toInt(),
        rim = 0xFF7447C9.toInt(),
        ear = 0xFFB79BF5.toInt(),
        cheek = 0x66FF9AC8,
        pupil = 0xFF2D1B4E.toInt(),
        mouth = 0xFF2D1B4E.toInt(),
    )

    val SUNNY = PetLook(
        id = "sunny",
        label = "Nắng",
        bodyTop = 0xFFFFE08A.toInt(),
        bodyBottom = 0xFFFFB300.toInt(),
        rim = 0xFFD18A00.toInt(),
        ear = 0xFFFFCF5C.toInt(),
        cheek = 0x66FF7A59,
        pupil = 0xFF4A2B00.toInt(),
        mouth = 0xFF4A2B00.toInt(),
    )

    val ALL: List<PetLook> = listOf(BONG, MINT, PEACH, LAVENDER, SUNNY)

    fun byId(id: String?): PetLook = ALL.firstOrNull { it.id == id } ?: BONG
}
