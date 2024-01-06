package com.selfdot.dungeons.dungeon;

public record DungeonPassage(int x, int y, Side side) {

    public static DungeonPassage complement(DungeonPassage passage) {
        return switch (passage.side()) {
            case TOP -> new DungeonPassage(passage.x, passage.y + 1, Side.BOTTOM);
            case BOTTOM -> new DungeonPassage(passage.x, passage.y - 1, Side.TOP);
            case LEFT -> new DungeonPassage(passage.x - 1, passage.y, Side.RIGHT);
            case RIGHT -> new DungeonPassage(passage.x + 1, passage.y, Side.LEFT);
        };
    }

}
