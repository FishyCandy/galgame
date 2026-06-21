package org.galgame;

import javax.imageio.ImageIO;
import com.adonax.audiocue.AudioCue;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayerPanel extends JPanel {
    private JFrame parentFrame;
    private MainMenuPanel mainMenuPanel;
    private MusicPlayer musicPlayer;
    private Font titleFont, buttonFont;
    private BufferedImage bgImage, defaultCoverImage;
    private List<MusicFileInfo> musicList = new ArrayList<>();
    private int currentIndex = -1;
    private Timer progressTimer, playlistAnimTimer;
    
    // UI 组件
    private JLabel albumArtLabel, songTitleLabel, timeCurrentLabel, timeTotalLabel;
    private JButton prevBtn, playPauseBtn, nextBtn, stopBtn;
    private JButton seqBtn, singleLoopBtn, randomBtn;
    private JSlider volumeSlider, progressSlider;
    private JPanel playlistPanel;
    private JScrollPane playlistScroll;
    private JPanel playlistContent;
    private boolean playlistVisible = false;
    private float playlistSlideX = 0f; // 0=隐藏, 1=完全显示
    private int playlistWidth = 280;
    
    // 播放模式
    private enum PlayMode { SEQUENTIAL, SINGLE_LOOP, RANDOM }
    private PlayMode playMode = PlayMode.SEQUENTIAL;
    
    // 进度条拖拽中
    private boolean progressDragging = false;

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
        
        scanMusicFiles();
        createUI();
        ;
        
        // 进度更新定时器
        progressTimer = new Timer(200, e -> updateProgress());
        progressTimer.start();
    }
    
    // ---- 音乐文件扫描 ----
    private void scanMusicFiles() {
        musicList.clear();
        File dir = new File("player_music");
        if (!dir.exists() || !dir.isDirectory()) {
            try {
                URL u = getClass().getResource("/player_music");
                if (u != null) dir = new File(u.toURI());
            } catch (Exception ignored) {}
        }
        File[] wavs = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".wav"));
        if (wavs != null) {
            for (File wav : wavs) {
                // 匹配封面图片
                String baseName = wav.getName();
                baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                File cover = null;
                for (String ext : new String[]{".jpg", ".png", ".jpeg"}) {
                    File f = new File(dir, baseName + ext);
                    if (f.exists()) { cover = f; break; }
                }
                musicList.add(new MusicFileInfo(wav, cover));
            }
        }
        Collections.sort(musicList, (a, b) -> a.wavFile.getName().compareToIgnoreCase(b.wavFile.getName()));
    }
    
    // ---- 创建 UI ----
    private void createUI() {
        // 标题
        JLabel titleLabel = new JLabel("\u266B \u97F3\u4E50\u9274\u8D4F", SwingConstants.CENTER);
        titleLabel.setFont(titleFont.deriveFont(48f));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel);
        
        // 返回按钮 X
        JButton returnBtn = new JButton("X");
        returnBtn.setFont(buttonFont.deriveFont(Font.BOLD, 22f));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.setContentAreaFilled(false);
        returnBtn.setBorderPainted(false);
        returnBtn.setFocusPainted(false);
        returnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnBtn.addActionListener(e -> {
            musicPlayer.stopImmediately();
            mainMenuPanel.showPreviousPanel();
        });
        add(returnBtn);
        
        // 专辑封面（可点击切换播放列表）
        albumArtLabel = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 毛玻璃外框
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                
                BufferedImage cover = getCurrentCover();
                if (cover != null) {
                    int m = 12;
                    g2.drawImage(cover, m, m, getWidth()-2*m, getHeight()-2*m, this);
                } else if (defaultCoverImage != null) {
                    int m = 12;
                    g2.drawImage(defaultCoverImage, m, m, getWidth()-2*m, getHeight()-2*m, this);
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
        
        // 歌曲标题
        songTitleLabel = new JLabel(musicList.isEmpty() ? "\u6682\u65E0\u97F3\u4E50" : "\u70B9\u51FB\u64AD\u653E", SwingConstants.CENTER);
        songTitleLabel.setFont(buttonFont.deriveFont(26f));
        songTitleLabel.setForeground(new Color(255, 255, 255, 220));
        add(songTitleLabel);
        
        // 时间标签
        timeCurrentLabel = new JLabel("00:00");
        timeCurrentLabel.setFont(buttonFont.deriveFont(14f));
        timeCurrentLabel.setForeground(new Color(255, 255, 255, 180));
        add(timeCurrentLabel);
        
        timeTotalLabel = new JLabel("00:00");
        timeTotalLabel.setFont(buttonFont.deriveFont(14f));
        timeTotalLabel.setForeground(new Color(255, 255, 255, 180));
        add(timeTotalLabel);
        
        // 进度条
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setOpaque(false);
        progressSlider.setFocusable(false);
        progressSlider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { progressDragging = true; }
            public void mouseReleased(MouseEvent e) {
                progressDragging = false;
                if (musicPlayer.isPlaying() && currentIndex >= 0) {
                    double fraction = progressSlider.getValue() / 1000.0;
                    double framePos = musicPlayer.getFrameLength() * fraction;
                    musicPlayer.setFramePosition(framePos);
                }
            }
        });
        add(progressSlider);
        
        // 控制按钮
        prevBtn = createControlButton("\u23EE"); // ⏮
        prevBtn.addActionListener(e -> playPrevious());
        add(prevBtn);
        
        playPauseBtn = createControlButton("\u25B6"); // ▶
        playPauseBtn.addActionListener(e -> togglePlayPause());
        add(playPauseBtn);
        
        nextBtn = createControlButton("\u23ED"); // ⏭
        nextBtn.addActionListener(e -> playNext());
        add(nextBtn);
        
        stopBtn = createControlButton("\u23F9"); // ⏹
        stopBtn.addActionListener(e -> stopPlayback());
        add(stopBtn);
        
        // 音量滑块
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> {
            double vol = volumeSlider.getValue() / 100.0;
            MusicPlayer.setGlobalVolume(vol);
        });
        add(volumeSlider);
        
        JLabel volLabel = new JLabel("\uD83D\uDD0A");
        volLabel.setFont(buttonFont.deriveFont(16f));
        volLabel.setForeground(new Color(255, 255, 255, 180));
        add(volLabel);
        
        // 播放模式按钮
        seqBtn = createModeButton("\uD83D\uDD01"); // 顺序
        seqBtn.setToolTipText("\u987A\u5E8F\u64AD\u653E");
        seqBtn.addActionListener(e -> setPlayMode(PlayMode.SEQUENTIAL));
        add(seqBtn);
        
        singleLoopBtn = createModeButton("\uD83D\uDD02"); // 单曲循环
        singleLoopBtn.setToolTipText("\u5355\u66F2\u5FAA\u73AF");
        singleLoopBtn.addActionListener(e -> setPlayMode(PlayMode.SINGLE_LOOP));
        add(singleLoopBtn);
        
        randomBtn = createModeButton("\uD83D\uDD00"); // 随机
        randomBtn.setToolTipText("\u968F\u673A\u64AD\u653E");
        randomBtn.addActionListener(e -> setPlayMode(PlayMode.RANDOM));
        add(randomBtn);
        
        updateModeButtons();
        
        // 播放列表面板
        createPlaylistPanel();
    }
    
    private JButton createControlButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(buttonFont.deriveFont(22f));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
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
    
    private JButton createModeButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(buttonFont.deriveFont(18f));
        btn.setForeground(new Color(255, 255, 255, 120));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    // ---- 播放列表 ----
    private void createPlaylistPanel() {
        playlistPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 40, 230));
                g2.fillRoundRect(5, 5, getWidth()-10, getHeight()-10, 16, 16);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(5, 5, getWidth()-11, getHeight()-11, 16, 16);
                g2.dispose();
            }
        };
        playlistPanel.setLayout(new BorderLayout());
        playlistPanel.setOpaque(false);
        
        JLabel listTitle = new JLabel("  \u64AD\u653E\u5217\u8868", SwingConstants.LEFT);
        listTitle.setFont(buttonFont.deriveFont(18f));
        listTitle.setForeground(new Color(255, 255, 255, 200));
        listTitle.setPreferredSize(new Dimension(playlistWidth, 35));
        playlistPanel.add(listTitle, BorderLayout.NORTH);
        
        playlistContent = new JPanel();
        playlistContent.setLayout(new BoxLayout(playlistContent, BoxLayout.Y_AXIS));
        playlistContent.setOpaque(false);
        
        playlistScroll = new JScrollPane(playlistContent);
        playlistScroll.setOpaque(false);
        playlistScroll.getViewport().setOpaque(false);
        playlistScroll.setBorder(null);
        playlistScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        playlistPanel.add(playlistScroll, BorderLayout.CENTER);
        
        playlistPanel.setVisible(false);
        add(playlistPanel);
        
        refreshPlaylistItems();
    }
    
    private void refreshPlaylistItems() {
        playlistContent.removeAll();
        for (int i = 0; i < musicList.size(); i++) {
            MusicFileInfo info = musicList.get(i);
            int idx = i;
            JPanel item = new JPanel(new BorderLayout(10, 0));
            item.setOpaque(false);
            item.setMaximumSize(new Dimension(playlistWidth - 20, 40));
            item.setPreferredSize(new Dimension(playlistWidth - 20, 40));
            item.setMinimumSize(new Dimension(playlistWidth - 20, 40));
            
            String songName = info.getDisplayName();
            JLabel nameLabel = new JLabel((idx + 1) + ". " + songName);
            nameLabel.setFont(buttonFont.deriveFont(14f));
            nameLabel.setForeground(idx == currentIndex ? new Color(255, 255, 150) : new Color(255, 255, 255, 180));
            
            JLabel durLabel = new JLabel(formatTime(getWavDuration(info.wavFile)));
            durLabel.setFont(buttonFont.deriveFont(12f));
            durLabel.setForeground(new Color(255, 255, 255, 140));
            
            item.add(nameLabel, BorderLayout.CENTER);
            item.add(durLabel, BorderLayout.EAST);
            
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { playAtIndex(idx); }
                public void mouseEntered(MouseEvent e) { item.setBackground(new Color(255,255,255,30)); item.setOpaque(true); item.repaint(); }
                public void mouseExited(MouseEvent e) { item.setOpaque(false); item.repaint(); }
            });
            
            playlistContent.add(item);
            playlistContent.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        playlistContent.revalidate();
        playlistContent.repaint();
    }
    
    private void togglePlaylist() {
        playlistVisible = !playlistVisible;
        if (playlistVisible) {
            playlistPanel.setVisible(true);
            // 动画滑出
            startPlaylistAnimation(true);
        } else {
            startPlaylistAnimation(false);
        }
    }
    
    private void startPlaylistAnimation(boolean show) {
        if (playlistAnimTimer != null && playlistAnimTimer.isRunning()) {
            playlistAnimTimer.stop();
        }
        playlistAnimTimer = new Timer(10, e -> {
            if (show) {
                playlistSlideX += 0.08f;
                if (playlistSlideX >= 1f) {
                    playlistSlideX = 1f;
                    playlistAnimTimer.stop();
                }
            } else {
                playlistSlideX -= 0.08f;
                if (playlistSlideX <= 0f) {
                    playlistSlideX = 0f;
                    playlistPanel.setVisible(false);
                    playlistAnimTimer.stop();
                }
            }
            doLayout();
            repaint();
        });
        playlistAnimTimer.start();
    }
    
    // ---- 布局 ----
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        int margin = 30;
        int coverSize = Math.min(200, h - 180);
        
        // 标题
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\u266B"))
                c.setBounds((w - 300) / 2, 10, 300, 50);
            else if (c instanceof JButton && ((JButton)c).getText().equals("X"))
                c.setBounds(w - 55, 8, 45, 45);
        }
        
        int albumX = margin;
        int albumY = 80;
        albumArtLabel.setBounds(albumX, albumY, coverSize, coverSize);
        
        // 歌曲标题
        int infoX = albumX + coverSize + 30;
        int infoW = w - infoX - margin;
        songTitleLabel.setBounds(infoX, albumY + 10, infoW, 35);
        
        // 进度条区域
        int progY = albumY + coverSize - 60;
        timeCurrentLabel.setBounds(infoX, progY, 60, 20);
        timeTotalLabel.setBounds(infoX + infoW - 60, progY, 60, 20);
        progressSlider.setBounds(infoX + 60, progY - 3, infoW - 120, 26);
        
        // 控制按钮
        int btnY = albumY + coverSize + 15;
        int btnSize = 45;
        int totalBtnW = btnSize * 4 + 15 * 3;
        int btnStartX = infoX + (infoW - totalBtnW) / 2;
        prevBtn.setBounds(btnStartX, btnY, btnSize, btnSize);
        playPauseBtn.setBounds(btnStartX + btnSize + 15, btnY, btnSize, btnSize);
        nextBtn.setBounds(btnStartX + (btnSize + 15) * 2, btnY, btnSize, btnSize);
        stopBtn.setBounds(btnStartX + (btnSize + 15) * 3, btnY, btnSize, btnSize);
        
        // 音量
        int volY = btnY + btnSize + 15;
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\uD83D\uDD0A"))
                c.setBounds(infoX, volY + 2, 25, 20);
        }
        volumeSlider.setBounds(infoX + 25, volY - 3, 120, 26);
        
        // 播放模式
        int modeY = volY;
        int modeX = infoX + 160;
        seqBtn.setBounds(modeX, modeY, 35, 30);
        singleLoopBtn.setBounds(modeX + 40, modeY, 35, 30);
        randomBtn.setBounds(modeX + 80, modeY, 35, 30);
        
        // 播放列表
        int plX = albumX + (int)(playlistSlideX * playlistWidth);
        playlistPanel.setBounds(plX, albumY, playlistWidth, coverSize + 80);
    }
    
    // ---- 播放控制 ----
    private void playAtIndex(int index) {
        if (index < 0 || index >= musicList.size()) return;
        currentIndex = index;
        playCurrent();
        refreshPlaylistItems();
        albumArtLabel.repaint();
        if (playlistVisible) togglePlaylist();
    }
    
    private void playCurrent() {
        if (currentIndex < 0 || currentIndex >= musicList.size()) return;
        MusicFileInfo info = musicList.get(currentIndex);
        musicPlayer.play(info.wavFile);
        musicPlayer.setLooping(playMode == PlayMode.SINGLE_LOOP);
        updatePlayPauseButton(true);
        songTitleLabel.setText(info.getDisplayName());
        albumArtLabel.repaint();
    }
    
    private void togglePlayPause() {
        if (currentIndex < 0) {
            if (!musicList.isEmpty()) {
                playAtIndex(0);
            }
            return;
        }
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
            updatePlayPauseButton(false);
        } else {
            musicPlayer.resume();
            // 如果resume不工作，重新播放
            if (!musicPlayer.isPlaying()) {
                playCurrent();
            }
            updatePlayPauseButton(true);
        }
    }
    
    private void playPrevious() {
        if (musicList.isEmpty()) return;
        if (currentIndex < 0) { playAtIndex(0); return; }
        int next = currentIndex - 1;
        if (next < 0) next = musicList.size() - 1;
        playAtIndex(next);
    }
    
    private void playNext() {
        if (musicList.isEmpty()) return;
        if (currentIndex < 0) { playAtIndex(0); return; }
        int next;
        if (playMode == PlayMode.RANDOM) {
            next = (int)(Math.random() * musicList.size());
            if (musicList.size() > 1 && next == currentIndex) {
                next = (next + 1) % musicList.size();
            }
        } else {
            next = currentIndex + 1;
            if (next >= musicList.size()) {
                if (playMode == PlayMode.SEQUENTIAL) {
                    next = 0;
                } else {
                    next = 0; // single loop handled by setLooping
                }
            }
        }
        playAtIndex(next);
    }
    
    private void stopPlayback() {
        musicPlayer.stopImmediately();
        updatePlayPauseButton(false);
        progressSlider.setValue(0);
        timeCurrentLabel.setText("00:00");
    }
    
    private void updatePlayPauseButton(boolean playing) {
        playPauseBtn.setText(playing ? "\u23F8" : "\u25B6"); // ⏸ or ▶
    }
    
    private void setPlayMode(PlayMode mode) {
        this.playMode = mode;
        updateModeButtons();
        musicPlayer.setLooping(mode == PlayMode.SINGLE_LOOP);
    }
    
    private void updateModeButtons() {
        seqBtn.setForeground(playMode == PlayMode.SEQUENTIAL ? Color.WHITE : new Color(255,255,255,120));
        singleLoopBtn.setForeground(playMode == PlayMode.SINGLE_LOOP ? Color.WHITE : new Color(255,255,255,120));
        randomBtn.setForeground(playMode == PlayMode.RANDOM ? Color.WHITE : new Color(255,255,255,120));
    }
    
    // ---- 进度更新 ----
    private void updateProgress() {
        if (progressDragging) return;
        if (musicPlayer.isPlaying()) {
            long posUs = musicPlayer.getMicrosecondPosition();
            long totalUs = musicPlayer.getMicrosecondLength();
            if (totalUs > 0) {
                int val = (int)(posUs * 1000 / totalUs);
                progressSlider.setValue(Math.min(val, 1000));
                timeCurrentLabel.setText(formatTime(posUs));
                timeTotalLabel.setText(formatTime(totalUs));
            }
            // 检查是否播放结束
            if (posUs >= totalUs && totalUs > 0) {
                onTrackEnd();
            }
        }
    }
    
    private void onTrackEnd() {
        if (playMode == PlayMode.SINGLE_LOOP) {
            playCurrent();
        } else {
            playNext();
        }
    }
    
    // ---- 辅助方法 ----
    private BufferedImage getCurrentCover() {
        if (currentIndex >= 0 && currentIndex < musicList.size()) {
            MusicFileInfo info = musicList.get(currentIndex);
            if (info.coverImage != null) return info.coverImage;
        }
        return defaultCoverImage;
    }
    
    private String formatTime(long microseconds) {
        long totalSec = microseconds / 1_000_000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
    
    private long getWavDuration(File wavFile) {
        try {
            AudioCue cue = AudioCue.makeStereoCue(wavFile.toURI().toURL(), 1);
            cue.open();
            long duration = cue.getMicrosecondLength();
            cue.close();
            return duration;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void stopMusic() {
        musicPlayer.stopImmediately();
    }
    
    // ---- 背景绘制 ----
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
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
    
    // ---- 音乐文件信息 ----
    private static class MusicFileInfo {
        File wavFile;
        File coverFile;
        BufferedImage coverImage;
        
        MusicFileInfo(File wav, File cover) {
            this.wavFile = wav;
            this.coverFile = cover;
            if (cover != null && cover.exists()) {
                try { coverImage = ImageIO.read(cover); } catch (Exception e) { coverImage = null; }
            }
        }
        
        String getDisplayName() {
            String name = wavFile.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            // 去掉开头的数字和点
            name = name.replaceFirst("^\\d+[\\.\\-\\s]*", "");
            return name;
        }
    }
}
