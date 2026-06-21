package org.galgame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 音乐鉴赏面板 —— 完整音乐播放器
 * 扫描 player_music 文件夹，播放 .wav 文件，
 * 支持专辑封面、播放列表、进度条、音量控制、播放模式切换。
 */
public class MusicPlayerPanel extends JPanel {

    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private MusicPlayer musicPlayer;
    private Font titleFont, buttonFont;
    private BufferedImage bgImage, defaultCoverImage;
    private java.util.List<File> musicFiles = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isPlaying = false;

    private enum PlayMode { SEQUENTIAL, REPEAT_ONE, SHUFFLE }
    private PlayMode playMode = PlayMode.SEQUENTIAL;

    private JLabel albumArtLabel, songTitleLabel, timeLabel;
    private JSlider progressSlider, volumeSlider;
    private boolean isSeeking = false;
    private JButton prevBtn, playBtn, nextBtn, stopBtn, refreshBtn, returnBtn;
    private JToggleButton seqBtn, repeatBtn, shuffleBtn;
    private JPanel playlistPanel;
    private JList<String> playlistList;
    private DefaultListModel<String> playlistModel;
    private boolean isPlaylistVisible = false;
    private Timer progressTimer;
    private static final String MUSIC_DIR = "player_music";

    public MusicPlayerPanel(JFrame frame, MainMenuPanel mainMenu, Font titleFont, Font buttonFont) {
        this.parentFrame = frame;
        this.mainMenuPanel = mainMenu;
        this.titleFont = titleFont;
        this.buttonFont = buttonFont;
        this.musicPlayer = new MusicPlayer();
        this.musicPlayer.setLooping(false);
        setLayout(null);
        setOpaque(false);
        try { InputStream s = getClass().getResourceAsStream("/images/player_bg.jpg"); if (s != null) bgImage = ImageIO.read(s); } catch (Exception e) {}
        try { InputStream s = getClass().getResourceAsStream("/images/record.png"); if (s != null) defaultCoverImage = ImageIO.read(s); } catch (Exception e) {}
        buildUI(); scanMusicFiles(); updateAlbumArt();
        progressTimer = new Timer(200, e -> updateProgress()); progressTimer.start();
        new Timer(300, e -> checkPlaybackFinished()).start();
    }

    private void buildUI() {
        returnBtn = createReturnButton();
        returnBtn.addActionListener(e -> mainMenuPanel.showPreviousPanel());
        returnBtn.setBounds(0, 0, 50, 50);
        add(returnBtn);

        JLabel titleLabel = new JLabel("\u266B \u97F3\u4E50\u9274\u8D4F");
        titleLabel.setFont(titleFont.deriveFont(42f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel);

        // 专辑封面
        albumArtLabel = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                BufferedImage cover = getCurrentAlbumArt();
                if (cover != null) {
                    int m = 15;
                    g2.drawImage(cover, m, m, getWidth()-2*m, getHeight()-2*m, this);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        albumArtLabel.setOpaque(false);
        albumArtLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        albumArtLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { togglePlaylist(); }
        });
        add(albumArtLabel);

        // 歌曲名
        songTitleLabel = new JLabel("\u672A\u5728\u64AD\u653E");
        songTitleLabel.setFont(buttonFont.deriveFont(22f));
        songTitleLabel.setForeground(Color.WHITE);
        songTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitleLabel);

        // 时间标签
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setFont(buttonFont.deriveFont(16f));
        timeLabel.setForeground(new Color(255, 255, 255, 200));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(timeLabel);

