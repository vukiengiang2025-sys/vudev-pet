# KẾ HOẠCH PHÁT TRIỂN — VUDEV PET (thú ảo nổi trên màn hình)

Tài liệu lập ngày 2026-11-16. Trạng thái: **chờ duyệt, chưa viết mã nguồn**.
Sản phẩm tách khỏi VUDEV ANDEE 2; Andee chỉ là nguồn tham chiếu/khung hình.

## 0. Quyết định sản phẩm

- Tên làm việc: **VUDEV PET** (tên chính thức chờ chủ dự án chốt).
- Một ứng dụng Android độc lập, **không cần Accessibility**, chỉ cần quyền hiển thị trên ứng dụng khác.
- Mục tiêu MVP: một thú ảo sống trên màn hình, tự đi lại/ngồi/ngủ, phản ứng khi chạm/vuốt, có nhu cầu (đói, vui, năng lượng, sạch sẽ), nói bằng bong bóng + TTS tiếng Việt offline, có thông báo khi đói.
- Không có mạng ở MVP: **bỏ quyền INTERNET** — điểm mạnh về quyền riêng tư, đồng thời tránh mọi chi phí API.

## 1. Tầm nhìn & phạm vi

### 1.1 Trong phạm vi MVP (bản 0.1)
1. Overlay thú trên màn hình chính và ứng dụng khác.
2. Hành vi tự động: idle, đi loanh quanh, ngồi, ngủ, chơi; phản ứng chạm/vuốt/kéo.
3. Hệ nhu cầu theo thời gian thực, có tính thời gian offline khi mở lại.
4. Tương tác chăm sóc: cho ăn, vuốt ve, tắm, chơi, đi ngủ.
5. Giao tiếp offline: bong bóng thoại tiếng Việt + TTS hệ thống (vi-VN).
6. Thông báo "thú đang đói/buồn" (có giờ yên tĩnh).
7. Onboarding quyền + đặt tên + chọn hình dáng.
8. Bảng trạng thái (đói/vui/năng lượng/sạch) và lưu trạng thái qua các lần mở.

### 1.2 Sau MVP (0.2+)
- Widget màn hình chính hiển thị mặt thú + thanh trạng thái.
- Mini-game 1 phút ("hứng bánh", "bắt bóng") tăng vui/năng lượng.
- Nhiều look (bóng biểu cảm + mèo/chó), phụ kiện/trang trí.
- Lịch sinh hoạt theo giờ thực (sáng chào, tối buồn ngủ).
- Nhật ký chăm sóc, chuỗi ngày chăm tốt.
- (Tùy chọn, tách riêng) chế độ "nói chuyện bằng AI" nếu sau này muốn.

### 1.3 Ngoài phạm vi
- Không điều khiển ứng dụng khác, không đọc màn hình, không Accessibility.
- Không Cloud, không tài khoản, không quảng cáo, không telemetry.
- Không phải game nhiều người.

## 2. Nguyên tắc thiết kế

1. **Nhẹ và riêng tư**: không mạng, không log nội dung, APK nhỏ, pin thấp.
2. **Việt Nam trước**: tiếng Việt mặc định, giọng đọc vi-VN, lời thoại tự nhiên (không dịch máy).
3. **Thú phải "sống"**: hành vi có mục đích, không lặp máy móc; mỗi lần mở thấy khác nhau.
4. **Không chặn người dùng**: bong bóng ngắn, không che nút, có chế độ ẩn/khóa vị trí.
5. **Tách renderer khỏi nghiệp vụ** để sau này làm game hoặc thêm look dễ.

## 3. Kiến trúc kỹ thuật

Một repo mới (đề xuất vudev-pet), 3 module Gradle:

    petcore/     Kotlin thuần: look, pose, renderer, emotion, behavior, needs (không phụ thuộc Android service)
    app/         Android: overlay service, activity chính/settings, TTS, notification, widget
    tools/       Pipeline sinh sprite/animation (Python, không đóng gói vào APK)

### 3.1 Thành phần chính trong app

