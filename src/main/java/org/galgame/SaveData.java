package org.galgame;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 4L;
    private int currentIndex;
    private List<Dialogue> dialogues;
    private byte[] thumbnailBytes;
    private Date saveTime;
    private String currentSceneId;
    private int currentCommandIndex;
    private String currentBgPath;
    private String currentSpritePath;
    private Map<String, Integer> scores;  // 隐藏分

    public SaveData(int currentIndex, List<Dialogue> dialogues, byte[] thumbnailBytes,
                    String currentSceneId, int currentCommandIndex,
                    String currentBgPath, String currentSpritePath,
                    Map<String, Integer> scores) {
        this.currentIndex = currentIndex;
        this.dialogues = dialogues;
        this.thumbnailBytes = thumbnailBytes;
        this.saveTime = new Date();
        this.currentSceneId = currentSceneId;
        this.currentCommandIndex = currentCommandIndex;
        this.currentBgPath = currentBgPath;
        this.currentSpritePath = currentSpritePath;
        this.scores = scores;
    }

    public int getCurrentIndex() { return currentIndex; }
    public List<Dialogue> getDialogues() { return dialogues; }
    public byte[] getThumbnailBytes() { return thumbnailBytes; }
    public Date getSaveTime() { return saveTime; }
    public String getCurrentSceneId() { return currentSceneId; }
    public int getCurrentCommandIndex() { return currentCommandIndex; }
    public String getCurrentBgPath() { return currentBgPath; }
    public String getCurrentSpritePath() { return currentSpritePath; }
    public Map<String, Integer> getScores() { return scores; }
}