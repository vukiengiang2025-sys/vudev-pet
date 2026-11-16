# Quyết định — VUDEV PET

| # | Quyết định | Lý do |
|---|---|---|
| 1 | Clean-room, không sao chép mã/asset Andee | Chưa xác minh quyền tái sử dụng thương mại; đảm bảo sở hữu độc lập |
| 2 | Thú dạng bóng biểu cảm cho MVP | Không cần art/sprite; tập trung vào cảm xúc và hành vi |
| 3 | Tên VUDEV PET, applicationId com.vudev.pet | Chủ dự án chốt |
| 4 | MVP dừng ở M4 (M5–M7 sau) | Ưu tiên một thú cưng "sống" thật sự |
| 5 | Canvas renderer thay vì OpenGL | Nhẹ, ổn định trên máy yếu, dễ test |
| 6 | Không AndroidX | Giảm dependency, APK nhỏ, build nhanh trên máy 4 GB |
| 7 | Persistence JSON + SharedPreferences | Không cần Room cho dữ liệu nhỏ |
| 8 | Không quyền INTERNET | Offline, riêng tư, không chi phí |
| 9 | minSdk 26, target/compile 34 | Phù hợp thiết bị thật và SDK đang có |
| 10 | 5 tính cách, MVP triển khai đủ cấu trúc + 2 tính cách nổi bật | Yêu cầu prompt; dữ liệu hoá để dễ mở rộng |
