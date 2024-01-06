package com.selfdot.dungeons.dungeon;

import java.util.ArrayDeque;
import java.util.Queue;

public class DungeonGenerator {

    private static final DungeonGenerator INSTANCE = new DungeonGenerator();
    private DungeonGenerator() { }
    public static DungeonGenerator getInstance() {
        return INSTANCE;
    }

    private static final int SLOWNESS = 5;

    private final Queue<Runnable> generationQueue = new ArrayDeque<>();
    private int tickCount = 0;

    public void addTask(Runnable task) {
        generationQueue.add(task);
    }

    public void onTick() {
        if (SLOWNESS <= 0) {
            while (!generationQueue.isEmpty()) generationQueue.poll().run();
            return;
        }
        if (++tickCount % SLOWNESS != 0) return;
        if (!generationQueue.isEmpty()) generationQueue.poll().run();
    }

}
