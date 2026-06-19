package org.galgame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChoiceDialog extends JDialog {
    private String selectedTarget = null;

    public ChoiceDialog(Frame owner, List<ChoiceOption> options) {
        super(owner, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 200));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(new Color(40, 40, 60));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 180, 255, 150), 2),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));
        cardPanel.setOpaque(true);

        for (ChoiceOption opt : options) {
            JButton btn = new JButton(opt.getText());
            btn.setFont(new Font("微软雅黑", Font.PLAIN, 22));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(80, 60, 150));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.setBackground(new Color(120, 80, 200));
                }
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.setBackground(new Color(80, 60, 150));
                }
            });
            btn.addActionListener(e -> {
                selectedTarget = opt.getTarget();
                dispose();
            });
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardPanel.add(btn);
            cardPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }

        mainPanel.add(cardPanel);
        setContentPane(mainPanel);
        setSize(500, 350);
        setLocationRelativeTo(owner);
    }

    public String getSelectedTarget() {
        return selectedTarget;
    }
}