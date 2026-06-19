package org.galgame;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 更稳定的Swing + JavaFX混合转场实现。
 * 核心思路：将JavaFX作为纯动画层，短暂覆盖在Swing内容之上。
 */
public class ImprovedFadeHelper {

    private static boolean isFxInitialized = false;
    private static volatile boolean isTransitioning = false;

    public static void switchWithFade(JFrame frame, JPanel targetPanel, int durationMs) {
        // 防止快速点击导致动画冲突
        if (isTransitioning) return;
        isTransitioning = true;

        // 1. 初始化JavaFX环境 (只做一次)
        if (!isFxInitialized) {
            // JFXPanel的构造方法会触发JavaFX工具包的启动
            new JFXPanel();
            isFxInitialized = true;
        }

        // 2. 在EDT线程中捕获当前Swing面板的快照
        Component currentContent = frame.getContentPane();
        BufferedImage currentSnapshot = captureSwingComponent(currentContent);
        int width = frame.getWidth();
        int height = frame.getHeight();

        // 3. 创建并覆盖一个临时的JFXPanel (EDT线程)
        JFXPanel fxOverlay = new JFXPanel();
        fxOverlay.setBounds(0, 0, width, height);
        // 关键：设为透明，让下层Swing背景可见，避免黑屏闪烁
        fxOverlay.setOpaque(false);

        JLayeredPane layeredPane = frame.getRootPane().getLayeredPane();
        layeredPane.removeAll();
        layeredPane.add(frame.getContentPane(), JLayeredPane.DEFAULT_LAYER);
        // 将JFXPanel置于顶层 (MODAL_LAYER)
        layeredPane.add(fxOverlay, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();

        // 4. 在JavaFX线程中创建动画场景
        Platform.runLater(() -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, width, height, Color.TRANSPARENT);
            // 确保场景背景透明
            scene.setFill(Color.TRANSPARENT);

            // 将Swing截图转换为JavaFX图像
            WritableImage fxImage = convertToFxImage(currentSnapshot);
            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(false);
            root.getChildren().add(imageView);

            // 将场景设置到JFXPanel
            fxOverlay.setScene(scene);

            // 5. 执行淡出动画
            FadeTransition fadeOut = new FadeTransition(Duration.millis(durationMs / 2), imageView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                // 动画结束，回到EDT线程执行Swing内容切换
                SwingUtilities.invokeLater(() -> {
                    // 切换Swing内容
                    frame.setContentPane(targetPanel);
                    frame.revalidate();
                    frame.repaint();

                    // 捕获新面板快照
                    BufferedImage nextSnapshot = captureSwingComponent(targetPanel);

                    // 回到FX线程，准备淡入动画
                    Platform.runLater(() -> {
                        WritableImage fxNext = convertToFxImage(nextSnapshot);
                        ImageView nextView = new ImageView(fxNext);
                        nextView.setFitWidth(width);
                        nextView.setFitHeight(height);
                        nextView.setPreserveRatio(false);
                        nextView.setOpacity(0.0);

                        root.getChildren().clear();
                        root.getChildren().add(nextView);

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(durationMs / 2), nextView);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.setOnFinished(e2 -> {
                            // 淡入结束，回到EDT，移除JFXPanel
                            SwingUtilities.invokeLater(() -> {
                                layeredPane.removeAll();
                                layeredPane.add(targetPanel, JLayeredPane.DEFAULT_LAYER);
                                layeredPane.revalidate();
                                layeredPane.repaint();
                                // 释放转场锁
                                isTransitioning = false;
                            });
                        });
                        fadeIn.play();
                    });
                });
            });
            fadeOut.play();
        });
    }

    /**
     * 捕获Swing组件的截图 (必须在EDT线程调用)
     */
    private static BufferedImage captureSwingComponent(Component comp) {
        BufferedImage img = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        comp.paint(g);
        g.dispose();
        return img;
    }

    /**
     * 将AWT的BufferedImage转换为JavaFX的WritableImage (必须在FX线程调用)
     */
    private static WritableImage convertToFxImage(BufferedImage img) {
        WritableImage fxImage = new WritableImage(img.getWidth(), img.getHeight());
        javafx.scene.image.PixelWriter writer = fxImage.getPixelWriter();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                writer.setArgb(x, y, argb);
            }
        }
        return fxImage;
    }
}