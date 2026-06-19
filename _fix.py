import re

filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Change 1: Add spriteImage field after bgImage
content = content.replace(
    "private BufferedImage bgImage;\n\n    private StoryManager storyManager;",
    "private BufferedImage bgImage;\n    private BufferedImage spriteImage; // \u4eba\u7269\u5dee\u5206\u56fe\n\n    private StoryManager storyManager;"
)

# Change 2: Replace loadSpriteImage method
old_load = '''    private void loadSpriteImage(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                java.net.URL imgUrl = getClass().getResource("/" + path);
                if (imgUrl != null) {
                    ImageIcon icon = new ImageIcon(imgUrl);
                    Image img = icon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(img));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println("\u5dee\u5206\u56fe\u672a\u627e\u5230: " + path);
    }'''

new_load = '''    private void loadSpriteImage(String path) {
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

content = content.replace(old_load, new_load)

# Change 3: Replace hideSprite method
old_hide = '''    private void hideSprite() {
        imageLabel.setIcon(createPlaceholderIcon(" ", 400, 300));
    }'''

new_hide = '''    private void hideSprite() {
        spriteImage = null;
        repaint();
    }'''

content = content.replace(old_hide, new_hide)

# Change 4: Add sprite drawing in paintComponent before transitionAlpha block
old_paint = '''        // \u80cc\u666f\u8f6c\u573a\u9ed1\u5e55\uff08\u8986\u76d6\u5728\u80cc\u666f\u4e4b\u4e0a\uff09
        if (transitionAlpha > 0.01f) {'''

new_paint = '''        // \u7ed8\u5236\u4eba\u7269\u5dee\u5206\u56fe\uff08\u5e95\u90e8\u52302/3\u9ad8\u5ea6\uff09
        if (spriteImage != null) {
            Graphics2D g2s = (Graphics2D) g.create();
            int spriteHeight = getHeight() * 2 / 3;
            double ratio = (double) spriteImage.getWidth(null) / spriteImage.getHeight(null);
            int spriteWidth = (int) (spriteHeight * ratio);
            int x = (getWidth() - spriteWidth) / 2;
            int y = getHeight() - spriteHeight;
            g2s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2s.drawImage(spriteImage, x, y, spriteWidth, spriteHeight, this);
            g2s.dispose();
        }

        // \u80cc\u666f\u8f6c\u573a\u9ed1\u5e55\uff08\u8986\u76d6\u5728\u80cc\u666f\u4e4b\u4e0a\uff09
        if (transitionAlpha > 0.01f) {'''

content = content.replace(old_paint, new_paint)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done - all 4 changes applied")
