import sys
sys.stdout.reconfigure(encoding='utf-8')

path = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\SaveLoadPanel.java"
with open(path, 'r', encoding='utf-8-sig') as f:
    lines = f.readlines()

# Find the constructor: after "add(topBar, BorderLayout.NORTH);" at ~line 60,
# need to insert slotPanel before "setBackground"
# Line 60: '        add(topBar, BorderLayout.NORTH);'
# Line 62: '        setBackground(new Color(30, 30, 60));'

for i, line in enumerate(lines):
    if 'add(topBar, BorderLayout.NORTH);' in line and i < 70:
        insert_idx = i + 1  # after this line
        slot_lines = [
            '\n',
            '        // 存档槽区域\n',
            '        JPanel slotPanel = new JPanel(new GridLayout(2, 3, 20, 20));\n',
            '        slotPanel.setOpaque(false);\n',
            '        slotPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));\n',
            '        for (int i = 0; i < 6; i++) {\n',
            '            slotPanel.add(createSlot(i));\n',
            '        }\n',
            '        add(slotPanel, BorderLayout.CENTER);\n',
        ]
        lines[insert_idx:insert_idx] = slot_lines
        print(f"Added slotPanel after line {i+1}")
        break
else:
    print("Could not find add(topBar, ...) line")

with open(path, 'w', encoding='utf-8') as f:
    f.write(''.join(lines))
print(f"Done, {len(lines)} lines written")
