package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FadeTransitionPanel extends JPanel {
    private BufferedImage image;
    private float opacity = 1.0f;

    public void setOpacity(float opacity) {
        this.opacity = opacity;
        repaint();
    }

    public float getOpacity() {
        return opacity;
    }

    // 直接传入截好的图片
    public FadeTransitionPanel(BufferedImage image) {
        this.image = image;
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        g2.dispose();
    }
}