| Thành phần | Trách nhiệm |
|---|---|
| PetApplication | khởi tạo store, TTS, đọc trạng thái đã lưu |
| PetOverlayService | foreground service sở hữu WindowManager, vòng tick, vẽ thú, xử lý chạm/kéo |
| BehaviorEngine | máy trạng thái hành vi + bộ chọn hành vi theo nhu cầu/thời gian |
| NeedsEngine | tính suy giảm theo thời gian thực, áp dụng offline catch-up |
| MovementEngine | vị trí, vận tốc, hướng, biên màn hình, tránh vùng cấm |
| EmotionBridge | nối mood/hành vi sang renderer (mặt, dáng) |
| SpeechBubble | bong bóng thoại trên overlay, hàng đợi câu nói |
| TtsAdapter | Android TextToSpeech vi-VN, có/không đọc to |
| PetStore | đọc/ghi JSON trạng thái, settings |
| PetHomeActivity | màn hình chăm sóc, onboarding, thống kê, cài đặt |
| ReminderScheduler | AlarmManager/WorkManager cho thông báo đói/buồn |
| PetWidgetProvider | RemoteViews cho widget (giai đoạn 0.2) |

### 3.2 Vòng đời

1. Người dùng mở app lần đầu → onboarding quyền (overlay, thông báo) → đặt tên/chọn look.
2. Bật "Thú sống trên màn hình" → PetOverlayService chạy foreground, thêm cửa sổ overlay.
3. Tick 30–60 fps khi đang di chuyển, giảm còn ~10 fps khi idle; tắt hẳn khi màn hình tắt.
4. Mỗi 60 giây: NeedsEngine cập nhật; BehaviorEngine chọn hành vi mới nếu cần.
5. Mở app chăm sóc → activity đọc chung PetStore; thao tác cho ăn/tắm chuyển ý định sang service qua binder/intent.
6. Thoát app: service vẫn chạy (foreground); trạng thái lưu định kỳ và khi màn hình tắt.

## 4. Tái sử dụng từ Andee (bản đồ cụ thể)

| Lấy | Từ Andee | Việc phải làm |
|---|---|---|
| Renderer bóng + look | core/ui/ball/EmotionBallRenderer.kt (2.641 dòng), BallLook.kt (2.453), Pose.kt (154) | Copy, giữ nguyên header bản quyền; bỏ tham chiếu brain/chat |
| Mood + state machine cảm xúc | Mood.kt (361), EmotionState.kt (717) | Copy; thêm điều kiện từ NeedsEngine |
| Lớp vẽ GL | ui/ball/EmotionBallTextureView.kt (223), AndroidGl.kt | Copy; thêm API đổi look, vị trí, tỉ lệ |
| Thú ảo / persona | core/pet/Pet.kt (184), PetPersonas.kt, ui/PhonePetView.kt (337) | Dùng cho look mèo/chó ở 0.2 |
| Bong bóng + subtitle | Phần bong bóng trong FloatingWindowUi.kt | **Viết lại nhỏ** (~300 dòng), không copy 4k dòng |
| Quản lý cửa sổ overlay | FloatingWindowUi.kt (chỉ phần WindowManager, drag, edge ripple) | Trích ý tưởng, viết lại gọn |
| Sân khấu/nền | Stage.kt, StageView.kt, Backdrop.kt | Tùy chọn cho chế độ "phòng của thú" |
| Thư viện biểu cảm | ExpressionsUi.kt (412) | Dùng làm màn hình thử look |
| Pipeline animation | tools/pet_anim.py, pet_sprites.py, tools/pet-anim/webm | Giữ để sinh animation mới |
| Glass/settings | ui/Glass.kt | Có thể dùng lại style thẻ cài đặt |

**Không lấy**: ScreenBodyService, CommandDispatcher, LocalBrain/LlmClient/ProviderStore, Notebook, BodyWs, ASR, hub.

