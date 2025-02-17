package com.kitoglav.openpacdynmap.shapes;


import com.flowpowered.math.vector.Vector2d;
import com.kitoglav.openpacdynmap.util.ChunkPosDirection;
import com.kitoglav.openpacdynmap.util.DonutBridgeBuilder;
import com.kitoglav.openpacdynmap.util.OpenPACClaimsUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;
import java.util.stream.Collectors;

public record ShapeHolder(Shape baseShape, Shape[] holes) {

    public static ShapeHolder create(Set<ChunkPos> chunks) {
        return new ShapeHolder(
                createBaseShape(chunks),
                OpenPACClaimsUtils.createChunkGroups(cutoutChunks(chunks))
                        .stream()
                        .map(ShapeHolder::createBaseShape)
                        .toArray(Shape[]::new)
        );
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("baseShape", baseShape)
                .append("holes", holes)
                .toString();
    }

    private static Shape createBaseShape(Set<ChunkPos> chunks) {
        ChunkPos firstChunk = getBound(chunks, Math::min);
        while (!chunks.contains(firstChunk)) {
            // Step right until we hit a real chunk
            firstChunk = ChunkPosDirection.RIGHT.add(firstChunk);
        }
        final List<Vector2d> points = new ArrayList<>();
        points.add(new Vector2d(firstChunk.getStartX(), firstChunk.getStartZ()));

        ChunkPos current = firstChunk;
        ChunkPosDirection direction = ChunkPosDirection.RIGHT;
        do {
            points.add(vector(ChunkPosDirection.getCorner(current, direction, direction.getRight())));
            final ChunkPos next = direction.add(current);
            if (chunks.contains(next)) {
                final ChunkPos right = direction.getRight().add(next);
                if (chunks.contains(right)) {
                    current = right;
                    direction = direction.getRight();
                } else {
                    current = next;
                }
            } else {
                direction = direction.getLeft();
            }
        } while (!current.equals(firstChunk) || direction != ChunkPosDirection.RIGHT);

        return new Shape(simplifyPoints(points));
    }

    private static Set<ChunkPos> cutoutChunks(Set<ChunkPos> chunks) {
        final ChunkPos minChunk = getBound(chunks, Math::min);
        final ChunkPos maxChunk = getBound(chunks, Math::max);

        final Queue<ChunkPos> toVisit = new ArrayDeque<>();
        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                if (x > minChunk.x && x < maxChunk.x && z > minChunk.z && z < maxChunk.z) continue;
                final ChunkPos chunk = new ChunkPos(x, z);
                if (chunks.contains(chunk)) continue;
                toVisit.add(chunk);
            }
        }

        final Set<ChunkPos> outsideChunks = new HashSet<>(toVisit);
        while (!toVisit.isEmpty()) {
            final ChunkPos chunk = toVisit.remove();
            for (final ChunkPosDirection dir : ChunkPosDirection.values()) {
                final ChunkPos offsetPos = dir.add(chunk);
                if (
                        offsetPos.x < minChunk.x || offsetPos.x > maxChunk.x ||
                                offsetPos.z < minChunk.z || offsetPos.z > maxChunk.z ||
                                chunks.contains(offsetPos) || !outsideChunks.add(offsetPos)
                ) continue;
                toVisit.add(offsetPos);
            }
        }

        return ChunkPos.stream(minChunk, maxChunk)
                .filter(c -> !chunks.contains(c) && !outsideChunks.contains(c))
                .collect(Collectors.toSet());
    }

    private static Vector2d vector(BlockPos pos) {
        return new Vector2d(pos.getX(), pos.getZ());
    }

    private static List<Vector2d> simplifyPoints(List<Vector2d> points) {
        if (points.size() < 4) {
            return points;
        }

        final List<Vector2d> result = new ArrayList<>();
        result.add(points.getFirst());

        for (int i = 1; i < points.size() - 1; i++) {
            final Vector2d last = points.get(i - 1);
            final Vector2d point = points.get(i);
            final Vector2d next = points.get(i + 1);
            if (!point.sub(last).normalize().equals(next.sub(point).normalize())) {
                result.add(point);
            }
        }

        final Vector2d lastPoint = points.getLast();
        if (!lastPoint.equals(points.getFirst())) {
            result.add(lastPoint);
        }

        return result;
    }

    private static ChunkPos getBound(Iterable<ChunkPos> chunks, IntSelector selector) {
        final Iterator<ChunkPos> iterator = chunks.iterator();
        final ChunkPos first = iterator.next();
        int x = first.x;
        int z = first.z;
        while (iterator.hasNext()) {
            final ChunkPos pos = iterator.next();
            x = selector.select(x, pos.x);
            z = selector.select(z, pos.z);
        }
        return new ChunkPos(x, z);
    }

    public Shape cutoutShape() {
        return DonutBridgeBuilder.process(this);
    }

    @FunctionalInterface
    private interface IntSelector {
        int select(int a, int b);
    }
}