package com.vudev.pet.core

/** Ngữ cảnh nói. App dùng để quyết định bong bóng + TTS, petcore chỉ chọn câu. */
enum class SpeechContext {
    MORNING,
    NOON,
    EVENING,
    NIGHT,
    HUNGRY,
    SLEEPY,
    DIRTY,
    SAD,
    HAPPY,
    PETTED,
    FED,
    BATHED,
    PLAYED,
    MISSED_YOU,
    BACK,
    IDLE_CHAT,
    DRAGGED,
    TALK,
}

/**
 * Kho lời thoại tiếng Việt, offline 100%. Viết theo ngữ cảnh, câu ngắn để đọc và hiện
 * bong bóng trên điện thoại; không dùng API dịch.
 */
object SpeechBank {

    private val general: Map<SpeechContext, List<String>> = mapOf(
        SpeechContext.MORNING to listOf(
            "Chào buổi sáng! Hôm nay mình đi loanh quanh nhé.",
            "Dậy rồi à? Mình vừa ngủ được một giấc.",
            "Buổi sáng tốt lành, mình thích nắng sớm.",
            "Chào bạn, mình đói bụng rồi nè.",
        ),
        SpeechContext.NOON to listOf(
            "Trưa rồi, bạn ăn gì chưa?",
            "Mình buồn ngủ quá, cho mình chợp mắt chút nhé.",
            "Giữa trưa nóng ghê, mình muốn uống nước.",
        ),
        SpeechContext.EVENING to listOf(
            "Chiều rồi, mình đi dạo một chút nha.",
            "Hoàng hôn đẹp quá, bạn có thấy không?",
            "Tối nay mình muốn chơi với bạn.",
        ),
        SpeechContext.NIGHT to listOf(
            "Khuya rồi, bạn nghỉ sớm nhé.",
            "Mình buồn ngủ rồi, chúc bạn ngủ ngon.",
            "Đêm yên tĩnh quá, mình ngủ đây.",
        ),
        SpeechContext.HUNGRY to listOf(
            "Bụng mình kêu rồi, cho mình ăn chút đi.",
            "Mình đói lắm rồi, bạn ơi.",
            "Có gì ăn không? Mình ăn một miếng thôi.",
            "Đói quá, mình không chơi được nữa.",
        ),
        SpeechContext.SLEEPY to listOf(
            "Mình mệt rồi, muốn ngủ một lát.",
            "Mắt mình díp lại rồi, ngủ chút nha.",
            "Cho mình ngủ một giấc nhé.",
        ),
        SpeechContext.DIRTY to listOf(
            "Mình bụi quá rồi, tắm cho mình nhé.",
            "Người mình hơi bẩn, bạn giúp mình tắm nha.",
            "Muốn được tắm quá đi.",
        ),
        SpeechContext.SAD to listOf(
            "Mình hơi buồn, chơi với mình một chút nhé.",
            "Lâu rồi không ai để ý mình.",
            "Bạn đâu rồi, mình cô đơn quá.",
        ),
        SpeechContext.HAPPY to listOf(
            "Hôm nay mình vui lắm!",
            "Mình thích ở bên bạn.",
            "Bạn làm mình cười đó.",
            "Mình đang có một ngày tuyệt vời.",
        ),
        SpeechContext.PETTED to listOf(
            "Hihi, mình thích được vuốt ve.",
            "Vuốt nữa đi, mình thích lắm.",
            "Mình lim dim rồi nè.",
            "Nhẹ tay thôi, mình nhột.",
        ),
        SpeechContext.FED to listOf(
            "Ngon quá! Cảm ơn bạn.",
            "No rồi, mình vui lắm.",
            "Món này mình thích nè.",
        ),
        SpeechContext.BATHED to listOf(
            "Sạch sẽ rồi, mình thơm chưa?",
            "Tắm xong thích ghê.",
            "Cảm ơn bạn, mình mát quá.",
        ),
        SpeechContext.PLAYED to listOf(
            "Chơi vui quá! Chơi nữa nhé.",
            "Mình mệt nhưng mà vui.",
            "Bạn giỏi lắm, mình thua rồi.",
        ),
        SpeechContext.MISSED_YOU to listOf(
            "Bạn đi đâu lâu thế? Mình nhớ bạn.",
            "Cuối cùng cũng quay lại, mình chờ mãi.",
            "Mình tưởng bạn quên mình rồi.",
        ),
        SpeechContext.BACK to listOf(
            "Bạn về rồi! Mình vui quá.",
            "A, bạn quay lại rồi.",
            "Mình nhớ bạn lắm đó.",
        ),
        SpeechContext.IDLE_CHAT to listOf(
            "Bạn đang làm gì đó?",
            "Hôm nay của bạn thế nào?",
            "Mình đang ngắm màn hình nè.",
            "Có gì vui kể mình nghe đi.",
            "Mình thích ở đây với bạn.",
        ),
        SpeechContext.DRAGGED to listOf(
            "Ơ, mình bay lên rồi!",
            "Thả mình xuống nhẹ thôi!",
            "Đi đâu thế?",
        ),
        SpeechContext.TALK to listOf(
            "Mình đây.",
            "Nghe nè.",
            "Bạn muốn nói gì với mình?",
        ),
    )

