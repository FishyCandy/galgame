package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
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

    private StrokableLabel characterLabel;
    private StrokableTextArea lineArea;
    private JButton autoPlayBtn, logBtn;
    private JPanel dialogPanel;
    private JPanel controlPanel;
    private JPanel leftSpacer;
    private JPanel rightSpacer;
    private Timer autoTimer;
    private boolean isAutoPlaying = false;
    private boolean isEnding = false;           // 故事结束状态标志
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
    private static float globalDialogAlpha = 0.5f;

    public static void setAutoPlayDelay(int ms) {
        autoPlayDelayMs = ms;
    }
    public static void setDialogAlpha(float alpha) {
        globalDialogAlpha = alpha;
    }          // 对话框透明度 (0~1)
    private boolean isBgTransitioning = false;   // 是否正在转场
    private boolean clickPending = false;
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
        final int STEPS = 15; // ~240ms @ 16ms/tick
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

        final int STEPS = 15; // ~240ms @ 16ms/tick
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
            protected void paintChildren(Graphics g) {
                // 转场动画时文字随覆盖层淡入淡出，静态状态下文字保持完全不透明
                if (isBgTransitioning) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dialogFadeAlpha));
                    super.paintChildren(g2);
                    g2.dispose();
                } else {
                    super.paintChildren(g);
                }
            }
        };
        dialogPanel.setOpaque(false);
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        characterLabel = new StrokableLabel("");
        characterLabel.setFont(dialogFont.deriveFont(26f));
        characterLabel.setForeground(currentTextColor);

        lineArea = new StrokableTextArea();
        lineArea.setFont(dialogFont.deriveFont(22f));
        lineArea.setForeground(currentTextColor);
        lineArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lineArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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

        // wrapper包裹对话框，全屏模式下左右各留边距，居中显示；同时绘制全宽半透明覆盖层
        JPanel dialogWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // 全宽半透明覆盖层，透明度由 dialogFadeAlpha 控制
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int alpha = Math.round(180 * dialogFadeAlpha);
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void doLayout() {
                // 检测是否全屏模式
                java.awt.GraphicsDevice gd = parentFrame.getGraphicsConfiguration().getDevice();
                boolean fullscreen = (gd.getFullScreenWindow() == parentFrame);
                int margin = parentFrame.getWidth() / 6;
                if (leftSpacer != null) leftSpacer.setPreferredSize(new java.awt.Dimension(margin, 1));
                if (rightSpacer != null) rightSpacer.setPreferredSize(new java.awt.Dimension(margin, 1));
                super.doLayout();
            }
        };
        dialogWrapper.setOpaque(false);
        leftSpacer = new JPanel(); leftSpacer.setOpaque(false);
        rightSpacer = new JPanel(); rightSpacer.setOpaque(false);
        dialogWrapper.add(leftSpacer, BorderLayout.WEST);
        dialogWrapper.add(rightSpacer, BorderLayout.EAST);
        dialogWrapper.add(dialogPanel, BorderLayout.CENTER);
        add(dialogWrapper, BorderLayout.SOUTH);
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        controlPanel.setOpaque(false);

        autoPlayBtn = createGlassButton("Auto");
        logBtn = createGlassButton("Log");
        JButton saveBtn = createGlassButton("Save");
        JButton loadBtn = createGlassButton("Load");
        JButton returnBtn = createGlassButton("Return");
        JButton settingBtn = createGlassButton("Setting");

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

        settingBtn.addActionListener(e -> {
            SettingsPanel settingsPanel = new SettingsPanel(parentFrame, mainMenuPanel,
                    mainMenuPanel.getTitleFont(), mainMenuPanel.getButtonFont());
            mainMenuPanel.switchToPanel(settingsPanel);
        });

        for (JButton btn : new JButton[]{autoPlayBtn, logBtn, saveBtn, loadBtn, returnBtn, settingBtn}) {
            controlPanel.add(btn);
        }
        add(controlPanel, BorderLayout.NORTH);
    }

    private JButton createGlassButton(String text) {
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 150));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.setStroke(new BasicStroke(6f));
                    g2.drawRoundRect(3, 3, getWidth()-7, getHeight()-7, 20, 20);
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 20, 20);
                }
