package org.galgame;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class GalGameApp {
    public static void main(String[] args) {
        // 设置 FlatLaf 深色主题
        FlatDarkLaf.setup();

        // 初始化按钮音效
        SoundEffects.init();

        // 提前初始化 JavaFX 工具包
        try { new javafx.embed.swing.JFXPanel(); } catch (Exception e) { System.err.println("JavaFX涓嶅彲鐢ㄣ€?缁х画杩愯"); }

        // 开启全局字体抗锯齿
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("我想完成结课作业");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 根据屏幕比例计算等比例窗口大小（宽度固定1024）
            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            int contentHeight = (int) (1024.0 / screenSize.getWidth() * screenSize.getHeight());

            // 创建主菜单面板
            MainMenuPanel mainMenu = new MainMenuPanel(frame);
            mainMenu.setPreferredSize(new java.awt.Dimension(1024, contentHeight));

            frame.setContentPane(mainMenu);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}