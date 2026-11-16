# BLOCKERS — VUDEV PET

Cập nhật: 2026-11-16 14:45.

## Đang chặn (tạm thời)
1. Thiết bị Redmi 15 5G mất kết nối adb ("no devices/emulators found") ngay sau khi cài APK lúc 14:34. Hệ quả: chưa kiểm tra được trên máy thật các mục M2/M4 (thú hiện trên màn hình, kéo/vuốt/chạm/giữ, TTS tiếng Việt, khôi phục trạng thái).
   - Không chặn code, build và unit test (đã xong).
   - Cách xử lý: cắm lại cáp USB, bật USB debugging, chấp nhận RSA; hoặc tải APK từ GitHub Release để cài trực tiếp.
   - Khi có thiết bị: cài APK, chạy appops set com.vudev.pet SYSTEM_ALERT_WINDOW allow, mở app và bấm "Cho mình ra màn hình".

## Không có blocker kỹ thuật khác
- Không cần mạng, không cần API key, không cần quyền hệ thống.
