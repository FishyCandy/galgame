package org.galgame;

import java.util.List;
import java.util.Map;

public class StoryData {
    public String start;
    public Map<String, Object> scenes;  // value 可以是 String(文件路径) 或 SceneData(内联)

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
        public String color;
        public String bgm;
        public String bg;
        public String sprite;

        // ---- 隐藏分系统字段 ----
        public String var;       // 变量名（用于 set/check）
        public Integer value;    // 设置值（用于 set）
        public Integer min;      // 最低阈值（用于 check）
        public String fallback;  // 不满足条件时跳转的场景（用于 check）
    }

    public static class ChoiceData {
        public String text;
        public String target;
        public Map<String, Integer> score;  // 选择后各变量的变化，如 {"affection": 5}
    }
}