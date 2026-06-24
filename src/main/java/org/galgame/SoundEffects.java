package org.galgame;

import com.adonax.audiocue.AudioCue;
import java.net.URL;

/**
 * 按钮音效工具类 —— 预加载悬停和点击音效，全局音量跟随 MusicPlayer
 */
public class SoundEffects {
    private static AudioCue hoverCue;
    private static AudioCue clickCue;
    private static int hoverInstance = -1;
    private static int clickInstance = -1;
    private static boolean initialized = false;
    private static double sfxVolume = 0.8;

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            URL hoverUrl = SoundEffects.class.getResource("/sounds/hover.wav");
            if (hoverUrl != null) {
                hoverCue = AudioCue.makeStereoCue(hoverUrl, 2); // 最多2个同时播放
                hoverCue.open();
            }
            URL clickUrl = SoundEffects.class.getResource("/sounds/click.wav");
            if (clickUrl != null) {
                clickCue = AudioCue.makeStereoCue(clickUrl, 2);
                clickCue.open();
            }
        } catch (Exception e) {
            System.err.println("音效加载失败: " + e.getMessage());
        }
    }

    /** 播放悬停音效 */
    public static void playHover() {
        if (hoverCue == null) return;
        try {
            hoverInstance = hoverCue.play(sfxVolume);
        } catch (Exception ignored) {}
    }

    /** 播放点击音效 */
    public static void playClick() {
        if (clickCue == null) return;
        try {
            clickInstance = clickCue.play(sfxVolume);
        } catch (Exception ignored) {}
    }

    /** 更新音效音量（跟随全局音量） */
    public static void updateVolume() {
        double vol = sfxVolume;
        if (hoverCue != null && hoverInstance != -1 && hoverCue.getIsPlaying(hoverInstance)) {
            hoverCue.setVolume(hoverInstance, vol);
        }
        if (clickCue != null && clickInstance != -1 && clickCue.getIsPlaying(clickInstance)) {
            clickCue.setVolume(clickInstance, vol);
        }
    }

    /** 释放资源 */

    /** 设置音效音量（0.0~1.0） */
    public static void setVolume(double vol) {
        sfxVolume = vol;
    }

    public static void close() {
        if (hoverCue != null) { hoverCue.close(); hoverCue = null; }
        if (clickCue != null) { clickCue.close(); clickCue = null; }
    }
}
