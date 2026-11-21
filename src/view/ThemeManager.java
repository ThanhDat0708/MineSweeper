package view;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Quản lý bảng màu, font và Look & Feel của toàn bộ giao diện.
 * Mọi thành phần tự vẽ đều đọc màu từ {@link Palette} để đổi theme đồng loạt.
 */
public final class ThemeManager {

    /** Các bộ màu có sẵn. */
    public enum Theme {
        FOREST("Rừng xanh"),
        NIGHT("Đêm huyền bí"),
        AUTUMN("Mùa thu");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** Bảng màu đầy đủ cho board, nhân vật, nút và bộ đếm LED. */
    public record Palette(
            Color appBackground,
            Color boardBackground,
            Color boardFrame,
            Color boardFrameDark,
            Color controlBackground,
            Color controlBorder,
            Color grassTop,
            Color grassBottom,
            Color grassHighlight,
            Color grassShadow,
            Color dirtTop,
            Color dirtBottom,
            Color dirtEdge,
            Color dirtHighlight,
            Color gridLine,
            Color text,
            Color textMuted,
            Color accent,
            Color accentDark,
            Color accentLight,
            Color button,
            Color buttonHover,
            Color buttonBorder,
            Color buttonText,
            Color ledBackground,
            Color ledOn,
            Color ledOff,
            Color flagRed,
            Color flagRedDark,
            Color poleWood,
            Color poleWoodDark,
            Color bombBody,
            Color bombHighlight,
            Color spark,
            Color skyTop,
            Color skyBottom,
            Color hill,
            Color ground,
            Color groundLine,
            Color[] numbers) {

        public Palette {
            numbers = numbers.clone();
        }

        @Override
        public Color[] numbers() {
            return numbers.clone();
        }
    }

    private static final Theme[] THEMES = Theme.values();

    private static final String FONT_FAMILY = pickFontFamily();

    private Theme current = Theme.FOREST;
    private Palette palette = createPalette(current);

