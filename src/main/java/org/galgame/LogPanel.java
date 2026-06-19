package org.galgame;

import javax.swing.*;
import java.awt.*;
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
    private JPanel returnPanel; // 引用，用于返回
    private List<String> history;
    private Font dialogFont;
    private Font nameFont;
    private Timer bgAnimTimer;

    public LogPanel(JFrame frame, JPanel gamePanel, List<String> history, Font dialogFont) {
        this.parentFrame = frame;
        this.gamePanel = gamePanel;
        this.history = new ArrayList<>(history); // 防御性拷贝
        this.dialogFont = dialogFont;
        this.nameFont = dialogFont.deriveFont(Font.BOLD, dialogFont.getSize2D());

        setLayout(new BorderLayout());
        setOpaque(false);

        // ---- 顶部：返回按钮 + 标题 ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JButton backBtn = createGlassButton("← 返回游戏");
        backBtn.addActionListener(e -> returnToGame());
        topBar.add(backBtn, BorderLayout.WEST);

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
        card.setMaximumSize(new Dimension(700, 90));
        card.setMinimumSize(new Dimension(700, 90));
        card.setPreferredSize(new Dimension(700, 90));

        // 左侧角色名（在毛玻璃框外）
        JLabel nameLabel = new JLabel(showName ? who : "");
        nameLabel.setFont(nameFont);
        nameLabel.setForeground(new Color(255, 220, 150));
        nameLabel.setPreferredSize(new Dimension(120, 36));
        nameLabel.setMinimumSize(new Dimension(120, 36));
        nameLabel.setMaximumSize(new Dimension(120, 36));
        nameLabel.setHorizontalAlignment(JLabel.CENTER);
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

        JLabel textLabel = new JLabel("<html><body style='width:450px'>" + escapeHtml(text) + "</body></html>");
        textLabel.setFont(dialogFont);
        textLabel.setForeground(Color.WHITE);
        textLabel.setVerticalAlignment(JLabel.CENTER);
        dialogueBox.add(textLabel, BorderLayout.CENTER);

        card.add(dialogueBox, BorderLayout.CENTER);
        return card;
    }

    /** HTML转义，防止台词中的特殊字符破坏HTML */
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 创建毛玻璃按钮（与GamePanel一致） */
    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 150));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override
            public void setOpaque(boolean opaque) { super.setOpaque(false); }
        };
        btn.setFont(nameFont.deriveFont(14f));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(new Color(255, 255, 200));
                btn.repaint();
            }
            public void mouseExited(MouseEvent e) {
                btn.setForeground(Color.WHITE);
                btn.repaint();
            }
        });
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
