package org.galgame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;

/**
 * 故事管理器，负责加载 JSON 剧本，管理当前场景和命令索引。
 */
public class StoryManager {
    private StoryData storyData;
    private String currentSceneId;
    private int currentCommandIndex;
    private List<StoryData.CommandData> currentCommands;
    private boolean isEnd = false;

    public StoryManager() {
        loadStory("/story.json");
    }

    private void loadStory(String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.err.println("剧本文件未找到: " + path);
                return;
            }
            storyData = mapper.readValue(is, StoryData.class);
            currentSceneId = storyData.start;
            currentCommands = storyData.scenes.get(currentSceneId).commands;
            currentCommandIndex = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取下一条指令，并自动推进索引。
     * 处理 jump 和 end 指令。
     * @return 下一条指令，若故事结束或出错返回 null
     */
    public StoryData.CommandData nextCommand() {
        if (isEnd) return null;
        if (currentCommands == null) return null;

        // 如果当前索引超出，则结束
        while (currentCommandIndex >= currentCommands.size()) {
            return null;
        }

        StoryData.CommandData cmd = currentCommands.get(currentCommandIndex++);

        // 处理 jump 指令
        if ("jump".equals(cmd.type)) {
            String target = cmd.target;
            if (target != null && storyData.scenes.containsKey(target)) {
                currentSceneId = target;
                currentCommands = storyData.scenes.get(target).commands;
                currentCommandIndex = 0;
                return nextCommand(); // 递归获取下一条
            } else {
                return null;
            }
        }

        // 处理 end
        if ("end".equals(cmd.type)) {
            isEnd = true;
            return cmd;
        }

        return cmd;
    }

    /**
     * 外部跳转（用于选项选择后）
     */
    public void jumpToScene(String sceneId) {
        if (storyData.scenes.containsKey(sceneId)) {
            currentSceneId = sceneId;
            currentCommands = storyData.scenes.get(sceneId).commands;
            currentCommandIndex = 0;
        }
    }

    public boolean isEnd() {
        return isEnd;
    }

    // ---------- 新增 getter/setter，用于存档/读档 ----------
    public String getCurrentSceneId() {
        return currentSceneId;
    }

    public int getCurrentCommandIndex() {
        return currentCommandIndex;
    }

    public void setCurrentSceneId(String sceneId) {
        if (storyData.scenes.containsKey(sceneId)) {
            this.currentSceneId = sceneId;
            this.currentCommands = storyData.scenes.get(sceneId).commands;
            this.currentCommandIndex = 0;
        } else {
            System.err.println("场景不存在: " + sceneId);
        }
    }

    public void setCurrentCommandIndex(int index) {
        if (currentCommands != null && index >= 0 && index < currentCommands.size()) {
            this.currentCommandIndex = index;
        } else {
            System.err.println("命令索引无效: " + index);
        }
    }
}