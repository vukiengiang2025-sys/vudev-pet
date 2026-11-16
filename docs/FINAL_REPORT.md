# BÁO CÁO CUỐI — VUDEV PET (MVP M0-M4)

Cập nhật: 2026-11-16 14:45. Trạng thái: mã nguồn + build + unit test hoàn tất; kiểm thử trên thiết bị thật đang chờ kết nối lại adb.

## A. Tổng quan
Đã hoàn thành M0 đến M4 ở mức mã nguồn và build:
- Thú cưng bóng biểu cảm có 9 mood, chớp mắt, thở, nhún khi đi, hạt hiệu ứng (tim, Zzz, lấp lánh, vụn bánh, bong bóng, giận).
- 5 nhu cầu (đói, vui, năng lượng, sạch sẽ, tình cảm), suy giảm theo thời gian thực, offline catch-up tối đa 24 giờ, không có cơ chế chết.
- 11 hành vi (IDLE, WANDER, SIT, SLEEP, PLAY, EAT, BATHE, PETTED, DRAGGED, SULK, TALK), chọn theo điểm nhu cầu + giờ + tính cách + lịch sử; không lặp quá 3 lần; người dùng luôn ngắt được.
- 5 tính cách có trọng số hành vi và tốc độ tiêu hao năng lượng khác nhau.
- Overlay service: thú nổi trên màn hình, kéo thả, đi lại, ẩn/hiện, menu vòng (ăn, chơi, tắm, vuốt, trạng thái, cài đặt, ẩn).
- Bong bóng + TTS tiếng Việt offline; giờ yên tĩnh; máy thiếu giọng Việt vẫn hiện chữ.
- Lưu trạng thái JSON nguyên tử + SharedPreferences; thông báo khi đói (cooldown 6 giờ); không quyền INTERNET, không Accessibility.

## B. Kỹ thuật
- 3 module: petcore (Kotlin thuần), renderer (Android library, Canvas), app (Activity/Service).
- Không AndroidX/AppCompat/Compose: Activity thuần, view lập trình, giảm dependency và APK.
- Renderer Canvas thay vì OpenGL: nhẹ, ổn định trên máy yếu, giới hạn FPS theo trạng thái.
- Persistence JSON + SharedPreferences; không Room.
- Foreground service kiểu specialUse (API 34); overlay bằng SYSTEM_ALERT_WINDOW.
- Không có mã/asset sao chép từ dự án khác (clean-room).

## C. Kiểm thử
- Unit test petcore: 44 test, 0 lỗi.
- Build: petcore, renderer, app đều biên dịch thành công; APK debug khoảng 0.96 MB.
- Chưa chạy: instrumentation test và kiểm thử thủ công trên máy thật (thiết bị mất kết nối adb lúc 14:34).

## D. Hiệu năng
- Kích thước APK: 959.010 byte (khoảng 0.94 MiB).
- RAM/FPS/pin: chưa đo trên thiết bị; thiết kế mục tiêu RAM dưới 80 MB, 15 FPS khi nghỉ, 60 FPS khi đi/tương tác, dừng vẽ khi màn hình tắt.

## E. Lỗi còn tồn tại
1. Chưa xác minh overlay và tương tác trên HyperOS/Android 16 (thiết bị mất kết nối). Mức độ: cao với nghiệm thu M2/M4, không ảnh hưởng build.
2. TTS tiếng Việt phụ thuộc gói giọng của máy; đã xử lý fallback hiện chữ nhưng chưa thử trên máy.
3. Chưa có khôi phục overlay sau khởi động lại máy (dự kiến M5).
4. Chưa có widget/mini-game (M6).

## F. Bàn giao
- Source: /home/vukgg04/vscode/vudev-pet; repo GitHub: https://github.com/vukiengiang2025-sys/vudev-pet
- APK tải trực tiếp (CI tạo, mỗi lần push main): https://github.com/vukiengiang2025-sys/vudev-pet/releases/download/latest-apk/app-debug.apk
- APK release đã kiểm tra: 940.826 byte, SHA-256 c1400b35b0e258e5a44023d6719f2d6cf23ea2c5b0b41fb255ea9250b869d7bb, package com.vudev.pet, minSdk 26, targetSdk 34, không INTERNET/không Accessibility.
- Artifact CI: vudev-pet-debug (tab Actions).
- Build: export JAVA_HOME trỏ JDK 17, ANDROID_HOME trỏ SDK, chạy ./gradlew :petcore:test :app:assembleDebug.
- Cài: adb install -r app-debug.apk; hoặc tải APK từ Release.
- Kiểm thử trên Redmi: bật quyền Hiển thị trên ứng dụng khác, bật Tự khởi chạy và Không giới hạn pin, mở app bấm "Cho mình ra màn hình".
- CI: workflow build-apk xanh (run 43825296152), artifact + release latest-apk đã tạo.

## G. Việc tiếp theo
1. Có lại adb: cài APK, cấp quyền, kiểm tra overlay/tương tác/TTS, chụp ảnh, sửa lỗi phát hiện.
2. M5: onboarding đầy đủ, thông báo đói/buồn, tự phục hồi overlay sau reboot, tối ưu HyperOS.
3. M6: widget, mini-game 1 phút, thêm look/nhân vật.
4. M7: stress test, đo pin/RAM, polish, chuẩn bị phát hành.