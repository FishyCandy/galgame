package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * 游戏设置页面
 */
public class SettingsPanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private Font titleFont;
    private Font buttonFont;
    private BufferedImage bgImage;

    private JSlider volumeSlider;
    private JSlider autoSpeedSlider;
    private JSlider dialogAlphaSlider;
    private JRadioButton windowedRadio;
    private JRadioButton fullscreenRadio;
    private JLabel volumeValueLabel;
    private JLabel autoSpeedValueLabel;
    private JLabel dialogAlphaValueLabel;

    private static int globalVolume = 80;
    private static int autoPlayDelayMs = 2000;
    private static float dialogAlpha = 0.85f;

    public SettingsPanel(JFrame frame, MainMenuPanel mainMenu, Font titleFont, Font buttonFont) {
        this.parentFrame = frame;
        this.mainMenuPanel = mainMenu;
        this.titleFont = titleFont;
        this.buttonFont = buttonFont;

        setLayout(new GridBagLayout());
        setOpaque(false);

        try {
            java.io.InputStream imgStream = getClass().getResourceAsStream("/menu_bg.jpg");
            if (imgStream != null) {
                bgImage = ImageIO.read(imgStream);
            }
        } catch (Exception e) {
            bgImage = null;
        }

        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 30, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel("\u2699  游 戏 设 置");
        titleLabel.setFont(titleFont.deriveFont(40f));
        titleLabel.setForeground(Color.WHITE);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("X");
        closeBtn.setFont(buttonFont.deriveFont(22f));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setOpaque(false);
        closeBtn.addActionListener(e -> mainMenuPanel.showPreviousPanel());
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 100, 100));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
            }
        });

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);
        titleRow.add(closePanel, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 25, 10);
        mainPanel.add(titleRow, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 5, 5);
        JLabel displayLabel = createSettingLabel("\u6E38\u620F\u663E\u793A");
        mainPanel.add(displayLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 5, 10);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        radioPanel.setOpaque(false);
        windowedRadio = createRadioButton("\u7A97\u53E3");
        fullscreenRadio = createRadioButton("\u5168\u5C4F");
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(windowedRadio);
        displayGroup.add(fullscreenRadio);
        windowedRadio.setSelected(true);

        windowedRadio.addActionListener(e -> setFullScreen(false));
        fullscreenRadio.addActionListener(e -> setFullScreen(true));

        radioPanel.add(windowedRadio);
        radioPanel.add(fullscreenRadio);
        mainPanel.add(radioPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 10, 5, 5);
        JLabel volumeLabel = createSettingLabel("\u4E3B\u97F3\u91CF");
        mainPanel.add(volumeLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 5, 10);
        JPanel volumeRow = new JPanel(new BorderLayout(10, 0));
        volumeRow.setOpaque(false);
        volumeSlider = new JSlider(0, 100, globalVolume);
        styleSlider(volumeSlider);
        volumeValueLabel = new JLabel(globalVolume + "%");
        volumeValueLabel.setFont(buttonFont.deriveFont(18f));
        volumeValueLabel.setForeground(Color.WHITE);
        volumeValueLabel.setPreferredSize(new Dimension(50, 25));
        volumeSlider.addChangeListener(e -> {
            int val = volumeSlider.getValue();
            globalVolume = val;
            volumeValueLabel.setText(val + "%");
            MusicPlayer.setGlobalVolume(val / 100.0);
        });
        volumeRow.add(volumeSlider, BorderLayout.CENTER);
        volumeRow.add(volumeValueLabel, BorderLayout.EAST);
        mainPanel.add(volumeRow, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(10, 10, 5, 5);
        JLabel autoSpeedLabel = createSettingLabel("\u81EA\u52A8\u64AD\u653E\u901F\u5EA6");
        mainPanel.add(autoSpeedLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 5, 10);
        JPanel autoSpeedRow = new JPanel(new BorderLayout(10, 0));
        autoSpeedRow.setOpaque(false);
        int initialAutoDelay = (autoPlayDelayMs - 1000) / 100;
        autoSpeedSlider = new JSlider(0, 90, Math.max(0, Math.min(90, initialAutoDelay)));
        styleSlider(autoSpeedSlider);
        int seconds = autoPlayDelayMs / 1000;
        autoSpeedValueLabel = new JLabel(seconds + "\u79D2");
        autoSpeedValueLabel.setFont(buttonFont.deriveFont(18f));
        autoSpeedValueLabel.setForeground(Color.WHITE);
        autoSpeedValueLabel.setPreferredSize(new Dimension(50, 25));
        autoSpeedSlider.addChangeListener(e -> {
            int val = autoSpeedSlider.getValue();
            autoPlayDelayMs = 1000 + val * 100;
            int sec = autoPlayDelayMs / 1000;
            autoSpeedValueLabel.setText(sec + "\u79D2");
            GamePanel.setAutoPlayDelay(autoPlayDelayMs);
        });
        autoSpeedRow.add(autoSpeedSlider, BorderLayout.CENTER);
        autoSpeedRow.add(autoSpeedValueLabel, BorderLayout.EAST);
        mainPanel.add(autoSpeedRow, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 10, 5, 5);
        JLabel alphaLabel = createSettingLabel("\u5BF9\u8BDD\u6846\u900F\u660E\u5EA6");
        mainPanel.add(alphaLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 5, 10);
        JPanel alphaRow = new JPanel(new BorderLayout(10, 0));
        alphaRow.setOpaque(false);
        int initialAlpha = (int) (dialogAlpha * 100);
        dialogAlphaSlider = new JSlider(10, 100, initialAlpha);
        styleSlider(dialogAlphaSlider);
        dialogAlphaValueLabel = new JLabel(initialAlpha + "%");
        dialogAlphaValueLabel.setFont(buttonFont.deriveFont(18f));
        dialogAlphaValueLabel.setForeground(Color.WHITE);
        dialogAlphaValueLabel.setPreferredSize(new Dimension(50, 25));
        dialogAlphaSlider.addChangeListener(e -> {
            int val = dialogAlphaSlider.getValue();
            dialogAlpha = val / 100.0f;
            dialogAlphaValueLabel.setText(val + "%");
            GamePanel.setDialogAlpha(dialogAlpha);
        });
        alphaRow.add(dialogAlphaSlider, BorderLayout.CENTER);
        alphaRow.add(dialogAlphaValueLabel, BorderLayout.EAST);
        mainPanel.add(alphaRow, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, 10));
        mainPanel.add(spacer, gbc);

        GridBagConstraints frameGbc = new GridBagConstraints();
        add(mainPanel, frameGbc);
    }

    private JLabel createSettingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(buttonFont.deriveFont(22f));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JRadioButton createRadioButton(String text) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(buttonFont.deriveFont(20f));
        rb.setForeground(Color.WHITE);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return rb;
    }

    private void styleSlider(JSlider slider) {
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
    }

    private void setFullScreen(boolean fullscreen) {
        if (fullscreen) {
            parentFrame.dispose();
            parentFrame.setUndecorated(true);
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            parentFrame.setVisible(true);
            gd.setFullScreenWindow(parentFrame);
        } else {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gd.setFullScreenWindow(null);
            parentFrame.dispose();
            parentFrame.setUndecorated(false);
            parentFrame.setSize(1024, 640);
            parentFrame.setLocationRelativeTo(null);
            parentFrame.setVisible(true);
        }
    }

    public static int getGlobalVolume() { return globalVolume; }
    public static int getAutoPlayDelayMs() { return autoPlayDelayMs; }
    public static float getDialogAlpha() { return dialogAlpha; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                    getWidth(), getHeight(), new Color(10, 10, 30));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }
}
