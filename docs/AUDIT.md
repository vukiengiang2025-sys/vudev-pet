# AUDIT — VUDEV PET (Giai đoạn 0)

Ngày: 2026-11-16. Máy: Linux Mint 22.3, 4 nhân, RAM 3,7 GB (khả dụng ~1,4 GB), /home còn ~36 GB.

## Môi trường
| Thành phần | Trạng thái |
|---|---|
| JDK | Temurin 17 tại ~/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 (dùng cho build) |
| Gradle | wrapper 9.3.1 (copy wrapper chuẩn) |
| Android SDK | ~/Android/Sdk: platforms android-34/36, build-tools 35/36, platform-tools |
| adb | thiết bị 25057RN09G (Redmi 15 5G), Android 16, arm64-v8a, đang kết nối |
| Mạng | GitHub/Maven/Google reachable; không proxy |
| Git | 2.43; gh CLI 2.104 đã đăng nhập (vukiengiang2025-sys) |

## Kết luận
- Đủ điều kiện build APK và test trên thiết bị thật.
- Máy RAM thấp: build một dự án tại một thời điểm, heap 1,8 GB, tắt parallel.
- Không cần cài thêm công cụ; tái sử dụng toolchain của môi trường.
- Không cài Android Studio/emulator.

## Giới hạn đã biết
- Không có JDK 21 cho build này (dùng 17).
- Thiết bị có thể mất kết nối USB giữa phiên; khi đó dùng GitHub Actions để lấy APK.
