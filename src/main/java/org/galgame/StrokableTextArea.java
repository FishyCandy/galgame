package org.galgame;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.text.AttributedString;

/**
 * 带黑色描边的JTextArea，用于游戏对话框台词显示。
 */
public class StrokableTextArea extends JTextArea {
    private Color outlineColor = Color.BLACK;
    private float outlineWidth = 2.5f;

    public StrokableTextArea() {
        setOpaque(false);
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFocusable(false);
        setHighlighter(null);
        setCaretColor(new Color(0, 0, 0, 0));
        getCaret().setVisible(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        Font defaultFont = new Font("微软雅黑", Font.PLAIN, 22);
        super.setFont(defaultFont);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        String text = getText();
        if (text == null || text.isEmpty()) {
            g2.dispose();
            return;
        }

        Insets insets = getInsets();
        float x = insets.left;
        float y = insets.top;
        Font currentFont = getFont();
        FontRenderContext frc = g2.getFontRenderContext();
        int maxWidth = getWidth() - insets.left - insets.right;
        if (maxWidth <= 0) maxWidth = 400;

        try {
            AttributedString as = new AttributedString(text);
            as.addAttribute(java.awt.font.TextAttribute.FONT, currentFont);
            java.awt.font.LineBreakMeasurer measurer =
                new java.awt.font.LineBreakMeasurer(as.getIterator(), frc);

            while (measurer.getPosition() < text.length()) {
                TextLayout layout = measurer.nextLayout(maxWidth);
                if (layout == null) break;

                float ascent = layout.getAscent();
                Shape outline = layout.getOutline(
                    AffineTransform.getTranslateInstance(x, y + ascent));

                // 先绘制黑色描边
                g2.setColor(outlineColor);
                g2.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);

                // 再绘制文字本体
                g2.setColor(getForeground());
                g2.fill(outline);

                y += layout.getAscent() + layout.getDescent() + layout.getLeading();
            }
        } catch (Exception e) {
            // 回退到默认绘制
            super.paintComponent(g2);
        }
        g2.dispose();
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        setCaretPosition(0);
    }
}