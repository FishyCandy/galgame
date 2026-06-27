package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;

public class SaveLoadPanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private GamePanel gamePanel;
    private Mode mode;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd, HH:mm:ss");
    private BufferedImage bgImage; // 背景图

    public enum Mode { SAVE, LOAD }

    public SaveLoadPanel(JFrame frame, MainMenuPanel mainMenu, GamePanel gamePanel, Mode mode) {
        this.parentFrame = frame;
        this.mainMenuPanel = mainMenu;
        this.gamePanel = gamePanel;
        this.mode = mode;

        // 加载背景图
        try {
            bgImage = ImageIO.read(getClass().getResourceAsStream("/images/saveload_bg.jpg"));
        } catch (Exception e) {
            // 背景图不存在，使用渐变后备
            bgImage = null;
        }

        setLayout(new BorderLayout());
        setOpaque(false);

        // 顶部栏：标题 + 返回按钮
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel(mode == Mode.SAVE ? "存档" : "读档");
        titleLabel.setFont(mainMenuPanel.getTitleFont().deriveFont(40f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topBar.add(titleLabel, BorderLayout.CENTER);

        JButton returnBtn = createReturnButton();
        returnBtn.addActionListener(e -> {
            mainMenuPanel.showPreviousPanel();
        });
        topBar.add(returnBtn, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 存档槽区域
        JPanel slotPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        slotPanel.setOpaque(false);
        slotPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        for (int i = 0; i < 6; i++) {
            slotPanel.add(createSlot(i));
        }
        add(slotPanel, BorderLayout.CENTER);

        setBackground(new Color(30, 30, 60));
    }

    // 自定义缩略图面板（内部类）
    private class ThumbPanel extends JPanel {
        private BufferedImage thumbImage = null;
        private String emptyText = "空";

        public void setThumbImage(BufferedImage img) {
            this.thumbImage = img;
            repaint();
        }

        public void setEmptyText(String text) {
            this.emptyText = text;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 绘制白色边框（方框）
            g2.setColor(Color.WHITE);
            g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
            // 填充内容
            if (thumbImage != null) {
                // 绘制缩略图，填满整个面板
                g2.drawImage(thumbImage, 0, 0, getWidth(), getHeight(), null);
            } else {
                // 显示空文本
                g2.setColor(Color.LIGHT_GRAY);
                g2.setFont(mainMenuPanel.getButtonFont().deriveFont(20f));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(emptyText)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(emptyText, x, y);
            }
            g2.dispose();
        }
    }

    private JPanel createSlot(int index) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(200, 150));

        File saveFile = new File("save_" + index + ".dat");

        // 使用自定义缩略图面板
        ThumbPanel thumbPanel = new ThumbPanel();
        thumbPanel.setPreferredSize(new Dimension(240, 135));
        thumbPanel.setMinimumSize(new Dimension(240, 135));
        thumbPanel.setMaximumSize(new Dimension(240, 135));
        thumbPanel.setOpaque(false);

        // 底部信息标签（时间）
        JLabel infoLabel = new JLabel();
        infoLabel.setFont(mainMenuPanel.getButtonFont().deriveFont(14f));
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setHorizontalAlignment(JLabel.CENTER);

        // 填充数据
        if (saveFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
                SaveData data = (SaveData) ois.readObject();
                if (data.getThumbnailBytes() != null && data.getThumbnailBytes().length > 0) {
                    BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(data.getThumbnailBytes()));
                    // 高质量缩放至 200x112，完全填满
                    BufferedImage scaledImg = new BufferedImage(240, 135, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = scaledImg.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.drawImage(thumb, 0, 0, 240, 135, null);
                    g2d.dispose();
                    thumbPanel.setThumbImage(scaledImg);
                } else {
                    thumbPanel.setEmptyText("无预览");
                }
                infoLabel.setText(dateFormat.format(data.getSaveTime()));
            } catch (Exception e) {
                thumbPanel.setEmptyText("损坏");
                infoLabel.setText("无法读取");
            }
        } else {
            thumbPanel.setEmptyText("空");
            infoLabel.setText("空槽位");
        }

        card.add(thumbPanel, BorderLayout.CENTER);
        card.add(infoLabel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSlotClick(index, saveFile);
            }
        });
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 让缩略图区域和标签也可以点击
        thumbPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSlotClick(index, saveFile);
            }
        });
        thumbPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        infoLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSlotClick(index, saveFile);
            }
        });

        return card;
    }

    private void handleSlotClick(int index, File saveFile) {
        if (mode == Mode.SAVE) {
            if (saveFile.exists()) {
                showOverlayConfirm("该槽已有存档，是否覆盖？", "确定覆盖", "取消", () -> {
                    if (gamePanel != null) {
                        boolean success = gamePanel.saveGameToFile(index);
                        if (success) refreshSlots();
                    } else {
                        showOverlayMessage("无法存档：未在游戏中。", "确定");
                    }
                });
            } else {
                if (gamePanel != null) {
                    boolean success = gamePanel.saveGameToFile(index);
                    if (success) refreshSlots();
                } else {
                    showOverlayMessage("无法存档：未在游戏中。", "确定");
                }
            }
        } else {
            if (!saveFile.exists()) {
                showOverlayMessage("该槽为空，无法读档。", "确定");
                return;
            }
            showOverlayConfirm("是否从该槽读档？", "确定读档", "取消", () -> {
                if (gamePanel != null) {
                    if (gamePanel.loadGameFromFile(saveFile)) {
                        mainMenuPanel.stopMenuMusic();
                        transitionToPanel(gamePanel);
                    }
                } else {
                    GamePanel gp = new GamePanel(parentFrame, mainMenuPanel);
                    if (gp.loadGameFromFile(saveFile)) {
                        mainMenuPanel.stopMenuMusic();
                        transitionToPanel(gp);
                    }
                }
            });
        }
    }


    // ---------- 读档黑屏过渡动画 ----------
    private void transitionToPanel(JPanel targetPanel) {
        JLayeredPane layeredPane = parentFrame.getLayeredPane();
        final float[] alphaArr = {0f};
        JPanel blackScreen = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, (int)(alphaArr[0] * 255)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        blackScreen.setOpaque(false);
        blackScreen.setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
        layeredPane.add(blackScreen, JLayeredPane.MODAL_LAYER);
        javax.swing.Timer timer = new javax.swing.Timer(20, null);
        timer.addActionListener(new java.awt.event.ActionListener() {
            int phase = 0;
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (phase == 0) {
                    alphaArr[0] += 0.05f;
                    if (alphaArr[0] >= 1f) {
                        alphaArr[0] = 1f;
                        phase = 1;
                        mainMenuPanel.switchToPanel(targetPanel);
                    }
                } else if (phase == 1) {
                    phase = 2;
                } else {
                    alphaArr[0] -= 0.05f;
                    if (alphaArr[0] <= 0f) {
                        alphaArr[0] = 0f;
                        timer.stop();
                        layeredPane.remove(blackScreen);
                        layeredPane.revalidate();
                        layeredPane.repaint();
                    }
                }
                blackScreen.repaint();
            }
        });
        timer.start();
    }

    // ---------- 覆盖层弹窗（替代JOptionPane） ----------
    private void showOverlayConfirm(String message, String confirmText, String cancelText, Runnable onConfirm) {
        JLayeredPane layeredPane = parentFrame.getLayeredPane();
        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        JLabel label = new JLabel(message);
        label.setFont(mainMenuPanel.getButtonFont().deriveFont(20f));
        label.setForeground(new Color(255, 255, 255, 200));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton confirmBtn = createOverlayButton(confirmText);
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> {
            layeredPane.remove(overlay);
            layeredPane.revalidate();
            layeredPane.repaint();
            if (onConfirm != null) onConfirm.run();
        });
        card.add(confirmBtn);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        JButton cancelBtn = createOverlayButton(cancelText);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> {
            layeredPane.remove(overlay);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
        card.add(cancelBtn);
        overlay.add(card);
        layeredPane.add(overlay, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private void showOverlayMessage(String message, String confirmText) {
        JLayeredPane layeredPane = parentFrame.getLayeredPane();
        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        JLabel label = new JLabel(message);
        label.setFont(mainMenuPanel.getButtonFont().deriveFont(20f));
        label.setForeground(new Color(255, 255, 255, 200));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);
        card.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton confirmBtn = createOverlayButton(confirmText);
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> {
            layeredPane.remove(overlay);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
        card.add(confirmBtn);
        overlay.add(card);
        layeredPane.add(overlay, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private JButton createOverlayButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private boolean pressed = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); SoundEffects.playHover(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                    public void mousePressed(MouseEvent e) { pressed = true; repaint(); SoundEffects.playClick(); }
                    public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fillColor = pressed ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 60);
                g2.setColor(fillColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.setStroke(new BasicStroke(6f));
                    g2.drawRoundRect(3, 3, getWidth()-7, getHeight()-7, 30, 30);
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 30, 30);
                }
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int offset = pressed ? 2 : 0;
                int x = (getWidth() - fm.stringWidth(getText())) / 2 + offset;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + offset;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override
            public void setOpaque(boolean opaque) { super.setOpaque(false); }
        };
        btn.setFont(mainMenuPanel.getButtonFont());
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(300, 60));
        btn.setMaximumSize(new Dimension(300, 60));
        btn.setMinimumSize(new Dimension(300, 60));
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
    private void refreshSlots() {
        // 简单刷新：重新构建整个面板
        removeAll();
        // 重新添加顶部栏
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel(mode == Mode.SAVE ? "存档" : "读档");
        titleLabel.setFont(mainMenuPanel.getTitleFont().deriveFont(40f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topBar.add(titleLabel, BorderLayout.CENTER);

        JButton returnBtn = createReturnButton();
        returnBtn.addActionListener(e -> {
            mainMenuPanel.showPreviousPanel();
        });
        topBar.add(returnBtn, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        JPanel slotPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        slotPanel.setOpaque(false);
        slotPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        for (int i = 0; i < 6; i++) {
            slotPanel.add(createSlot(i));
        }
        add(slotPanel, BorderLayout.CENTER);


        revalidate();
        repaint();
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private boolean pressed = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); SoundEffects.playHover(); }
                    public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                    public void mousePressed(java.awt.event.MouseEvent e) { pressed = true; repaint(); SoundEffects.playClick(); }
                    public void mouseReleased(java.awt.event.MouseEvent e) { pressed = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fillColor = pressed ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 60);
                g2.setColor(fillColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.setStroke(new BasicStroke(6f));
                    g2.drawRoundRect(3, 3, getWidth()-7, getHeight()-7, 30, 30);
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 30, 30);
                }
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int offset = pressed ? 2 : 0;
                int x = (getWidth() - fm.stringWidth(getText())) / 2 + offset;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + offset;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override
            public void setOpaque(boolean opaque) { super.setOpaque(false); }
        };
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(new Color(255, 255, 200));
                btn.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.WHITE);
                btn.repaint();
            }
        });
        return btn;
    }

    /** 创建统一的图片返回按钮（鼠标悬停发光效果） */
    private JButton createReturnButton() {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/images/return_icon.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                JButton btn = new JButton(new ImageIcon(scaled)) {
                    private boolean pressed = false;
                    {
                        addMouseListener(new MouseAdapter() {
                            public void mousePressed(MouseEvent e) { pressed = true; repaint(); SoundEffects.playClick(); }
                            public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                        });
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        // 光圈常驻
                        {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            int cx = getWidth() / 2;
                            int cy = getHeight() / 2;
                            int r = Math.min(getWidth(), getHeight()) / 2 - 2;
                            // 光圈内背景
                            g2.setColor(new Color(0, 0, 0, 80));
                            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
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
        } catch (Exception e) {
        }
        JButton btn = new JButton("✕");
        btn.setFont(mainMenuPanel.getTitleFont().deriveFont(22f));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (bgImage != null) {
            // 绘制背景图，拉伸至全屏
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 后备渐变背景
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                    getWidth(), getHeight(), new Color(10, 10, 30));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }
}
