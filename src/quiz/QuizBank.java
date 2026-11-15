package quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * Sinh câu hỏi thách đố theo màn. Màn càng cao thì câu hỏi càng khó.
 * Trộn giữa câu đố dân gian và phép tính được sinh ngẫu nhiên.
 */
public final class QuizBank {

    /** Mỗi màn một câu đố: {câu hỏi, đáp án 1..4, chỉ số đáp án đúng}. */
    private static final String[][] RIDDLES = {
            {"Con gì càng to càng nhỏ?", "Con cua", "Con voi", "Con kiến", "Con rùa", "0"},
            {"Cái gì càng rửa càng bẩn?", "Nước", "Xà phòng", "Khăn lau", "Bàn chải", "0"},
            {"Cái gì có răng mà không có miệng?", "Cái lược", "Cái cốc", "Cái ghế", "Cái bút", "0"},
            {"Một năm có bao nhiêu tháng có 28 ngày?",
                    "1 tháng", "6 tháng", "12 tháng", "2 tháng", "2"},
            {"Cái gì đi khắp thế giới mà vẫn ở một chỗ?",
                    "Con tem", "Con chim", "Mặt trời", "Cái bóng", "0"},
            {"5 con mèo bắt 5 con chuột trong 5 phút. Vậy 100 con mèo bắt 100 con chuột trong bao lâu?",
                    "5 phút", "100 phút", "20 phút", "1 phút", "0"},
            {"Cái gì càng đào càng lớn?", "Cái hố", "Cái ao", "Cái giếng", "Cái bóng", "0"},
            {"Bạn có 3 quả táo và lấy 2 quả. Bạn còn bao nhiêu quả?",
                    "1 quả", "2 quả", "3 quả", "0 quả", "1"},
            {"Đi 10km về phía bắc, rẽ phải 10km, rẽ phải 10km, rẽ phải 10km. Bạn đang ở đâu?",
                    "Điểm xuất phát", "Cách điểm xuất phát 10km",
                    "Cách điểm xuất phát 20km", "Không xác định", "0"},
            {"Cái gì thuộc về bạn nhưng người khác dùng nhiều hơn bạn?",
                    "Tên của bạn", "Điện thoại của bạn", "Tiền của bạn", "Nhà của bạn", "0"}
    };

    private final Random random;

    public QuizBank(Random random) {
        this.random = (random != null) ? random : new Random();
    }

    /**
     * Trả về câu hỏi phù hợp với màn hiện tại (1..10).
     */
    public Quiz nextForLevel(int level) {
        int clamped = Math.max(1, Math.min(RIDDLES.length, level));
        if (random.nextInt(100) < 45) {
            return riddle(clamped);
        }
        return arithmetic(clamped);
    }

    private Quiz riddle(int level) {
        String[] data = RIDDLES[level - 1];
        return new Quiz(data[0],
                new String[]{data[1], data[2], data[3], data[4]},
                Integer.parseInt(data[5]));
    }

    private Quiz arithmetic(int level) {
        int a;
        int b;
        int c;
        int d;
        String question;
        int answer;

        switch (level) {
            case 1:
                a = 1 + random.nextInt(9);
                b = 1 + random.nextInt(9);
                question = a + " + " + b + " = ?";
                answer = a + b;
                break;
            case 2:
                a = 10 + random.nextInt(20);
                b = 1 + random.nextInt(9);
                question = a + " - " + b + " = ?";
                answer = a - b;
                break;
            case 3:
                a = 2 + random.nextInt(8);
                b = 2 + random.nextInt(8);
                question = a + " × " + b + " = ?";
                answer = a * b;
                break;
            case 4:
                a = 2 + random.nextInt(9);
                b = 2 + random.nextInt(9);
                c = 1 + random.nextInt(20);
                question = a + " × " + b + " + " + c + " = ?";
                answer = a * b + c;
                break;
            case 5:
                a = 3 + random.nextInt(9);
                b = 2 + random.nextInt(9);
                c = 1 + random.nextInt(a * b - 1);
                question = a + " × " + b + " - " + c + " = ?";
                answer = a * b - c;
                break;
            case 6:
                a = 2 + random.nextInt(9);
                b = 2 + random.nextInt(9);
                c = 2 + random.nextInt(5);
                question = "(" + a + " + " + b + ") × " + c + " = ?";
                answer = (a + b) * c;
                break;
            case 7:
                a = 2 + random.nextInt(9);
                b = 1 + random.nextInt(20);
                question = a + "² + " + b + " = ?";
                answer = a * a + b;
                break;
            case 8:
                a = (1 + random.nextInt(9)) * 10;
                b = (2 + random.nextInt(19)) * 10;
                question = a + "% của " + b + " = ?";
                answer = a * b / 100;
                break;
            case 9:
                a = 10 + random.nextInt(30);
                b = 2 + random.nextInt(8);
                c = 2 + random.nextInt(8);
                d = 1 + random.nextInt(20);
                question = "(" + a + " + " + b + ") × " + c + " - " + d + " = ?";
                answer = (a + b) * c - d;
                break;
            case 10:
            default:
                a = 3 + random.nextInt(12);
                b = 3 + random.nextInt(12);
                c = 2 + random.nextInt(9);
                d = 2 + random.nextInt(9);
                question = a + " × " + b + " + " + c + " × " + d + " = ?";
                answer = a * b + c * d;
                break;
        }

        return numericQuiz(question, answer);
    }

    private Quiz numericQuiz(String question, int answer) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        values.add(answer);
        int guard = 0;
        while (values.size() < 4 && guard++ < 200) {
            int delta;
            switch (random.nextInt(4)) {
                case 0:
                    delta = 1 + random.nextInt(5);
                    break;
                case 1:
                    delta = -(1 + random.nextInt(5));
                    break;
                case 2:
                    delta = 10 + random.nextInt(10);
                    break;
                default:
                    delta = -(10 + random.nextInt(10));
                    break;
            }
            int candidate = answer + delta;
            if (candidate < 0) {
                candidate = Math.abs(candidate) + 1;
            }
            if (candidate != answer) {
                values.add(candidate);
            }
        }
        while (values.size() < 4) {
            values.add(answer + values.size() * 7 + 1);
        }

        List<Integer> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, random);
        String[] options = new String[shuffled.size()];
        int correctIndex = 0;
        for (int i = 0; i < shuffled.size(); i++) {
            options[i] = String.valueOf(shuffled.get(i));
            if (shuffled.get(i) == answer) {
                correctIndex = i;
            }
        }
        return new Quiz(question, options, correctIndex);
    }
}
