package com.flechazo.modernfurniture.block;

import net.minecraft.util.StringRepresentable;

public enum DisplayColor implements StringRepresentable {
    BLACK("black"),
    WHITE("white");

    private final String name;

    DisplayColor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