    /** Cài FlatLaf nếu có trong classpath; nếu không thì dùng Nimbus/System. */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Throwable flatLafMissing) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Throwable ignored) {
                // Giữ Look & Feel mặc định của hệ điều hành.
            }
        }
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 12);
    }

    public Theme getTheme() {
        return current;
    }

    public Palette getPalette() {
        return palette;
    }

    public String getThemeName() {
        return current.getDisplayName();
    }

    /** Chuyển sang theme kế tiếp và trả về palette mới. */
    public Palette nextTheme() {
        int index = (current.ordinal() + 1) % THEMES.length;
        current = THEMES[index];
        palette = createPalette(current);
        return palette;
    }

    public void setTheme(Theme theme) {
        if (theme == null) {
            return;
        }
        current = theme;
        palette = createPalette(theme);
    }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    private static String pickFontFamily() {
        try {
            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            for (String preferred : new String[]{"Segoe UI", "Tahoma", "SansSerif"}) {
                for (String family : families) {
                    if (family.equalsIgnoreCase(preferred)) {
                        return family;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Môi trường không có GraphicsEnvironment đầy đủ.
        }
        return Font.SANS_SERIF;
    }

    private static Palette createPalette(Theme theme) {
        switch (theme) {
            case NIGHT:
                return new Palette(
                        new Color(15, 21, 36), new Color(25, 33, 52),
                        new Color(49, 42, 66), new Color(31, 26, 44),
                        new Color(23, 31, 49), new Color(77, 98, 142),
                        new Color(67, 113, 101), new Color(37, 75, 70),
                        new Color(119, 176, 160), new Color(21, 48, 46),
                        new Color(79, 64, 71), new Color(126, 104, 102),
                        new Color(51, 41, 47), new Color(166, 142, 134),
                        new Color(29, 41, 59),
                        new Color(226, 237, 255), new Color(149, 166, 197),
                        new Color(118, 178, 255), new Color(58, 103, 170),
                        new Color(188, 221, 255),
                        new Color(38, 52, 78), new Color(52, 72, 106),
                        new Color(96, 126, 174), new Color(232, 241, 255),
                        new Color(13, 19, 31), new Color(100, 216, 255),
                        new Color(34, 47, 65),
                        new Color(232, 72, 82), new Color(158, 34, 43),
                        new Color(135, 91, 56), new Color(78, 48, 27),
                        new Color(20, 20, 30), new Color(112, 124, 158),
                        new Color(140, 226, 255),
                        new Color(18, 28, 57), new Color(57, 78, 120),
                        new Color(31, 46, 80), new Color(26, 40, 59),
                        new Color(43, 62, 87),
                        new Color[]{
                                new Color(96, 176, 255), new Color(104, 224, 140),
                                new Color(255, 110, 110), new Color(198, 140, 255),
                                new Color(236, 144, 102), new Color(80, 224, 224),
                                new Color(236, 240, 255), new Color(170, 180, 200)
                        });
            case AUTUMN:
                return new Palette(
                        new Color(58, 34, 20), new Color(78, 46, 26),
                        new Color(106, 65, 32), new Color(67, 40, 20),
                        new Color(73, 43, 24), new Color(162, 106, 57),
                        new Color(191, 153, 63), new Color(146, 107, 40),
                        new Color(238, 208, 113), new Color(99, 69, 27),
                        new Color(123, 78, 49), new Color(177, 118, 76),
                        new Color(88, 53, 32), new Color(211, 158, 106),
                        new Color(88, 52, 30),
                        new Color(255, 241, 215), new Color(213, 179, 141),
                        new Color(238, 138, 58), new Color(156, 78, 28),
                        new Color(255, 198, 122),
                        new Color(92, 55, 29), new Color(119, 73, 38),
                        new Color(191, 129, 67), new Color(255, 241, 215),
                        new Color(43, 26, 15), new Color(255, 144, 62),
                        new Color(62, 39, 24),
                        new Color(224, 56, 50), new Color(152, 30, 29),
                        new Color(144, 95, 49), new Color(88, 55, 25),
                        new Color(40, 32, 30), new Color(132, 112, 98),
                        new Color(255, 204, 84),
                        new Color(246, 191, 122), new Color(255, 236, 203),
                        new Color(185, 119, 65), new Color(151, 93, 47),
                        new Color(120, 73, 37),
                        new Color[]{
                                new Color(48, 112, 224), new Color(52, 158, 72),
                                new Color(222, 62, 50), new Color(164, 94, 224),
                                new Color(178, 76, 48), new Color(34, 164, 164),
                                new Color(80, 58, 44), new Color(132, 124, 118)
                        });
            case FOREST:
            default:
                return new Palette(
                        new Color(27, 43, 31), new Color(45, 71, 42),
                        new Color(78, 51, 29), new Color(52, 34, 19),
                        new Color(36, 57, 39), new Color(99, 141, 83),
                        new Color(118, 190, 86), new Color(72, 142, 54),
                        new Color(175, 228, 130), new Color(43, 94, 35),
                        new Color(124, 90, 57), new Color(181, 140, 94),
                        new Color(92, 64, 40), new Color(207, 168, 121),
                        new Color(57, 86, 49),
                        new Color(240, 247, 228), new Color(176, 198, 166),
                        new Color(241, 181, 63), new Color(166, 110, 28),
                        new Color(255, 224, 136),
                        new Color(50, 78, 52), new Color(65, 99, 66),
                        new Color(120, 164, 100), new Color(244, 250, 234),
                        new Color(18, 27, 18), new Color(255, 88, 64),
                        new Color(54, 38, 32),
                        new Color(226, 54, 54), new Color(160, 30, 30),
                        new Color(143, 95, 48), new Color(88, 54, 24),
                        new Color(38, 38, 44), new Color(112, 112, 122),
                        new Color(255, 198, 64),
                        new Color(151, 203, 235), new Color(232, 246, 255),
                        new Color(112, 172, 104), new Color(92, 156, 74),
                        new Color(72, 128, 58),
                        new Color[]{
                                new Color(56, 132, 255), new Color(58, 190, 90),
                                new Color(238, 74, 74), new Color(176, 107, 240),
                                new Color(196, 92, 66), new Color(44, 190, 190),
                                new Color(235, 235, 235), new Color(160, 165, 170)
                        });
        }
    }

    @Override
    public String toString() {
        return Arrays.asList(THEMES).indexOf(current) + ": " + current.getDisplayName();
    }
}
