# Build và cài VUDEV PET

## Yêu cầu
- JDK 17.
- Android SDK với platform 34 và build-tools 34.0.0.
- Linux/macOS/Windows có Gradle wrapper (đã kèm trong repo).

## Build

    export JAVA_HOME=<đường dẫn JDK 17>
    export ANDROID_HOME=$HOME/Android/Sdk
    cd ~/vscode/vudev-pet
    ./gradlew :petcore:test :app:assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk

Máy RAM thấp (khoảng 4 GB): thêm -Dorg.gradle.jvmargs="-Xmx1600m -XX:MaxMetaspaceSize=640m" --max-workers=2 và không chạy song song nhiều build.

## Cài trên điện thoại

Cách 1 — qua USB/ADB:

    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell appops set com.vudev.pet SYSTEM_ALERT_WINDOW allow
    adb shell pm grant com.vudev.pet android.permission.POST_NOTIFICATIONS
    adb shell am start -n com.vudev.pet/.MainActivity

Cách 2 — tải trực tiếp (không cần máy tính):

    https://github.com/vukiengiang2025-sys/vudev-pet/releases/download/latest-apk/app-debug.apk

Mở tệp, cho phép cài từ nguồn không xác định nếu được hỏi.

## Sau khi cài
1. Mở app, bấm nút "Cho mình ra màn hình".
2. Bật quyền "Hiển thị trên ứng dụng khác" cho VUDEV PET; quay lại app (tự bật overlay).
3. Cấp quyền thông báo để nhận nhắc khi thú đói.
4. Trên HyperOS/MIUI: bật Tự khởi chạy và Không giới hạn pin cho VUDEV PET.
5. Thao tác: chạm để phản ứng, vuốt để vuốt ve, kéo để di chuyển, giữ 1 giây để mở menu chăm sóc.

## Gỡ lỗi
- Không thấy thú: kiểm tra quyền Hiển thị trên ứng dụng khác; tắt/bật lại overlay trong app.
- Thú biến mất sau một lúc: bật Tự khởi chạy và miễn tối ưu pin (HyperOS hay kill overlay).
- Không đọc tiếng Việt: máy chưa có gói giọng TTS tiếng Việt; app vẫn hiện bong bóng chữ. Cài giọng trong Cài đặt → Ngôn ngữ và nhập → Chuyển văn bản thành giọng nói.
- Lấy log: adb logcat -s PetOverlay:* AndroidRuntime:E
