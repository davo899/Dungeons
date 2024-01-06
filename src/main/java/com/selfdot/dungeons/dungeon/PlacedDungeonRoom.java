package com.selfdot.dungeons.dungeon;

import io.github.jdiemke.triangulation.Vector2D;
import org.joml.Vector2i;

public record PlacedDungeonRoom(DungeonRoom room, Vector2i position) {

    public Vector2D centre() {
        Vector2i size = room.getSize();
        return new Vector2D(position.x + (size.x / 2d), position.y + (size.y / 2d));
    }

}
