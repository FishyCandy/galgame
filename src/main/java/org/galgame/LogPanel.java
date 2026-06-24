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
 * 台词回顾页面 —— 静态列表布局。
 * 左侧固定正方形人物缩略图（连续发言归并），右侧玻璃卡片显示台词文本。
 * 默认台词半透明，鼠标悬停台词变为完全不透明，头像不变。
 * 台词不可被鼠标选中。
 */
public class LogPanel extends JPanel {
    private JFrame parentFrame;
    private JPanel gamePanel;
    private Font dialogFont;
    private Font nameFont;
    private Font textFont;
    private Font titleFont;

    private static final int AVATAR_SIZE = 72;
    private static final int ROW_HEIGHT = 110;
    private Map<String, BufferedImage> avatarCache = new HashMap<>();
    private java.util.List<DialogueRow> rowPanels = new ArrayList<>();

    public LogPanel(JFrame frame, JPanel gamePanel, java.util.List<String> history, Font dialogFont) {
        this.parentFrame = frame;
        this.gamePanel = gamePanel;
        this.dialogFont = dialogFont;
        this.nameFont = dialogFont.deriveFont(30f);
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
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        buildRows(listPanel, history);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // 包裹层：左右各留 1/6 窗口宽度的空白
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int gap = centerWrapper.getWidth() / 6;
                scrollPane.setBorder(BorderFactory.createEmptyBorder(10, gap, 10, gap));
            }
        });
        centerWrapper.add(scrollPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
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

            DialogueRow row = new DialogueRow(who, text, isFirst);
            row.setAlignmentX(LEFT_ALIGNMENT);
            rowPanels.add(row);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(8));
        }
    }

    private BufferedImage loadAvatar(String characterName) {
        if (characterName == null || characterName.isEmpty()) return null;
        if (avatarCache.containsKey(characterName)) return avatarCache.get(characterName);
        BufferedImage avatar = null;
        try {
            InputStream is = getClass().getResourceAsStream(
                    "/images/avatars/" + characterName + ".png");
            if (is != null) {
                avatar = ImageIO.read(is);
                is.close();
            }
        } catch (Exception ignored) {}
        avatarCache.put(characterName, avatar);
        return avatar;
    }

    // ============ 单条台词行 ============
    private class DialogueRow extends JPanel {
        String characterName;
        String text;
        boolean isFirst;
        boolean hovered = false;
        BufferedImage avatar;

        DialogueRow(String characterName, String text, boolean isFirst) {
            this.characterName = characterName;
            this.text = text;
            this.isFirst = isFirst;
            this.avatar = loadAvatar(characterName);

            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(800, ROW_HEIGHT));
            setMinimumSize(new Dimension(100, ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            Insets ins = getInsets();

            // 左侧头像区域宽度（有头像的起始位置）
            int avatarLeft = ins.left + 5;
            // 卡片起始X：统一为头像右侧位置，保证所有卡片对齐
            int cardLeft = avatarLeft + AVATAR_SIZE + 15;
            int cardRightPad = 10;
            int cardW = Math.max(100, w - cardLeft - cardRightPad);
            int cardY = ins.top + 5;
            int cardH = h - ins.top - ins.bottom - 10;

            // ---- 绘制头像（仅首句且存在头像） ----
            if (isFirst && avatar != null) {
                int avatarY = cardY + (cardH - AVATAR_SIZE) / 2;
                // 正方形头像
                Shape clip = new Rectangle(avatarLeft, avatarY, AVATAR_SIZE, AVATAR_SIZE);
                g2.setClip(clip);
                g2.drawImage(avatar, avatarLeft, avatarY, AVATAR_SIZE, AVATAR_SIZE, null);
                g2.setClip(null);
                // 头像边框
                g2.setColor(new Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(avatarLeft, avatarY, AVATAR_SIZE, AVATAR_SIZE);

                // 角色名（头像下方）
                g2.setFont(nameFont);
                FontMetrics nm = g2.getFontMetrics();
                int nameX = avatarLeft + (AVATAR_SIZE - nm.stringWidth(characterName)) / 2;
                int nameY = avatarY + AVATAR_SIZE + nm.getAscent() + 5;
                drawStrokedText(g2, characterName, nameX, nameY, 200);
            }

            // ---- 台词毛玻璃卡片 ----
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(cardLeft, cardY, cardW, cardH, 18, 18);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cardLeft, cardY, cardW - 1, cardH - 1, 18, 18);

            // ---- 台词文本（默认半透明，悬停不透明） ----
            int textAlpha = hovered ? 230 : 100;
            int textX = cardLeft + 14;
            int textY = cardY + 10;
            int textMaxW = cardW - 28;

            g2.setFont(textFont);
            FontMetrics fm = g2.getFontMetrics();
            java.util.List<String> wrappedLines = wrapText(text, fm, textMaxW);
            float lineY = textY + fm.getAscent();
            for (String line : wrappedLines) {
                drawStrokedText(g2, line, textX, (int) lineY, textAlpha);
                lineY += fm.getHeight();
            }

            g2.dispose();
        }

        private java.util.List<String> wrapText(String s, FontMetrics fm, int maxW) {
            java.util.List<String> res = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                String test = cur.toString() + c;
                if (fm.stringWidth(test) > maxW && cur.length() > 0) {
                    res.add(cur.toString());
                    cur = new StringBuilder();
                }
                cur.append(c);
            }
            if (cur.length() > 0) res.add(cur.toString());
            return res;
        }

        private void drawStrokedText(Graphics2D g2, String s, int x, int y, int alpha) {
            if (s.isEmpty()) return;
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.font.TextLayout tl = new java.awt.font.TextLayout(
                    s, g2.getFont(), g2.getFontRenderContext());
            Shape outline = tl.getOutline(AffineTransform.getTranslateInstance(x, y));
            g2.draw(outline);
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fill(outline);
        }
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
        fb.setFont(nameFont.deriveFont(22f));
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