### 4.1 Ghi chú giấy phép khi tái sử dụng
Andee là Apache-2.0 + điều kiện KUAFUAI. Nếu dùng lại mã/asset:
- Giữ thông báo bản quyền trong các tệp copy, thêm mục "dựa trên Andee của KUAFUAI" trong About/THIRD_PARTY_NOTICES.
- Không xóa tên/icon/thông báo Andee trong phần mềm phái sinh (A.1.a).
- Phát hành thương mại/nhúng vào sản phẩm bán ra cần văn bản đồng ý của KUAFUAI (A.1.d).
- Phương án B: **clean-room** — chỉ học ý tưởng, viết lại renderer/hành vi và tự vẽ asset; khi đó không phụ thuộc điều kiện trên. Quyết định trước khi phát hành, không cần trước khi làm prototype.

## 5. Bộ não hành vi (state machine)

### 5.1 Trạng thái

| Trạng thái | Mô tả | Thời lượng | Thoát khi |
|---|---|---|---|
| IDLE | đứng tại chỗ, thở, chớp mắt, nhìn quanh | 3–12 giây | hết giờ/chạm |
| WANDER | đi tới điểm ngẫu nhiên trong biên | 3–10 giây | tới đích/kẹt |
| SIT | ngồi, đung đưa | 5–20 giây | hết giờ/chạm |
| SLEEP | nhắm mắt, Zzz, hồi năng lượng | 10 phút–2 giờ (theo nhu cầu) | năng lượng đầy/người dùng chạm |
| PLAY | nhảy, lăn, đuổi theo con trỏ | 5–15 giây | hết giờ |
| EAT | ăn, hạt rơi, hạnh phúc | 4 giây | animation xong |
| BATHE | tắm, bọt, rùng mình | 4 giây | animation xong |
| PETTED | được vuốt, mắt cong, tim bay | 2 giây | animation xong |
| DRAGGED | bị kéo, chân đung đưa | theo tay người dùng | thả tay |
| SULK | quay lưng, buồn khi bị bỏ lâu | 10–60 giây | được vuốt/cho ăn |
| TALK | hiện bong bóng câu nói | 2–5 giây | hết câu |

### 5.2 Quy tắc chọn hành vi
- Mỗi 1–2 giây đánh giá lại nếu đang ở trạng thái "có thể ngắt".
- Trọng số: ưu tiên hành vi thỏa nhu cầu thấp nhất (đói cao → tìm ăn; năng lượng thấp → ngủ; vui thấp → PLAY/SULK).
- Thời gian trong ngày: sáng nhiều WANDER/PLAY; tối tăng SLEEP; đêm im lặng.
- Không lặp cùng hành vi quá 3 lần liên tiếp; thêm biến thể (đi trái/phải, ngồi nghiêng).
- Người dùng chạm/vuốt luôn được ưu tiên ngắt hành vi hiện tại (trừ SLEEP sâu có phản ứng chậm).

### 5.3 Chuyển động
- Toạ độ overlay trong biên màn hình, chừa status bar, thanh điều hướng, vùng dock.
- Đi bộ: vận tốc ~40–80 dp/s, có gia tốc nhẹ; lật hướng theo chiều đi.
- Kéo thả: dùng touch của overlay; khi thả, thú rơi nhẹ về "mặt đất" (đáy vùng cho phép) trong 300 ms.
- Người dùng chạm home/mở app khác: thú vẫn ở vị trí cũ; nếu ngoài biên sau khi xoay màn hình thì kéo vào.
- Tránh "kẹt" ở góc: nếu 5 giây không tới đích thì chọn đích mới.

## 6. Hệ nhu cầu

| Nhu cầu | 0–100 | Suy giảm | Hồi phục |
|---|---|---|---|
| Đói (càng cao càng đói) | 0 | +1 mỗi 8 phút (thức), +1 mỗi 20 phút (ngủ) | Ăn: -60; ăn vặt: -25 |
| Vui | 100 | -1 mỗi 10 phút; thêm -5 nếu bị bỏ > 12 giờ | Vuốt: +8; chơi: +20; mini-game: +25 |
| Năng lượng | 100 | -1 mỗi 6 phút (thức); -1 mỗi 2 phút (PLAY) | Ngủ: +10 mỗi 10 phút |
| Sạch sẽ | 100 | -1 mỗi 30 phút | Tắm: +80 |
| Tình cảm | 0–100 | tăng khi tương tác | giảm chậm nếu bỏ bê |

