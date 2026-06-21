package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 台词回顾页面 —— 以专门页面形式展示历史台词。
 * 左侧角色名，右侧台词，毛玻璃卡片样式，连续同一角色省略名字。
 */
public class LogPanel extends JPanel {
    private JFrame parentFrame;
    private JPanel gamePanel;
    private List<String> history;
    private Font dialogFont;
    private Font nameFont;
    private Timer bgAnimTimer;

    public LogPanel(JFrame frame, JPanel gamePanel, List<String> history, Font dialogFont) {
        this.parentFrame = frame;
        this.gamePanel = gamePanel;
        this.history = new ArrayList<>(history); // 防御性拷贝
        this.dialogFont = dialogFont;
        this.nameFont = dialogFont.deriveFont(30f);

        setLayout(new BorderLayout());
        setOpaque(false);

        // ---- 顶部：返回按钮 + 标题 ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

JButton returnBtn = createReturnButton();
        returnBtn.addActionListener(e -> returnToGame());
        topBar.add(returnBtn, BorderLayout.EAST);
        JLabel titleLabel = new JLabel("台词回顾", JLabel.CENTER);
        titleLabel.setFont(nameFont.deriveFont(28f));
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // ---- 中间：滚动台词列表 ----
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        String lastWho = null;
        for (String entry : history) {
            String who = "";
            String text = entry;
            int colonIdx = entry.indexOf("\uFF1A"); // 全角冒号
            if (colonIdx > 0) {
                who = entry.substring(0, colonIdx).trim();
                text = entry.substring(colonIdx + 1).trim();
            }

            boolean showName = !who.equals(lastWho);
            lastWho = who;

            JPanel card = createDialogueCard(who, text, showName);
            card.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(8)); // 卡片间距
        }

        // 包装进滚动面板
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        add(scrollPane, BorderLayout.CENTER);

        // ---- 背景动画（简单暗色渐变） ----
        bgAnimTimer = new Timer(30, e -> repaint());
        bgAnimTimer.start();
    }

    /**
     * 创建单条台词的毛玻璃卡片
     * @param who      角色名
     * @param text     台词内容
     * @param showName 是否显示角色名
     */
    private JPanel createDialogueCard(String who, String text, boolean showName) {
        // 外层容器：无毛玻璃，只负责排列角色名和台词框
        JPanel card = new JPanel(new BorderLayout(20, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        card.setMaximumSize(new Dimension(800, 200));
        card.setMinimumSize(new Dimension(800, 200));
        card.setPreferredSize(new Dimension(800, 200));

        // 左侧角色名（在毛玻璃框外）
        OutlineLabel nameLabel = new OutlineLabel(showName ? who : "");
        nameLabel.setFont(nameFont);
        nameLabel.setForeground(new Color(255, 220, 150));
        nameLabel.setPreferredSize(new Dimension(120, 36));
        nameLabel.setMinimumSize(new Dimension(120, 36));
        nameLabel.setMaximumSize(new Dimension(120, 36));
        nameLabel.setHorizontalAlignment(JLabel.CENTER);
        nameLabel.setVerticalAlignment(JLabel.TOP);
        card.add(nameLabel, BorderLayout.WEST);

        // 右侧台词框（毛玻璃只框台词）
        JPanel dialogueBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 140));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        dialogueBox.setOpaque(false);
        dialogueBox.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        OutlineTextArea textArea = new OutlineTextArea(text);
        textArea.setFont(dialogFont.deriveFont(26f));
        textArea.setForeground(Color.WHITE);
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        dialogueBox.add(textArea, BorderLayout.CENTER);

        card.add(dialogueBox, BorderLayout.CENTER);
        return card;
    }


    /** 创建统一的图片返回按钮（鼠标悬停发光效果） */
    private JButton createReturnButton() {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/images/return_icon.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                JButton btn = new JButton(new ImageIcon(scaled)) {
                    private boolean hovered = false;
                    {
                        addMouseListener(new MouseAdapter() {
                            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                        });
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (hovered) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            int cx = getWidth() / 2;
                            int cy = getHeight() / 2;
                            int r = Math.min(getWidth(), getHeight()) / 2 - 2;
                            // 外圈柔光
                            g2.setColor(new Color(255, 255, 255, 30));
                            g2.setStroke(new BasicStroke(8f));
                            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                            // 内圈亮光
                            g2.setColor(new Color(255, 255, 255, 90));
                            g2.setStroke(new BasicStroke(3f));
                            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                            g2.dispose();
                        }
                    }
                };
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;
            }
        } catch (Exception e) {
        }
        JButton btn = new JButton("✕");
        btn.setFont(nameFont.deriveFont(22f));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }


    /** 返回游戏画面 */
    private void returnToGame() {
        if (bgAnimTimer != null) bgAnimTimer.stop();
        parentFrame.setContentPane(gamePanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // 深色半透明背景
        g2.setColor(new Color(20, 20, 40, 220));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
