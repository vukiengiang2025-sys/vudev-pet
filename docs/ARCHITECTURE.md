# Kiến trúc — VUDEV PET

## Nguyên tắc
- Clean-room: không sao chép mã/asset từ Andee hay dự án khác.
- Offline: không quyền INTERNET, không API, không telemetry.
- Không Accessibility; chỉ overlay (SYSTEM_ALERT_WINDOW) và thông báo.

## Module
| Module | Loại | Nội dung |
|---|---|---|
| petcore | Kotlin JVM | Needs, Emotion, Personality, Behavior, Time, Memory, Speech selection, PetEngine. Có unit test JUnit. |
| renderer | Android library | PetLook, PetVisualState, PetRenderer (Canvas), PetView (View + Choreographer), Particle. Không chứa logic nghiệp vụ. |
| app | Android application | MainActivity, SettingsActivity, PetOverlayService, PetRepository (JSON), PetSpeech (TTS), notifications, permission flow. |

## Luồng
1. PetEngine (petcore) giữ PetState: needs, mood, behavior, memory, personality.
2. PetOverlayService tạo PetView (renderer), tick theo Choreographer; mỗi giây hỏi PetEngine cập nhật; chuyển mood/behavior thành visual state.
3. Touch: chạm/vuốt/kéo/giữ → interaction → PetEngine.applyInteraction → mood/needs mới → renderer + speech.
4. Persistence: PetRepository ghi JSON nguyên tử vào filesDir; offline catch-up khi mở lại.
5. TTS: PetSpeech dùng Android TextToSpeech vi-VN; nếu thiếu giọng chỉ hiện bong bóng.

## Quyết định kỹ thuật chính
- Renderer MVP dùng Canvas (không OpenGL): đủ cho bóng biểu cảm, nhẹ, dễ kiểm soát pin; có thể thay bằng GL sau này mà không đổi petcore.
- Không AndroidX/AppCompat: Activity thuần + view lập trình, giảm dependency và APK.
- Persistence JSON bằng org.json (có sẵn trên Android) + SharedPreferences cho settings.
- Foreground service kiểu specialUse (API 34) cho overlay.
