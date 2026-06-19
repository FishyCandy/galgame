import re

filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    c = f.read()

# === Change 1: Add sprite transition fields after bg transition fields ===
old_fields = '''    private Timer bgTransitionTimer;             // \u8f6c\u573a\u52a8\u753b\u5b9a\u65f6\u5668'''

new_fields = '''    private Timer bgTransitionTimer;             // \u8f6c\u573a\u52a8\u753b\u5b9a\u65f6\u5668

    // ---- \u5dee\u5206\u56fe\u8f6c\u573a\u76f8\u5173\u5b57\u6bb5 ----
    private float spriteAlpha = 1f;              // \u5dee\u5206\u56fe\u900f\u660e\u5ea6 (0~1)
    private boolean isSpriteTransitioning = false;
    private String pendingSpritePath = null;
    private Timer spriteTransitionTimer;
    private int spriteFadeStep = 0;
    private int spriteFadePhase = 0;             // 0=\u6de1\u51fa, 1=\u6de1\u5165'''

c = c.replace(old_fields, new_fields)

# === Change 2: Rewrite loadSpriteImage with fade transition ===
old_load = '''    // ---------- \u4eba\u7269\u5dee\u5206\u56fe ----------
    private void loadSpriteImage(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                java.net.URL imgUrl = getClass().getResource("/" + path);
                if (imgUrl != null) {
                    spriteImage = ImageIO.read(imgUrl);
                    repaint();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println("\u5dee\u5206\u56fe\u672a\u627e\u5230: " + path);
    }'''

new_load = '''    // ---------- \u4eba\u7269\u5dee\u5206\u56fe ----------
    private void loadSpriteImage(String path) {
        if (path == null || path.isEmpty()) return;
        pendingSpritePath = path;

        // \u505c\u6b62\u4e4b\u524d\u7684\u8f6c\u573a\u5b9a\u65f6\u5668
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }

        if (spriteImage == null) {
            // \u6ca1\u6709\u5f53\u524d\u5dee\u5206\u56fe\uff0c\u76f4\u63a5\u52a0\u8f7d\u5e76\u6de1\u5165
            loadSpriteFromPending();
            spriteAlpha = 0f;
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 1; // \u76f4\u63a5\u8fdb\u5165\u6de1\u5165\u9636\u6bb5
            startSpriteFadeTimer();
        } else {
            // \u6709\u5f53\u524d\u5dee\u5206\u56fe\uff0c\u5148\u6de1\u51fa
            isSpriteTransitioning = true;
            spriteFadeStep = 0;
            spriteFadePhase = 0; // \u5148\u6de1\u51fa
            spriteAlpha = 1f;
            startSpriteFadeTimer();
        }
    }

    private void loadSpriteFromPending() {
        try {
            java.net.URL imgUrl = getClass().getResource("/" + pendingSpritePath);
            if (imgUrl != null) {
                spriteImage = ImageIO.read(imgUrl);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("\u5dee\u5206\u56fe\u672a\u627e\u5230: " + pendingSpritePath);
    }

    private void startSpriteFadeTimer() {
        final int STEPS = 30; // ~480ms @ 16ms/tick
        spriteTransitionTimer = new Timer(16, null);
        spriteTransitionTimer.addActionListener(e -> {
            spriteFadeStep++;
            float progress = Math.min(1f, (float) spriteFadeStep / STEPS);

            if (spriteFadePhase == 0) {
                // \u6de1\u51fa
                spriteAlpha = 1f - progress;
                if (progress >= 1f) {
                    // \u5207\u6362\u5230\u65b0\u56fe\uff0c\u5f00\u59cb\u6de1\u5165
                    loadSpriteFromPending();
                    spriteAlpha = 0f;
                    spriteFadePhase = 1;
                    spriteFadeStep = 0;
                }
            } else if (spriteFadePhase == 1) {
                // \u6de1\u5165
                spriteAlpha = progress;
                if (progress >= 1f) {
                    spriteAlpha = 1f;
                    isSpriteTransitioning = false;
                    spriteTransitionTimer.stop();
                }
            }
            repaint();
        });
        spriteTransitionTimer.start();
    }'''

c = c.replace(old_load, new_load)

# === Change 3: Rewrite hideSprite with fade-out ===
old_hide = '''    private void hideSprite() {
        spriteImage = null;
        repaint();
    }'''

