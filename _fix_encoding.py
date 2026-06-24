import os
base = r"D:\ideaDocuments\galgame\src\main\java\org\galgame"
files_to_check = ["LogPanel.java", "MainMenuPanel.java", "MusicPlayer.java", "StrokableTextArea.java", "StrokableLabel.java"]
for fname in files_to_check:
    path = os.path.join(base, fname)
    with open(path, "rb") as f:
        raw = f.read()
    text = raw.decode("utf-8")
    lines = text.split("\n")
    for i, line in enumerate(lines):
        for ch in line:
            cp = ord(ch)
            if 0x80 <= cp <= 0x9F:
                print(f"\n{fname}:{i+1}: contains C1 control char U+{cp:04X}")
                print(f"  >>> {line[:100]}")
                break
            if 0xD7 <= cp <= 0xDF and cp != 0xD7:
                print(f"\n{fname}:{i+1}: suspicious char U+{cp:04X}")
                print(f"  >>> {line[:100]}")
                break
print("--- Scan done ---")