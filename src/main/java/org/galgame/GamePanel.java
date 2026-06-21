package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GamePanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private BufferedImage bgImage;
    private BufferedImage spriteImage; // 人物差分图
    private String currentBgPath = null;     // 当前背景路径
    private String currentSpritePath = null; // 当前差分图路径

    private StoryManager storyManager;
    private boolean waitingForChoice = false;

    private JLabel characterLabel;
    private JTextArea lineArea;
    private JButton autoPlayBtn, logBtn;
    private JPanel dialogPanel;
    private Timer autoTimer;
    private boolean isAutoPlaying = false;
    private List<String> history = new ArrayList<>();

    private Color currentTextColor = Color.WHITE;
    private Font dialogFont;
    private Font buttonFont;
    private MusicPlayer musicPlayer;

    // ---- 背景转场相关字段 ----
    private float transitionAlpha = 0f;          // 黑幕透明度 (0~1)
    private float dialogFadeAlpha = 1f;
    // ---- 全局设置（由设置页面控制） ----
    private static int autoPlayDelayMs = 2000;
    private static float globalDialogAlpha = 0.85f;

    public static void setAutoPlayDelay(int ms) {
        autoPlayDelayMs = ms;
    }
    public static void setDialogAlpha(float alpha) {
        globalDialogAlpha = alpha;
    }          // 对话框透明度 (0~1)
    private boolean isBgTransitioning = false;   // 是否正在转场
    private int fadeStep = 0;
    private int fadePhase = 0;                   // 0=淡出至黑, 1=从黑淡入
    private String pendingBgPath = null;         // 待切换的背景图路径
    private Timer bgTransitionTimer;             // 转场动画定时器

    // ---- 差分图转场相关字段 ----
    private float spriteAlpha = 1f;              // 差分图透明度 (0~1)
    private boolean isSpriteTransitioning = false;
    private String pendingSpritePath = null;
    private Timer spriteTransitionTimer;
    private int spriteFadeStep = 0;
    private int spriteFadePhase = 0;             // 0=淡出, 1=淡入

    public GamePanel(JFrame frame, MainMenuPanel mainMenu) {
        this.parentFrame = frame;
        this.mainMenuPanel = mainMenu;
        setLayout(new BorderLayout());
        setOpaque(false);

        musicPlayer = new MusicPlayer();
        musicPlayer.setLooping(true);

        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/MyTextFont.ttf");
            if (fontStream != null) {
                Font rawFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(rawFont);
                dialogFont = rawFont.deriveFont(24f);
            } else {
                dialogFont = new Font("微软雅黑", Font.PLAIN, 24);
                System.err.println("字体文件未找到，使用系统默认字体");
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialogFont = new Font("微软雅黑", Font.PLAIN, 24);
        }

        // 加载按钮字体（用于选项按钮）
        try {
            InputStream btnFontStream = getClass().getResourceAsStream("/fonts/MyButtonFont.ttf");
            if (btnFontStream != null) {
                buttonFont = Font.createFont(Font.TRUETYPE_FONT, btnFontStream).deriveFont(24f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(buttonFont);
            } else {
                buttonFont = new Font("微软雅黑", Font.BOLD, 24);
            }
        } catch (Exception e) {
            buttonFont = new Font("微软雅黑", Font.BOLD, 24);
        }

        storyManager = new StoryManager();

        // 初始状态：全黑背景，对话框不可见
        bgImage = null;
        transitionAlpha = 1f;
        dialogFadeAlpha = 0f;

        createDialogPanel();
        createControlPanel();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == GamePanel.this && !waitingForChoice && !isBgTransitioning) {
                    nextCommand();
                }
            }
        });

        autoTimer = new Timer(autoPlayDelayMs, e -> {
            if (!isBgTransitioning) nextCommand();
        });
    }





    // ---------- 人物差分图 ----------
    private void loadSpriteImage(String path) {
        if (path == null || path.isEmpty()) return;
        pendingSpritePath = path;

        // 停止之前的转场定时器
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }

        if (spriteImage == null) {
            // 没有当前差分图，直接加载并淡入
            loadSpriteFromPending();
            spriteAlpha = 0f;
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 1; // 直接进入淡入阶段
            startSpriteFadeTimer();
        } else {
            // 有当前差分图，先淡出
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 0; // 先淡出
            spriteAlpha = 1f;
            startSpriteFadeTimer();
        }
    }

    private void loadSpriteFromPending() {
        try {
            java.net.URL imgUrl = getClass().getResource("/" + pendingSpritePath);
            if (imgUrl != null) {
                spriteImage = ImageIO.read(imgUrl);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("差分图未找到: " + pendingSpritePath);
    }

    private void startSpriteFadeTimer() {
        final int STEPS = 20; // ~320ms @ 16ms/tick
        spriteTransitionTimer = new Timer(16, null);
        spriteTransitionTimer.addActionListener(e -> {
            spriteFadeStep++;
            float progress = Math.min(1f, (float) spriteFadeStep / STEPS);

            if (spriteFadePhase == 0) {
                // 淡出
                spriteAlpha = 1f - progress;
                if (progress >= 1f) {
                    // 切换到新图，开始淡入
                    loadSpriteFromPending();
                    spriteAlpha = 0f;
                    spriteFadePhase = 1;
                    spriteFadeStep = 0;
                }
            } else if (spriteFadePhase == 1) {
                // 淡入
                spriteAlpha = progress;
                if (progress >= 1f) {
                    spriteAlpha = 1f;
                    isSpriteTransitioning = false;
                    spriteTransitionTimer.stop();
                }
            }
            repaint();
        });
        spriteTransitionTimer.start();
    }

    private void hideSprite() {
        if (spriteImage == null) return;

        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }

        isSpriteTransitioning = true;
        spriteFadeStep = 0;
        spriteFadePhase = 0; // 淡出
        spriteAlpha = 1f;

        final int STEPS = 20;
        spriteTransitionTimer = new Timer(16, null);
        spriteTransitionTimer.addActionListener(e -> {
            spriteFadeStep++;
            float progress = Math.min(1f, (float) spriteFadeStep / STEPS);
            spriteAlpha = 1f - progress;
            if (progress >= 1f) {
                spriteImage = null;
                spriteAlpha = 1f;
                isSpriteTransitioning = false;
                spriteTransitionTimer.stop();
            }
            repaint();
        });
        spriteTransitionTimer.start();
    }

    private void createDialogPanel() {
        dialogPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 对话框背景和边框随 dialogFadeAlpha 淡出/淡入
                int bgAlpha = Math.round(160 * dialogFadeAlpha);
                int borderAlpha = Math.round(180 * dialogFadeAlpha);
                int innerAlpha = Math.round(30 * dialogFadeAlpha);

                g2.setColor(new Color(255, 255, 255, bgAlpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, borderAlpha));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 30, 30);
                g2.setColor(new Color(255, 255, 255, innerAlpha));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintChildren(Graphics g) {
                // 让对话框内的所有子组件（角色名、台词）也随 dialogFadeAlpha 淡出/淡入
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dialogFadeAlpha));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        dialogPanel.setOpaque(false);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        characterLabel = new JLabel("");
        characterLabel.setFont(dialogFont.deriveFont(26f));
        characterLabel.setForeground(currentTextColor);

        lineArea = new JTextArea(3, 35);
        lineArea.setFont(dialogFont.deriveFont(22f));
        lineArea.setLineWrap(true);
        lineArea.setWrapStyleWord(true);
        lineArea.setEditable(false);
        lineArea.setOpaque(false);
        lineArea.setForeground(currentTextColor);
        lineArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // 彻底隐藏光标和选中效果（galgame对话框不应有光标）
        lineArea.getCaret().setVisible(false);           // 隐藏光标闪烁
        lineArea.setCaretColor(new java.awt.Color(0, 0, 0, 0));     // 光标颜色透明
        lineArea.setSelectionColor(new java.awt.Color(0, 0, 0, 0)); // 选中背景透明
        lineArea.setSelectedTextColor(lineArea.getForeground());     // 选中文字颜色不变
        lineArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!waitingForChoice && !isBgTransitioning) nextCommand();
            }
        });

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(characterLabel, BorderLayout.NORTH);
        textPanel.add(lineArea, BorderLayout.CENTER);
        dialogPanel.add(textPanel, BorderLayout.CENTER);
        // 固定对话框大小，防止尺寸变化
        dialogPanel.setPreferredSize(new java.awt.Dimension(400, 200));

        add(dialogPanel, BorderLayout.SOUTH);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        controlPanel.setOpaque(false);

        autoPlayBtn = createGlassButton("Auto");
        logBtn = createGlassButton("Log");
        JButton saveBtn = createGlassButton("Save");
        JButton loadBtn = createGlassButton("Load");
        JButton returnBtn = createGlassButton("Return");
        JButton musicBtn = createGlassButton("Music");

        autoPlayBtn.addActionListener(e -> toggleAutoPlay());
        logBtn.addActionListener(e -> showLog());

        saveBtn.addActionListener(e -> {
            SaveLoadPanel panel = new SaveLoadPanel(parentFrame, mainMenuPanel, this, SaveLoadPanel.Mode.SAVE);
            mainMenuPanel.switchToPanel(panel);
        });

        loadBtn.addActionListener(e -> {
            SaveLoadPanel panel = new SaveLoadPanel(parentFrame, mainMenuPanel, this, SaveLoadPanel.Mode.LOAD);
            mainMenuPanel.switchToPanel(panel);
        });

        returnBtn.addActionListener(e -> confirmReturn());

        musicBtn.addActionListener(e -> {
            MusicPlayerPanel musicPanel = new MusicPlayerPanel(parentFrame, mainMenuPanel,
                    mainMenuPanel.getTitleFont(), mainMenuPanel.getButtonFont());
            mainMenuPanel.switchToPanel(musicPanel);
        });

        for (JButton btn : new JButton[]{autoPlayBtn, logBtn, saveBtn, loadBtn, returnBtn, musicBtn}) {
            controlPanel.add(btn);
        }
        add(controlPanel, BorderLayout.NORTH);
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 150));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                    getWidth(), getHeight(), new Color(10, 10, 30));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        // 绘制人物差分图（全屏高度，支持淡入淡出）
        if (spriteImage != null) {
            Graphics2D g2s = (Graphics2D) g.create();
            g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, spriteAlpha));
            int spriteHeight = getHeight();
            double ratio = (double) spriteImage.getWidth(null) / spriteImage.getHeight(null);
            int spriteWidth = (int) (spriteHeight * ratio);
            int x = (getWidth() - spriteWidth) / 2;
            int y = getHeight() - spriteHeight;
            g2s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2s.drawImage(spriteImage, x, y, spriteWidth, spriteHeight, this);
            g2s.dispose();
        }

        // 背景转场黑幕（覆盖在背景之上）
        if (transitionAlpha > 0.01f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitionAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ---------- 核心游戏逻辑 ----------
    public void startGame() {
        history.clear();
        // 重置转场状态：从全黑开始淡入
        if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) {
            bgTransitionTimer.stop();
        }
        bgImage = null;
        transitionAlpha = 1f;
        dialogFadeAlpha = 0f;
        isBgTransitioning = false;
        spriteImage = null;
        currentBgPath = null;
        currentSpritePath = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");
        lineArea.setText("");
        storyManager = new StoryManager();
        waitingForChoice = false;
        currentTextColor = Color.WHITE;
        updateDisplay();
    }

    public void resetGame() {
        // 重置转场状态
        if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) {
            bgTransitionTimer.stop();
        }
        if (autoTimer != null && autoTimer.isRunning()) {
            autoTimer.stop();
            isAutoPlaying = false;
            autoPlayBtn.setText("Auto");
        }
        bgImage = null;
        transitionAlpha = 1f;
        dialogFadeAlpha = 0f;
        isBgTransitioning = false;
        spriteImage = null;
        currentBgPath = null;
        currentSpritePath = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");
        lineArea.setText("");
        history.clear();
        storyManager = new StoryManager();
        waitingForChoice = false;
        currentTextColor = Color.WHITE;
        stopMusic();
        updateDisplay();
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stopImmediately();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    // ---------- 背景转场 ----------

    /**
     * 加载背景图片
     */
    private void loadBgImage(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                InputStream is = getClass().getResourceAsStream("/" + path);
                if (is != null) {
                    bgImage = ImageIO.read(is);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println("背景图未找到: " + path);
    }

    /**
     * 启动背景转场动画：
     * 阶段0：原背景 + 对话框渐入黑幕（逐渐消失）
     * 阶段1：黑幕中切换背景，再渐入新背景 + 空对话框
     */
    private void startBgTransition(String newBgPath) {
        if (isBgTransitioning) return;
        pendingBgPath = newBgPath;
        fadeStep = 0;
        isBgTransitioning = true;

        if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) {
            bgTransitionTimer.stop();
        }

        // 如果没有当前背景（首次加载），跳过淡出阶段，直接从黑色淡入
        if (bgImage == null) {
            // 直接加载新背景
            loadBgImage(pendingBgPath);
            transitionAlpha = 1f;   // 全黑覆盖
            dialogFadeAlpha = 0f;   // 对话框不可见
            fadePhase = 1;          // 跳过阶段0，直接进入淡入
            characterLabel.setText("");
            lineArea.setText("");
        } else {
            transitionAlpha = 0f;
            dialogFadeAlpha = globalDialogAlpha;
            fadePhase = 0;
        }

        final int STEPS_PER_PHASE = 50; // ~800ms @ 16ms/tick

        bgTransitionTimer = new Timer(16, null);
        bgTransitionTimer.addActionListener(e -> {
            if (fadePhase == 0) {
                // 第一阶段：淡出到黑幕（背景 + 对话框 + 台词一同消失）
                fadeStep++;
                float progress = Math.min(1f, (float) fadeStep / STEPS_PER_PHASE);
                transitionAlpha = progress;
                dialogFadeAlpha = 1f - progress;

                if (progress >= 1f) {
                    // 切换到新背景，清空台词
                    loadBgImage(pendingBgPath);
                    characterLabel.setText("");
                    lineArea.setText("");
                    fadePhase = 1;
                    fadeStep = 0;
                }
            } else if (fadePhase == 1) {
                // 第二阶段：从黑幕淡入（新背景 + 空对话框出现）
                fadeStep++;
                float progress = Math.min(1f, (float) fadeStep / STEPS_PER_PHASE);
                transitionAlpha = 1f - progress;
                dialogFadeAlpha = progress;

                if (progress >= 1f) {
                    transitionAlpha = 0f;
                    dialogFadeAlpha = globalDialogAlpha;
                    isBgTransitioning = false;
                    bgTransitionTimer.stop();
                }
            }
            repaint();
        });

        bgTransitionTimer.start();
    }

    public void updateDisplay() {
        if (storyManager.isEnd()) {
            if (autoTimer != null && autoTimer.isRunning()) {
                autoTimer.stop();
                isAutoPlaying = false;
                autoPlayBtn.setText("Auto");
            }
            JOptionPane.showMessageDialog(this, "故事已结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> mainMenuPanel.showMainMenu());
            return;
        }

        if (waitingForChoice) return;

        StoryData.CommandData cmd = storyManager.nextCommand();
        if (cmd == null) {
            if (autoTimer != null && autoTimer.isRunning()) {
                autoTimer.stop();
                isAutoPlaying = false;
                autoPlayBtn.setText("Auto");
            }
            JOptionPane.showMessageDialog(this, "故事已结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> mainMenuPanel.showMainMenu());
            return;
        }

        switch (cmd.type) {

            case "show":
                // 旧版show指令已废弃，自动跳过
                updateDisplay();
                return;

            case "say":
                history.add(cmd.who + "：" + cmd.text);
                characterLabel.setText(cmd.who);
                lineArea.setText(cmd.text);

                if (cmd.color != null && !cmd.color.isEmpty()) {
                    try {
                        currentTextColor = Color.decode(cmd.color);
                    } catch (NumberFormatException e) {}
                }
                characterLabel.setForeground(currentTextColor);
                lineArea.setForeground(currentTextColor);

                revalidate();
                repaint();
                break;

            case "choice":
                waitingForChoice = true;
                showChoices(cmd.choices);
                break;

            case "bgm":
                if (cmd.bgm != null && !cmd.bgm.isEmpty()) {
                    URL musicUrl = getClass().getResource("/music/" + cmd.bgm);
                    if (musicUrl != null) {
                        musicPlayer.fadeTo(musicUrl);
                    } else {
                        System.err.println("音乐文件未找到: " + cmd.bgm);
                    }
                }
                updateDisplay();
                return;

            case "sprite":
                // 显示人物差分图（具有延续性）
                if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                    currentSpritePath = cmd.sprite;
                    loadSpriteImage(cmd.sprite);
                }
                updateDisplay();
                return;

            case "sprite_hide":
                // 隐藏人物差分图
                currentSpritePath = null;
                hideSprite();
                updateDisplay();
                return;

            case "set":
                // 设置隐藏分变量
                if (cmd.var != null && cmd.value != null) {
                    storyManager.setScore(cmd.var, cmd.value);
                }
                updateDisplay();
                return;

            case "check":
                // 检查分数条件，跳转到对应场景
                if (cmd.var != null && cmd.min != null && cmd.target != null) {
                    String resultScene = storyManager.checkCondition(cmd.var, cmd.min, cmd.target, cmd.fallback);
                    if (resultScene != null) {
                        storyManager.jumpToScene(resultScene);
                        updateDisplay();
                        return;
                    }
                }
                updateDisplay();
                return;

            case "bg":
                // 背景切换指令：启动淡入淡出转场
                if (cmd.bg != null && !cmd.bg.isEmpty()) {
                    currentBgPath = cmd.bg;
                    startBgTransition(cmd.bg);
                }
                // 不立即推进到下一条指令，等待转场完成后的用户点击
                return;

            case "end":
                if (autoTimer != null && autoTimer.isRunning()) {
                    autoTimer.stop();
                    isAutoPlaying = false;
                    autoPlayBtn.setText("Auto");
                }
                JOptionPane.showMessageDialog(this, "故事结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> mainMenuPanel.showMainMenu());
                break;

            default:
                break;
        }
        revalidate();
        repaint();
    }

    private void showChoices(List<StoryData.ChoiceData> choiceDataList) {
        // 使用 frame 的 JLayeredPane 实现全屏覆盖（包括顶部按钮栏和底部对话框）
        JLayeredPane layeredPane = parentFrame.getLayeredPane();

        // 全屏半透明暗幕 + 居中选项按钮（带淡入动画）
        JPanel overlay = new JPanel(new GridBagLayout()) {
            private float alpha = 0f;
            private Timer fadeInTimer;
            {
                setOpaque(false);
                setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
                fadeInTimer = new Timer(30, e -> {
                    alpha += 0.06f;
                    if (alpha >= 1f) {
                        alpha = 1f;
                        fadeInTimer.stop();
                    }
                    repaint();
                });
                fadeInTimer.start();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, (int)(200 * alpha)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // 居中竖直排列的选项按钮（统一大小，无外框）
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        // 标题
        JLabel title = new JLabel("—— 请做出选择 ——");
        title.setFont(buttonFont.deriveFont(22f));
        title.setForeground(new Color(255, 255, 255, 200));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 30)));

        // 固定大小的选项按钮
        for (ChoiceOption opt : getChoiceOptions(choiceDataList)) {
            JButton btn = createChoiceButton(opt.getText());
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            // 固定按钮大小：宽 420，高 60
            Dimension btnSize = new Dimension(420, 60);
            btn.setPreferredSize(btnSize);
            btn.setMaximumSize(btnSize);
            btn.setMinimumSize(btnSize);
            btn.addActionListener(e -> {
                layeredPane.remove(overlay);
                layeredPane.revalidate();
                layeredPane.repaint();
                storyManager.jumpToScene(opt.getTarget());
                // 应用隐藏分
                if (opt.getScore() != null) {
                    for (Map.Entry<String, Integer> scoreEntry : opt.getScore().entrySet()) {
                        storyManager.addScore(scoreEntry.getKey(), scoreEntry.getValue());
                    }
                }
                waitingForChoice = false;
                updateDisplay();
            });
            card.add(btn);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
        }

        overlay.add(card);
        layeredPane.add(overlay, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    // 辅助方法：将 ChoiceData 转换为 ChoiceOption 列表
    private List<ChoiceOption> getChoiceOptions(List<StoryData.ChoiceData> choiceDataList) {
        List<ChoiceOption> options = new ArrayList<>();
        for (StoryData.ChoiceData cd : choiceDataList) {
            options.add(new ChoiceOption(cd.text, cd.target, cd.score));
        }
        return options;
    }

    // 创建毛玻璃风格选项按钮（与主菜单 createGlassButton 风格一致）
    private JButton createChoiceButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
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
        btn.setFont(buttonFont);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
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

    private void nextCommand() {
        if (waitingForChoice) return;
        if (isBgTransitioning) return;
        if (isSpriteTransitioning) return;
        updateDisplay();
    }

    private void toggleAutoPlay() {
        if (isAutoPlaying) {
            autoTimer.stop();
            isAutoPlaying = false;
            autoPlayBtn.setText("Auto");
        } else {
            if (storyManager.isEnd()) {
                storyManager = new StoryManager();
                updateDisplay();
            }
            autoTimer.setDelay(autoPlayDelayMs);
            autoTimer.start();
            isAutoPlaying = true;
            autoPlayBtn.setText("Stop");
        }
    }

    private void showLog() {
        LogPanel logPanel = new LogPanel(parentFrame, this, new java.util.ArrayList<>(history), dialogFont);
        parentFrame.setContentPane(logPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }

    // ---------- 存档/读档核心方法 ----------
    public boolean saveGameToFile(int slot) {
        if (storyManager == null) {
            JOptionPane.showMessageDialog(this, "没有可存档的进度。", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File saveFile = new File("save_" + slot + ".dat");
        try {
            BufferedImage thumbnail = captureThumbnail();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", baos);
            byte[] thumbBytes = baos.toByteArray();
            String sceneId = storyManager.getCurrentSceneId();
            int cmdIndex = storyManager.getCurrentCommandIndex();
            SaveData data = new SaveData(0, null, thumbBytes, sceneId, cmdIndex,
                    currentBgPath, currentSpritePath, storyManager.getScores());
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
                oos.writeObject(data);
            }
            JOptionPane.showMessageDialog(this, "存档成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "存档失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean loadGameFromFile(File saveFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            SaveData data = (SaveData) ois.readObject();
            storyManager.setCurrentSceneId(data.getCurrentSceneId());
            int savedCmdIndex = data.getCurrentCommandIndex();

            // 重置所有转场状态
            if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) bgTransitionTimer.stop();
            if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) spriteTransitionTimer.stop();
            transitionAlpha = 0f;
            dialogFadeAlpha = globalDialogAlpha;
            isBgTransitioning = false;
            spriteAlpha = 1f;
            isSpriteTransitioning = false;
            history.clear();
            waitingForChoice = false;

            // 恢复隐藏分
            if (data.getScores() != null) {
                storyManager.setScores(data.getScores());
            }

            // 重放场景中所有bg/sprite/bgm指令来重建状态（从索引0到存档位置）
            bgImage = null;
            currentBgPath = null;
            spriteImage = null;
            currentSpritePath = null;
            storyManager.setCurrentCommandIndex(0);
            for (int i = 0; i < savedCmdIndex; i++) {
                StoryData.CommandData cmd = storyManager.nextCommand();
                if (cmd == null) break;
                switch (cmd.type) {
                    case "bg":
                        if (cmd.bg != null && !cmd.bg.isEmpty()) {
                            loadBgImage(cmd.bg);
                            currentBgPath = cmd.bg;
                        }
                        break;
                    case "sprite":
                        if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                            try {
                                java.net.URL imgUrl = getClass().getResource("/" + cmd.sprite);
                                if (imgUrl != null) {
                                    spriteImage = ImageIO.read(imgUrl);
                                    currentSpritePath = cmd.sprite;
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        break;
                    case "sprite_hide":
                        spriteImage = null;
                        currentSpritePath = null;
                        break;
                    case "bgm":
                        if (cmd.bgm != null && !cmd.bgm.isEmpty()) {
                            URL musicUrl = getClass().getResource("/music/" + cmd.bgm);
                            if (musicUrl != null) {
                                musicPlayer.fadeTo(musicUrl);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            // 回退一步：存档时的索引指向下一条指令，回退到已显示的指令
            storyManager.setCurrentCommandIndex(savedCmdIndex);
            if (savedCmdIndex > 0) {
                storyManager.setCurrentCommandIndex(savedCmdIndex - 1);
            }
            updateDisplay();
            JOptionPane.showMessageDialog(this, "读档成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "读档失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private BufferedImage captureThumbnail() {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        paint(g);
        g.dispose();
        Image scaled = img.getScaledInstance(160, 90, Image.SCALE_SMOOTH);
        BufferedImage thumb = new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return thumb;
    }

    // ---------- 返回标题 ----------
    private void confirmReturn() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean wasFullScreen = (gd.getFullScreenWindow() != null);

        int option = JOptionPane.showConfirmDialog(
                this,
                "返回标题页面，未保存的进度会丢失，你确定返回吗？",
                "确认返回",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            if (isAutoPlaying) {
                toggleAutoPlay();
            }
            stopMusic();
            SwingUtilities.invokeLater(() -> {
                // 全屏模式下，先正常退出全屏，再切换回主菜单
                if (wasFullScreen) {
                    gd.setFullScreenWindow(null);
                    parentFrame.dispose();
                    parentFrame.setUndecorated(false);
                    parentFrame.setSize(1024, 640);
                    parentFrame.setLocationRelativeTo(null);
                    parentFrame.setVisible(true);
                }
                mainMenuPanel.showMainMenu();
            });
        }
    }
public List<?> getDialogues() { return null; }
}





