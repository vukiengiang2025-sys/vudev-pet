# TIẾN ĐỘ — VUDEV PET

Cập nhật: 2026-11-16 14:45 (Asia/Ho_Chi_Minh).

## Trạng thái milestone

| Mốc | Trạng thái | Bằng chứng |
|---|---|---|
| M0 — Nền dự án | XONG | 3 module Gradle build được: petcore, renderer, app; tài liệu AUDIT/ARCHITECTURE/DECISIONS |
| M1 — Renderer | XONG (code + build) | PetRenderer/PetView vẽ bóng, mắt, miệng, chớp mắt, 9 mood, hạt hiệu ứng; build APK thành công |
| M2 — Overlay | XONG (code + build), chờ kiểm tra máy | PetOverlayService: overlay, kéo thả, đi lại, ẩn/hiện, menu; chưa chạy trên máy do mất adb |
| M3 — Pet Brain | XONG | Needs/Emotion/Behavior/Personality/Memory/Time/PetEngine; 44 unit test xanh |
| M4 — Tương tác và nói | XONG (code + build), chờ kiểm tra máy | Menu chăm sóc, bong bóng, TTS vi-VN, settings, giờ yên tĩnh; APK 0.96 MB |
| M5–M7 | chưa làm | Theo kế hoạch sau MVP |

## Kiểm thử
- Chạy: ./gradlew :petcore:test
- Kết quả: 44 test, 0 lỗi (needs, clock, personality, behavior, emotion, engine, memory, speech selector).
- Build: ./gradlew :app:assembleDebug — BUILD SUCCESSFUL, APK khoảng 0.96 MB.

## Việc đang làm
- Đẩy repo lên GitHub, chạy GitHub Actions để có APK tải trực tiếp (điện thoại đang mất kết nối adb).

## Việc tiếp theo
1. Cài APK trên Redmi 15 5G, cấp quyền overlay, kiểm tra thú hiện trên màn hình, kéo/vuốt/chạm/giữ, TTS tiếng Việt.
2. Chụp ảnh màn hình làm bằng chứng; cập nhật FINAL_REPORT.
3. Sau nghiệm thu M4: M5 onboarding/thông báo/HyperOS; M6 widget/mini-game; M7 tối ưu.