- **Offline catch-up**: khi mở lại, tính theo (now - lastSeen), giới hạn tối đa 24 giờ để tránh thú "chết đói" sau một tuần (chỉ buồn/đói ở mức thấp nhất, không chết).
- Ngưỡng hành vi: đói ≥ 70 → xin ăn; năng lượng ≤ 20 → ngủ; vui ≤ 25 → buồn; sạch ≤ 30 → xin tắm.
- Không có cơ chế "chết": sản phẩm thư giãn, không gây áp lực.

## 7. Giao tiếp offline (bong bóng + TTS)

- Kho câu chia theo ngữ cảnh: chào theo giờ, khi đói, khi buồn ngủ, sau khi ăn, sau khi tắm, khi được vuốt, khi bị kéo, khi lâu không gặp, khi đêm khuya, khi pin yếu máy.
- Mỗi ngữ cảnh 8–15 câu tiếng Việt tự nhiên; tránh lặp bằng cách xoay vòng có trọng số.
- Bong bóng: tối đa 2 dòng, tự ẩn sau 3–5 giây, không che nút; có thể tắt.
- TTS: Android TextToSpeech vi-VN; nếu máy thiếu giọng Việt thì chỉ hiện chữ (không lỗi).
- Cài đặt: bật/tắt tiếng, tốc độ đọc, cao độ, âm lượng; giờ yên tĩnh không đọc.
- (Sau này) nếu muốn "thông minh": thêm tuỳ chọn dùng LLM, tách hoàn toàn khỏi MVP.

## 8. Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Chạm 1 lần | phản ứng ngắn (vui/ngạc nhiên), câu nói theo ngữ cảnh |
| Chạm 2 lần | mở bong bóng "nói chuyện" (câu ngẫu nhiên) |
| Giữ 1 giây | mở menu vòng: Cho ăn · Chơi · Tắm · Trạng thái · Ẩn · Cài đặt |
| Vuốt qua lại trên thú | vuốt ve: mắt cong, tim bay, +vui |
| Kéo | di chuyển thú; thả để đặt |
| Kéo lên mép trên | "ngủ" nhanh (tuỳ chọn) |
| Chạm vào bong bóng | đóng bong bóng |

- Menu thao tác nhanh: 6 nút lớn, dễ bấm; hỗ trợ màn hình nhỏ.
- Chế độ "không làm phiền": thú thu nhỏ vào mép, chỉ ngủ.

## 9. Overlay & vòng đời service

- Cửa sổ TYPE_APPLICATION_OVERLAY, không focusable, không chặn chạm ngoài vùng thú; kích thước theo look.
- Foreground service với notification mức thấp "VUDEV Pet đang chơi cùng bạn"; chạm notification mở app.
- Nhận ACTION_SCREEN_OFF/ON, ACTION_USER_PRESENT để tạm dừng vẽ/tiết kiệm pin.
- Boot receiver (tuỳ chọn): khôi phục overlay sau khởi động lại nếu người dùng bật "luôn chạy".
- Chống bị hệ thống kill: hướng dẫn HyperOS bật autostart + miễn tối ưu pin trong onboarding; tự phục hồi khi service bị kill bằng alarm định kỳ (không lạm dụng).
- Khi xoay màn hình/đổi kích thước: cập nhật biên, kéo thú vào trong.
- Không dùng Accessibility; không đọc nội dung màn hình.

## 10. Lưu trữ & dữ liệu

