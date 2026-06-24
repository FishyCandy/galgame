package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

/**
 * 带黑色描边的JLabel，用于角色名显示。
 */
public class StrokableLabel extends JLabel {
    private Color outlineColor = Color.BLACK;
    private float outlineWidth = 2.5f;

    public StrokableLabel(String text) {
        super(text);
        setOpaque(false);
    }

    public StrokableLabel() {
        this("");
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        setVisible(text != null && !text.isEmpty());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        String text = getText();
        if (text != null && !text.isEmpty()) {
            Font font = getFont();
            FontMetrics fm = g2.getFontMetrics(font);
            FontRenderContext frc = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(text, font, frc);

            int x;
            switch (getHorizontalAlignment()) {
                case RIGHT:  x = getWidth() - fm.stringWidth(text) - getInsets().right; break;
                case CENTER: x = (getWidth() - fm.stringWidth(text)) / 2; break;
                default:     x = getInsets().left; break;
            }
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

            Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y));
            g2.setColor(outlineColor);
            g2.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(outline);

            g2.setColor(getForeground());
            g2.fill(outline);
        }
        g2.dispose();
    }
}
