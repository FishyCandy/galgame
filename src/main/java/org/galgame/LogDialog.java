package org.galgame;


import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LogDialog extends JDialog {
    public LogDialog(Frame owner, List<Dialogue> dialogues, int currentIndex) {
        super(owner, "台词回顾", true);
        setSize(500, 400);
        setLocationRelativeTo(owner);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        logArea.setBackground(UIManager.getColor("Panel.background"));
        logArea.setForeground(UIManager.getColor("Label.foreground"));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= currentIndex && i < dialogues.size(); i++) {
            Dialogue d = dialogues.get(i);
            if (d.getType() == Dialogue.Type.NORMAL) {
                sb.append(d.getCharacter()).append("：").append(d.getLine()).append("\n");
            } else {
                sb.append("【选项】\n");
                if (d.getChoices() != null) {
                    for (Choice c : d.getChoices()) {
                        sb.append("  - ").append(c.getText()).append("\n");
                    }
                }
            }
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("关闭");
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}