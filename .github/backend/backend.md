# BACKEND: MODEL / GAME LOGIC (package `model`)

(Đọc kèm `00_CHUNG.md`. Chỉ viết package `model` và test. Chưa viết giao diện.)

## 1. Nhiệm vụ
Xử lý toàn bộ nghiệp vụ trò chơi, không phụ thuộc giao diện.

## 2. Chức năng bắt buộc
- Quản lý bàn cờ bằng mảng hai chiều (2D array).
- `Board` nhận `java.util.Random` (hoặc seed) qua constructor để test lặp lại được.
- **Sinh mìn lười (lazy):** chỉ sinh mìn SAU lần mở ô đầu tiên.
  Ô được click và cả 8 ô lân cận đều không có mìn (như Minesweeper chuẩn).
  Nếu bàn quá nhiều mìn không đủ chỗ, chỉ cần đảm bảo riêng ô được click an toàn.
- Tính số mìn xung quanh mỗi ô (tối đa 8 ô lân cận), xử lý đúng ở góc và cạnh.
- **Flood Fill dùng BFS với `ArrayDeque`, không đệ quy** (tránh `StackOverflowError` ở Expert).
  Mỗi ô chỉ xử lý một lần, không mở ô đang có cờ, không mở ô mìn.
- Quản lý trạng thái từng ô (`CellState`), số mìn còn lại (tổng mìn - số cờ, được phép âm)
  và trạng thái game (`GameStatus`).
- Thắng: số ô đã mở = tổng ô - tổng mìn. Thua: mở trúng mìn.
- Khi thắng: tự động cắm cờ các mìn còn lại (remaining = 0).
- Khi thua: ghi nhận ô mìn bị click (`isExploded`) và cờ sai (`isWrongFlag`).

## 3. Quy tắc nghiệp vụ
- Không mở lại ô đã mở. Không cắm cờ lên ô đã mở.
- Ô đang có cờ không thể mở bằng `reveal` (phải gỡ cờ trước).
- Game đã WON/LOST thì `reveal` và `toggleFlag` không làm gì (trả về rỗng / false).
- `GameStatus`: READY (chưa click) → PLAYING (sau click đầu) → WON/LOST.
- `toggleFlag` ở trạng thái READY vẫn được phép.
- Tọa độ ngoài bàn cờ: không ném lỗi khi gọi thao tác, trả về rỗng / false.
  Các hàm truy vấn có thể ném `IndexOutOfBoundsException` kèm thông báo rõ ràng.

## 4. Ràng buộc kỹ thuật
- TUYỆT ĐỐI không import `javax.swing.*` hay `java.awt.*` (kể cả `java.awt.Point`;
  dùng `Position` record thay thế). Không có JFrame, JPanel, JButton.
- Chỉ trả dữ liệu chỉ đọc ra ngoài. Không để lộ đối tượng `Cell` có thể sửa đổi.
- Class `Game` là facade, cung cấp ĐÚNG các phương thức trong hợp đồng ở `00_CHUNG.md` mục 6.
- Encapsulation: field `private`, constructor hợp lý, không dùng magic number.
- `Difficulty` chứa sẵn rows, cols, mines.
- Comment tiếng Việt cho: sinh mìn, tính số mìn kề, Flood Fill, kiểm tra thắng/thua.

## 5. Kiểm thử (`test/model/GameTest.java`)
JUnit 5 (hoặc lớp test thuần có `main`), dùng `Random` có seed cố định. Tối thiểu kiểm tra:
- Click đầu không trúng mìn (lặp nhiều seed, kể cả 8 ô lân cận).
- Tổng mìn đúng bằng cấu hình của từng cấp độ.
- Số mìn kề đúng ở góc, cạnh, giữa.
- Flood Fill không mở ô có cờ, không mở ô mìn, không lặp vô hạn.
- Thắng/thua đúng; không thao tác được sau khi kết thúc.
- Số mìn còn lại đúng (kể cả khi cắm quá nhiều cờ, ra số âm).
- Không cắm cờ lên ô đã mở; không mở ô đang có cờ.

## 6. Giải thích cần đi kèm
Sau code, giải thích ngắn thuật toán: sinh mìn + click đầu an toàn, tính số mìn kề,
Flood Fill (BFS), kiểm tra thắng/thua.

## 7. Tiêu chí nghiệm thu Backend
1. `model` không import Swing/AWT.
2. Click đầu luôn an toàn (3×3 khi đủ chỗ).
3. Flood Fill BFS chính xác, không đệ quy, không mở ô có cờ.
4. Thắng/thua, cờ, số mìn còn lại (kể cả âm) chính xác.
5. Toàn bộ test đều qua, chạy được không cần giao diện.