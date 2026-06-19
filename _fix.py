filepath = r"D:\ideaDocuments\galgame\src\main\java\org\galgame\GamePanel.java"
with open(filepath, "r", encoding="utf-8") as f:
    c = f.read()

# Add "show" case that auto-advances before "say" case
old = '''        switch (cmd.type) {\n\n\n            case "say":'''
new = '''        switch (cmd.type) {\n\n            case "show":\n                // 旧版show指令已废弃，自动跳过\n                updateDisplay();\n                return;\n\n            case "say":'''

if old in c:
    c = c.replace(old, new)
    print("Replaced successfully")
elif new in c:
    print("Already patched")
else:
    print("Pattern not found, checking content...")
    # Try to find the switch statement
    idx = c.find('switch (cmd.type)')
    if idx >= 0:
        print("Switch found at index", idx)
        print("Context:", repr(c[idx:idx+120]))
    else:
        print("Switch NOT found!")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(c)
