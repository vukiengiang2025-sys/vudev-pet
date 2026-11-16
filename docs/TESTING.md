# Kiểm thử — VUDEV PET

Cập nhật: 2026-11-16.

## Unit test petcore

    cd ~/vscode/vudev-pet
    export JAVA_HOME=<JDK 17>
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :petcore:test

Kết quả: **51 test, 0 lỗi**.

Nhóm test:
- NeedsTest: clamp, suy giảm khi thức/ngủ, tính cách làm lệch tốc độ, tương tác cho ăn/chơi/tắm/vuốt, mostUrgent.
- PetClockTest: chênh lệch thời gian, đồng hồ lùi, giới hạn 24h, buổi trong ngày.
- PersonalityTest: trọng số hành vi và tốc độ tiêu hao khác nhau; fallback.
- BehaviorEngineTest: không lặp 3 lần, ngủ khi kiệt sức, đói thì xin ăn (TALK), bẩn thì xin tắm, thời lượng hợp lệ, không tự chọn hành vi chỉ dành cho người dùng.
- EmotionEngineTest: hành vi thắng nhu cầu, giận khi đói+buồn, buồn ngủ, dỗi khi bị bỏ, hạnh phúc.
- PetEngineTest: offline catch-up 2h/24h, đồng hồ lùi, ngủ hồi năng lượng, ngủ đủ thì dậy, cho ăn cập nhật ký ức, tương tác đánh thức, không lặp hành vi, snapshot, tên.
- ReminderPolicyTest: nhắc đói một lần/cooldown, giờ yên tĩnh không nhắc, bỏ bê hơn 24 giờ, buồn, không nhắc khi khỏe, reset cooldown, không gửi buồn ngay sau bỏ bê.
- MemoryAndSpeechTest: giờ kể từ tương tác, giới hạn recent, mọi ngữ cảnh có câu tiếng Việt cho mọi tính cách, cooldown/anti-repeat của SpeechSelector.

## Build

    ./gradlew :app:assembleDebug

Kết quả: BUILD SUCCESSFUL; APK app/build/outputs/apk/debug/app-debug.apk (khoảng 0.94-0.96 MB).

## CI (GitHub Actions)

Workflow .github/workflows/build-apk.yml: chmod gradlew, JDK 17, cài platform 34/build-tools 34, cache Gradle, chạy :petcore:test, :app:assembleDebug, upload artifact vudev-pet-debug, và cập nhật release latest-apk trên mỗi push nhánh main.

## Chưa chạy
- Instrumentation test trên máy thật: thiết bị mất kết nối adb lúc 14:34; xem BLOCKERS.md.
- Đo RAM/pin/FPS trên máy: chưa có thiết bị.