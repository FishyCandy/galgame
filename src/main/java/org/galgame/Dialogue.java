package org.galgame;


import java.io.Serializable;
import java.util.List;

public class Dialogue implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type { NORMAL, CHOICE }

    private Type type;
    private String character;
    private String line;
    private String imagePath;
    private List<Choice> choices;

    public Dialogue(String character, String line, String imagePath) {
        this.type = Type.NORMAL;
        this.character = character;
        this.line = line;
        this.imagePath = imagePath;
        this.choices = null;
    }

    public Dialogue(List<Choice> choices, String imagePath) {
        this.type = Type.CHOICE;
        this.character = "选项";
        this.line = "";
        this.imagePath = imagePath;
        this.choices = choices;
    }

    public Type getType() { return type; }
    public String getCharacter() { return character; }
    public String getLine() { return line; }
    public String getImagePath() { return imagePath; }
    public List<Choice> getChoices() { return choices; }
}