- pet.json: tên, look, needs, lastSeen, tổng tương tác, chuỗi ngày chăm, vị trí cuối.
- settings.json: kích thước, độ mờ, khóa vị trí, âm thanh, giờ yên tĩnh, ngôn ngữ, thông báo.
- Ghi nguyên tử (ghi tệp tạm rồi rename), có version schema để nâng cấp.
- Sao lưu/khôi phục thủ công (xuất/nhập JSON) ở 0.2.
- Không gửi dữ liệu ra ngoài; không quyền INTERNET.

## 11. Thông báo & widget

- Thông báo khi: đói ≥ 85, buồn ≥ 80, bỏ bê > 24 giờ, thú ngủ dậy (tuỳ chọn).
- Tối đa 2 thông báo/ngày; giờ yên tĩnh 22:00–07:00 (cấu hình).
- Kênh thông báo riêng, ưu tiên thấp; Android 13 xin quyền POST_NOTIFICATIONS.
- Widget (0.2): mặt thú + 3 thanh trạng thái; cập nhật mỗi 30 phút và khi mở app; bấm mở app.

## 12. Mini-game 1 phút (0.2)

- "Hứng bánh": thú chạy theo bánh rơi, chạm để nhặt; 60 giây; kết quả +vui/+năng lượng, mở khoá đồ ăn mới.
- Chạy trong Activity riêng (Canvas view), không ảnh hưởng overlay.
- Không quảng cáo, không mua bằng tiền thật; phần thưởng chỉ là item trong app.

## 13. Cài đặt & cá nhân hoá

- Tên thú, look (bóng biểu cảm / mèo / chó), màu.
- Kích thước (nhỏ/vừa/lớn), độ mờ, khóa vị trí, luôn trên cùng.
- Âm thanh: bật/tắt, giọng đọc, tốc độ, cao độ, giờ yên tĩnh.
- Hành vi: mức năng động (ít/vừa/nhiều), tần suất nói.
- Thông báo: bật/tắt, ngưỡng.
- Ngôn ngữ: vi (mặc định), en, zh — dùng resource, thêm ngôn ngữ không sửa logic.
- Thông tin: phiên bản, giấy phép, ghi công Andee (nếu tái sử dụng mã).

## 14. Hiệu năng, pin, tương thích

- Mục tiêu: RAM < 80 MB, pin < 3%/giờ khi overlay bật, không ANR.
- Vẽ 60 fps khi di chuyển, 10 fps idle, 0 fps khi màn hình tắt.
- Không giữ wake lock; dùng Handler/Choreographer, tránh timer dày.
- APK mục tiêu < 20 MB (không model AI, không mạng).
- Tương thích Android 10–16; test kỹ HyperOS (Redmi 15 5G), Android 16; tai thỏ, cử chỉ, xoay ngang, chế độ tối.
- Không yêu cầu quyền đặc biệt ngoài overlay + thông báo.

## 15. Bảo mật & quyền riêng tư

- Không INTERNET, không analytics, không crash reporting gửi ra ngoài.
- Không lưu nội dung người dùng; chỉ trạng thái thú và cài đặt.
- Quyền tối thiểu; giải thích từng quyền trong onboarding.

## 16. Pháp lý (cổng quyết định trước khi phát hành)

1. Prototype dùng mã Andee: được cho mục đích cá nhân/nội bộ/nghiên cứu; giữ notices.
2. Phát hành công khai dạng phái sinh: phải giữ nhận diện Andee theo A.1.a, ghi rõ đã sửa đổi, kèm LICENSE/NOTICE.
3. Phát hành thương mại/nhúng sản phẩm: cần văn bản đồng ý của KUAFUAI (A.1.d) — hoặc chọn clean-room (viết lại renderer/hành vi, asset tự làm) để sở hữu độc lập.
4. Quyết định này phải chốt **trước** khi phát hành, không cần trước khi làm prototype.

## 17. Kế hoạch milestone

