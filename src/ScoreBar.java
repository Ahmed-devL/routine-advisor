import javax.swing.*;
import java.awt.*;

/**
 * A custom-painted horizontal bar showing a 0-100 score.
 * Green = good, amber = fair, red = poor. Painted by hand (instead of using
 * JProgressBar) so the colours look the same on Windows, macOS and Linux.
 */
public class ScoreBar extends JComponent {
    static final Color GOOD = new Color(39, 150, 96);
    static final Color FAIR = new Color(226, 160, 30);
    static final Color POOR = new Color(205, 70, 55);

    private final int value;

    public ScoreBar(int value) {
        this.value = Math.max(0, Math.min(100, value));
        setPreferredSize(new Dimension(220, 16));
    }

    static Color colorFor(int score) {
        if (score >= 75) return GOOD;
        if (score >= 50) return FAIR;
        return POOR;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(225, 232, 227));
        g2.fillRoundRect(0, 0, w, h, h, h);
        int fill = (int) Math.round(w * value / 100.0);
        if (fill > 0) {
            g2.setColor(colorFor(value));
            g2.fillRoundRect(0, 0, Math.max(fill, h), h, h, h);
        }
        g2.dispose();
    }
}
