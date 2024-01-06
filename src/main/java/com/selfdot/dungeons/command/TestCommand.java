package com.selfdot.dungeons.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.selfdot.dungeons.dungeon.DungeonFloor;
import com.selfdot.dungeons.dungeon.DungeonRoom;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class TestCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        int n = IntegerArgumentType.getInteger(context, "n");
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        DungeonFloor dungeonFloor = new DungeonFloor(n, n);
        boolean hasSpace = true;
        while (hasSpace) hasSpace = dungeonFloor.placeRandomly(DungeonRoom.random(5, 5));
        dungeonFloor.connectAllRooms();
        dungeonFloor.placeInWorld(player.getWorld(), player.getBlockPos().add(0, -3, 0));
        return SINGLE_SUCCESS;
    }

}