| Mốc | Nội dung | Thời lượng | Tiêu chí nghiệm thu |
|---|---|---|---|
| M0 | Lập repo mới, tách module, ghi nhận quyết định pháp lý | 0,5 ngày | Repo build được app rỗng, có PLAN.md |
| M1 | Đưa renderer vào Activity độc lập: hiện look, chạm đổi cảm xúc, kéo di chuyển | 2–3 ngày | APK cài trên Redmi, 60 fps, 6 look, không crash |
| M2 | Overlay service + đi lại tự động (IDLE/WANDER/SIT) + phản ứng chạm/kéo | 3–4 ngày | Thú nổi trên màn hình chính và app khác, đi trong biên, không chặn chạm |
| M3 | NeedsEngine + offline catch-up + SLEEP/EAT/BATHE + lưu trạng thái | 3–4 ngày | Tắt app 2 giờ mở lại thấy nhu cầu giảm đúng, ngủ hồi năng lượng |
| M4 | Bong bóng + kho câu tiếng Việt + TTS vi-VN + menu thao tác + settings | 3 ngày | Có câu nói theo ngữ cảnh, tắt/bật tiếng, menu 6 nút hoạt động |
| M5 | Onboarding quyền + thông báo đói/buồn + giờ yên tĩnh | 2 ngày | Thông báo đúng ngưỡng, onboarding chạy trên máy sạch |
| M6 | Widget + mini-game 1 phút | 2–3 ngày | Widget cập nhật, mini-game chơi được, cộng điểm đúng |
| M7 | Polish: animation, hiệu ứng, tối ưu pin, test, APK release + CI | 2–3 ngày | Báo cáo test, APK release ký, CI xanh |

Tổng: khoảng **18–22 ngày làm việc** cho một người; **MVP dùng được sau M4 (~2 tuần)**.

## 18. Kiểm thử & nghiệm thu

- Thiết bị chính: Redmi 15 5G (Android 16, HyperOS); thêm 1 máy Android 11–13 nếu có.
- Kịch bản: cài sạch → onboarding → bật overlay → tắt/mở màn hình → chuyển app → xoay ngang → khởi động lại máy → tắt app 2 giờ → thông báo đói → cho ăn → tắm → ngủ → mini-game.
- Kiểm tra pin 1 giờ, RAM, ANR, logcat không lỗi.
- Kiểm tra TTS tiếng Việt trên máy thiếu giọng (chỉ hiện chữ).
- Không dùng Accessibility; xác nhận app không xin quyền đó.

## 19. Rủi ro & phương án

| Rủi ro | Ảnh hưởng | Phương án |
|---|---|---|
| HyperOS kill overlay/service | Thú biến mất | Hướng dẫn autostart + miễn pin; tự phục hồi; thông báo trạng thái |
| Renderer GL phức tạp khi tách | Chậm M1 | Giữ nguyên lớp GL, chỉ thay "nguồn mood" |
| Pin bị hao | Người dùng gỡ | Giảm fps, tắt khi màn hình off, chế độ tiết kiệm |
| Giấy phép khi phát hành | Không được phát hành | Clean-room hoặc xin phép trước |
| Art asset thiếu | Nhìn đơn điệu | Dùng bóng có sẵn (0 art) trước; pipeline sprite có sẵn cho mèo/chó |
| Nội dung lời thoại lặp | Nhàm | Kho câu lớn, xoay vòng theo ngữ cảnh, thêm biến thể |

## 20. Việc cần chủ dự án quyết định

1. **Pháp lý**: dùng lại mã Andee cho prototype rồi xin phép/clean-room trước khi phát hành — hay clean-room ngay từ đầu?
2. **Hình dáng MVP**: bóng biểu cảm (0 art, nhanh nhất) hay mèo/chó sprite (cần thêm animation)?
3. **Tên & package**: tên hiển thị và applicationId (ví dụ com.vudev.pet)?
4. **Phạm vi MVP**: chốt M0–M4 (không widget/mini-game) hay làm tới M6?

## 21. Bước tiếp theo sau khi duyệt

1. Tạo repo vudev-pet + khung 3 module + PLAN.md (chưa code tính năng).
2. M1: dựng app demo hiển thị look + cảm xúc + kéo, cài lên máy đang kết nối để duyệt cảm giác.
3. Duyệt M1 xong mới sang overlay (M2).
