package org.galgame;

import com.adonax.audiocue.AudioCue;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayer {
    private AudioCue audioCue;
    private int instanceId = -1;
    private String currentFile = null;
    private Thread fadeThread = null;
    private boolean loop = false;
    // 全局音量设置（0.0 ~ 1.0）
    private static double globalVolume = 0.8;
    private static final List<MusicPlayer> instances = new ArrayList<>();

    public MusicPlayer() {
        synchronized (instances) {
            instances.add(this);
        }
    }

    public static void setGlobalVolume(double volume) {
        globalVolume = Math.max(0.0, Math.min(1.0, volume));
        synchronized (instances) {
            for (MusicPlayer mp : instances) {
                mp.applyVolume();
            }
            SoundEffects.updateVolume();
        }
    }

    public static double getGlobalVolume() {
        return globalVolume;
    }

    public void applyVolume() {
        if (audioCue != null && instanceId != -1 && audioCue.getIsPlaying(instanceId)) {
            audioCue.setVolume(instanceId, globalVolume);
        }
    }

    private static final int FADE_STEPS = 100;
    private static final int STEP_INTERVAL_MS = 15;

    public void play(File file) {
        try {
            play(file.toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
            currentFile = null;
        }
    }

    public void play(URL url) {
        stopImmediately();
        try {
            audioCue = AudioCue.makeStereoCue(url, 1);
            audioCue.open();
            instanceId = audioCue.play(0.0);
            if (loop) {
                audioCue.setLooping(instanceId, -1);
            }
            fadeTo(globalVolume, null);
            currentFile = url.getFile();
        } catch (Exception e) {
            // 捕获 AudioCue 加载失败（如格式不支持）
            System.err.println("❌ 音乐加载失败，请确保文件是 16-bit PCM WAV 格式: " + url.getFile());
            e.printStackTrace();
            // 释放资源
            if (audioCue != null) {
                audioCue.close();
                audioCue = null;
            }
            instanceId = -1;
            currentFile = null;
            // 可以通过回调通知 UI（但简化处理）
        }
    }

    public void fadeTo(URL url) {
        String newFile = url.getFile();
        if (newFile.equals(currentFile)) return;

        if (audioCue != null && instanceId != -1 && audioCue.getIsPlaying(instanceId)) {
            fadeTo(0.0, () -> {
                stopImmediately();
                play(url);
            });
        } else {
            play(url);
        }
    }

    public void fadeTo(File file) {
        try {
            fadeTo(file.toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (audioCue != null && instanceId != -1 && audioCue.getIsPlaying(instanceId)) {
            audioCue.stop(instanceId);
        }
    }

    public void resume() {
        if (audioCue != null && instanceId != -1 && !audioCue.getIsPlaying(instanceId) && audioCue.getIsActive(instanceId)) {
            audioCue.start(instanceId);
        }
    }

    public void stopImmediately() {
        if (fadeThread != null && fadeThread.isAlive()) {
            fadeThread.interrupt();
            try {
                fadeThread.join(100); // 等待线程结束
            } catch (InterruptedException ignored) {}
        }
        if (audioCue != null) {
            audioCue.close();
            audioCue = null;
        }
        instanceId = -1;
        currentFile = null;
    }

    public void stopWithFadeOut() {
        if (audioCue != null && instanceId != -1 && audioCue.getIsPlaying(instanceId)) {
            fadeTo(0.0, this::stopImmediately);
        } else {
            stopImmediately();
        }
    }

    public boolean isPlaying() {
        if (audioCue == null || instanceId == -1) return false;
        return audioCue.getIsPlaying(instanceId);
    }

    /** 获取当前播放位置（帧） */
    public double getFramePosition() {
        if (audioCue == null || instanceId == -1) return 0;
        return audioCue.getFramePosition(instanceId);
    }

    /** 获取当前播放位置（微秒）*/
    public long getMicrosecondPosition() {
        if (audioCue == null || instanceId == -1) return 0;
        long totalFrames = audioCue.getFrameLength();
        long totalMicros = audioCue.getMicrosecondLength();
        if (totalFrames <= 0 || totalMicros <= 0) return 0;
        double currentFrame = audioCue.getFramePosition(instanceId);
        return (long)(currentFrame / totalFrames * totalMicros);
    }

    /** 获取总帧数 */
    public long getFrameLength() {
        if (audioCue == null) return 0;
        return audioCue.getFrameLength();
    }

    /** 获取总时长（微秒） */
    public long getMicrosecondLength() {
        if (audioCue == null) return 0;
        return audioCue.getMicrosecondLength();
    }

    /** 设置播放位置（帧） */
    public void setFramePosition(double frame) {
        if (audioCue != null && instanceId != -1) {
            audioCue.setFramePosition(instanceId, frame);
        }
    }

    /** 设置播放位置（0.0~1.0 比例） */
    public void setFractionalPosition(double fraction) {
        if (audioCue != null && instanceId != -1 && audioCue.getIsActive(instanceId)) {
            audioCue.setFractionalPosition(instanceId, fraction);
        }
    }

    public void seekTo(double fraction) {
        if (audioCue != null && instanceId != -1 && audioCue.getIsActive(instanceId)) {
            boolean wasPlaying = audioCue.getIsPlaying(instanceId);
            audioCue.stop(instanceId);
            audioCue.setFractionalPosition(instanceId, fraction);
            if (wasPlaying) {
                audioCue.start(instanceId);
            }
        }
    }

    public String getCurrentFile() {
        return currentFile;
    }

    // ---------- 循环控制 ----------
    public void setLooping(boolean loop) {
        this.loop = loop;
        if (audioCue != null && instanceId != -1 && audioCue.getIsActive(instanceId)) {
            audioCue.setLooping(instanceId, loop ? -1 : 0);
        }
    }

    public boolean isLooping() {
        return loop;
    }

    // ---------- 私有淡入淡出 ----------
    private void fadeTo(double targetVolume, Runnable onComplete) {
        if (audioCue == null || instanceId == -1) {
            if (onComplete != null) onComplete.run();
            return;
        }
        if (fadeThread != null && fadeThread.isAlive()) {
            fadeThread.interrupt();
        }

        double start = audioCue.getVolume(instanceId);
        double step = (targetVolume - start) / FADE_STEPS;

        fadeThread = new Thread(() -> {
            try {
                for (int i = 0; i < FADE_STEPS; i++) {
                    double current = start + step * (i + 1);
                    if ((step > 0 && current >= targetVolume) || (step < 0 && current <= targetVolume)) {
                        current = targetVolume;
                    }
                    audioCue.setVolume(instanceId, current);
                    Thread.sleep(STEP_INTERVAL_MS);
                }
                audioCue.setVolume(instanceId, targetVolume);
                if (onComplete != null) {
                    javax.swing.SwingUtilities.invokeLater(onComplete);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        fadeThread.setDaemon(true);
        fadeThread.start();
    }
}
