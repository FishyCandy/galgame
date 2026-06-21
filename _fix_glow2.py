import sys
sys.stdout.reconfigure(encoding='utf-8')

old_mouse = '''btn.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 3, true));
                    }
                    public void mouseExited(MouseEvent e) {
                        btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    }
                });'''

new_mouse = '''btn.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        btn.setBorderPainted(true);
                        btn.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 120), 3, true));
                    }
                    public void mouseExited(MouseEvent e) {
                        btn.setBorderPainted(false);
                        btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    }
                });'''

for fname in ['LogPanel.java', 'SaveLoadPanel.java', 'SettingsPanel.java']:
    path = f'D:/ideaDocuments/galgame/src/main/java/org/galgame/{fname}'
    with open(path, 'r', encoding='utf-8-sig') as f:
        content = f.read()
    if old_mouse in content:
        content = content.replace(old_mouse, new_mouse)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'{fname}: fixed')
    else:
        print(f'{fname}: NOT found')
