package com.selfdot.dungeons;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.selfdot.dungeons.command.TestCommand;
import com.selfdot.dungeons.dungeon.DungeonGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class DungeonsMod implements ModInitializer {

    public static final int TILE_SIZE = 7;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void registerCommands(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess commandRegistryAccess,
        CommandManager.RegistrationEnvironment registrationEnvironment
    ) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>
            literal("test")
            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>
                argument("n", integer())
                .executes(new TestCommand())
            )
        );
    }

    private void onTick(MinecraftServer server) {
        DungeonGenerator.getInstance().onTick();
    }

}
