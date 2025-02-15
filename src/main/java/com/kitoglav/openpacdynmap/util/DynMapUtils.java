package com.kitoglav.openpacdynmap.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.ServerWorldProperties;

import java.util.function.Supplier;

public class DynMapUtils {
    public static String getDynMapWorldName(Identifier id, MinecraftServer server) {
        return getDynmapWorldName(id, () -> ((ServerWorldProperties) server.getOverworld().getLevelProperties()).getLevelName());
    }

    private static String getDynmapWorldName(Identifier id, Supplier<String> overworldNameGetter) {
        return switch (id.toString()) {
            case "minecraft:overworld" -> overworldNameGetter.get();
            case "minecraft:the_nether" -> "DIM-1";
            case "minecraft:the_end" -> "DIM1";
            default -> id.getNamespace() + "_" + id.getPath();
        };
    }
}
