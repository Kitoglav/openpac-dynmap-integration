package com.kitoglav.openpacdynmap.util;

import com.kitoglav.openpacdynmap.OpenPACDynMapIntegration;
import com.kitoglav.openpacdynmap.shapes.ShapeHolder;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.StringUtils;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;

import java.util.*;

public class OpenPACClaimsUtils {

    public static String getClaimName(IServerPlayerClaimInfoAPI playerClaimInfo) {
        String name = playerClaimInfo.getClaimsName();
        if (StringUtils.isBlank(name)) {
            name = playerClaimInfo.getPlayerUsername();
            if (name.length() > 2 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
                return name.substring(1, name.length() - 1) + " claim";
            } else {
                return name + "'s claim";
            }
        } else {
            return name;
        }
    }
    public static List<ShapeHolder> createShapes(Set<ChunkPos> chunks) {
        return createChunkGroups(chunks)
                .stream()
                .map(ShapeHolder::create)
                .toList();
    }
    public static List<Set<ChunkPos>> createChunkGroups(Set<ChunkPos> chunks) {
        final List<Set<ChunkPos>> result = new ArrayList<>();
        final Set<ChunkPos> visited = new HashSet<>();
        for (final ChunkPos chunk : chunks) {
            if (visited.contains(chunk)) continue;
            final Set<ChunkPos> neighbors = findNeighbors(chunk, chunks);
            result.add(neighbors);
            visited.addAll(neighbors);
        }
        return result;
    }
    public static Set<ChunkPos> findNeighbors(ChunkPos chunk, Set<ChunkPos> chunks) {
        if (!chunks.contains(chunk)) {
            OpenPACDynMapIntegration.error(new IllegalArgumentException("chunks must contain chunk to find neighbors!"));
            return null;
        }
        final Set<ChunkPos> visited = new HashSet<>();
        final Queue<ChunkPos> toVisit = new ArrayDeque<>();
        visited.add(chunk);
        toVisit.add(chunk);
        while (!toVisit.isEmpty()) {
            final ChunkPos visiting = toVisit.remove();
            for (final ChunkPosDirection dir : ChunkPosDirection.values()) {
                final ChunkPos offsetPos = dir.add(visiting);
                if (!chunks.contains(offsetPos) || !visited.add(offsetPos)) continue;
                toVisit.add(offsetPos);
            }
        }
        return visited;
    }
}
