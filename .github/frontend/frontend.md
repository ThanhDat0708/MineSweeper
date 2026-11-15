# FRONTEND: VIEW + CONTROLLER (packages `view`, `controller`, và `Main.java`)

(Đọc kèm `00_CHUNG.md`. Backend `model` đã được viết sẵn; chỉ gọi qua API ở mục 6
của file chung. KHÔNG viết lại hay sửa logic Model.)

## 1. Nhiệm vụ
Xây dựng giao diện Swing và xử lý tương tác người chơi.
Frontend KHÔNG tự xử lý thuật toán dò mìn; mọi dữ liệu lấy từ `Game`.

## 2. Thành phần Swing
`JFrame` (cửa sổ chính), `JPanel` (chia khu vực), `JButton` (mỗi ô), `GridLayout` (lưới),
`JLabel` (thời gian, số mìn còn lại, trạng thái).

## 3. Phân vai
- `GameController`: giữ tham chiếu `Game`, xử lý sự kiện từ View, gọi Model,
  rồi yêu cầu View cập nhật. Quản lý `javax.swing.Timer`.
- `MainFrame`: cửa sổ chính, thanh trên, chọn cấp độ, nút New Game.
- `GamePanel`: lưới các `CellButton`, có phương thức cập nhật ô theo yêu cầu của Controller.
- `CellButton`: vẽ một ô (số, cờ, mìn), tự vẽ bằng `Graphics2D`.
- `Main`: khởi tạo bằng `SwingUtilities.invokeLater`.

View chỉ có các phương thức "hiển thị" (ví dụ `showCell(row, col, state, adjacent, isMine, ...)`,
`setMineCounter(int)`, `setTime(int)`, `rebuildGrid(rows, cols)`). Nêu rõ tên các phương thức này.
View không chứa logic dò mìn.

## 4. Chức năng giao diện
1. Hiển thị bàn cờ dạng lưới.
2. Chuột trái mở ô; chuột phải cắm/gỡ cờ.
3. Hiển thị số 1–8 với màu khác nhau (1 xanh dương, 2 xanh lá, 3 đỏ, 4 xanh navy,
   5 nâu đỏ, 6 teal, 7 đen, 8 xám).
4. Ô mìn hiện biểu tượng mìn khi thua; ô mìn bị click có nền đỏ;
   cờ sai hiển thị biểu tượng riêng (mìn bị gạch chéo).
5. Ô có cờ hiển thị biểu tượng cờ.
6. Cập nhật giao diện ngay sau mỗi thao tác.
7. `JOptionPane` thông báo thắng (kèm thời gian) hoặc thua.
8. Nút New Game; chọn cấp độ bằng menu hoặc `JComboBox`.
9. Bộ đếm thời gian.

## 5. Quy tắc kỹ thuật
- **Chuột phải:** dùng `MouseListener`/`MouseAdapter` với `SwingUtilities.isRightMouseButton`,
  KHÔNG dùng `ActionListener`.
- **Timer:** `javax.swing.Timer` tick 1 giây.
  - Chỉ bắt đầu ở lần mở ô đầu tiên (không phải lúc bấm New Game; cắm cờ trước chưa tính giờ).
  - Dừng khi thắng/thua; về 0 khi New Game; tối đa hiển thị 999.
  - Khi New Game hoặc đổi cấp độ phải dừng Timer cũ, không để nhiều Timer chạy chồng.
- Mọi thao tác UI chạy trên EDT.
- Đổi cấp độ: tạo lại lưới, gọi `pack()`, `setLocationRelativeTo(null)`.
- Chỉ cập nhật các ô thay đổi (dựa trên `List<Position>` mà `reveal` trả về), không vẽ lại toàn bàn mỗi lần,
  trừ khi thắng/thua (cần hiện mìn, cờ sai).
- Sau khi game kết thúc, bỏ qua click trên bàn cờ.

## 6. Giao diện (UI/UX)
- Phong cách Desktop game gọn gàng. Thanh trên: **số mìn còn lại | nút New Game | thời gian | chọn cấp độ**;
  bên dưới là bàn cờ.
- Ô vuông cố định khoảng 30–32 px, căn chỉnh chính xác. `setResizable(false)`.
- Ô chưa mở và ô đã mở khác màu nền/viền rõ ràng.
- Biểu tượng mìn/cờ vẽ bằng `Graphics2D` (override `paintComponent` của `CellButton`),
  KHÔNG dùng emoji/font vì có thể lỗi hiển thị.
- Số mìn còn lại hiển thị được số âm; định dạng 3 chữ số (ví dụ `010`, `-02`).

## 7. Chất lượng code
- Code biên dịch và chạy được, không pseudocode, không TODO.
- Comment tiếng Việt cho luồng xử lý click và quản lý Timer.
- Xử lý biên: click liên tiếp nhanh, đổi cấp độ giữa ván, click sau khi kết thúc.

## 8. Giải thích cần đi kèm
- Cách Frontend và Backend giao tiếp, đặc biệt luồng xử lý một cú click chuột:
  View → Controller → Game → trả kết quả → Controller → View cập nhật.
- Hướng dẫn tạo project, cấu hình JDK, biên dịch và chạy bằng IntelliJ IDEA / NetBeans
  và bằng dòng lệnh `javac` / `java`.
- Hướng dẫn kiểm thử thủ công các chức năng và các lỗi có thể gặp.

## 9. Tùy chọn mở rộng (làm sau khi phần chính hoàn tất)
Chế độ Custom, chord click, cờ dấu hỏi (?), lưu thời gian kỷ lục theo cấp độ.
(Lưu ý: Custom, chord click, dấu hỏi cần Backend bổ sung API tương ứng.)

## 10. Tiêu chí nghiệm thu Frontend
1. `view` không gọi trực tiếp `model`; Controller là cầu nối duy nhất.
2. Chuột trái/phải hoạt động đúng; cờ hiển thị đúng.
3. Hiển thị đúng mìn, cờ sai, nền đỏ ô bị click khi thua.
4. Timer chạy đúng (bắt đầu ở click đầu, dừng khi kết thúc, tối đa 999), không bị chồng Timer.
5. Đổi cấp độ và New Game hoạt động bình thường, cửa sổ co giãn đúng.
6. Không lỗi runtime trong thao tác thông thường.