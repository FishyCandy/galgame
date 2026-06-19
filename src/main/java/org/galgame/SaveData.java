package org.galgame;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 3L;
    private int currentIndex;
    private List<Dialogue> dialogues;
    private byte[] thumbnailBytes;
    private Date saveTime;
    private String currentSceneId;
    private int currentCommandIndex;

    public SaveData(int currentIndex, List<Dialogue> dialogues, byte[] thumbnailBytes,
                    String currentSceneId, int currentCommandIndex) {
        this.currentIndex = currentIndex;
        this.dialogues = dialogues;
        this.thumbnailBytes = thumbnailBytes;
        this.saveTime = new Date();
        this.currentSceneId = currentSceneId;
        this.currentCommandIndex = currentCommandIndex;
    }

    public int getCurrentIndex() { return currentIndex; }
    public List<Dialogue> getDialogues() { return dialogues; }
    public byte[] getThumbnailBytes() { return thumbnailBytes; }
    public Date getSaveTime() { return saveTime; }
    public String getCurrentSceneId() { return currentSceneId; }
    public int getCurrentCommandIndex() { return currentCommandIndex; }
}