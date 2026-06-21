package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class OutlineTextArea extends JPanel {
    private String text = "";
    private Color textColor = Color.WHITE;
    private Color outlineColor = Color.BLACK;
    private float outlineWidth = 2.5f;
    private Font textFont;
    private List<TextLayout> lines = new ArrayList<>();

    public OutlineTextArea() {
        setOpaque(false);
        textFont = new Font("微软雅黑", Font.PLAIN, 22);
    }

    public OutlineTextArea(String initialText) {
        this();
        setText(initialText);
    }

    public void setText(String text) {
        this.text = (text != null) ? text : "";
        lines.clear();
        repaint();
    }

    public String getText() { return text; }

    @Override
    public void setFont(Font font) {
        this.textFont = font;
        super.setFont(font);
        lines.clear();
        repaint();
    }

    @Override
    public Font getFont() { return textFont; }

    @Override
    public void setForeground(Color color) {
        this.textColor = color;
        super.setForeground(color);
        repaint();
    }

    @Override
    public Color getForeground() { return textColor; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        if (text == null || text.isEmpty()) { g2.dispose(); return; }

        Insets insets = getInsets();
        float x = insets.left;
        float y = insets.top;
        FontRenderContext frc = g2.getFontRenderContext();
        float wrappingWidth = Math.max(100, getWidth() - insets.left - insets.right);

        AttributedString as = new AttributedString(text);
        as.addAttribute(java.awt.font.TextAttribute.FONT, textFont);
        LineBreakMeasurer measurer = new LineBreakMeasurer(as.getIterator(), frc);

        while (measurer.getPosition() < text.length()) {
            TextLayout layout = measurer.nextLayout(wrappingWidth);
            if (layout == null) break;

            float ascent = layout.getAscent();
            Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y + ascent));
            g2.setColor(outlineColor);
            g2.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(outline);
            g2.setColor(textColor);
            g2.fill(outline);

            y += layout.getAscent() + layout.getDescent() + layout.getLeading();
        }
        g2.dispose();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        repaint();
    }
}