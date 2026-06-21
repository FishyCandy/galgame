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
    private boolean playlistVisible = false;
    private float playlistSlideX = 0f;
    
    private JLabel albumArtLabel, songTitleLabel, timeCurrentLabel, timeTotalLabel;
    private JButton prevBtn, playPauseBtn, nextBtn;
    private JButton seqBtn, singleLoopBtn, randomBtn;
    private JSlider volumeSlider, progressSlider;
    private JPanel playlistPanel;
    private JScrollPane playlistScroll;
    private JPanel playlistContent;
    
    private enum PlayMode { SEQUENTIAL, SINGLE_LOOP, RANDOM }
    private PlayMode playMode = PlayMode.SEQUENTIAL;
    private boolean progressDragging = false;
    private JPanel glassPanel;

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
        
        progressTimer = new Timer(200, e -> updateProgress());
        progressTimer.start();
    }
    
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
    
    private void createUI() {
        // 毛玻璃容器（框住封面+四行控件）
        glassPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                g2.dispose();
            }
        };
        glassPanel.setLayout(null);
        glassPanel.setOpaque(false);
        add(glassPanel);

        JLabel titleLabel = new JLabel("\u266B \u97F3\u4E50\u9274\u8D4F", SwingConstants.CENTER);
        titleLabel.setFont(titleFont.deriveFont(48f));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel);
        
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
        
        // 专辑封面
        albumArtLabel = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 20, 20);
                BufferedImage cover = getCurrentCover();
                int m = 12;
                if (cover != null) {
                    g2.drawImage(cover, m, m, getWidth()-2*m, getHeight()-2*m, this);
                } else if (defaultCoverImage != null) {
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
        
        songTitleLabel = new JLabel(musicList.isEmpty() ? "\u6682\u65E0\u97F3\u4E50" : "\u70B9\u51FB\u64AD\u653E", SwingConstants.CENTER);
        songTitleLabel.setFont(buttonFont.deriveFont(20f));
        songTitleLabel.setForeground(new Color(255, 255, 255, 220));
        add(songTitleLabel);
        
        // 第1行: ⏮ ▶/⏸ ⏭
        prevBtn = createControlButton("\u23EE");
        prevBtn.addActionListener(e -> playPrevious());
        add(prevBtn);
        
        playPauseBtn = createControlButton("\u25B6");
        playPauseBtn.addActionListener(e -> togglePlayPause());
        add(playPauseBtn);
        
        nextBtn = createControlButton("\u23ED");
        nextBtn.addActionListener(e -> playNext());
        add(nextBtn);
        
        // 第2行: 🔁 🔂 🔀
        seqBtn = createModeButton("\uD83D\uDD01");
        seqBtn.setToolTipText("\u987A\u5E8F\u64AD\u653E");
        seqBtn.addActionListener(e -> setPlayMode(PlayMode.SEQUENTIAL));
        add(seqBtn);
        
        singleLoopBtn = createModeButton("\uD83D\uDD02");
        singleLoopBtn.setToolTipText("\u5355\u66F2\u5FAA\u73AF");
        singleLoopBtn.addActionListener(e -> setPlayMode(PlayMode.SINGLE_LOOP));
        add(singleLoopBtn);
        
        randomBtn = createModeButton("\uD83D\uDD00");
        randomBtn.setToolTipText("\u968F\u673A\u64AD\u653E");
        randomBtn.addActionListener(e -> setPlayMode(PlayMode.RANDOM));
        add(randomBtn);
        updateModeButtons();
        
        // 第3行: 进度条
        timeCurrentLabel = new JLabel("00:00");
        timeCurrentLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        timeCurrentLabel.setForeground(new Color(255, 255, 255, 180));
        add(timeCurrentLabel);
        
        timeTotalLabel = new JLabel("00:00");
        timeTotalLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        timeTotalLabel.setForeground(new Color(255, 255, 255, 180));
        add(timeTotalLabel);
        
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
        
        // 第4行: 音量
        JLabel volLabel = new JLabel("\uD83D\uDD0A");
        volLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        volLabel.setForeground(new Color(255, 255, 255, 180));
        add(volLabel);
        
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> {
            double vol = volumeSlider.getValue() / 100.0;
            MusicPlayer.setGlobalVolume(vol);
        });
        add(volumeSlider);
        
        // 播放列表
        createPlaylistPanel();
    }
    
    private JButton createControlButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Dialog", Font.PLAIN, 22));
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
        btn.setFont(new Font("Dialog", Font.PLAIN, 18));
        btn.setForeground(new Color(255, 255, 255, 120));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    private void createPlaylistPanel() {
        playlistPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 40, 210));
                g2.fillRoundRect(5, 5, getWidth()-10, getHeight()-10, 16, 16);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(5, 5, getWidth()-11, getHeight()-11, 16, 16);
                g2.dispose();
            }
        };
        playlistPanel.setLayout(new BorderLayout(0, 0));
        playlistPanel.setOpaque(false);
        
        JLabel listTitle = new JLabel("\u64AD\u653E\u5217\u8868", SwingConstants.LEFT);
        listTitle.setFont(buttonFont.deriveFont(18f));
        listTitle.setForeground(new Color(255, 255, 255, 200));
        listTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        playlistPanel.add(listTitle, BorderLayout.NORTH);
        
        playlistContent = new JPanel();
        playlistContent.setLayout(new BoxLayout(playlistContent, BoxLayout.Y_AXIS));
        playlistContent.setOpaque(false);
        
        playlistScroll = new JScrollPane(playlistContent);
        playlistScroll.setOpaque(false);
        playlistScroll.getViewport().setOpaque(false);
        playlistScroll.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
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
            JPanel item = new JPanel(new BorderLayout(12, 0));
            item.setOpaque(false);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            item.setPreferredSize(new Dimension(200, 52));
            item.setMinimumSize(new Dimension(200, 52));
            
            String songName = info.getDisplayName();
            JLabel nameLabel = new JLabel((idx + 1) + ". " + songName);
            nameLabel.setFont(buttonFont.deriveFont(22f));
            nameLabel.setForeground(idx == currentIndex ? new Color(255, 255, 150) : new Color(255, 255, 255, 200));
            
            JLabel durLabel = new JLabel(formatTime(getWavDuration(info.wavFile)));
            durLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
            durLabel.setForeground(new Color(255, 255, 255, 160));
            
            item.add(nameLabel, BorderLayout.CENTER);
            item.add(durLabel, BorderLayout.EAST);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { playAtIndex(idx); }
                public void mouseEntered(MouseEvent e) { item.setBackground(new Color(255,255,255,25)); item.setOpaque(true); item.repaint(); }
                public void mouseExited(MouseEvent e) { item.setOpaque(false); item.repaint(); }
            });
            
            playlistContent.add(item);
            playlistContent.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        playlistContent.revalidate();
        playlistContent.repaint();
    }
    
    public void doLayout() {
        int w = getWidth(), h = getHeight();
        int margin = 40;
        
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\u266B"))
                c.setBounds((w - 300) / 2, 8, 300, 45);
            else if (c instanceof JButton && ((JButton)c).getText().equals("X"))
                c.setBounds(w - 50, 8, 40, 40);
        }
        
        int coverSize = Math.max(w / 3, 200);
        int coverX = margin;
        int coverY = 80;
        
        // 毛玻璃面板包围封面+四行
        int glassH = coverSize + 38 + 36*4 + 6*3 + 16;
        glassPanel.setBounds(coverX - 12, coverY - 12, coverSize + 24, glassH);
        
        albumArtLabel.setBounds(coverX, coverY, coverSize, coverSize);
        songTitleLabel.setBounds(coverX, coverY + coverSize + 8, coverSize, 25);
        
        int ctrlY = coverY + coverSize + 38;
        int ctrlW = coverSize;
        int rowH = 36;
        int gap = 6;
        
        // 第1行：3个控制按钮
        int btnCount = 3;
        int btnW = (ctrlW - gap * (btnCount - 1)) / btnCount;
        int row1Y = ctrlY;
        prevBtn.setBounds(coverX, row1Y, btnW, rowH);
        playPauseBtn.setBounds(coverX + btnW + gap, row1Y, btnW, rowH);
        nextBtn.setBounds(coverX + (btnW + gap) * 2, row1Y, btnW, rowH);
        
        // 第2行：3个模式按钮
        int modeCount = 3;
        int modeW = (ctrlW - gap * (modeCount - 1)) / modeCount;
        int row2Y = row1Y + rowH + gap;
        seqBtn.setBounds(coverX, row2Y, modeW, rowH);
        singleLoopBtn.setBounds(coverX + modeW + gap, row2Y, modeW, rowH);
        randomBtn.setBounds(coverX + (modeW + gap) * 2, row2Y, modeW, rowH);
        
        // 第3行：进度条（排头对齐）
        int row3Y = row2Y + rowH + gap;
        timeCurrentLabel.setBounds(coverX, row3Y + 5, 42, rowH - 10);
        progressSlider.setBounds(coverX + 44, row3Y + 5, ctrlW - 88, rowH - 10);
        timeTotalLabel.setBounds(coverX + ctrlW - 42, row3Y + 5, 42, rowH - 10);
        
        // 第4行：音量
        int row4Y = row3Y + rowH + gap;
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel)c).getText().contains("\uD83D\uDD0A"))
                c.setBounds(coverX, row4Y + 5, 25, rowH - 10);
        }
        volumeSlider.setBounds(coverX + 28, row4Y + 5, ctrlW - 28, rowH - 10);
        
        // 播放列表（滑出动画）
        int glassRight = coverX + coverSize + 12; int plFullX = glassRight + 8;
        int plW = Math.min(280, w - plFullX - 20);
        if (plW < 150) { plW = 150; plFullX = w - plW - margin; }
        int plDisplayX = (int)(plFullX + (1f - playlistSlideX) * plW);
        playlistPanel.setBounds(plDisplayX, coverY - 12, plW, glassH);
        
        // 如果播放列表可见但宽度太窄，也隐藏掉右侧区域的内容
        if (playlistVisible) {
            songTitleLabel.setVisible(false);
        } else {
            songTitleLabel.setVisible(true);
        }
    }
    
    private void togglePlaylist() {
        playlistVisible = !playlistVisible;
        if (playlistVisible) {
            playlistPanel.setVisible(true);
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
    
    // ---- 播放控制 ----
    private void playAtIndex(int index) {
        if (index < 0 || index >= musicList.size()) return;
        currentIndex = index;
        playCurrent();
        refreshPlaylistItems();
        albumArtLabel.repaint();
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
            if (!musicList.isEmpty()) { playAtIndex(0); }
            return;
        }
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
            updatePlayPauseButton(false);
        } else {
            musicPlayer.resume();
            if (!musicPlayer.isPlaying()) { playCurrent(); }
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
            if (next >= musicList.size()) { next = 0; }
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
        playPauseBtn.setText(playing ? "\u23F8" : "\u25B6");
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
            if (posUs >= totalUs && totalUs > 0) { onTrackEnd(); }
        }
    }
    
    private void onTrackEnd() {
        if (playMode == PlayMode.SINGLE_LOOP) {
            playCurrent();
        } else {
            playNext();
        }
    }
    
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
    
    public void stopMusic() { musicPlayer.stopImmediately(); }
    
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
            name = name.replaceFirst("^\\d+[\\.\\-\\s]*", "");
            return name;
        }
    }
}
