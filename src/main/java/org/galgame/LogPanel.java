package org.galgame;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import java.io.InputStream;
import java.util.*;

/**
 * 台词回顾页面 —— 静态列表布局（与音乐鉴赏播放列表风格一致）。
 * 左侧正方形头像（连续发言归并），右侧台词文本，无角色名。
 * 默认整行半透明，悬停时整行变为完全不透明（覆盖整个游戏窗口宽度）。
 * 台词不可被鼠标选中。
 */
public class LogPanel extends JPanel {
    private JFrame parentFrame;
    private JPanel gamePanel;
    private Font dialogFont;
    private Font textFont;
    private Font titleFont;
    private JScrollPane scrollPane;

    private static final int AVATAR_SIZE = 72;
    private static final int ROW_HEIGHT = 80;
    private Map<String, BufferedImage> avatarCache = new HashMap<>();

    public LogPanel(JFrame frame, JPanel gamePanel, java.util.List<String> history, Font dialogFont) {
        this.parentFrame = frame;
        this.gamePanel = gamePanel;
        this.dialogFont = dialogFont;
        this.textFont = dialogFont.deriveFont(26f);
        this.titleFont = dialogFont.deriveFont(28f);

        setLayout(new BorderLayout());
        setOpaque(false);

        // ---- 顶部栏：标题 + 返回按钮 ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JButton returnBtn = createReturnButton();
        returnBtn.addActionListener(e -> returnToGame());
        topBar.add(returnBtn, BorderLayout.EAST);

        JLabel titleLabel = new JLabel("\u53f0\u8bcd\u56de\u987e", JLabel.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // ---- 中间：可滚动的台词列表 ----
        // 使用 BoxLayout Y_AXIS 确保每行从上往下排列，宽度填满
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        buildRows(listPanel, history);

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // 不加左右 gap 到 scrollPane——每行自己负责内容偏移，背景高亮覆盖整个窗口宽度
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(scrollPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // 打开时自动滚动到顶部
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    private void buildRows(JPanel listPanel, java.util.List<String> history) {
        String lastWho = null;
        for (String entry : history) {
            String who = "";
            String text = entry;
            int colonIdx = entry.indexOf("\uff1a");
            if (colonIdx > 0) {
                who = entry.substring(0, colonIdx).trim();
                text = entry.substring(colonIdx + 1).trim();
            }
            boolean isFirst = !who.equals(lastWho);
            lastWho = who;

            JPanel row = createRow(who, text, isFirst);
            row.setAlignmentX(LEFT_ALIGNMENT);
            listPanel.add(row);
        }
    }

    private JPanel createRow(String characterName, String text, boolean isFirst) {
        BufferedImage avatar = isFirst ? loadAvatar(characterName) : null;

        JPanel row = new JPanel(null) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // ---- 整行背景：默认半透明，悬停时整行变为不透明（覆盖 w 全宽） ----
                int bgAlpha = hovered ? 80 : 0;
                if (bgAlpha > 0) {
                    g2.setColor(new Color(255, 255, 255, bgAlpha));
                    g2.fillRect(0, 0, w, h);
                }

                // 内容偏移：从头像/文字起始处 = 窗口宽度的 1/6
                int contentLeft = w / 6;
                int avatarLeft = contentLeft;
                int textLeft = avatarLeft + AVATAR_SIZE + 15;

                // ---- 绘制头像（仅首句且存在头像） ----
                if (isFirst && avatar != null) {
                    int avY = (h - AVATAR_SIZE) / 2;
                    Shape clip = new Rectangle(avatarLeft, avY, AVATAR_SIZE, AVATAR_SIZE);
                    g2.setClip(clip);
                    g2.drawImage(avatar, avatarLeft, avY, AVATAR_SIZE, AVATAR_SIZE, null);
                    g2.setClip(null);
                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRect(avatarLeft, avY, AVATAR_SIZE, AVATAR_SIZE);
                }

                // ---- 台词文本（默认半透明，悬停不透明） ----
                int textAlpha = hovered ? 230 : 100;
                int textX = textLeft;
                int textY = (h - g2.getFontMetrics(textFont).getHeight()) / 2 + g2.getFontMetrics(textFont).getAscent();

                g2.setFont(textFont);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.font.TextLayout tl = new java.awt.font.TextLayout(text, g2.getFont(), g2.getFontRenderContext());
                Shape outline = tl.getOutline(AffineTransform.getTranslateInstance(textX, textY));
                g2.draw(outline);
                g2.setColor(new Color(255, 255, 255, textAlpha));
                g2.fill(outline);

                g2.dispose();
            }
        };

        row.setOpaque(false);
        row.setPreferredSize(new Dimension(800, ROW_HEIGHT));
        row.setMinimumSize(new Dimension(100, ROW_HEIGHT));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

        return row;
    }

    private BufferedImage loadAvatar(String characterName) {
        if (characterName == null || characterName.isEmpty()) return null;
        if (avatarCache.containsKey(characterName)) return avatarCache.get(characterName);
        BufferedImage avatar = null;
        try {
            InputStream is = getClass().getResourceAsStream("/images/avatars/" + characterName + ".png");
            if (is != null) {
                avatar = ImageIO.read(is);
                is.close();
            }
        } catch (Exception ignored) {}
        avatarCache.put(characterName, avatar);
        return avatar;
    }

    // ============ 返回按钮（光圈常驻 + 按下效果） ============
    private JButton createReturnButton() {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/images/return_icon.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                JButton btn = new JButton(new ImageIcon(scaled)) {
                    private boolean pressed = false;
                    {
                        addMouseListener(new MouseAdapter() {
                            public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                            public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                        });
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        int r = Math.min(getWidth(), getHeight()) / 2 - 2;
                        g2.setColor(new Color(0, 0, 0, 80));
                        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(new Color(255, 255, 255, 30));
                        g2.setStroke(new BasicStroke(8f));
                        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(new Color(255, 255, 255, 90));
                        g2.setStroke(new BasicStroke(3f));
                        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                        g2.dispose();
                        if (pressed) {
                            Graphics2D g3 = (Graphics2D) g.create();
                            g3.translate(1, 1);
                            super.paintComponent(g3);
                            g3.dispose();
                        } else {
                            super.paintComponent(g);
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
        } catch (Exception ignored) {}
        JButton fb = new JButton("\u2715");
        fb.setFont(dialogFont.deriveFont(22f));
        fb.setForeground(Color.WHITE);
        fb.setFocusPainted(false);
        fb.setContentAreaFilled(false);
        fb.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        fb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return fb;
    }

    private void returnToGame() {
        parentFrame.setContentPane(gamePanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(20, 20, 40, 220));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
