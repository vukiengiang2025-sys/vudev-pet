# TIẾN ĐỘ — VUDEV PET

Cập nhật: 2026-11-16 14:55 (Asia/Ho_Chi_Minh).

## Trạng thái milestone

| Mốc | Trạng thái | Bằng chứng |
|---|---|---|
| M0 — Nền dự án | XONG | 3 module Gradle build được: petcore, renderer, app; tài liệu AUDIT/ARCHITECTURE/DECISIONS |
| M1 — Renderer | XONG (code + build) | PetRenderer/PetView: bóng, mắt, miệng, chớp mắt, 9 mood, hạt hiệu ứng |
| M2 — Overlay | XONG + đã kiểm tra máy thật | Overlay, kéo thả, chạm, vuốt, menu, di chuyển, tắt/mở màn hình đều hoạt động trên Redmi 15 5G |
| M3 — Pet Brain | XONG | 44 unit test xanh; needs/emotion/behavior/personality/memory/time/engine |
| M4 — Tương tác và nói | XONG + đã kiểm tra máy thật | TTS vi-VN ready=true, menu đủ 8 mục, cho ăn/chơi/vuốt/kéo hoạt động; APK ~0.96 MB |
| M5 — Onboarding/thông báo/phục hồi | XONG phần lớn, đã kiểm tra máy | Màn hình kiểm tra quyền (overlay/thông báo/pin), nút miễn tối ưu pin, hướng dẫn HyperOS, nhắc đói/buồn/bỏ bê, tự phục hồi overlay sau force-stop; boot notification có code, chưa test khởi động lại thật |
| M6–M7 | chưa làm | Widget/mini-game, stress test, phát hành |

## CI / GitHub
- Repo: https://github.com/vukiengiang2025-sys/vudev-pet
- Workflow build-apk: XANH (run 43826186610, commit 9cd5df7), artifact vudev-pet-debug.
- Release tải trực tiếp: https://github.com/vukiengiang2025-sys/vudev-pet/releases/tag/latest-apk
- APK mới nhất: https://github.com/vukiengiang2025-sys/vudev-pet/releases/download/latest-apk/app-debug.apk
- APK release: khoảng 940.8 KB (sha256 xem log CI hoặc tải tệp từ release)
- Đã kiểm tra APK release bằng aapt: package com.vudev.pet, label VUDEV PET, minSdk 26, targetSdk 34, chỉ 4 quyền (SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE) — không có INTERNET, không Accessibility.

## Việc đang làm
- Đã kiểm tra M2/M4/M5 trên Redmi 15 5G (xem docs/DEVICE-TEST.md). Chuẩn bị M6: widget màn hình chính + mini-game 1 phút.

## Việc tiếp theo
1. Cài APK từ release, cấp quyền overlay + thông báo; kiểm tra thú hiện trên màn hình, tương tác, TTS.
2. Ghi nhận lỗi thực tế và sửa; cập nhật FINAL_REPORT.
3. M5: onboarding/thông báo/HyperOS; M6: widget/mini-game; M7: tối ưu.