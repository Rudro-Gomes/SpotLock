package ui;

import model.SlotStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

// Shared Swing styling for ParkVault: palette, fonts, and the custom-painted
// header, cards, buttons and parking-slot glyphs. Keeps every screen consistent.
public final class UITheme {

    // ---- Palette: deep navy with a gold accent ----
    public static final Color BACKGROUND = new Color(0xF4, 0xF6, 0xFA);
    public static final Color CARD = Color.WHITE;
    public static final Color PRIMARY = new Color(0x16, 0x25, 0x47);
    public static final Color PRIMARY_LIGHT = new Color(0x2C, 0x43, 0x74);
    public static final Color PRIMARY_DARK = new Color(0x0E, 0x19, 0x33);
    public static final Color ACCENT = new Color(0xE0, 0xAE, 0x3C);
    public static final Color ACCENT_DARK = new Color(0xC2, 0x93, 0x2B);
    public static final Color BLUE = new Color(0x2E, 0x74, 0xD6);
    public static final Color SUCCESS = new Color(0x2F, 0xA3, 0x6B);
    public static final Color SUCCESS_DARK = new Color(0x24, 0x82, 0x54);
    public static final Color DANGER = new Color(0xD1, 0x4B, 0x45);
    public static final Color DISABLED_GREY = new Color(0x9A, 0xA2, 0xAD);
    public static final Color TEXT = new Color(0x1E, 0x27, 0x33);
    public static final Color TEXT_MUTED = new Color(0x7A, 0x85, 0x93);
    public static final Color BORDER = new Color(0xE2, 0xE7, 0xEE);

    private static final String FAMILY = pickFamily(
            "Inter", "SF Pro Text", "Helvetica Neue", "Segoe UI", "SansSerif");

