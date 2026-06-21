package org.galgame;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class GalGameApp {
    public static void main(String[] args) {
        // 设置 FlatLaf 深色主题
        FlatDarkLaf.setup();

        // 提前初始化 JavaFX 工具包
        new javafx.embed.swing.JFXPanel();

        // 开启全局字体抗锯齿
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("恋语之境");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1024, 640);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            // 创建主菜单面板
            MainMenuPanel mainMenu = new MainMenuPanel(frame);
            frame.setContentPane(mainMenu);
            frame.setVisible(true);
        });
    }
}