    private val byPersonality: Map<Personality, Map<SpeechContext, List<String>>> = mapOf(
        Personality.PLAYFUL to mapOf(
            SpeechContext.IDLE_CHAT to listOf("Trốn tìm không? Mình thắng đó!", "Nhìn mình nè, mình nhào lộn được không?"),
            SpeechContext.PLAYED to listOf("Chơi nữa! Chơi nữa!", "Mình chưa mệt đâu!"),
        ),
        Personality.SHY to mapOf(
            SpeechContext.IDLE_CHAT to listOf("Bạn... đang nhìn mình à?", "Mình hơi ngại."),
            SpeechContext.BACK to listOf("Bạn về rồi... mình chờ lâu lắm."),
        ),
        Personality.LAZY to mapOf(
            SpeechContext.SLEEPY to listOf("Ngủ thêm năm phút nữa thôi...", "Đừng gọi mình, mình đang mơ."),
            SpeechContext.IDLE_CHAT to listOf("Mình nằm đây thôi.", "Lăn qua lăn lại cũng mệt."),
        ),
        Personality.FRIENDLY to mapOf(
            SpeechContext.IDLE_CHAT to listOf("Mình luôn ở đây với bạn.", "Kể mình nghe ngày hôm nay đi."),
        ),
        Personality.CURIOUS to mapOf(
            SpeechContext.IDLE_CHAT to listOf("Cái đó là gì vậy?", "Bạn đang xem gì thế?", "Mình muốn khám phá chỗ mới."),
        ),
    )

    fun phrases(context: SpeechContext, personality: Personality): List<String> {
        val personal = byPersonality[personality]?.get(context).orEmpty()
        return personal + general[context].orEmpty()
    }

    fun allContexts(): Set<SpeechContext> = general.keys
}

/**
 * Chọn câu nói có giới hạn tần suất và không lặp ngay câu trước.
 * Trả null nghĩa là nên im lặng lúc này.
 */
class SpeechSelector(
    var minIntervalMs: Long = 15_000L,
) {
    private var lastContext: SpeechContext? = null
    private var lastPhrase: String? = null
    private var lastAtMs: Long = 0L

    fun pick(
        context: SpeechContext,
        personality: Personality,
        nowWallMs: Long,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): String? {
        if (lastContext == context && lastAtMs > 0L && nowWallMs - lastAtMs < minIntervalMs) return null
        val candidates = SpeechBank.phrases(context, personality).filter { it != lastPhrase }
        if (candidates.isEmpty()) return null
        val phrase = candidates[random.nextInt(candidates.size)]
        lastContext = context
        lastPhrase = phrase
        lastAtMs = nowWallMs
        return phrase
    }

    fun reset() {
        lastContext = null
        lastPhrase = null
        lastAtMs = 0L
    }
}
