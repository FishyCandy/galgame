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

public class GamePanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private BufferedImage bgImage;

    private StoryManager storyManager;
    private boolean waitingForChoice = false;

    private JLabel imageLabel;
    private JLabel characterLabel;
    private JTextArea lineArea;
    private JButton autoPlayBtn, logBtn;
    private JPanel dialogPanel;
    private Timer autoTimer;
    private boolean isAutoPlaying = false;
    private List<String> history = new ArrayList<>();

    private Color currentTextColor = Color.WHITE;
    private Font dialogFont;
    private MusicPlayer musicPlayer;

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

        try {
            bgImage = ImageIO.read(getClass().getResourceAsStream("/game_bg.jpg"));
        } catch (Exception e) {
            bgImage = null;
        }

        storyManager = new StoryManager();

        createImagePanel();
        createDialogPanel();
        createControlPanel();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == GamePanel.this && !waitingForChoice) {
                    nextCommand();
                }
            }
        });

        autoTimer = new Timer(2000, e -> nextCommand());
        updateDisplay();
    }

    // ---------- UI创建（与之前相同） ----------
    private void createImagePanel() {
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 300));
        imageLabel.setOpaque(false);
        setImagePlaceholder();
        add(imageLabel, BorderLayout.CENTER);
    }

    private void setImagePlaceholder() {
        imageLabel.setIcon(createPlaceholderIcon("差分图", 400, 300));
    }

    private ImageIcon createPlaceholderIcon(String text, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(0, 0, w, h, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("楷体", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
        return new ImageIcon(img);
    }

    private void loadImage(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                java.net.URL imgUrl = getClass().getResource("/" + path);
                if (imgUrl != null) {
                    ImageIcon icon = new ImageIcon(imgUrl);
                    Image img = icon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(img));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String charName = characterLabel.getText();
        if (charName == null || charName.isEmpty()) charName = "未知";
        imageLabel.setIcon(createPlaceholderIcon(charName, 400, 300));
    }

    private void createDialogPanel() {
        dialogPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 30, 30);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(2, 2, getWidth()-5, getHeight()-5, 30, 30);
                g2.dispose();
                super.paintComponent(g);
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
        lineArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!waitingForChoice) nextCommand();
            }
        });

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(characterLabel, BorderLayout.NORTH);
        textPanel.add(lineArea, BorderLayout.CENTER);
        dialogPanel.add(textPanel, BorderLayout.CENTER);

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
                g2.setColor(Color.WHITE);
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
    }

    // ---------- 核心游戏逻辑 ----------
    public void startGame() {
        history.clear();
        storyManager = new StoryManager();
        waitingForChoice = false;
        currentTextColor = Color.WHITE;
        updateDisplay();
    }

    public void resetGame() {
        if (autoTimer != null && autoTimer.isRunning()) {
            autoTimer.stop();
            isAutoPlaying = false;
            autoPlayBtn.setText("Auto");
        }
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

    public void updateDisplay() {
        if (storyManager.isEnd()) {
            JOptionPane.showMessageDialog(this, "故事已结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
            mainMenuPanel.showMainMenu();
            return;
        }

        if (waitingForChoice) return;

        StoryData.CommandData cmd = storyManager.nextCommand();
        if (cmd == null) {
            JOptionPane.showMessageDialog(this, "故事已结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
            mainMenuPanel.showMainMenu();
            return;
        }

        switch (cmd.type) {
            case "show":
                characterLabel.setText("");
                lineArea.setText("");
                loadImage(cmd.image);
                revalidate();
                repaint();
                break;

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

                if (cmd.image != null) loadImage(cmd.image);
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

            case "end":
                JOptionPane.showMessageDialog(this, "故事结束。", "提示", JOptionPane.INFORMATION_MESSAGE);
                mainMenuPanel.showMainMenu();
                break;

            default:
                break;
        }
        revalidate();
        repaint();
    }

    private void showChoices(List<StoryData.ChoiceData> choiceDataList) {
        List<ChoiceOption> options = new ArrayList<>();
        for (StoryData.ChoiceData cd : choiceDataList) {
            options.add(new ChoiceOption(cd.text, cd.target));
        }
        ChoiceDialog dialog = new ChoiceDialog(parentFrame, options);
        dialog.setVisible(true);
        String target = dialog.getSelectedTarget();
        if (target != null) {
            storyManager.jumpToScene(target);
            waitingForChoice = false;
            updateDisplay();
        } else {
            waitingForChoice = false;
            mainMenuPanel.showMainMenu();
        }
    }

    private void nextCommand() {
        if (waitingForChoice) return;
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
            autoTimer.start();
            isAutoPlaying = true;
            autoPlayBtn.setText("Stop");
        }
    }

    private void showLog() {
        JTextArea area = new JTextArea();
        for (String s : history) {
            area.append(s + "\n");
        }
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "台词回顾", JOptionPane.PLAIN_MESSAGE);
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
            SaveData data = new SaveData(0, null, thumbBytes, sceneId, cmdIndex);
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
            storyManager.setCurrentCommandIndex(data.getCurrentCommandIndex());
            history.clear();
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
            mainMenuPanel.showMainMenu();
        }
    }

    public List<?> getDialogues() { return null; }
}