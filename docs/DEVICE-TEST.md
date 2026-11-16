# Kiểm thử trên thiết bị thật — VUDEV PET

Thiết bị: Redmi 15 5G (25057RN09G), Android 16 / HyperOS, 1080x2340, 450 dpi, arm64-v8a.
Ngày: 2026-11-16, khoảng 17:10-17:25 (Asia/Ho_Chi_Minh).
APK: bản debug build từ commit "polish interaction" (trước khi cập nhật tài liệu này).

## Kết quả đã kiểm tra (đều thực hiện qua adb, có ảnh chụp)

| Hạng mục | Kết quả |
|---|---|
| Cài đặt | Cài được sau khi xác nhận hộp thoại MIUI "Cài đặt ứng dụng này qua USB?" (AdbInstallActivity) |
| Mở app | Không crash; giao diện tiếng Việt; render bóng, mắt, má ho, miệng cười, bóng đổ |
| TTS tiếng Việt | Log: tts status=0 langResult=1 vietnamese=true ready=true; đọc câu "Món này mình thích nè." |
| Cho ăn | Đói 20 → 0, vui 69 → 75, hành vi EAT, mood HAPPY, ký ức last_feed được lưu |
| Overlay | Bật từ app: canDraw=true, service foreground, cửa sổ APPLICATION_OVERLAY 421x421 px (150dp), thông báo foreground đúng |
| Thú trên màn hình chính | Có, kèm bong bóng thoại; ảnh overlay-home.png |
| Chạm | Thú phản ứng, TTS "Bạn làm mình cười đó.", tương tác tăng, mood EXCITED |
| Giữ 1 giây | Menu hiện đủ: Cho ăn, Chơi, Tắm, Vuốt ve, Trạng thái, Cài đặt, Ẩn, Đóng (ảnh overlay-menu.png) |
| Chơi từ menu | Vui 75 → 91.9, năng lượng giảm, TTS "Chơi vui quá! Chơi nữa nhé." |
| Vuốt ve | Cử chỉ qua lại trong vùng chạm: last_pet được ghi, TTS "Vuốt nữa đi, mình thích lắm.", mood HAPPY |
| Kéo thả | Cửa sổ di chuyển tới (92,899), lưu overlay_x/overlay_y vào SharedPreferences |
| Tắt/mở màn hình | Service vẫn foreground; sau khi mở lại vẫn còn overlay; không crash |
| Bộ nhớ | PSS 55.976 KB trước, 61.307 KB sau khi test (RSS 138-145 MB) — đạt mục tiêu PSS < 80 MB |

Ảnh: docs/screenshots/main-screen.png, overlay-home.png, overlay-menu.png, feed-reaction.png.

## Lỗi phát hiện trong lúc test và đã sửa

1. Menu và bong bóng quá trong suốt, chữ khó đọc trên nền ứng dụng khác → đổi nền sang trắng đục.
2. Thú tiếp tục đi lại trong lúc người dùng giữ/ kéo → dừng vòng di chuyển khi ACTION_DOWN, tiếp tục sau khi thả.
3. Tự nói quá thường xuyên (khoảng 20 giây/lần) → giới hạn 60 giây/lần cho lời tự động.
4. Mắt có thể kẹt trạng thái nhắm sau khi hết buồn ngủ → reset chớp mắt khi rời mood SLEEPY.
5. Chưa có lời chào khi mở app → thêm greeting theo buổi/trạng thái một lần mỗi phiên.

## Kiểm thử bổ sung M5 (18:16-18:30)

| Hạng mục | Kết quả |
|---|---|
| Cài lại bản polish | Cài thành công; MIUI đã nhớ lựa chọn nên không hỏi lại hộp thoại USB |
| Tự phục hồi overlay | Force-stop app rồi mở lại: MainActivity tự bật lại PetOverlayService (log starting overlay service, canDraw=true) mà không cần bấm nút |
| Giữ trạng thái | Cho ăn, force-stop, mở lại: total=1, hunger=0 vẫn giữ; không crash |
| Thông báo đói | id=1002, channel pet_reminder, text: Bông đang đói bụng, bạn cho ăn nhé. |
| Thông báo buồn | id=1004, channel pet_reminder (khi vui <= 30) |
| Thông báo bỏ bê | id=1005, channel pet_reminder (khi vui <= 25 và hơn 24 giờ không tương tác) |
| Thông báo sau khởi động | id=1006 do BootReceiver tạo; shell không gửi được BOOT_COMPLETED chuẩn trên máy này nên chưa test khởi động lại thật |
| Lặp giọng đọc | Đã sửa: chỉ một engine TTS đọc khi overlay đang chạy; greeting trễ 700 ms chờ TTS khởi tạo |
| Màn hình kiểm tra quyền | Có trong app: quyền overlay, thông báo, miễn tối ưu pin |

Ghi chú: trên Android 16, appops SYSTEM_ALERT_WINDOW phải set theo UID (appops set --uid com.vudev.pet SYSTEM_ALERT_WINDOW allow) thì Settings.canDrawOverlays mới trả true; người dùng bật qua màn hình hệ thống không bị ảnh hưởng.
## Chưa kiểm tra được (blocker hiện tại)

- Khôi phục sau khởi động lại máy thật: chưa test (shell không gửi được BOOT_COMPLETED; cần khởi động lại máy thật).
- Đo pin nhiều giờ và máy Android 11-13: chưa test.
- Hộp thoại xác nhận cài adb của MIUI: đã xử lý; lần cài đặt sau hệ thống nhớ lựa chọn.

## Cách kiểm tra lại khi mở khoá máy

    adb install -r app-debug.apk       # trên máy sẽ hiện hộp thoại MIUI, bấm "Cài đặt"
    adb shell appops set com.vudev.pet SYSTEM_ALERT_WINDOW allow
    adb shell pm grant com.vudev.pet android.permission.POST_NOTIFICATIONS
    adb shell am start -n com.vudev.pet/.MainActivity

Hoặc tải APK từ Release và cài trực tiếp trên điện thoại (không cần adb).