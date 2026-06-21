import sys, re
sys.stdout.reconfigure(encoding='utf-8')

# Common replacement: add glow hover effect to the JButton inside createReturnButton
# We need to replace this pattern in the icon button creation:
#     JButton btn = new JButton(new ImageIcon(scaled));
#     btn.setOpaque(false);
#     btn.setContentAreaFilled(false);
#     btn.setBorderPainted(false);
#     btn.setFocusPainted(false);
#     btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
#     btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
#     return btn;

old_pattern = '''JButton btn = new JButton(new ImageIcon(scaled));
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;'''

new_pattern = '''JButton btn = new JButton(new ImageIcon(scaled)) {
                    private boolean hovered = false;
                    {
                        addMouseListener(new MouseAdapter() {
                            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                        });
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (hovered) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            int cx = getWidth() / 2, cy = getHeight() / 2;
                            int r = Math.min(getWidth(), getHeight()) / 2 - 2;
                            g2.setColor(new Color(255, 255, 255, 80));
                            g2.setStroke(new BasicStroke(3f));
                            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                            g2.setColor(new Color(255, 255, 255, 30));
                            g2.setStroke(new BasicStroke(6f));
                            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                            g2.dispose();
                        }
                    }
                };
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return btn;'''

for fname in ['LogPanel.java', 'SaveLoadPanel.java', 'SettingsPanel.java']:
    path = f'D:/ideaDocuments/galgame/src/main/java/org/galgame/{fname}'
    with open(path, 'r', encoding='utf-8-sig') as f:
        content = f.read()
    
    if old_pattern in content:
        content = content.replace(old_pattern, new_pattern)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'{fname}: glow effect added')
    else:
        print(f'{fname}: pattern NOT found, checking...')
        # Print what's around "new ImageIcon(scaled)"
        idx = content.find('new ImageIcon(scaled)')
        if idx >= 0:
            print(content[idx:idx+300])
        else:
            print('  ImageIcon not found!')
