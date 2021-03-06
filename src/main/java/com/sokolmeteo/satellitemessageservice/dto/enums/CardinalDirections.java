package com.sokolmeteo.satellitemessageservice.dto.enums;

public enum CardinalDirections {
    NORTH("N;"),
    SOUTH("S;"),
    WEST("W;"),
    EAST("E;");

    private String literal;

    CardinalDirections(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

}