// 丁香紫描边效果
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout tl = new TextLayout(getText(), getFont(), frc);
                java.awt.geom.Rectangle2D tlBounds = tl.getBounds();
                int offset = pressed ? 2 : 0;
                float textX = (getWidth() - (float) tlBounds.getWidth()) / 2 + offset;
                float textY = (getHeight() + tl.getAscent() - tl.getDescent()) / 2f + offset;
                AffineTransform at = AffineTransform.getTranslateInstance(textX, textY);
                Shape outline = tl.getOutline(at);
                // 丁香紫描边
                g2.setColor(new Color(190, 145, 220));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);
                // 白色填充
                g2.setColor(getForeground());
                g2.fill(outline);
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
            // 等比缩放裁剪，保持图片比例不变形
            int panelW = getWidth();
            int panelH = getHeight();
            double panelRatio = (double) panelW / panelH;
            double imgRatio = (double) bgImage.getWidth() / bgImage.getHeight();
            int drawW, drawH, drawX, drawY;
            if (panelRatio > imgRatio) {
                // 面板更宽，按宽度缩放，裁剪上下
                drawW = panelW;
                drawH = (int) (panelW / imgRatio);
                drawX = 0;
                drawY = (panelH - drawH) / 2;
            } else {
                // 面板更高，按高度缩放，裁剪左右
                drawH = panelH;
                drawW = (int) (panelH * imgRatio);
                drawX = (panelW - drawW) / 2;
                drawY = 0;
            }
            g.drawImage(bgImage, drawX, drawY, drawW, drawH, this);
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
        isEnding = false;
        currentTextColor = Color.WHITE;
        dialogPanel.setVisible(true);
        controlPanel.setVisible(true);
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
        isEnding = false;
        currentTextColor = Color.WHITE;
        dialogPanel.setVisible(true);
        controlPanel.setVisible(true);
        stopMusic();
        updateDisplay();
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stopImmediately();
        }
    }

    /**
     * 同步对话框透明度（从设置页返回时调用）
     */
    public void syncDialogAlpha() {
        dialogFadeAlpha = globalDialogAlpha;
        repaint();
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

        final int STEPS_PER_PHASE = 30; // ~480ms @ 16ms/tick

        bgTransitionTimer = new Timer(16, null);
        bgTransitionTimer.addActionListener(e -> {
            if (fadePhase == 0) {
                // 第一阶段：淡出到黑幕（背景 + 对话框 + 台词一同消失）
                fadeStep++;
                float progress = Math.min(1f, (float) fadeStep / STEPS_PER_PHASE);
                transitionAlpha = progress;
                dialogFadeAlpha = globalDialogAlpha * (1f - progress);

                if (progress >= 1f) {
                    // 切换到新背景，清空台词
                    loadBgImage(pendingBgPath);
                    characterLabel.setText("");
                    lineArea.setText("");
                    fadePhase = 1;
                    fadeStep = 0;
                }
            } else if (fadePhase == 1) {
                // 第二阶段：从黑幕淡入（新背景 + 空覆盖层出现）
                fadeStep++;
                float progress = Math.min(1f, (float) fadeStep / STEPS_PER_PHASE);
                transitionAlpha = 1f - progress;
                dialogFadeAlpha = globalDialogAlpha * progress;

                if (progress >= 1f) {
                    transitionAlpha = 0f;
                    dialogFadeAlpha = globalDialogAlpha;
                    isBgTransitioning = false;
                    bgTransitionTimer.stop();
                    if (clickPending) {
                        clickPending = false;
                        nextCommand();
                    } else if (!isEnding) {
                        updateDisplay();
                    }
                }
            }
            repaint();
        });

        bgTransitionTimer.start();
    }

    public void updateDisplay() {
        if (isEnding) {
            // 故事结束：不弹窗，显示返回确认覆盖层
            confirmReturn();
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
            // 命令结束：不弹窗，设为ending状态
            isEnding = true;
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
                // 设置故事结束标志，播放ending背景图
                isEnding = true;
                dialogPanel.setVisible(false);
                controlPanel.setVisible(false);
                if (cmd.image != null && !cmd.image.isEmpty()) {
                    currentBgPath = cmd.image;
                    startBgTransition(cmd.image);
                }
                return;


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
                    alpha += 0.35f;
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
// 丁香紫描边效果
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout tl = new TextLayout(getText(), getFont(), frc);
                java.awt.geom.Rectangle2D tlBounds = tl.getBounds();
                int offset = pressed ? 2 : 0;
                float textX = (getWidth() - (float) tlBounds.getWidth()) / 2 + offset;
                float textY = (getHeight() + tl.getAscent() - tl.getDescent()) / 2f + offset;
                AffineTransform at = AffineTransform.getTranslateInstance(textX, textY);
                Shape outline = tl.getOutline(at);
                // 丁香紫描边
                g2.setColor(new Color(190, 145, 220));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);
                // 白色填充
                g2.setColor(getForeground());
                g2.fill(outline);
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
        if (isBgTransitioning) {
            if (isEnding) clickPending = true;
            return;
        }
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
        LogPanel logPanel = new LogPanel(parentFrame, this, new java.util.ArrayList<>(history), dialogFont, bgImage);
        parentFrame.setContentPane(logPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }
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
            SaveData data = new SaveData(thumbBytes, sceneId, cmdIndex,
                    currentBgPath, currentSpritePath, storyManager.getScores());
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
                oos.writeObject(data);
            }
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
                    case "say":
                        if (cmd.who != null && cmd.text != null) {
                            history.add(cmd.who + "：" + cmd.text);
                        }
                        break;
                    default:
                        break;
                }
            }

            // 重放结束后，cmdIndex 已经等于 savedCmdIndex（指向下一条未处理的指令）
            // 直接从历史记录中恢复最后一句台词的显示，避免重复添加
            storyManager.setCurrentCommandIndex(savedCmdIndex);
            if (!history.isEmpty()) {
                String lastEntry = history.get(history.size() - 1);
                int colonIdx = lastEntry.indexOf('：');
                if (colonIdx > 0) {
                    characterLabel.setText(lastEntry.substring(0, colonIdx));
                    lineArea.setText(lastEntry.substring(colonIdx + 1));
                }
            }
            waitingForChoice = false;
            revalidate();
            repaint();
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "读档失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private BufferedImage captureThumbnail() {
        int thumbW = 260;
        int thumbH = (int) (260.0 * getHeight() / getWidth());
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        paint(g);
        g.dispose();
        Image scaled = img.getScaledInstance(thumbW, thumbH, Image.SCALE_SMOOTH);
        BufferedImage thumb = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return thumb;
    }
    // ---------- 返回标题 ----------
    private void confirmReturn() {
        // 使用覆盖层替代JOptionPane，避免全屏下弹窗导致窗口最小化
        JLayeredPane layeredPane = parentFrame.getLayeredPane();

        // 半透明暗幕 + 居中确认按钮
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

        // 居中竖直排列的确认按钮
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        JLabel title = new JLabel((isEnding ? "游戏结束，你确定返回吗？" : "返回标题页面，未保存的进度会丢失，你确定返回吗？"));
        title.setFont(buttonFont.deriveFont(20f));
        title.setForeground(new Color(255, 255, 255, 200));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 30)));

        // "确定返回"按钮
        JButton confirmBtn = createChoiceButton("确定返回");
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension btnSize = new Dimension(300, 60);
        confirmBtn.setPreferredSize(btnSize);
        confirmBtn.setMaximumSize(btnSize);
        confirmBtn.setMinimumSize(btnSize);
        confirmBtn.addActionListener(e -> {
            layeredPane.remove(overlay);
            layeredPane.revalidate();
            layeredPane.repaint();
            if (isAutoPlaying) { toggleAutoPlay(); }
            stopMusic();
            mainMenuPanel.showMainMenu();
        });
        card.add(confirmBtn);
        card.add(Box.createRigidArea(new Dimension(0, 15)));

        // "取消"按钮
        JButton cancelBtn = createChoiceButton("取消");
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.setPreferredSize(btnSize);
        cancelBtn.setMaximumSize(btnSize);
        cancelBtn.setMinimumSize(btnSize);
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

}