    public static final Font FONT_TITLE = new Font(FAMILY, Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font FONT_HEADER = new Font(FAMILY, Font.BOLD, 15);
    public static final Font FONT_BODY = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font(FAMILY, Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font FONT_BUTTON = new Font(FAMILY, Font.BOLD, 13);
    public static final Font FONT_SLOT = new Font(FAMILY, Font.BOLD, 12);
    public static final Font FONT_EYEBROW = tracked(new Font(FAMILY, Font.BOLD, 11), 0.10);

    private static final int RADIUS = 14;
    private static final int SHADOW = 5;

    private UITheme() {
    }

    private static String pickFamily(String... candidates) {
        java.util.List<String> available = java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : candidates) {
            if (available.contains(name)) {
                return name;
            }
        }
        return "SansSerif";
    }

    private static Font tracked(Font font, double tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return font.deriveFont(attrs);
    }

    // Formats an amount of money the ParkVault way, e.g. "Tk.120.00".
    public static String money(double amount) {
        return "Tk." + String.format("%.2f", amount);
    }

    static Color mix(Color a, Color b, double ratio) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * ratio),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * ratio),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * ratio));
    }

    public static void apply() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }

        UIManager.put("control", BACKGROUND);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", new Color(0xC7, 0xCE, 0xD6));
        UIManager.put("nimbusFocus", BLUE);
        UIManager.put("nimbusSelectionBackground", BLUE);
        UIManager.put("text", TEXT);
        UIManager.put("info", CARD);

        UIManager.put("List.background", CARD);
        UIManager.put("List.font", FONT_BODY);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TableHeader.font", FONT_BODY_BOLD);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("ScrollPane.background", CARD);

        UIManager.put("OptionPane.background", CARD);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BUTTON);
        UIManager.put("OptionPane.border", new EmptyBorder(18, 18, 14, 18));
        UIManager.put("TextField.font", FONT_BODY);
    }

    // ---- Header: navy gradient with a gold rule along the bottom ----
    public static class Header extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), PRIMARY_LIGHT));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(ACCENT);
            g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JPanel headerPanel(String title, String subtitle) {
        Header header = new Header();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(20, 26, 22, 26));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);

        if (subtitle != null) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(FONT_SUBTITLE);
            subtitleLabel.setForeground(new Color(0xB9, 0xC7, 0xDE));
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitleLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
            header.add(subtitleLabel);
        }
        return header;
    }

    // ---- Card: rounded white surface with a soft drop shadow ----
    public static class Card extends JPanel {
        public Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBackground(CARD);
            setBorder(new EmptyBorder(16, 18, 16 + SHADOW, 18 + SHADOW));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - SHADOW;
            int h = getHeight() - SHADOW;

            for (int i = 0; i < SHADOW; i++) {
                g2.setColor(new Color(30, 39, 51, 7));
                g2.fillRoundRect(i, i + 1, w, h, RADIUS, RADIUS);
            }
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JPanel cardPanel(LayoutManager layout) {
        return new Card(layout);
    }

    // Small uppercase label that sits above a control.
    public static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(FONT_EYEBROW);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    // ---- Buttons ----
    public static class RoundedButton extends JButton {
        private final Color base;
        private final Color hover;
        private final Color pressed;
        private Color outline;

        public RoundedButton(String text, Color base, Color hover, Color pressed) {
            super(text);
            this.base = base;
            this.hover = hover;
            this.pressed = pressed;
            setFont(FONT_BUTTON);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(11, 22, 11, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setOutline(Color outline) {
            this.outline = outline;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = base;
            if (!isEnabled()) {
                fill = new Color(0xC4, 0xCA, 0xD2);
            } else if (getModel().isPressed()) {
                fill = pressed;
            } else if (getModel().isRollover()) {
                fill = hover;
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static RoundedButton primaryButton(String text) {
        return new RoundedButton(text, PRIMARY, PRIMARY_LIGHT, PRIMARY_DARK);
    }

    public static RoundedButton accentButton(String text) {
        RoundedButton button = new RoundedButton(text, ACCENT, mix(ACCENT, Color.WHITE, 0.15), ACCENT_DARK);
        button.setForeground(PRIMARY_DARK);
        return button;
    }

    public static RoundedButton successButton(String text) {
        return new RoundedButton(text, SUCCESS, mix(SUCCESS, Color.WHITE, 0.12), SUCCESS_DARK);
    }

    // Muted style for secondary actions, so they do not compete with the main one.
    public static RoundedButton secondaryButton(String text) {
        RoundedButton button = new RoundedButton(text,
                CARD,
                new Color(0xEE, 0xF1, 0xF6),
                new Color(0xDF, 0xE4, 0xEB));
        button.setForeground(TEXT);
        button.setOutline(new Color(0xD3, 0xDA, 0xE3));
        return button;
    }

    // Base fill colour for a slot in a given status.
    public static Color colorForStatus(SlotStatus status) {
        switch (status) {
            case FREE:     return SUCCESS;
            case OCCUPIED: return DANGER;
            default:       return DISABLED_GREY;
        }
    }

    // ---- Parking slot glyph: a rounded space with a small kerb line ----
    public static class SlotButton extends JButton {
        private final SlotStatus status;
        private final boolean interactive;

        public SlotButton(String code, SlotStatus status) {
            super(code);
            this.status = status;
            this.interactive = status != SlotStatus.DISABLED;
            setFont(FONT_SLOT);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setPreferredSize(new Dimension(72, 54));
            if (interactive) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setEnabled(false);
            }
            setToolTipText("Slot " + code + " - " + status);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = colorForStatus(status);
            if (interactive && getModel().isPressed()) {
                base = mix(base, Color.BLACK, 0.18);
            } else if (interactive && getModel().isRollover()) {
                base = mix(base, Color.WHITE, 0.16);
            }

            int w = getWidth();
            int h = getHeight();

            // Parking space body plus a darker kerb bar at the bottom.
            g2.setColor(base);
            g2.fillRoundRect(0, 0, w, h - 5, 10, 10);
            g2.setColor(mix(base, Color.BLACK, 0.22));
            g2.fillRoundRect(4, h - 5, w - 8, 4, 3, 3);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Rounded colour chip used by the legend.
    public static class Swatch extends JPanel {
        private final Color color;

        public Swatch(Color color) {
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(14, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 1, 14, 12, 5, 5);
            g2.dispose();
        }
    }
}
