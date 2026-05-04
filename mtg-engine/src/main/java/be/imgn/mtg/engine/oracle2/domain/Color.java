package be.imgn.mtg.engine.oracle2.domain;

/// MTG colors ({@mtg.rule 105}).
public enum Color implements Parseable {
    WHITE("White"),
    BLUE("Blue"),
    BLACK("Black"),
    RED("Red"),
    GREEN("Green");

    private final String text;

    Color(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
