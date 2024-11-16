package me.levitate.quill.menu.filler;

import lombok.Getter;

@Getter
public enum FillPattern {
    BORDER(new String[]{
        "XXXXXXXXX",
        "X       X",
        "X       X",
        "X       X",
        "X       X",
        "XXXXXXXXX"
    }),
    
    CORNERS(new String[]{
        "X       X",
        "         ",
        "         ",
        "         ",
        "         ",
        "X       X"
    }),
    
    TOP_BORDER(new String[]{
        "XXXXXXXXX",
        "         ",
        "         ",
        "         ",
        "         ",
        "         "
    }),

    LEFT_BORDER(new String[]{
            "X        ",
            "X        ",
            "X        ",
            "X        ",
            "X        ",
            "X        "
    }),

    RIGHT_BORDER(new String[]{
            "        X",
            "        X",
            "        X",
            "        X",
            "        X",
            "        X"
    }),
    
    BOTTOM_BORDER(new String[]{
        "         ",
        "         ",
        "         ",
        "         ",
        "         ",
        "XXXXXXXXX"
    });

    private final String[] pattern;

    FillPattern(String[] pattern) {
        this.pattern = pattern;
    }
}