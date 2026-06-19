package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * 全屏转场面板，用于实现淡入淡出效果
 */
public class TransitionPanel extends JPanel {
    private BufferedImage image;
    private float alpha = 1.0f;
    private Timer timer;
    private boolean fadingIn = false; // true=淡入，false=淡出
    private Consumer<Float> onProgress; // 进度回调
    private Runnable onComplete;

    public TransitionPanel(BufferedImage image, float startAlpha, float endAlpha, int durationMs,
                           Runnable onComplete) {
        this.image = image;
        this.alpha = startAlpha;
        this.onComplete = onComplete;
        setOpaque(false);
        setLayout(null);

        float step = (endAlpha - startAlpha) / (durationMs / 16f);
        timer = new Timer(16, e -> {
            alpha += step;
            if ((step > 0 && alpha >= endAlpha) || (step < 0 && alpha <= endAlpha)) {
                alpha = endAlpha;
                timer.stop();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 设置透明度
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        g2.dispose();
    }

    // 外部停止（安全）
    public void stop() {
        if (timer != null && timer.isRunning()) timer.stop();
    }
}