package quiz;

/**
 * Một câu hỏi trắc nghiệm dùng khi người chơi mở trúng bom.
 */
public record Quiz(String question, String[] options, int correctIndex) {

    public Quiz {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Câu hỏi không được rỗng");
        }
        if (options == null || options.length < 2) {
            throw new IllegalArgumentException("Cần ít nhất 2 đáp án");
        }
        options = options.clone();
        if (correctIndex < 0 || correctIndex >= options.length) {
            throw new IllegalArgumentException("Chỉ số đáp án đúng không hợp lệ");
        }
    }

    @Override
    public String[] options() {
        return options.clone();
    }
}
