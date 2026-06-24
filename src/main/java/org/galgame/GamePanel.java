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
    private BufferedImage spriteImage; // 浜虹墿宸垎鍥?
    private String currentBgPath = null;     // 褰撳墠鑳屾櫙璺緞
    private String currentSpritePath = null; // 褰撳墠宸垎鍥捐矾寰?

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
    private boolean isEnding = false;           // 鏁呬簨缁撴潫鐘舵€佹爣蹇?
    private List<String> history = new ArrayList<>();

    private Color currentTextColor = Color.WHITE;
    private Font dialogFont;
    private Font buttonFont;
    private MusicPlayer musicPlayer;

    // ---- 鑳屾櫙杞満鐩稿叧瀛楁 ----
    private float transitionAlpha = 0f;          // 榛戝箷閫忔槑搴?(0~1)
    private float dialogFadeAlpha = 1f;
    // ---- 鍏ㄥ眬璁剧疆锛堢敱璁剧疆椤甸潰鎺у埗锛?----
    private static int autoPlayDelayMs = 2000;
    private static float globalDialogAlpha = 0.5f;

    public static void setAutoPlayDelay(int ms) {
        autoPlayDelayMs = ms;
    }
    public static void setDialogAlpha(float alpha) {
        globalDialogAlpha = alpha;
    }          // 瀵硅瘽妗嗛€忔槑搴?(0~1)
    private boolean isBgTransitioning = false;   // 鏄惁姝ｅ湪杞満
    private boolean clickPending = false;
    private int fadeStep = 0;
    private int fadePhase = 0;                   // 0=娣″嚭鑷抽粦, 1=浠庨粦娣″叆
    private String pendingBgPath = null;         // 寰呭垏鎹㈢殑鑳屾櫙鍥捐矾寰?
    private Timer bgTransitionTimer;             // 杞満鍔ㄧ敾瀹氭椂鍣?

    // ---- 宸垎鍥捐浆鍦虹浉鍏冲瓧娈?----
    private float spriteAlpha = 1f;              // 宸垎鍥鹃€忔槑搴?(0~1)
    private boolean isSpriteTransitioning = false;
    private String pendingSpritePath = null;
    private Timer spriteTransitionTimer;
    private int spriteFadeStep = 0;
    private int spriteFadePhase = 0;             // 0=娣″嚭, 1=娣″叆

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
                dialogFont = new Font("寰蒋闆呴粦", Font.PLAIN, 24);
                System.err.println("瀛椾綋鏂囦欢鏈壘鍒帮紝浣跨敤绯荤粺榛樿瀛椾綋");
            }
        } catch (Exception e) {
            e.printStackTrace();
            dialogFont = new Font("寰蒋闆呴粦", Font.PLAIN, 24);
        }

        // 鍔犺浇鎸夐挳瀛椾綋锛堢敤浜庨€夐」鎸夐挳锛?
        try {
            InputStream btnFontStream = getClass().getResourceAsStream("/fonts/MyButtonFont.ttf");
            if (btnFontStream != null) {
                buttonFont = Font.createFont(Font.TRUETYPE_FONT, btnFontStream).deriveFont(24f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(buttonFont);
            } else {
                buttonFont = new Font("寰蒋闆呴粦", Font.BOLD, 24);
            }
        } catch (Exception e) {
            buttonFont = new Font("寰蒋闆呴粦", Font.BOLD, 24);
        }

        storyManager = new StoryManager();

        // 鍒濆鐘舵€侊細鍏ㄩ粦鑳屾櫙锛屽璇濇涓嶅彲瑙?
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





    // ---------- 浜虹墿宸垎鍥?----------
    private void loadSpriteImage(String path) {
        if (path == null || path.isEmpty()) return;
        pendingSpritePath = path;

        // 鍋滄涔嬪墠鐨勮浆鍦哄畾鏃跺櫒
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }

        if (spriteImage == null) {
            // 娌℃湁褰撳墠宸垎鍥撅紝鐩存帴鍔犺浇骞舵贰鍏?
            loadSpriteFromPending();
            spriteAlpha = 0f;
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 1; // 鐩存帴杩涘叆娣″叆闃舵
            startSpriteFadeTimer();
        } else {
            // 鏈夊綋鍓嶅樊鍒嗗浘锛屽厛娣″嚭
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 0; // 鍏堟贰鍑?
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
        System.err.println("宸垎鍥炬湭鎵惧埌: " + pendingSpritePath);
    }

    private void startSpriteFadeTimer() {
        final int STEPS = 20; // ~320ms @ 16ms/tick
        spriteTransitionTimer = new Timer(16, null);
        spriteTransitionTimer.addActionListener(e -> {
            spriteFadeStep++;
            float progress = Math.min(1f, (float) spriteFadeStep / STEPS);

            if (spriteFadePhase == 0) {
                // 娣″嚭
                spriteAlpha = 1f - progress;
                if (progress >= 1f) {
                    // 鍒囨崲鍒版柊鍥撅紝寮€濮嬫贰鍏?
                    loadSpriteFromPending();
                    spriteAlpha = 0f;
                    spriteFadePhase = 1;
                    spriteFadeStep = 0;
                }
            } else if (spriteFadePhase == 1) {
                // 娣″叆
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
        spriteFadePhase = 0; // 娣″嚭
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
            protected void paintChildren(Graphics g) {
                // 杞満鍔ㄧ敾鏃舵枃瀛楅殢瑕嗙洊灞傛贰鍏ユ贰鍑猴紝闈欐€佺姸鎬佷笅鏂囧瓧淇濇寔瀹屽叏涓嶉€忔槑
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
        // 鍥哄畾瀵硅瘽妗嗗ぇ灏忥紝闃叉灏哄鍙樺寲
        dialogPanel.setPreferredSize(new java.awt.Dimension(400, 200));

        // wrapper鍖呰９瀵硅瘽妗嗭紝鍏ㄥ睆妯″紡涓嬪乏鍙冲悇鐣欒竟璺濓紝灞呬腑鏄剧ず锛涘悓鏃剁粯鍒跺叏瀹藉崐閫忔槑瑕嗙洊灞?
        JPanel dialogWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                // 鍏ㄥ鍗婇€忔槑瑕嗙洊灞傦紝閫忔槑搴︾敱 dialogFadeAlpha 鎺у埗
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
                // 妫€娴嬫槸鍚﹀叏灞忔ā寮?
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
// 涓侀绱弿杈规晥鏋?
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout tl = new TextLayout(getText(), getFont(), frc);
                java.awt.geom.Rectangle2D tlBounds = tl.getBounds();
                int offset = pressed ? 2 : 0;
                float textX = (getWidth() - (float) tlBounds.getWidth()) / 2 + offset;
                float textY = (getHeight() + tl.getAscent() - tl.getDescent()) / 2f + offset;
                AffineTransform at = AffineTransform.getTranslateInstance(textX, textY);
                Shape outline = tl.getOutline(at);
                // 涓侀绱弿杈?
                g2.setColor(new Color(190, 145, 220));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);
                // 鐧借壊濉厖
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
            // 绛夋瘮缂╂斁瑁佸壀锛屼繚鎸佸浘鐗囨瘮渚嬩笉鍙樺舰
            int panelW = getWidth();
            int panelH = getHeight();
            double panelRatio = (double) panelW / panelH;
            double imgRatio = (double) bgImage.getWidth() / bgImage.getHeight();
            int drawW, drawH, drawX, drawY;
            if (panelRatio > imgRatio) {
                // 闈㈡澘鏇村锛屾寜瀹藉害缂╂斁锛岃鍓笂涓?
                drawW = panelW;
                drawH = (int) (panelW / imgRatio);
                drawX = 0;
                drawY = (panelH - drawH) / 2;
            } else {
                // 闈㈡澘鏇撮珮锛屾寜楂樺害缂╂斁锛岃鍓乏鍙?
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

        // 缁樺埗浜虹墿宸垎鍥撅紙鍏ㄥ睆楂樺害锛屾敮鎸佹贰鍏ユ贰鍑猴級
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

        // 鑳屾櫙杞満榛戝箷锛堣鐩栧湪鑳屾櫙涔嬩笂锛?
        if (transitionAlpha > 0.01f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitionAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // ---------- 鏍稿績娓告垙閫昏緫 ----------
    public void startGame() {
        history.clear();
        // 閲嶇疆杞満鐘舵€侊細浠庡叏榛戝紑濮嬫贰鍏?
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
        // 閲嶇疆杞満鐘舵€?
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
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * 鍚屾瀵硅瘽妗嗛€忔槑搴︼紙浠庤缃〉杩斿洖鏃惰皟鐢級
     */
    public void syncDialogAlpha() {
        dialogFadeAlpha = globalDialogAlpha;
        repaint();
    }


    // ---------- 鑳屾櫙杞満 ----------

    /**
     * 鍔犺浇鑳屾櫙鍥剧墖
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
        System.err.println("鑳屾櫙鍥炬湭鎵惧埌: " + path);
    }

    /**
     * 鍚姩鑳屾櫙杞満鍔ㄧ敾锛?
     * 闃舵0锛氬師鑳屾櫙 + 瀵硅瘽妗嗘笎鍏ラ粦骞曪紙閫愭笎娑堝け锛?
     * 闃舵1锛氶粦骞曚腑鍒囨崲鑳屾櫙锛屽啀娓愬叆鏂拌儗鏅?+ 绌哄璇濇
     */
    private void startBgTransition(String newBgPath) {
        if (isBgTransitioning) return;
        pendingBgPath = newBgPath;
        fadeStep = 0;
        isBgTransitioning = true;

        if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) {
            bgTransitionTimer.stop();
        }

        // 濡傛灉娌℃湁褰撳墠鑳屾櫙锛堥娆″姞杞斤級锛岃烦杩囨贰鍑洪樁娈碉紝鐩存帴浠庨粦鑹叉贰鍏?
        if (bgImage == null) {
            // 鐩存帴鍔犺浇鏂拌儗鏅?
            loadBgImage(pendingBgPath);
            transitionAlpha = 1f;   // 鍏ㄩ粦瑕嗙洊
            dialogFadeAlpha = 0f;   // 瀵硅瘽妗嗕笉鍙
            fadePhase = 1;          // 璺宠繃闃舵0锛岀洿鎺ヨ繘鍏ユ贰鍏?
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
                // 绗竴闃舵锛氭贰鍑哄埌榛戝箷锛堣儗鏅?+ 瀵硅瘽妗?+ 鍙拌瘝涓€鍚屾秷澶憋級
                fadeStep++;
                float progress = Math.min(1f, (float) fadeStep / STEPS_PER_PHASE);
                transitionAlpha = progress;
                dialogFadeAlpha = globalDialogAlpha * (1f - progress);

                if (progress >= 1f) {
                    // 鍒囨崲鍒版柊鑳屾櫙锛屾竻绌哄彴璇?
                    loadBgImage(pendingBgPath);
                    characterLabel.setText("");
                    lineArea.setText("");
                    fadePhase = 1;
                    fadeStep = 0;
                }
            } else if (fadePhase == 1) {
                // 绗簩闃舵锛氫粠榛戝箷娣″叆锛堟柊鑳屾櫙 + 绌鸿鐩栧眰鍑虹幇锛?
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
            // 鏁呬簨缁撴潫锛氫笉寮圭獥锛屾樉绀鸿繑鍥炵‘璁よ鐩栧眰
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
            // 鍛戒护缁撴潫锛氫笉寮圭獥锛岃涓篹nding鐘舵€?
            isEnding = true;
            return;
        }
switch (cmd.type) {

            case "show":
                // 鏃х増show鎸囦护宸插簾寮冿紝鑷姩璺宠繃
                updateDisplay();
                return;

            case "say":
                history.add(cmd.who + "锛? + cmd.text);
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
                        System.err.println("闊充箰鏂囦欢鏈壘鍒? " + cmd.bgm);
                    }
                }
                updateDisplay();
                return;

            case "sprite":
                // 鏄剧ず浜虹墿宸垎鍥撅紙鍏锋湁寤剁画鎬э級
                if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                    currentSpritePath = cmd.sprite;
                    loadSpriteImage(cmd.sprite);
                }
                updateDisplay();
                return;

            case "sprite_hide":
                // 闅愯棌浜虹墿宸垎鍥?
                currentSpritePath = null;
                hideSprite();
                updateDisplay();
                return;

            case "set":
                // 璁剧疆闅愯棌鍒嗗彉閲?
                if (cmd.var != null && cmd.value != null) {
                    storyManager.setScore(cmd.var, cmd.value);
                }
                updateDisplay();
                return;

            case "check":
                // 妫€鏌ュ垎鏁版潯浠讹紝璺宠浆鍒板搴斿満鏅?
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
                // 鑳屾櫙鍒囨崲鎸囦护锛氬惎鍔ㄦ贰鍏ユ贰鍑鸿浆鍦?
                if (cmd.bg != null && !cmd.bg.isEmpty()) {
                    currentBgPath = cmd.bg;
                    startBgTransition(cmd.bg);
                }
                // 涓嶇珛鍗虫帹杩涘埌涓嬩竴鏉℃寚浠わ紝绛夊緟杞満瀹屾垚鍚庣殑鐢ㄦ埛鐐瑰嚮
                return;

            case "end":
                if (autoTimer != null && autoTimer.isRunning()) {
                    autoTimer.stop();
                    isAutoPlaying = false;
                    autoPlayBtn.setText("Auto");
                }
                // 璁剧疆鏁呬簨缁撴潫鏍囧織锛屾挱鏀緀nding鑳屾櫙鍥?
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
        // 浣跨敤 frame 鐨?JLayeredPane 瀹炵幇鍏ㄥ睆瑕嗙洊锛堝寘鎷《閮ㄦ寜閽爮鍜屽簳閮ㄥ璇濇锛?
        JLayeredPane layeredPane = parentFrame.getLayeredPane();

        // 鍏ㄥ睆鍗婇€忔槑鏆楀箷 + 灞呬腑閫夐」鎸夐挳锛堝甫娣″叆鍔ㄧ敾锛?
        JPanel overlay = new JPanel(new GridBagLayout()) {
            private float alpha = 0f;
            private Timer fadeInTimer;
            {
                setOpaque(false);
                setBounds(0, 0, parentFrame.getWidth(), parentFrame.getHeight());
                fadeInTimer = new Timer(30, e -> {
                    alpha += 0.2f;
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

        // 灞呬腑绔栫洿鎺掑垪鐨勯€夐」鎸夐挳锛堢粺涓€澶у皬锛屾棤澶栨锛?
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        // 鏍囬
        JLabel title = new JLabel("鈥斺€?璇峰仛鍑洪€夋嫨 鈥斺€?);
        title.setFont(buttonFont.deriveFont(22f));
        title.setForeground(new Color(255, 255, 255, 200));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 30)));

        // 鍥哄畾澶у皬鐨勯€夐」鎸夐挳
        for (ChoiceOption opt : getChoiceOptions(choiceDataList)) {
            JButton btn = createChoiceButton(opt.getText());
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            // 鍥哄畾鎸夐挳澶у皬锛氬 420锛岄珮 60
            Dimension btnSize = new Dimension(420, 60);
            btn.setPreferredSize(btnSize);
            btn.setMaximumSize(btnSize);
            btn.setMinimumSize(btnSize);
            btn.addActionListener(e -> {
                layeredPane.remove(overlay);
                layeredPane.revalidate();
                layeredPane.repaint();
                storyManager.jumpToScene(opt.getTarget());
                // 搴旂敤闅愯棌鍒?
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

    // 杈呭姪鏂规硶锛氬皢 ChoiceData 杞崲涓?ChoiceOption 鍒楄〃
    private List<ChoiceOption> getChoiceOptions(List<StoryData.ChoiceData> choiceDataList) {
        List<ChoiceOption> options = new ArrayList<>();
        for (StoryData.ChoiceData cd : choiceDataList) {
            options.add(new ChoiceOption(cd.text, cd.target, cd.score));
        }
        return options;
    }

    // 鍒涘缓姣涚幓鐠冮鏍奸€夐」鎸夐挳锛堜笌涓昏彍鍗?createGlassButton 椋庢牸涓€鑷达級
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
// 涓侀绱弿杈规晥鏋?
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout tl = new TextLayout(getText(), getFont(), frc);
                java.awt.geom.Rectangle2D tlBounds = tl.getBounds();
                int offset = pressed ? 2 : 0;
                float textX = (getWidth() - (float) tlBounds.getWidth()) / 2 + offset;
                float textY = (getHeight() + tl.getAscent() - tl.getDescent()) / 2f + offset;
                AffineTransform at = AffineTransform.getTranslateInstance(textX, textY);
                Shape outline = tl.getOutline(at);
                // 涓侀绱弿杈?
                g2.setColor(new Color(190, 145, 220));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outline);
                // 鐧借壊濉厖
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
        LogPanel logPanel = new LogPanel(parentFrame, this, new java.util.ArrayList<>(history), dialogFont);
        parentFrame.setContentPane(logPanel);
        parentFrame.revalidate();
        parentFrame.repaint();
    }
    public boolean saveGameToFile(int slot) {
        if (storyManager == null) {
            JOptionPane.showMessageDialog(this, "娌℃湁鍙瓨妗ｇ殑杩涘害銆?, "閿欒", JOptionPane.ERROR_MESSAGE);
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
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "瀛樻。澶辫触锛? + ex.getMessage(), "閿欒", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean loadGameFromFile(File saveFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            SaveData data = (SaveData) ois.readObject();
            storyManager.setCurrentSceneId(data.getCurrentSceneId());
            int savedCmdIndex = data.getCurrentCommandIndex();

            // 閲嶇疆鎵€鏈夎浆鍦虹姸鎬?
            if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) bgTransitionTimer.stop();
            if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) spriteTransitionTimer.stop();
            transitionAlpha = 0f;
            dialogFadeAlpha = globalDialogAlpha;
            isBgTransitioning = false;
            spriteAlpha = 1f;
            isSpriteTransitioning = false;
            history.clear();
            waitingForChoice = false;

            // 鎭㈠闅愯棌鍒?
            if (data.getScores() != null) {
                storyManager.setScores(data.getScores());
            }

            // 閲嶆斁鍦烘櫙涓墍鏈塨g/sprite/bgm鎸囦护鏉ラ噸寤虹姸鎬侊紙浠庣储寮?鍒板瓨妗ｄ綅缃級
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
                            history.add(cmd.who + "锛? + cmd.text);
                        }
                        break;
                    default:
                        break;
                }
            }

            // 鍥為€€涓€姝ワ細瀛樻。鏃剁殑绱㈠紩鎸囧悜涓嬩竴鏉℃寚浠わ紝鍥為€€鍒板凡鏄剧ず鐨勬寚浠?
            storyManager.setCurrentCommandIndex(savedCmdIndex);
            if (savedCmdIndex > 0) {
                storyManager.setCurrentCommandIndex(savedCmdIndex - 1);
            }
            updateDisplay();
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "璇绘。澶辫触锛?, "閿欒", JOptionPane.ERROR_MESSAGE);
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

    // ---------- 杩斿洖鏍囬 ----------
    private void confirmReturn() {
        // 浣跨敤瑕嗙洊灞傛浛浠OptionPane锛岄伩鍏嶅叏灞忎笅寮圭獥瀵艰嚧绐楀彛鏈€灏忓寲
        JLayeredPane layeredPane = parentFrame.getLayeredPane();

        // 鍗婇€忔槑鏆楀箷 + 灞呬腑纭鎸夐挳
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

        // 灞呬腑绔栫洿鎺掑垪鐨勭‘璁ゆ寜閽?
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        JLabel title = new JLabel((isEnding ? "娓告垙缁撴潫锛屼綘纭畾杩斿洖鍚楋紵" : "杩斿洖鏍囬椤甸潰锛屾湭淇濆瓨鐨勮繘搴︿細涓㈠け锛屼綘纭畾杩斿洖鍚楋紵"));
        title.setFont(buttonFont.deriveFont(20f));
        title.setForeground(new Color(255, 255, 255, 200));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 30)));

        // "纭畾杩斿洖"鎸夐挳
        JButton confirmBtn = createChoiceButton("纭畾杩斿洖");
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

        // "鍙栨秷"鎸夐挳
        JButton cancelBtn = createChoiceButton("鍙栨秷");
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
public List<?> getDialogues() { return null; }
}






