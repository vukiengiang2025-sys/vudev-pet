# TIẾN ĐỘ — VUDEV PET

Cập nhật: 2026-11-16 14:55 (Asia/Ho_Chi_Minh).

## Trạng thái milestone

| Mốc | Trạng thái | Bằng chứng |
|---|---|---|
| M0 — Nền dự án | XONG | 3 module Gradle build được: petcore, renderer, app; tài liệu AUDIT/ARCHITECTURE/DECISIONS |
| M1 — Renderer | XONG (code + build) | PetRenderer/PetView: bóng, mắt, miệng, chớp mắt, 9 mood, hạt hiệu ứng |
| M2 — Overlay | XONG (code + build), chờ kiểm tra máy | PetOverlayService: overlay, kéo thả, đi lại, ẩn/hiện, menu; chưa chạy trên máy do mất adb |
| M3 — Pet Brain | XONG | 44 unit test xanh; needs/emotion/behavior/personality/memory/time/engine |
| M4 — Tương tác và nói | XONG (code + build), chờ kiểm tra máy | Menu chăm sóc, bong bóng, TTS vi-VN, settings, giờ yên tĩnh; APK 0.94-0.96 MB |
| M5–M7 | chưa làm | Sau nghiệm thu MVP |

## CI / GitHub
- Repo: https://github.com/vukiengiang2025-sys/vudev-pet
- Workflow build-apk: XANH (run 43826186610, commit 9cd5df7), artifact vudev-pet-debug.
- Release tải trực tiếp: https://github.com/vukiengiang2025-sys/vudev-pet/releases/tag/latest-apk
- APK mới nhất: https://github.com/vukiengiang2025-sys/vudev-pet/releases/download/latest-apk/app-debug.apk
- APK release: khoảng 940.8 KB (sha256 xem log CI hoặc tải tệp từ release)
- Đã kiểm tra APK release bằng aapt: package com.vudev.pet, label VUDEV PET, minSdk 26, targetSdk 34, chỉ 4 quyền (SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE) — không có INTERNET, không Accessibility.

## Việc đang làm
- Chờ kết nối lại adb để cài và kiểm tra trên Redmi 15 5G (overlay, kéo/vuốt/chạm/giữ, TTS, khôi phục trạng thái).

## Việc tiếp theo
1. Cài APK từ release, cấp quyền overlay + thông báo; kiểm tra thú hiện trên màn hình, tương tác, TTS.
2. Ghi nhận lỗi thực tế và sửa; cập nhật FINAL_REPORT.
3. M5: onboarding/thông báo/HyperOS; M6: widget/mini-game; M7: tối ưu.