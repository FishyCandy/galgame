package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;

public class MainMenuPanel extends JPanel {
    private JFrame parentFrame;
    private GamePanel gamePanel;
    private JPanel previousPanel;
    private Timer animTimer;
    private float hue = 0.0f;
    private BufferedImage bgImage;

    private Font titleFont;
    private Font buttonFont;

    private MusicPlayer menuMusicPlayer;
    private static final String MENU_BGM = "menu_bgm.wav";

    public MainMenuPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridBagLayout());
        setOpaque(false);

        menuMusicPlayer = new MusicPlayer();
        menuMusicPlayer.setLooping(true);

        // 加载背景图
        try {
            InputStream imgStream = getClass().getResourceAsStream("/menu_bg.jpg");
            if (imgStream != null) {
                bgImage = ImageIO.read(imgStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 加载字体
        try {
            InputStream titleStream = getClass().getResourceAsStream("/fonts/MyTitleFont.ttf");
            if (titleStream != null) {
                titleFont = Font.createFont(Font.TRUETYPE_FONT, titleStream).deriveFont(64f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(titleFont);
            } else {
                titleFont = new Font("华文行楷", Font.BOLD, 64);
            }

            InputStream buttonStream = getClass().getResourceAsStream("/fonts/MyButtonFont.ttf");
            if (buttonStream != null) {
                buttonFont = Font.createFont(Font.TRUETYPE_FONT, buttonStream).deriveFont(24f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(buttonFont);
            } else {
                buttonFont = new Font("微软雅黑", Font.BOLD, 24);
            }
        } catch (Exception e) {
            e.printStackTrace();
            titleFont = new Font("华文行楷", Font.BOLD, 64);
            buttonFont = new Font("微软雅黑", Font.BOLD, 24);
        }

        // 按钮
        JButton startBtn = createGlassButton("开 始 游 戏");
        JButton exitBtn = createGlassButton("结 束 游 戏");
        JButton loadBtn = createGlassButton("载 入 存 档");
        JButton musicBtn = createGlassButton("音 乐 鉴 赏");
        JButton settingsBtn = createGlassButton("游 戏 设 置");

        startBtn.addActionListener(e -> startGame());
        exitBtn.addActionListener(e -> System.exit(0));
        loadBtn.addActionListener(e -> {
            SaveLoadPanel panel = new SaveLoadPanel(parentFrame, this, null, SaveLoadPanel.Mode.LOAD);
            switchToPanel(panel);
        });
        musicBtn.addActionListener(e -> {
            MusicPlayerPanel musicPanel = new MusicPlayerPanel(parentFrame, this, titleFont, buttonFont);
            switchToPanel(musicPanel);
        });
        settingsBtn.addActionListener(e -> {
            SettingsPanel settingsPanel = new SettingsPanel(parentFrame, this, titleFont, buttonFont);
            switchToPanel(settingsPanel);
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 20, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.add(startBtn);
        buttonPanel.add(exitBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(musicBtn);
        buttonPanel.add(settingsBtn);

        // 用一个占位行把内容推到底部
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add(Box.createVerticalGlue(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 15, 0);
        add(buttonPanel, gbc);

        JLabel footer = new JLabel("—— 愿你拥有美好的故事 ——");
        footer.setFont(new Font("楷体", Font.PLAIN, 20));
        footer.setForeground(new Color(255, 255, 255, 180));
        gbc.gridy = 2;
        gbc.insets = new Insets(15, 0, 40, 0);
        add(footer, gbc);

        if (bgImage == null) {
            animTimer = new Timer(50, e -> {
                hue += 0.005f;
                if (hue > 1.0f) hue = 0.0f;
                repaint();
            });
            animTimer.start();
        }

        playMenuMusic();
    }

    // ---------- Getter ----------
    public Font getTitleFont() { return titleFont; }
    public Font getButtonFont() { return buttonFont; }

    // ---------- 菜单音乐 ----------
    private void playMenuMusic() {
        try {
            URL musicUrl = getClass().getResource("/music/" + MENU_BGM);
            if (musicUrl != null) {
                menuMusicPlayer.play(musicUrl);
            } else {
                System.err.println("菜单背景音乐未找到: " + MENU_BGM);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMenuMusic() {
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stopImmediately();
        }
    }

    // ---------- 毛玻璃按钮 ----------
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
                // 按下时背景稍暗
                Color fillColor = pressed ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 60);
                g2.setColor(fillColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
                // 悬停发光效果
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
        btn.setFont(buttonFont);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
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

    // ---------- 背景绘制 ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c1 = Color.getHSBColor(hue, 0.6f, 0.3f);
            Color c2 = Color.getHSBColor((hue + 0.3f) % 1.0f, 0.7f, 0.2f);
            GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ---------- 页面切换方法（无动画） ----------
    public void switchToPanel(JPanel targetPanel) {
        previousPanel = (JPanel) parentFrame.getContentPane();
        parentFrame.setContentPane(targetPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    // ---------- 游戏启动 ----------
    private void startGame() {
        stopMenuMusic();
        if (gamePanel == null) {
            gamePanel = new GamePanel(parentFrame, this);
        }
        switchToPanel(gamePanel);
        if (animTimer != null) animTimer.stop();
        gamePanel.startGame();
    }

    // ---------- 返回主菜单 ----------
    public void showMainMenu() {
        previousPanel = (JPanel) parentFrame.getContentPane();
        if (gamePanel != null) {
            gamePanel.stopMusic();
        }
        switchToPanel(this);
        if (animTimer != null) animTimer.start();
        javax.swing.Timer delayTimer = new javax.swing.Timer(200, e -> {
            playMenuMusic();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    public void showPreviousPanel() {
        if (previousPanel != null) {
            JPanel target = previousPanel;
            previousPanel = (JPanel) parentFrame.getContentPane();
            parentFrame.setContentPane(target);
            parentFrame.revalidate();
            parentFrame.repaint();
            if (target instanceof GamePanel) {
                ((GamePanel) target).syncDialogAlpha();
            }
        } else {
            showMainMenu();
        }
    }
}
