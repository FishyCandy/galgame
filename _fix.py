import re

filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove imageLabel field declaration
content = content.replace("\n    private JLabel imageLabel;\n", "\n")

# 2. Remove createImagePanel() call from constructor
content = content.replace("\n        createImagePanel();\n", "\n")

# 3. Remove createImagePanel method
old_createImage = '''
    // ---------- UI创建 ----------
    private void createImagePanel() {
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 300));
        imageLabel.setOpaque(false);
        setImagePlaceholder();
        add(imageLabel, BorderLayout.CENTER);
    }
'''
content = content.replace(old_createImage, "\n")

# 4. Remove setImagePlaceholder method
old_setPlaceholder = '''
    private void setImagePlaceholder() {
        imageLabel.setIcon(createPlaceholderIcon("\u5dee\u5206\u56fe", 400, 300));
    }
'''
content = content.replace(old_setPlaceholder, "\n")

# 5. Remove createPlaceholderIcon method
old_placeholder = '''
    private ImageIcon createPlaceholderIcon(String text, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(0, 0, w, h, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("\u6977\u4f53", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
        return new ImageIcon(img);
    }
'''
content = content.replace(old_placeholder, "\n")

# 6. Remove loadImage method
old_loadImage = '''
    private void loadImage(String path) {
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
        String charName = characterLabel.getText();
        if (charName == null || charName.isEmpty()) charName = "\u672a\u77e5";
        imageLabel.setIcon(createPlaceholderIcon(charName, 400, 300));
    }
'''
content = content.replace(old_loadImage, "\n")

# 7. Remove "show" case from updateDisplay
old_show = '''            case "show":
                characterLabel.setText("");
                lineArea.setText("");
                loadImage(cmd.image);
                revalidate();
                repaint();
                break;
'''
content = content.replace(old_show, "\n")

# 8. Remove loadImage call from "say" case
old_say_load = '                if (cmd.image != null) loadImage(cmd.image);\n'
content = content.replace(old_say_load, "")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done - all old sprite/imageLabel code removed")
