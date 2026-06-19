package org.galgame;


import java.io.Serializable;

public class Choice implements Serializable {
    private static final long serialVersionUID = 1L;
    private String text;
    private int nextIndex;
    private String resultMessage;

    public Choice(String text, int nextIndex, String resultMessage) {
        this.text = text;
        this.nextIndex = nextIndex;
        this.resultMessage = resultMessage;
    }

    public String getText() { return text; }
    public int getNextIndex() { return nextIndex; }
    public String getResultMessage() { return resultMessage; }
}
