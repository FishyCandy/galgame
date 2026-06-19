package org.galgame;

import java.util.List;
import java.util.Map;

public class StoryData {
    public String start;
    public Map<String, SceneData> scenes;

    public static class SceneData {
        public List<CommandData> commands;
    }

    public static class CommandData {
        public String type;
        public String who;
        public String text;
        public String image;
        public List<ChoiceData> choices;
        public String target;
        public String color;   // 新增：颜色值，如 "#FFFFFF"
        public String bgm;   // 新增：背景音乐文件名（例如 "bgm_01.wav"）
        public String bg;     // 新增：背景图片路径（例如 "game_bg.jpg"）
    }

    public static class ChoiceData {
        public String text;
        public String target;
    }
}
