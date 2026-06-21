import sys
sys.stdout.reconfigure(encoding='utf-8')
with open(r'D:\ideaDocuments\galgame\src\main\java\org\galgame\SaveLoadPanel.java', 'r', encoding='utf-8-sig') as f:
    lines = f.readlines()
# Show constructor area (lines 35-75)
print('=== CONSTRUCTOR ===')
for i in range(35, 75):
    if i < len(lines):
        print(f'{i+1}: {lines[i].rstrip()}')
print()
print('=== REFRESHSLOTS ===')
for i in range(395, 440):
    if i < len(lines):
        print(f'{i+1}: {lines[i].rstrip()}')
