# Minesweeper - Khu rừng bom

Minesweeper - Khu rừng bom là trò chơi dò mìn được viết bằng Java và Java Swing. Dự án giữ luật dò mìn cơ bản nhưng bổ sung 10 màn chơi cho mỗi chế độ, ba mạng, câu đố trắc nghiệm, gợi ý, lưu tiến trình, bảng xếp hạng, theme giao diện và âm thanh.

## Chơi ngay trên trình duyệt

**[▶ Chơi Minesweeper - Khu rừng bom](https://thanhdat0708.github.io/MineSweeper/)**

Bản web mở trực tiếp trên trình duyệt, không cần tải game hay cài Java. Bản này dành
cho trải nghiệm nhanh và giữ các luật chính như ba chế độ, màn tăng độ khó, mở đầu an
toàn, cờ, dấu hỏi, gợi ý, ba mạng và câu đố khi gặp bom. Thành tích bản web được lưu
trên trình duyệt đang chơi.

Ứng dụng Java Swing trong repository là bản desktop đầy đủ; hai bản chạy riêng và dữ
liệu lưu/xếp hạng không đồng bộ với nhau. Nếu trang chơi chưa mở được, có thể GitHub
Pages chưa hoàn tất triển khai; hãy thử lại sau khi workflow **Deploy browser game to
GitHub Pages** chạy thành công.

## Tính năng

- Ba chế độ: Beginner, Intermediate và Expert.
- Mười màn cho mỗi chế độ với số bom tăng dần.
- Lần mở ô đầu tiên luôn an toàn trong vùng 3x3.
- Tự động mở các ô trống bằng thuật toán BFS.
- Cắm cờ, đánh dấu dấu hỏi và chức năng chord.
- Khi mở trúng bom, người chơi phải trả lời câu đố:
  - Trả lời đúng: bom được vô hiệu hóa.
  - Trả lời sai: bom nổ và mất một mạng.
- Ba mạng trong mỗi màn; sang màn mới sẽ giữ số mạng còn lại.
- Tối đa hai gợi ý an toàn cho mỗi màn.
- Tạm dừng, lưu tiến trình, đổi theme, bật/tắt âm thanh và điều chỉnh âm lượng.
- Bảng xếp hạng được lưu trong `leaderboard.txt`.

## Yêu cầu môi trường

- Java JDK hỗ trợ Java 17 trở lên.
- Hệ điều hành có hỗ trợ Java Swing.
- Thiết bị âm thanh là tùy chọn.
- Quyền đọc và ghi file trong thư mục chạy ứng dụng.

Kiểm tra Java đã cài đặt:

```bat
java -version
javac -version
```

## Cách tải dự án

Clone bằng Git:

```bat
git clone https://github.com/ThanhDat0708/MineSweeper.git
cd MineSweeper
```

Hoặc tải ZIP từ GitHub, giải nén rồi mở thư mục dự án bằng VS Code hoặc IDE Java.

## Cách chạy

### Chạy bằng VS Code

1. Mở thư mục dự án trong VS Code.
2. Mở file `src\Main.java`.
3. Chọn **Run Java** hoặc nhấn nút Run ở trên lớp `Main`.

### Chạy bằng Command Prompt

Tại thư mục gốc `MineSweeper`, chạy:

```bat
javac -encoding UTF-8 -d out src\Main.java src\model\*.java src\controller\*.java src\quiz\*.java src\score\*.java src\sound\*.java src\view\*.java
java -cp out Main
```

PowerShell có thể dùng cú pháp biên dịch tất cả file nguồn:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem src,test -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp out Main
```

Lệnh trên sẽ tạo các file bytecode trong thư mục `out`.

## Hướng dẫn chơi

### 1. Bắt đầu

1. Nhập tên người chơi.
2. Chọn Beginner, Intermediate hoặc Expert.
3. Nhấn **Bắt đầu**.

Tên người chơi đã từng xuất hiện trong bảng xếp hạng có thể được chọn lại trong ô nhập tên.

### 2. Bàn cờ

- Chuột trái: mở ô.
- Chuột phải: thay đổi trạng thái ô theo chuỗi `Ẩn -> Cờ -> Dấu hỏi -> Ẩn`.
- Nhấp chuột trái lên ô số: mở các ô xung quanh nếu đã cắm đủ số cờ tương ứng.

Số trên ô cho biết số bom nằm trong tối đa tám ô lân cận.

| Ô hiển thị | Ý nghĩa |
|---|---|
| Ô ẩn | Chưa được mở |
| Số | Số bom lân cận |
| Cờ | Người chơi cho rằng ô này có bom |
| Dấu hỏi | Người chơi chưa chắc chắn |
| Ô trống | Không có bom kề |
| Bom gỡ | Bom đã bị vô hiệu hóa |
| Bom nổ | Bom đã nổ sau câu trả lời sai |

### 3. Khi gặp bom

Khi mở trúng bom, trò chơi không kết thúc ngay. Một hộp thoại câu đố sẽ xuất hiện:

- Chọn đáp án đúng để gỡ bom.
- Chọn đáp án sai để bom nổ và mất một mạng.
- Đồng hồ tạm dừng trong khi người chơi trả lời.

Khi người chơi hết ba mạng, lượt chơi kết thúc.

### 4. Hoàn thành màn

Người chơi thắng màn khi mở hết tất cả ô an toàn. Các bom còn ẩn sẽ tự động được cắm cờ.

- Màn kế tiếp có số bom tăng hơn.
- Số mạng còn lại được giữ nguyên.
- Thời gian mỗi màn được cộng vào tổng thời gian lượt chơi.
- Hoàn thành màn 10 để ghi thành tích cao nhất của chế độ.

### 5. Chức năng hỗ trợ

- **Gợi ý:** mở sáng ngẫu nhiên một ô an toàn. Mỗi màn có tối đa hai gợi ý.
- **Tạm dừng:** dừng đồng hồ và khóa thao tác chơi.
- **Lưu:** lưu trạng thái bàn cờ, mạng, màn và thời gian vào `savegame.dat`.
- **Xếp hạng:** xem thành tích đã lưu.
- **Đổi theme:** thay đổi bộ màu giao diện.
- **Âm thanh:** bật/tắt âm thanh hoặc điều chỉnh âm lượng.

## Phím tắt

| Phím | Chức năng |
|---|---|
| `Chuột trái` | Mở ô |
| `Chuột phải` | Chuyển trạng thái ô |
| `H` | Gợi ý |
| `Space` | Tạm dừng hoặc tiếp tục |
| `Ctrl+S` | Lưu tiến trình |

## Chế độ chơi

| Chế độ | Kích thước | Bom màn 1 | Bom màn 10 |
|---|---:|---:|---:|
| Beginner | 9x9 | 8 | 17 |
| Intermediate | 16x16 | 28 | 46 |
| Expert | 16x30 | 65 | 101 |

Số bom tăng dần theo công thức cấu hình trong `Difficulty` và được giới hạn trong 10 màn.

## Kiểm thử

Dự án có hai bộ kiểm thử Java thuần, không cần JUnit:

```bat
javac -encoding UTF-8 -d out src\model\*.java test\model\GameTest.java
java -cp out model.GameTest

javac -encoding UTF-8 -d out src\score\*.java test\score\LeaderboardTest.java
java -cp out score.LeaderboardTest
```

Biên dịch và chạy đồng thời tất cả mã nguồn bằng PowerShell:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem src,test -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp out model.GameTest
java -cp out score.LeaderboardTest
```

Kết quả kiểm thử thực tế:

- `model.GameTest`: 10.484 lượt kiểm tra, 0 lỗi.
- `score.LeaderboardTest`: 29 lượt kiểm tra, 0 lỗi.

## Cấu trúc thư mục

```text
MineSweeper/
├── src/
│   ├── Main.java
│   ├── controller/       # Điều phối luồng game
│   ├── model/            # Bàn cờ, ô, game và chế độ
│   ├── quiz/             # Câu đố và ngân hàng câu hỏi
│   ├── score/            # Thành tích và bảng xếp hạng
│   ├── sound/            # Âm thanh tổng hợp
│   └── view/             # Giao diện Swing và theme
├── test/
│   ├── model/            # Kiểm thử model
│   └── score/            # Kiểm thử bảng xếp hạng
├── leaderboard.txt       # Dữ liệu bảng xếp hạng
├── savegame.dat          # Tiến trình đã lưu
└── README.md
```

## Lưu trữ dữ liệu

- `leaderboard.txt`: lưu thành tích người chơi bằng text UTF-8.
- `savegame.dat`: lưu trạng thái game bằng Java Serialization.

Hai file này được tạo hoặc cập nhật khi người chơi ghi thành tích hoặc lưu game. Không chia sẻ hoặc commit dữ liệu cá nhân nếu không cần.
