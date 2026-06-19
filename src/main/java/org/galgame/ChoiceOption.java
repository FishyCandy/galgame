package org.galgame;

import java.util.Map;

public class ChoiceOption {
    private String text;
    private String target;
    private Map<String, Integer> score;

    public ChoiceOption(String text, String target, Map<String, Integer> score) {
        this.text = text;
        this.target = target;
        this.score = score;
    }

    public String getText() { return text; }
    public String getTarget() { return target; }
    public Map<String, Integer> getScore() { return score; }
}