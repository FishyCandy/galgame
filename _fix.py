filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    c = f.read()

# ====== 1. Replace loadGameFromFile with replays-all-commands approach ======
old_load = '''    public boolean loadGameFromFile(File saveFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            SaveData data = (SaveData) ois.readObject();
            storyManager.setCurrentSceneId(data.getCurrentSceneId());
            storyManager.setCurrentCommandIndex(data.getCurrentCommandIndex());
            history.clear();
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
            updateDisplay();
            JOptionPane.showMessageDialog(this, "读档成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "读档失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }'''

new_load = '''    public boolean loadGameFromFile(File saveFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            SaveData data = (SaveData) ois.readObject();
            storyManager.setCurrentSceneId(data.getCurrentSceneId());
            int savedCmdIndex = data.getCurrentCommandIndex();

            // \u91cd\u7f6e\u6240\u6709\u8f6c\u573a\u72b6\u6001
            if (bgTransitionTimer != null && bgTransitionTimer.isRunning()) bgTransitionTimer.stop();
            if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) spriteTransitionTimer.stop();
            transitionAlpha = 0f;
            dialogFadeAlpha = 1f;
            isBgTransitioning = false;
            spriteAlpha = 1f;
            isSpriteTransitioning = false;
            history.clear();
            waitingForChoice = false;

            // \u91cd\u653e\u573a\u666f\u4e2d\u6240\u6709bg/sprite/bgm\u6307\u4ee4\u6765\u91cd\u5efa\u72b6\u6001\uff08\u4ece\u7d22\u5f150\u5230\u5b58\u6863\u4f4d\u7f6e\uff09
            bgImage = null;
            currentBgPath = null;
            spriteImage = null;
            currentSpritePath = null;
            storyManager.setCurrentCommandIndex(0);
            for (int i = 0; i < savedCmdIndex; i++) {
                StoryData.CommandData cmd = storyManager.nextCommand();
                if (cmd == null) break;
                switch (cmd.type) {
                    case "bg":
                        if (cmd.bg != null && !cmd.bg.isEmpty()) {
                            loadBgImage(cmd.bg);
                            currentBgPath = cmd.bg;
                        }
                        break;
                    case "sprite":
                        if (cmd.sprite != null && !cmd.sprite.isEmpty()) {
                            try {
                                java.net.URL imgUrl = getClass().getResource("/" + cmd.sprite);
                                if (imgUrl != null) {
                                    spriteImage = ImageIO.read(imgUrl);
                                    currentSpritePath = cmd.sprite;
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        break;
                    case "sprite_hide":
                        spriteImage = null;
                        currentSpritePath = null;
                        break;
                    case "bgm":
                        if (cmd.bgm != null && !cmd.bgm.isEmpty()) {
                            URL musicUrl = getClass().getResource("/music/" + cmd.bgm);
                            if (musicUrl != null) {
                                musicPlayer.fadeTo(musicUrl);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            // \u56de\u9000\u4e00\u6b65\uff1a\u5b58\u6863\u65f6\u7684\u7d22\u5f15\u6307\u5411\u4e0b\u4e00\u6761\u6307\u4ee4\uff0c\u56de\u9000\u5230\u5df2\u663e\u793a\u7684\u6307\u4ee4
            storyManager.setCurrentCommandIndex(savedCmdIndex);
            if (savedCmdIndex > 0) {
                storyManager.setCurrentCommandIndex(savedCmdIndex - 1);
            }
            updateDisplay();
            JOptionPane.showMessageDialog(this, "读档成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "读档失败！", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }'''

c = c.replace(old_load, new_load)

# ====== 2. Reset currentBgPath/currentSpritePath in startGame ======
old_start = '''        isBgTransitioning = false;
        spriteImage = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");'''

new_start = '''        isBgTransitioning = false;
        spriteImage = null;
        currentBgPath = null;
        currentSpritePath = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");'''

c = c.replace(old_start, new_start)

# ====== 3. Reset currentBgPath/currentSpritePath in resetGame ======
old_reset = '''        isBgTransitioning = false;
        spriteImage = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");
        lineArea.setText("");
        history.clear();'''

new_reset = '''        isBgTransitioning = false;
        spriteImage = null;
        currentBgPath = null;
        currentSpritePath = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");
        lineArea.setText("");
        history.clear();'''

c = c.replace(old_reset, new_reset)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(c)

print("Done")
