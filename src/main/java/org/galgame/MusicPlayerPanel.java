package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class MusicPlayerPanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private MusicPlayer musicPlayer;

    private DefaultListModel<String> listModel;
    private JList<String> musicList;
    private List<File> musicFiles = new ArrayList<>();

    private JButton playBtn, pauseBtn, returnBtn;

    private Font titleFont;
    private Font buttonFont;

    private BufferedImage recordImage;
    private BufferedImage bgImage;          // 背景图
    private double angle = 0.0;
    private Timer rotationTimer;
    private boolean isRotating = false;

    private static final String MUSIC_DIR = "player_music";

    public MusicPlayerPanel(JFrame frame, MainMenuPanel mainMenu, Font titleFont, Font buttonFont) {
        this.parentFrame = frame;
        this.mainMenuPanel = mainMenu;
        this.titleFont = titleFont;
        this.buttonFont = buttonFont;

        this.musicPlayer = new MusicPlayer();
        this.musicPlayer.setLooping(false);

        setLayout(new BorderLayout());
        setOpaque(false);

        // 加载背景图
        try {
            URL bgUrl = getClass().getResource("/images/player_bg.jpg");
            if (bgUrl != null) {
                bgImage = ImageIO.read(bgUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 标题
        JLabel titleLabel = new JLabel("🎵 音乐鉴赏");
        titleLabel.setFont(titleFont.deriveFont(48f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 中间区域
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        // --- 左侧毛玻璃列表 ---
        JPanel glassListPanel = new JPanel(new BorderLayout()) {
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
        glassListPanel.setOpaque(false);
        glassListPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        listModel = new DefaultListModel<>();
        scanMusicFiles();
        musicList = new JList<>(listModel);
        musicList.setFont(buttonFont.deriveFont(22f));
        musicList.setForeground(Color.WHITE);
        musicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        musicList.setOpaque(false);
        musicList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(false);
                label.setFont(buttonFont.deriveFont(22f));
                label.setForeground(isSelected ? Color.YELLOW : Color.WHITE);
                return label;
            }
        });
        musicList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = musicList.getSelectedIndex();
                    if (idx != -1 && idx < musicFiles.size()) {
                        File f = musicFiles.get(idx);
                        if (f != null && f.exists()) {
                            playMusic(f);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(musicList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        glassListPanel.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(glassListPanel);

        // --- 右侧唱片 ---
        try {
            URL imgUrl = getClass().getResource("/images/record.png");
            if (imgUrl != null) {
                recordImage = ImageIO.read(imgUrl);
            } else {
                createDefaultRecordImage();
            }
        } catch (Exception e) {
            createDefaultRecordImage();
        }

        JPanel recordPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (recordImage == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int imgW = recordImage.getWidth();
                int imgH = recordImage.getHeight();
                double scale = Math.min((double) w / imgW, (double) h / imgH);
                int drawW = (int) (imgW * scale);
                int drawH = (int) (imgH * scale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;
                AffineTransform at = new AffineTransform();
                at.translate(x + drawW/2.0, y + drawH/2.0);
                at.rotate(angle);
                at.translate(-drawW/2.0, -drawH/2.0);
                g2.drawImage(recordImage, at, null);
                g2.dispose();
            }
        };
        recordPanel.setOpaque(false);
        recordPanel.setPreferredSize(new Dimension(300, 300));

        centerPanel.add(recordPanel);

        add(centerPanel, BorderLayout.CENTER);

        // 底部控制栏
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        controlPanel.setOpaque(false);

        playBtn = createGlassButton("▶ 播放");
        pauseBtn = createGlassButton("⏸ 暂停");
        returnBtn = createGlassButton("↩ 返回");

        playBtn.addActionListener(e -> {
            int idx = musicList.getSelectedIndex();
            if (idx != -1 && idx < musicFiles.size()) {
                File f = musicFiles.get(idx);
                if (f != null && f.exists()) {
                    playMusic(f);
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一首歌", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        pauseBtn.addActionListener(e -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                pauseBtn.setText("▶ 继续");
                stopRotation();
            } else {
                musicPlayer.resume();
                pauseBtn.setText("⏸ 暂停");
                startRotation();
            }
        });

        returnBtn.addActionListener(e -> {
            musicPlayer.stopImmediately();
            stopRotation();
            mainMenuPanel.showMainMenu();
        });

        controlPanel.add(playBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(returnBtn);

        add(controlPanel, BorderLayout.SOUTH);

        if (buttonFont != null) {
            playBtn.setFont(buttonFont.deriveFont(20f));
            pauseBtn.setFont(buttonFont.deriveFont(20f));
            returnBtn.setFont(buttonFont.deriveFont(20f));
        }

        rotationTimer = new Timer(30, e -> {
            angle += 0.05;
            if (angle > 2 * Math.PI) angle -= 2 * Math.PI;
            repaint();
        });

        setBackground(new Color(30, 30, 60));
    }

    private void createDefaultRecordImage() {
        recordImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = recordImage.createGraphics();
        g2.setColor(Color.DARK_GRAY);
        g2.fillOval(0, 0, 300, 300);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("唱片", 120, 160);
        g2.dispose();
    }

    private void playMusic(File file) {
        mainMenuPanel.stopMenuMusic();
        musicPlayer.play(file);
        if (musicPlayer.getCurrentFile() == null) {
            JOptionPane.showMessageDialog(this, "该文件格式不支持，请转换为 16-bit PCM WAV。", "播放失败", JOptionPane.ERROR_MESSAGE);
            stopRotation();
            pauseBtn.setText("⏸ 暂停");
            return;
        }
        startRotation();
        pauseBtn.setText("⏸ 暂停");
    }

    private void startRotation() {
        if (!isRotating) {
            isRotating = true;
            rotationTimer.start();
        }
    }

    private void stopRotation() {
        if (isRotating) {
            isRotating = false;
            rotationTimer.stop();
        }
    }

    private void scanMusicFiles() {
        musicFiles.clear();
        listModel.clear();

        List<File> candidateDirs = new ArrayList<>();

        URL dirUrl = getClass().getResource("/" + MUSIC_DIR);
        if (dirUrl != null) {
            File dir = new File(dirUrl.getFile());
            if (dir.exists() && dir.isDirectory()) {
                candidateDirs.add(dir);
            }
        }

        File rootMusic = new File(MUSIC_DIR);
        if (rootMusic.exists() && rootMusic.isDirectory()) {
            candidateDirs.add(rootMusic);
        }

        File resourcesMusic = new File("src/main/resources/" + MUSIC_DIR);
        if (resourcesMusic.exists() && resourcesMusic.isDirectory()) {
            candidateDirs.add(resourcesMusic);
        }

        for (File dir : candidateDirs) {
            File[] files = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".flac");
            });
            if (files != null) {
                for (File f : files) {
                    boolean exists = false;
                    for (File existing : musicFiles) {
                        if (existing.getName().equals(f.getName())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        musicFiles.add(f);
                        listModel.addElement(f.getName());
                    }
                }
            }
        }

        if (listModel.isEmpty()) {
            listModel.addElement("请将音乐文件放入:");
            listModel.addElement("1. src/main/resources/player_music/");
            listModel.addElement("2. 项目根目录/player_music/");
            musicFiles.add(null);
        }
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                    getWidth(), getHeight(), new Color(10, 10, 30));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }
}