package org.galgame;

public class ChoiceOption {
    private String text;
    private String target;

    public ChoiceOption(String text, String target) {
        this.text = text;
        this.target = target;
    }

    public String getText() { return text; }
    public String getTarget() { return target; }
}