        // 进度条
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setOpaque(false);
        progressSlider.setFocusable(false);
        progressSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { isSeeking = true; }
            public void mouseReleased(MouseEvent e) {
                isSeeking = false;
                seekTo(progressSlider.getValue() / 1000.0);
            }
        });
        add(progressSlider);
        // 控制按钮
        prevBtn = createCtrlBtn("\u23EE", "上一首");
        prevBtn.addActionListener(e -> playPrev());
        add(prevBtn);

        playBtn = createCtrlBtn("\u25B6", "播放/暂停");
        playBtn.addActionListener(e -> togglePlayPause());
        add(playBtn);

        nextBtn = createCtrlBtn("\u23ED", "下一首");
        nextBtn.addActionListener(e -> playNext());
        add(nextBtn);

        stopBtn = createCtrlBtn("\u23F9", "停止");
        stopBtn.addActionListener(e -> stopPlayback());
        add(stopBtn);

        refreshBtn = createCtrlBtn("\uD83D\uDD04", "刷新列表");
        refreshBtn.addActionListener(e -> { scanMusicFiles(); updateAlbumArt(); songTitleLabel.setText("已刷新"); });
        add(refreshBtn);

        // 音量滑块
        JLabel volLabel = new JLabel("\uD83D\uDD0A");
        volLabel.setFont(buttonFont.deriveFont(18f));
        volLabel.setForeground(Color.WHITE);
        add(volLabel);

        volumeSlider = new JSlider(0, 100, (int)(MusicPlayer.getGlobalVolume() * 100));
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> MusicPlayer.setGlobalVolume(volumeSlider.getValue() / 100.0));
        add(volumeSlider);

        // 播放模式按钮
        ButtonGroup modeGroup = new ButtonGroup();
        seqBtn = createModeBtn("\uD83D\uDD01", "顺序播放", true);
        seqBtn.addActionListener(e -> { if (seqBtn.isSelected()) setPlayMode(PlayMode.SEQUENTIAL); });
        modeGroup.add(seqBtn); add(seqBtn);

        repeatBtn = createModeBtn("\uD83D\uDD02", "单曲循环", false);
        repeatBtn.addActionListener(e -> { if (repeatBtn.isSelected()) setPlayMode(PlayMode.REPEAT_ONE); });
        modeGroup.add(repeatBtn); add(repeatBtn);

        shuffleBtn = createModeBtn("\uD83D\uDD00", "随机播放", false);
        shuffleBtn.addActionListener(e -> { if (shuffleBtn.isSelected()) setPlayMode(PlayMode.SHUFFLE); });
        modeGroup.add(shuffleBtn); add(shuffleBtn);

        buildPlaylistPanel();
    }
    private JButton createCtrlBtn(String text, String tooltip) {
        JButton btn = new JButton(text) {
            private boolean hovered = false, pressed = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); SoundEffects.playHover(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                    public void mousePressed(MouseEvent e) { pressed = true; repaint(); SoundEffects.playClick(); }
                    public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                });
            }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int bw = getWidth(), bh = getHeight();
                g2.setColor(pressed ? new Color(255,255,255,40) : new Color(255,255,255,60));
                g2.fillRoundRect(0, 0, bw, bh, 16, 16);
                g2.setColor(new Color(255,255,255,120));
                g2.drawRoundRect(0, 0, bw-1, bh-1, 16, 16);
                if (hovered) {
                    g2.setColor(new Color(255,255,255,30)); g2.setStroke(new BasicStroke(5f));
                    g2.drawRoundRect(2, 2, bw-5, bh-5, 14, 14);
                    g2.setColor(new Color(255,255,255,70)); g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, bw-3, bh-3, 14, 14);
                }
                g2.setFont(getFont().deriveFont(Font.PLAIN, 20f));
                FontMetrics fm = g2.getFontMetrics();
                int off = pressed ? 1 : 0;
                g2.setColor(hovered ? new Color(255,255,200) : Color.WHITE);
                g2.drawString(getText(), (bw-fm.stringWidth(getText()))/2+off, (bh+fm.getAscent()-fm.getDescent())/2+off);
                g2.dispose();
            }
        };
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false); btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JToggleButton createModeBtn(String text, String tooltip, boolean selected) {
        JToggleButton btn = new JToggleButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); SoundEffects.playHover(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int bw = getWidth(), bh = getHeight();
                if (isSelected()) { g2.setColor(new Color(255,255,255,60)); g2.fillRoundRect(0,0,bw,bh,12,12); }
                g2.setColor(new Color(255,255,255,100)); g2.drawRoundRect(0,0,bw-1,bh-1,12,12);
                if (hovered) { g2.setColor(new Color(255,255,255,40)); g2.setStroke(new BasicStroke(4f)); g2.drawRoundRect(2,2,bw-5,bh-5,10,10); }
                g2.setFont(getFont().deriveFont(Font.PLAIN, 16f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(isSelected() ? new Color(255,255,200) : new Color(255,255,255,180));
                g2.drawString(getText(), (bw-fm.stringWidth(getText()))/2, (bh+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setToolTipText(tooltip); btn.setSelected(selected);
        btn.setFocusPainted(false); btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    private void buildPlaylistPanel() {
        playlistPanel = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 40, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        playlistPanel.setOpaque(false);
        playlistPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        playlistPanel.setVisible(false);

        JLabel plTitle = new JLabel("播放列表");
        plTitle.setFont(buttonFont.deriveFont(20f));
        plTitle.setForeground(Color.WHITE);
        plTitle.setHorizontalAlignment(SwingConstants.CENTER);
        playlistPanel.add(plTitle, BorderLayout.NORTH);

        playlistModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistModel);
        playlistList.setFont(buttonFont.deriveFont(18f));
        playlistList.setForeground(Color.WHITE);
        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistList.setOpaque(false);
        playlistList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                l.setOpaque(false); l.setFont(buttonFont.deriveFont(18f));
                l.setForeground(sel ? Color.YELLOW : (index == currentIndex ? new Color(255,255,200) : Color.WHITE));
                return l;
            }
        });
        playlistList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = playlistList.getSelectedIndex();
                if (idx != -1 && idx < musicFiles.size()) {
                    playAtIndex(idx);
                    playlistPanel.setVisible(false);
                    isPlaylistVisible = false;
                    doLayout(); repaint();
                }
            }
        });
        JScrollPane sp = new JScrollPane(playlistList);
        sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setBorder(null);
        playlistPanel.add(sp, BorderLayout.CENTER);
        add(playlistPanel);
    }
    private void togglePlaylist() {
        isPlaylistVisible = !isPlaylistVisible;
        playlistPanel.setVisible(isPlaylistVisible);
        if (isPlaylistVisible) updatePlaylistModel();
        doLayout(); repaint();
    }

    private void updatePlaylistModel() {
        playlistModel.clear();
        for (File f : musicFiles) playlistModel.addElement(getSongName(f));
    }

    private void playAtIndex(int index) {
        if (index < 0 || index >= musicFiles.size()) return;
        currentIndex = index;
        File f = musicFiles.get(index);
        if (f == null || !f.exists()) return;
        musicPlayer.stopImmediately();
        musicPlayer.setLooping(playMode == PlayMode.REPEAT_ONE);
        musicPlayer.play(f);
        isPlaying = true;
        playBtn.setText("\u23F8");
        songTitleLabel.setText(getSongName(f));
        updateAlbumArt();
        progressSlider.setValue(0);
        playlistList.setSelectedIndex(currentIndex);
        repaint();
    }

    private void togglePlayPause() {
        if (!isPlaying) {
            if (currentIndex >= 0 && currentIndex < musicFiles.size()) {
                File f = musicFiles.get(currentIndex);
                musicPlayer.play(f);
                musicPlayer.setLooping(playMode == PlayMode.REPEAT_ONE);
                isPlaying = true;
            } else if (!musicFiles.isEmpty()) {
                playAtIndex(0);
            }
            playBtn.setText("\u23F8");
        } else {
            musicPlayer.stopImmediately();
            isPlaying = false;
            playBtn.setText("\u25B6");
        }
        repaint();
    }

    private void stopPlayback() {
        musicPlayer.stopImmediately();
        isPlaying = false;
        playBtn.setText("\u25B6");
        progressSlider.setValue(0);
        timeLabel.setText("00:00 / 00:00");
        repaint();
    }

    private void playNext() {
        if (musicFiles.isEmpty()) return;
        int next = currentIndex + 1;
        if (next >= musicFiles.size()) next = 0;
        playAtIndex(next);
    }

    private void playPrev() {
        if (musicFiles.isEmpty()) return;
        int prev = currentIndex - 1;
        if (prev < 0) prev = musicFiles.size() - 1;
        playAtIndex(prev);
    }

    private void setPlayMode(PlayMode mode) {
        this.playMode = mode;
        musicPlayer.setLooping(mode == PlayMode.REPEAT_ONE);
    }
    private void checkPlaybackFinished() {
        if (!isPlaying || currentIndex < 0 || currentIndex >= musicFiles.size()) return;
        if (musicPlayer.isPlaying()) return;
        long fl = musicPlayer.getFrameLength();
        double fp = musicPlayer.getFramePosition();
        if (fl > 0 && fp >= fl - 500) {
            SwingUtilities.invokeLater(() -> {
                if (!musicPlayer.isPlaying()) {
                    switch (playMode) {
                        case SEQUENTIAL:
                            if (currentIndex + 1 < musicFiles.size()) playAtIndex(currentIndex + 1);
                            else { stopPlayback(); songTitleLabel.setText("播放完毕"); }
                            break;
                        case REPEAT_ONE: playAtIndex(currentIndex); break;
                        case SHUFFLE:
                            int r = (int)(Math.random() * musicFiles.size());
                            if (r == currentIndex && musicFiles.size() > 1) r = (r + 1) % musicFiles.size();
                            playAtIndex(r);
                            break;
                    }
                }
            });
        }
    }

    private void updateProgress() {
        if (!isPlaying || isSeeking || currentIndex < 0) return;
        long fl = musicPlayer.getFrameLength();
        if (fl <= 0) return;
        double fp = musicPlayer.getFramePosition();
        double p = fp / fl;
        progressSlider.setValue((int)(p * 1000));
        long totalUs = musicPlayer.getMicrosecondLength();
        timeLabel.setText(formatTime((long)(p * totalUs)) + " / " + formatTime(totalUs));
    }

    private void seekTo(double fraction) {
        if (currentIndex < 0 || currentIndex >= musicFiles.size()) return;
        long fl = musicPlayer.getFrameLength();
        if (fl > 0) musicPlayer.setFramePosition(fraction * fl);
    }

    private String getSongName(File f) {
        String n = f.getName();
        int d = n.lastIndexOf('.');
        return d > 0 ? n.substring(0, d) : n;
    }

    private String formatTime(long us) {
        long s = us / 1000000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
    private void scanMusicFiles() {
        musicFiles.clear(); playlistModel.clear();
        java.util.List<File> dirs = new ArrayList<>();
        dirs.add(new File(MUSIC_DIR));
        try {
            URL u = getClass().getResource("/player_music");
            if (u != null) dirs.add(new File(u.toURI()));
        } catch (Exception ignored) {}
        for (File dir : dirs) {
            File[] fs = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".wav"));
            if (fs != null) {
                for (File f : fs) {
                    boolean dup = false;
                    for (File e : musicFiles) { if (e.getName().equals(f.getName())) { dup = true; break; } }
                    if (!dup) musicFiles.add(f);
                }
            }
        }
        Collections.sort(musicFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File f : musicFiles) playlistModel.addElement(getSongName(f));
        if (musicFiles.isEmpty()) playlistModel.addElement("请将 .wav 文件放入 player_music 目录");
    }

    private BufferedImage getCurrentAlbumArt() {
        if (currentIndex >= 0 && currentIndex < musicFiles.size()) {
            File af = musicFiles.get(currentIndex);
            String bn = getSongName(af);
            File pd = af.getParentFile();
            for (String ext : new String[]{".jpg", ".png"}) {
                File cf = new File(pd, bn + ext);
                if (cf.exists()) { try { return ImageIO.read(cf); } catch (Exception ignored) {} }
            }
        }
        return defaultCoverImage;
    }

    private void updateAlbumArt() { albumArtLabel.repaint(); }
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        returnBtn.setBounds(w - 55, 8, 45, 45);

        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\u266B")) {
                c.setBounds((w - 300) / 2, 10, 300, 50);
            }
        }

        int coverS = Math.min(200, h - 180);
        int cx = 40, cy = 80;
        albumArtLabel.setBounds(cx, cy, coverS, coverS);

        int plW = 300;
        playlistPanel.setBounds(0, 30, plW, h - 60);

        int ix = cx + coverS + 30, iw = w - ix - 40;
        songTitleLabel.setBounds(ix, cy + 10, iw, 30);
        progressSlider.setBounds(ix, cy + 55, iw, 30);
        timeLabel.setBounds(ix, cy + 80, iw, 20);

        int btnS = 48, gap = 15;
        int btnY = cy + 110;
        int bwTotal = btnS * 5 + gap * 4;
        int bx = ix + (iw - bwTotal) / 2;
        prevBtn.setBounds(bx, btnY, btnS, btnS);
        playBtn.setBounds(bx + (btnS+gap), btnY, btnS, btnS);
        nextBtn.setBounds(bx + (btnS+gap)*2, btnY, btnS, btnS);
        stopBtn.setBounds(bx + (btnS+gap)*3, btnY, btnS, btnS);
        refreshBtn.setBounds(bx + (btnS+gap)*4, btnY, btnS, btnS);

        int volY = btnY + btnS + 15;
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\uD83D\uDD0A")) {
                c.setBounds(ix, volY, 30, 25);
            }
        }
        volumeSlider.setBounds(ix + 30, volY, iw - 130, 25);

        int modeX = w - 150;
        seqBtn.setBounds(modeX, volY - 3, 38, 32);
        repeatBtn.setBounds(modeX + 42, volY - 3, 38, 32);
        shuffleBtn.setBounds(modeX + 84, volY - 3, 38, 32);
    }
    private JButton createReturnButton() {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/images/return_icon.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                JButton btn = new JButton(new ImageIcon(scaled)) {
                    private boolean hovered = false;
                    {
                        addMouseListener(new MouseAdapter() {
                            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                        });
                    }
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (hovered) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            int cx = getWidth()/2, cy = getHeight()/2;
                            int r = Math.min(getWidth(), getHeight())/2 - 2;
                            g2.setColor(new Color(255,255,255,30));
                            g2.setStroke(new BasicStroke(8f));
                            g2.drawOval(cx-r, cy-r, r*2, r*2);
                            g2.setColor(new Color(255,255,255,90));
                            g2.setStroke(new BasicStroke(3f));
                            g2.drawOval(cx-r, cy-r, r*2, r*2);
                            g2.dispose();
                        }
                    }
                };
                btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;
            }
        } catch (Exception e) {}
        JButton btn = new JButton("X");
        btn.setFont(buttonFont.deriveFont(18f)); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void stopMusic() {
        musicPlayer.stopImmediately();
        isPlaying = false;
        playBtn.setText("\u25B6");
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (bgImage != null) {
            int pw = getWidth(), ph = getHeight();
            double pr = (double)pw/ph, ir = (double)bgImage.getWidth()/bgImage.getHeight();
            int dw, dh, dx, dy;
            if (pr > ir) { dw = pw; dh = (int)(pw/ir); dx = 0; dy = (ph-dh)/2; }
            else { dh = ph; dw = (int)(ph*ir); dx = (pw-dw)/2; dy = 0; }
            g2.drawImage(bgImage, dx, dy, dw, dh, this);
        } else {
            g2.setPaint(new GradientPaint(0,0,new Color(30,30,60), getWidth(),getHeight(),new Color(10,10,30)));
            g2.fillRect(0,0,getWidth(),getHeight());
        }
        g2.dispose();
    }
}