new_hide = '''    private void hideSprite() {
        if (spriteImage == null) return;

        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }

        isSpriteTransitioning = true;
        spriteFadeStep = 0;
        spriteFadePhase = 0; // \u6de1\u51fa
        spriteAlpha = 1f;

        final int STEPS = 30;
        spriteTransitionTimer = new Timer(16, null);
        spriteTransitionTimer.addActionListener(e -> {
            spriteFadeStep++;
            float progress = Math.min(1f, (float) spriteFadeStep / STEPS);
            spriteAlpha = 1f - progress;
            if (progress >= 1f) {
                spriteImage = null;
                spriteAlpha = 1f;
                isSpriteTransitioning = false;
                spriteTransitionTimer.stop();
            }
            repaint();
        });
        spriteTransitionTimer.start();
    }'''

c = c.replace(old_hide, new_hide)

# === Change 4: Use spriteAlpha in paintComponent ===
old_paint_sprite = '''        // \u7ed8\u5236\u4eba\u7269\u5dee\u5206\u56fe\uff08\u5e95\u90e8\u52302/3\u9ad8\u5ea6\uff09
        if (spriteImage != null) {
            Graphics2D g2s = (Graphics2D) g.create();
            int spriteHeight = getHeight();
            double ratio = (double) spriteImage.getWidth(null) / spriteImage.getHeight(null);
            int spriteWidth = (int) (spriteHeight * ratio);
            int x = (getWidth() - spriteWidth) / 2;
            int y = getHeight() - spriteHeight;
            g2s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2s.drawImage(spriteImage, x, y, spriteWidth, spriteHeight, this);
            g2s.dispose();
        }'''

new_paint_sprite = '''        // \u7ed8\u5236\u4eba\u7269\u5dee\u5206\u56fe\uff08\u5168\u5c4f\u9ad8\u5ea6\uff0c\u652f\u6301\u6de1\u5165\u6de1\u51fa\uff09
        if (spriteImage != null) {
            Graphics2D g2s = (Graphics2D) g.create();
            g2s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, spriteAlpha));
            int spriteHeight = getHeight();
            double ratio = (double) spriteImage.getWidth(null) / spriteImage.getHeight(null);
            int spriteWidth = (int) (spriteHeight * ratio);
            int x = (getWidth() - spriteWidth) / 2;
            int y = getHeight() - spriteHeight;
            g2s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2s.drawImage(spriteImage, x, y, spriteWidth, spriteHeight, this);
            g2s.dispose();
        }'''

c = c.replace(old_paint_sprite, new_paint_sprite)

# === Change 5: Block nextCommand on sprite transitioning ===
old_next = '''    private void nextCommand() {
        if (waitingForChoice) return;
        if (isBgTransitioning) return;
        updateDisplay();'''

new_next = '''    private void nextCommand() {
        if (waitingForChoice) return;
        if (isBgTransitioning) return;
        if (isSpriteTransitioning) return;
        updateDisplay();'''

c = c.replace(old_next, new_next)

# === Change 6: Reset sprite state in startGame ===
# Add sprite reset after isBgTransitioning = false in startGame
old_start_reset = '''        isBgTransitioning = false;
        characterLabel.setText("");'''

new_start_reset = '''        isBgTransitioning = false;
        spriteImage = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");'''

c = c.replace(old_start_reset, new_start_reset)

# === Change 6b: Reset sprite state in resetGame ===
old_reset_reset = '''        isBgTransitioning = false;
        characterLabel.setText("");
        lineArea.setText("");
        history.clear();'''

new_reset_reset = '''        isBgTransitioning = false;
        spriteImage = null;
        spriteAlpha = 1f;
        isSpriteTransitioning = false;
        if (spriteTransitionTimer != null && spriteTransitionTimer.isRunning()) {
            spriteTransitionTimer.stop();
        }
        characterLabel.setText("");
        lineArea.setText("");
        history.clear();'''

c = c.replace(old_reset_reset, new_reset_reset)

# === Change 7: Update "sprite" case to return (wait for transition like "bg" does) ===
# Actually, the sprite case should NOT return - the fade happens in background.
# But the sprite case currently breaks and then revalidate/repaint. That's fine.

# Write the file
with open(filepath, "w", encoding="utf-8") as f:
    f.write(c)

print("Done - all sprite fade transition changes applied")
