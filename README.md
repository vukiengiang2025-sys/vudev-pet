# VUDEV PET

Thú cưng ảo sống trên màn hình điện thoại Android. Hoàn toàn offline: **không Internet, không tài khoản, không API, không AI trả phí, không Accessibility**.

- Tự đi lại, ngồi, ngủ, chơi, dỗi; có 5 nhu cầu (đói, vui, năng lượng, sạch sẽ, tình cảm).
- 5 tính cách (tinh nghịch, nhút nhát, lười, thân thiện, tò mò) làm thay đổi hành vi thật, không chỉ lời thoại.
- Chạm để phản ứng, vuốt để vuốt ve, kéo để di chuyển, giữ để mở menu chăm sóc.
- Nói tiếng Việt bằng bong bóng + TextToSpeech hệ thống; máy thiếu giọng Việt vẫn hiện chữ.
- Nhớ tương tác gần đây; tắt app vẫn tính thời gian vắng mặt (tối đa 24 giờ).
- Nhẹ: APK dưới 1 MB, không quyền INTERNET.

## Tải APK

Bản mới nhất (cài trực tiếp, chữ ký debug):
https://github.com/vukiengiang2025-sys/vudev-pet/releases/download/latest-apk/app-debug.apk

Hoặc tải artifact **app-debug** trong tab Actions.

## Cài đặt

1. Tải APK và cài (cho phép cài từ nguồn không xác định nếu được hỏi).
2. Mở app, bấm **Cho mình ra màn hình**.
3. Android sẽ hỏi quyền **Hiển thị trên ứng dụng khác** (SYSTEM_ALERT_WINDOW) — bật cho VUDEV PET.
4. Quay lại app, bấm lại nút; thú sẽ xuất hiện trên màn hình.
5. Tuỳ chọn: cấp quyền thông báo để nhận nhắc khi thú đói.

Trên HyperOS/MIUI: bật thêm **Tự khởi chạy** và **Không giới hạn pin** cho VUDEV PET để overlay không bị hệ thống tắt.

## Chăm sóc

| Thao tác | Kết quả |
|---|---|
| Chạm | thú phản ứng và nói |
| Vuốt qua lại | vuốt ve, thú vui hơn |
| Kéo | di chuyển thú |
| Giữ 1 giây | menu: cho ăn, chơi, tắm, trạng thái, cài đặt, ẩn |

Nhu cầu giảm dần theo thời gian thực; cho ăn, tắm, chơi, vuốt ve sẽ hồi lại. Thú không chết và không gây áp lực.

## Build

    export JAVA_HOME=<JDK 17>
    export ANDROID_HOME=$HOME/Android/Sdk
    ./gradlew :petcore:test :app:assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk

## Kiến trúc

- petcore (Kotlin thuần): needs, emotion, behavior, personality, memory, speech selection, PetEngine — có unit test.
- renderer (Android library): PetRenderer/ PetView vẽ bóng biểu cảm bằng Canvas, không dùng OpenGL.
- app: MainActivity, SettingsActivity, PetOverlayService, lưu JSON + SharedPreferences, TTS, notification.

Không dùng AndroidX/Compose để giữ APK nhỏ và build nhẹ. Không có mã nào sao chép từ dự án khác.

## Giấy phép

Mã nguồn VUDEV PET viết mới (clean-room). Xem LICENSE.
