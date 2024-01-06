package com.selfdot.dungeons.dungeon;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector2i;

import java.util.*;

import static com.selfdot.dungeons.DungeonsMod.TILE_SIZE;

public class DungeonRoom {

    private static final Random RANDOM = new Random();

    private final Vector2i size = new Vector2i();
    private Set<DungeonPassage> passages;

    public DungeonRoom(int sizeX, int sizeY) {
        this.size.x = sizeX;
        this.size.y = sizeY;
        List<DungeonPassage> passageList = new ArrayList<>();
        for (int x = 0; x < size.x; x++) {
            passageList.add(new DungeonPassage(x, 0, Side.BOTTOM));
            passageList.add(new DungeonPassage(x, size.y - 1, Side.TOP));
        }
        for (int y = 0; y < size.y; y++) {
            passageList.add(new DungeonPassage(0, y, Side.LEFT));
            passageList.add(new DungeonPassage(size.x - 1, y, Side.RIGHT));
        }
        passages = new HashSet<>(passageList);
    }

    public Vector2i getSize() {
        return size;
    }

    public Set<DungeonPassage> getPassages() {
        return passages;
    }

    public void randomizePassages() {
        List<DungeonPassage> passageList = new ArrayList<>(passages);
        int maxPassages = RANDOM.nextInt(1, 5);
        while (passageList.size() > maxPassages) {
            passageList.remove(RANDOM.nextInt(passageList.size()));
        }
        passages = new HashSet<>(passageList);
    }

    public void placeInWorld(World world, BlockPos corner, Set<DungeonPassage> allPassages, Vector2i position) {
        BlockState block = Blocks.STONE_BRICKS.getDefaultState();
        for (int x = 0; x < size.x * TILE_SIZE; x++) {
            for (int z = 0; z < size.y * TILE_SIZE; z++) {
                world.setBlockState(corner.add(x, 0, z), block);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < size.x * TILE_SIZE; x++) {
                world.setBlockState(corner.add(x, y, 0), block);
                world.setBlockState(corner.add(x, y, (size.y * TILE_SIZE) - 1), block);
            }
            for (int z = 0; z < size.y * TILE_SIZE; z++) {
                world.setBlockState(corner.add(0, y, z), block);
                world.setBlockState(corner.add((size.x * TILE_SIZE) - 1, y, z), block);
            }
        }
        passages.forEach(passage -> {
            if (allPassages.contains(DungeonPassage.complement(
                new DungeonPassage(position.x() + passage.x(), position.y() + passage.y(), passage.side())
            ))) {
                for (int y = 1; y <= 3; y++) {
                    for (int i = 1; i < TILE_SIZE - 1; i++) {
                        switch (passage.side()) {
                            case TOP -> world.setBlockState(
                                corner.add(
                                    (passage.x() * TILE_SIZE) + i,
                                    y,
                                    (passage.y() * TILE_SIZE) + TILE_SIZE - 1
                                ),
                                Blocks.AIR.getDefaultState()
                            );
                            case BOTTOM -> world.setBlockState(
                                corner.add(
                                    (passage.x() * TILE_SIZE) + i,
                                    y,
                                    passage.y() * TILE_SIZE
                                ),
                                Blocks.AIR.getDefaultState()
                            );
                            case LEFT -> world.setBlockState(
                                corner.add(
                                    passage.x() * TILE_SIZE,
                                    y,
                                    (passage.y() * TILE_SIZE) + i
                                ),
                                Blocks.AIR.getDefaultState()
                            );
                            case RIGHT -> world.setBlockState(
                                corner.add(
                                    (passage.x() * TILE_SIZE) + TILE_SIZE - 1,
                                    y,
                                    (passage.y() * TILE_SIZE) + i
                                ),
                                Blocks.AIR.getDefaultState()
                            );
                        }
                    }
                }
            }
        });
    }

    public static DungeonRoom random(int maxX, int maxY) {
        DungeonRoom room = new DungeonRoom(RANDOM.nextInt(maxX) + 2, RANDOM.nextInt(maxY) + 2);
        room.randomizePassages();
        return room;
    }

}
