package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SaveLoadDialog extends JDialog {
    private GamePanel gamePanel;
    private JPanel listPanel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SaveLoadDialog(Frame owner, GamePanel gamePanel) {
        super(owner, "存档 / 读档", true);
        this.gamePanel = gamePanel;
        setSize(650, 450);
        setLocationRelativeTo(owner);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane scrollPane = new JScrollPane(listPanel);
        add(scrollPane, BorderLayout.CENTER);

        refreshSaveList();
    }

    private void refreshSaveList() {
        listPanel.removeAll();
        for (int i = 0; i < 10; i++) {
            File slotFile = new File("save_" + i + ".dat");
            if (!slotFile.exists()) {
                listPanel.add(createEmptySlot(i));
            } else {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(slotFile))) {
                    SaveData data = (SaveData) ois.readObject();
                    listPanel.add(createSaveSlot(i, data, slotFile));
                } catch (Exception e) {
                    listPanel.add(createEmptySlot(i)); // 读取失败当作空位
                }
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createEmptySlot(int slot) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(60, 60, 80));
        panel.setPreferredSize(new Dimension(600, 70));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 130), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        JLabel label = new JLabel("空位 " + (slot + 1));
        label.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        label.setForeground(Color.LIGHT_GRAY);
        panel.add(label, BorderLayout.CENTER);
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (gamePanel.saveGameToFile(slot)) {
                    dispose();
                }
            }
        });
        return panel;
    }

    private JPanel createSaveSlot(int slot, SaveData data, File file) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(50, 50, 70));
        panel.setPreferredSize(new Dimension(600, 70));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // 缩略图
        JLabel thumbLabel = new JLabel();
        if (data.getThumbnailBytes() != null && data.getThumbnailBytes().length > 0) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data.getThumbnailBytes()));
                Image scaled = img.getScaledInstance(80, 50, Image.SCALE_SMOOTH);
                thumbLabel.setIcon(new ImageIcon(scaled));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        panel.add(thumbLabel, BorderLayout.WEST);

        // 信息
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);
        String timeStr = dateFormat.format(data.getSaveTime());
        JLabel timeLabel = new JLabel("存档时间: " + timeStr);
        timeLabel.setForeground(Color.WHITE);
        JLabel progressLabel = new JLabel("进度: " + data.getCurrentIndex() + "/" + data.getDialogues().size());
        progressLabel.setForeground(Color.WHITE);
        infoPanel.add(timeLabel);
        infoPanel.add(progressLabel);
        panel.add(infoPanel, BorderLayout.CENTER);

        // 点击读档
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (gamePanel.loadGameFromFile(file)) {
                    dispose();
                }
            }
        });
        return panel;
    }

    public void refresh() {
        refreshSaveList();
    }
}
