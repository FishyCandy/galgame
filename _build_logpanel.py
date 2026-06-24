import os
java_file = r'D:\ideaDocuments\galgame\src\main\java\org\galgame\LogPanel.java'
content = open(r'D:\ideaDocuments\galgame\_logpanel_java.txt', 'r', encoding='utf-8').read()
with open(java_file, 'w', encoding='utf-8') as f:
    f.write(content)
print('SUCCESS')