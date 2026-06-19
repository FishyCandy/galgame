# ====== First, modify SaveData.java ======
filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\SaveData.java"
with open(filepath, "r", encoding="utf-8") as f:
    c = f.read()

# Add fields
c = c.replace(
    "    private String currentSceneId;\n    private int currentCommandIndex;",
    "    private String currentSceneId;\n    private int currentCommandIndex;\n    private String currentBgPath;\n    private String currentSpritePath;"
)

# Update constructor signature
c = c.replace(
    "    public SaveData(int currentIndex, List<Dialogue> dialogues, byte[] thumbnailBytes,\n                    String currentSceneId, int currentCommandIndex) {\n        this.currentIndex = currentIndex;\n        this.dialogues = dialogues;\n        this.thumbnailBytes = thumbnailBytes;\n        this.saveTime = new Date();\n        this.currentSceneId = currentSceneId;\n        this.currentCommandIndex = currentCommandIndex;\n    }",
    "    public SaveData(int currentIndex, List<Dialogue> dialogues, byte[] thumbnailBytes,\n                    String currentSceneId, int currentCommandIndex,\n                    String currentBgPath, String currentSpritePath) {\n        this.currentIndex = currentIndex;\n        this.dialogues = dialogues;\n        this.thumbnailBytes = thumbnailBytes;\n        this.saveTime = new Date();\n        this.currentSceneId = currentSceneId;\n        this.currentCommandIndex = currentCommandIndex;\n        this.currentBgPath = currentBgPath;\n        this.currentSpritePath = currentSpritePath;\n    }"
)

# Add getters
c = c.replace(
    "    public int getCurrentCommandIndex() { return currentCommandIndex; }\n}",
    "    public int getCurrentCommandIndex() { return currentCommandIndex; }\n    public String getCurrentBgPath() { return currentBgPath; }\n    public String getCurrentSpritePath() { return currentSpritePath; }\n}"
)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(c)

print("SaveData.java updated")

# ====== Now modify GamePanel.java ======
filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    c = f.read()

# 1. Add currentBgPath and currentSpritePath fields after bgImage/spriteImage
c = c.replace(
    "    private BufferedImage spriteImage; // \u4eba\u7269\u5dee\u5206\u56fe\n\n    private StoryManager storyManager;",
    "    private BufferedImage spriteImage; // \u4eba\u7269\u5dee\u5206\u56fe\n    private String currentBgPath = null;     // \u5f53\u524d\u80cc\u666f\u8def\u5f84\n    private String currentSpritePath = null; // \u5f53\u524d\u5dee\u5206\u56fe\u8def\u5f84\n\n    private StoryManager storyManager;"
)

# 2. Track currentBgPath in "bg" case handler
old_bg_case = '''            case "bg":
                // \u80cc\u666f\u5207\u6362\u6307\u4ee4\uff1a\u542f\u52a8\u6de1\u5165\u6de1\u51fa\u8f6c\u573a
                if (cmd.bg != null && !cmd.bg.isEmpty()) {
                    startBgTransition(cmd.bg);
                }'''

new_bg_case = '''            case "bg":
                // \u80cc\u666f\u5207\u6362\u6307\u4ee4\uff1a\u542f\u52a8\u6de1\u5165\u6de1\u51fa\u8f6c\u573a
                if (cmd.bg != null && !cmd.bg.isEmpty()) {
                    currentBgPath = cmd.bg;
                    startBgTransition(cmd.bg);
                }'''

c = c.replace(old_bg_case, new_bg_case)

# 3. Track currentSpritePath in "sprite" case handler
old_sprite_case = '''            case "sprite":
                // \u663e\u793a\u4eba\u7269\u5dee\u5206\u56fe\uff08\u5177\u6709\u5ef6\u7eed\u6027\uff09
                if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                    loadSpriteImage(cmd.sprite);
                }'''

new_sprite_case = '''            case "sprite":
                // \u663e\u793a\u4eba\u7269\u5dee\u5206\u56fe\uff08\u5177\u6709\u5ef6\u7eed\u6027\uff09
                if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                    currentSpritePath = cmd.sprite;
                    loadSpriteImage(cmd.sprite);
                }'''

c = c.replace(old_sprite_case, new_sprite_case)

# 4. Track currentSpritePath = null in "sprite_hide" case handler
old_sprite_hide_case = '''            case "sprite_hide":
                // \u9690\u85cf\u4eba\u7269\u5dee\u5206\u56fe
                hideSprite();'''

new_sprite_hide_case = '''            case "sprite_hide":
                // \u9690\u85cf\u4eba\u7269\u5dee\u5206\u56fe
                currentSpritePath = null;
                hideSprite();'''

c = c.replace(old_sprite_hide_case, new_sprite_hide_case)

# 5. Update saveGameToFile to include bg/sprite paths
old_save = '''            String sceneId = storyManager.getCurrentSceneId();
            int cmdIndex = storyManager.getCurrentCommandIndex();
            SaveData data = new SaveData(0, null, thumbBytes, sceneId, cmdIndex);'''

new_save = '''            String sceneId = storyManager.getCurrentSceneId();
            int cmdIndex = storyManager.getCurrentCommandIndex();
            SaveData data = new SaveData(0, null, thumbBytes, sceneId, cmdIndex,
                    currentBgPath, currentSpritePath);'''

c = c.replace(old_save, new_save)

# 6. Update loadGameFromFile to restore bg/sprite
old_load_restore = '''            history.clear();
            waitingForChoice = false;
            // \u56de\u9000\u4e00\u6b65\uff1a\u5b58\u6863\u65f6\u7684\u7d22\u5f15\u6307\u5411\u4e0b\u4e00\u6761\u6307\u4ee4\uff0c\u56de\u9000\u5230\u5df2\u663e\u793a\u7684\u6307\u4ee4
            int restoredIndex = storyManager.getCurrentCommandIndex();
            if (restoredIndex > 0) {
                storyManager.setCurrentCommandIndex(restoredIndex - 1);
            }
            updateDisplay();'''

new_load_restore = '''            history.clear();
            waitingForChoice = false;

            // \u6062\u590d\u80cc\u666f\u56fe\u548c\u5dee\u5206\u56fe
            if (data.getCurrentBgPath() != null && !data.getCurrentBgPath().isEmpty()) {
                loadBgImage(data.getCurrentBgPath());
                currentBgPath = data.getCurrentBgPath();
            } else {
                bgImage = null;
                currentBgPath = null;
            }
            if (data.getCurrentSpritePath() != null && !data.getCurrentSpritePath().isEmpty()) {
                try {
                    java.net.URL imgUrl = getClass().getResource("/" + data.getCurrentSpritePath());
                    if (imgUrl != null) {
                        spriteImage = ImageIO.read(imgUrl);
                        currentSpritePath = data.getCurrentSpritePath();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                spriteImage = null;
                currentSpritePath = null;
            }
            spriteAlpha = 1f;
            isSpriteTransitioning = false;

            // \u56de\u9000\u4e00\u6b65\uff1a\u5b58\u6863\u65f6\u7684\u7d22\u5f15\u6307\u5411\u4e0b\u4e00\u6761\u6307\u4ee4\uff0c\u56de\u9000\u5230\u5df2\u663e\u793a\u7684\u6307\u4ee4
            int restoredIndex = storyManager.getCurrentCommandIndex();
            if (restoredIndex > 0) {
                storyManager.setCurrentCommandIndex(restoredIndex - 1);
            }
            updateDisplay();'''

c = c.replace(old_load_restore, new_load_restore)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(c)

print("GamePanel.java updated")
