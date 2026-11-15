# MINESWEEPER JAVA SWING: PHẦN CHUNG

## 1. Vai trò
Bạn là lập trình viên Java chuyên nghiệp, giỏi OOP, kiến trúc phân tầng và Java Swing.
Hãy xây dựng game **Dò mìn (Minesweeper)** hoàn chỉnh, chạy thực tế trên Desktop,
không phải chương trình minh họa thuật toán.

## 2. Môi trường kỹ thuật
- JDK 17 trở lên, không dùng tính năng preview.
- Chỉ dùng Java thuần và Swing/AWT. Không dùng JavaFX, Maven, Gradle hay thư viện ngoài
  (riêng test được dùng JUnit 5).
- Mỗi file khai báo đúng `package`, mã hóa UTF-8 (có comment tiếng Việt).
- Biên dịch bằng `javac`, chạy bằng `java` hoặc IntelliJ IDEA / NetBeans.

## 3. Cấu trúc thư mục
```text
Minesweeper/
├── src/
│   ├── model/                  (BACKEND)
│   │   ├── CellState.java      enum: HIDDEN, REVEALED, FLAGGED
│   │   ├── Cell.java
│   │   ├── Board.java
│   │   ├── Difficulty.java     enum: BEGINNER, INTERMEDIATE, EXPERT
│   │   ├── GameStatus.java     enum: READY, PLAYING, WON, LOST
│   │   ├── Position.java       record Position(int row, int col)
│   │   └── Game.java           facade duy nhất Controller được gọi
│   ├── controller/             (FRONTEND)
│   │   └── GameController.java
│   ├── view/                   (FRONTEND)
│   │   ├── MainFrame.java
│   │   ├── GamePanel.java
│   │   └── CellButton.java
│   └── Main.java
└── test/model/GameTest.java
```

## 4. Quy tắc phụ thuộc (bắt buộc)
- `model` KHÔNG phụ thuộc `view` hay `controller`.
- `view` KHÔNG gọi trực tiếp `model`. Mọi tương tác đi qua `controller`.
- `controller` là cầu nối duy nhất: nhận sự kiện từ View, gọi Model, rồi yêu cầu View cập nhật.

## 5. Cấp độ
| Cấp độ | Kích thước | Số mìn |
|---|---:|---:|
| Beginner | 9 × 9 | 10 |
| Intermediate | 16 × 16 | 40 |
| Expert | 16 × 30 | 99 |

## 6. HỢP ĐỒNG GIAO TIẾP (API của lớp `Game`, hai bên phải tuân thủ đúng tên)
```java
// Khởi tạo / ván mới
Game(Difficulty difficulty, java.util.Random random)
void newGame(Difficulty difficulty)

// Thao tác (thay đổi trạng thái)
List<Position> reveal(int row, int col)   // trả về danh sách ô vừa được mở (kể cả Flood Fill).
                                          // Rỗng nếu thao tác không hợp lệ.
boolean toggleFlag(int row, int col)      // true nếu thay đổi thành công

// Truy vấn (chỉ đọc)
int getRows()
int getCols()
int getTotalMines()
int getRemainingMines()                   // = tổng mìn - số cờ, được phép âm
GameStatus getStatus()
CellState getCellState(int row, int col)
int getAdjacentMines(int row, int col)    // 0..8
boolean isMine(int row, int col)
boolean isExploded(int row, int col)      // ô mìn mà người chơi đã click trúng
boolean isWrongFlag(int row, int col)     // cờ cắm sai (chỉ có ý nghĩa khi đã thua)
```
Quy ước: `row` từ 0..getRows()-1, `col` từ 0..getCols()-1.

## 7. Quy trình trả lời
Trả lời theo từng lượt, mỗi lượt dừng ở cuối một file hoàn chỉnh (không cắt giữa file)
và chờ tôi nhắn "tiếp tục". Trước mỗi đoạn code phải ghi rõ đường dẫn file
(ví dụ `src/model/Board.java`).

## 8. Tiêu chí nghiệm thu chung
1. Biên dịch được bằng `javac` không cần thư viện ngoài, chạy được trên Desktop.
2. Backend và Frontend tách độc lập đúng mục 4.
3. API khớp đúng mục 6.
4. Không có lỗi biên dịch hay runtime trong thao tác thông thường.