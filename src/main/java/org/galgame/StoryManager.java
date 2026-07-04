package org.galgame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 故事管理器，负责加载 JSON 剧本，管理当前场景、命令索引和隐藏分。
 */
public class StoryManager {
    private Map<String, String> sceneIndex;       // 场景ID → 场景文件路径
    private Map<String, StoryData.SceneData> loadedScenes = new HashMap<>();  // 已加载的场景缓存
    private Map<String, Integer> scores = new HashMap<>();  // 隐藏分存储

    private String currentSceneId;
    private int currentCommandIndex;
    private List<StoryData.CommandData> currentCommands;
    private boolean isEnd = false;

    private static final ObjectMapper mapper = new ObjectMapper();

    public StoryManager() {
        loadStory("/story.json");
    }

    private void loadStory(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.err.println("剧本文件未找到: " + path);
                return;
            }
            StoryData storyData = mapper.readValue(is, StoryData.class);
            currentSceneId = storyData.start;

            // 构建场景索引：场景ID → 文件路径
            sceneIndex = new HashMap<>();
            for (Map.Entry<String, Object> entry : storyData.scenes.entrySet()) {
                sceneIndex.put(entry.getKey(), entry.getValue().toString());
            }

            // 加载起始场景
            loadScene(currentSceneId);
            currentCommandIndex = 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 按需加载场景文件（带缓存）
     */
    private void loadScene(String sceneId) {

        //如果这个场景之前加载过了，直接从缓存拿，不用重新读文件。
        if (loadedScenes.containsKey(sceneId)) {
            currentCommands = loadedScenes.get(sceneId).commands;
            return;
        }

        String filePath = sceneIndex.get(sceneId);
        if (filePath == null) {
            System.err.println("场景未在索引中找到: " + sceneId);
            currentCommands = null;
            return;
        }
        try {
            InputStream is = getClass().getResourceAsStream("/" + filePath);
            if (is == null) {
                System.err.println("场景文件未找到: " + filePath);
                currentCommands = null;
                return;
            }
            StoryData.SceneData scene = mapper.readValue(is, StoryData.SceneData.class);
            loadedScenes.put(sceneId, scene);
            currentCommands = scene.commands;
        } catch (Exception e) {
            e.printStackTrace();
            currentCommands = null;
        }
    }

    //隐藏分操作
    public int getScore(String var) {
        return scores.getOrDefault(var, 0);
    }

    public void setScore(String var, int value) {
        scores.put(var, value);
    }

    public void addScore(String var, int delta) {
        scores.put(var, scores.getOrDefault(var, 0) + delta);
    }

    //存档/读档时调用
    public Map<String, Integer> getScores() {
        return new HashMap<>(scores);
    }

    //存档/读档时调用
    public void setScores(Map<String, Integer> newScores) {
        scores.clear();
        if (newScores != null) {
            scores.putAll(newScores);
        }
    }

    /**
     * 检查分数条件。返回应跳转的场景ID，若无需跳转则返回 null。
     */
    public String checkCondition(String var, int min, String target, String fallback) {
        int current = scores.getOrDefault(var, 0);
        if (current >= min) {
            return target;
        }
        return fallback;
    }

    /**
     * 获取下一条指令，并自动推进索引。
     */
    public StoryData.CommandData nextCommand() {
        if (isEnd) return null;
        if (currentCommands == null) return null;

        while (currentCommandIndex >= currentCommands.size()) {
            return null;
        }

        StoryData.CommandData cmd = currentCommands.get(currentCommandIndex++);

        // 处理 jump 指令，jump：跳到另一场景，重新加载新场景json
        if ("jump".equals(cmd.type)) {
            String target = cmd.target;
            if (target != null && sceneIndex.containsKey(target)) {
                currentSceneId = target;
                loadScene(target);
                currentCommandIndex = 0;
                return nextCommand();
            }
            return null;
        }

        // 处理 end
        if ("end".equals(cmd.type)) {
            isEnd = true;
            return cmd;
        }

        return cmd;
    }

    //外部跳转（用于选项选择后）
    public void jumpToScene(String sceneId) {
        if (sceneIndex.containsKey(sceneId)) {
            currentSceneId = sceneId;
            loadScene(sceneId);
            currentCommandIndex = 0;
        }
    }

    public boolean isEnd() {
        return isEnd;
    }

    //getter/setter，用于存档/读档
    public String getCurrentSceneId() {
        return currentSceneId;
    }

    public int getCurrentCommandIndex() {
        return currentCommandIndex;
    }

    //读档时调用
    public void setCurrentSceneId(String sceneId) {
        if (sceneIndex.containsKey(sceneId)) {
            this.currentSceneId = sceneId;
            loadScene(sceneId);
            this.currentCommandIndex = 0;
        } else {
            System.err.println("场景不存在: " + sceneId);
        }
    }

    //读档时调用
    public void setCurrentCommandIndex(int index) {
        if (currentCommands != null && index >= 0 && index < currentCommands.size()) {
            this.currentCommandIndex = index;
        } else {
            System.err.println("命令索引无效: " + index);
        }
    }
}