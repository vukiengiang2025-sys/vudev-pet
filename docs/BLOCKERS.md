# BLOCKERS — VUDEV PET

Cập nhật: 2026-11-16 17:30.

## Đang chặn (tạm thời)
1. Điện thoại Redmi 15 5G đang khoá màn hình (cần PIN/vân tay). Sau bài test tắt/mở màn hình, adb không thể mở khoá. Vì vậy:
   - App đã bị gỡ trong lần cài lại bản polish; chưa cài lại được bản mới.
   - Chưa chạy được hộp thoại xác nhận cài đặt MIUI "Cài đặt ứng dụng này qua USB?".
   - Không thể tiếp tục test persistence sau force-stop, thông báo đói, đo pin.
   - Cách xử lý: mở khoá điện thoại, sau đó chạy lại lệnh cài trong docs/DEVICE-TEST.md hoặc tải APK từ Release.

2. HyperOS chặn cài adb cho tới khi người dùng xác nhận trên màn hình (AdbInstallActivity). Đây là hành vi của hệ thống, không phải lỗi app. Khi màn hình mở khoá, bấm "Cài đặt" trong hộp thoại là cài được.

## Không có blocker kỹ thuật khác
- Code, build, unit test, CI và release APK đều xanh.
- Không cần mạng, API key hay quyền hệ thống.
