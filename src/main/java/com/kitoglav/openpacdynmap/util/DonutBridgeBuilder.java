package com.kitoglav.openpacdynmap.util;

import com.flowpowered.math.vector.Vector2d;
import com.kitoglav.openpacdynmap.shapes.Shape;
import com.kitoglav.openpacdynmap.shapes.ShapeHolder;

import java.util.*;

public class DonutBridgeBuilder {

    public static Shape process(ShapeHolder holder) {
        List<Vector2d> exterior = sortPolygon(holder.baseShape().points(), true);
        List<List<Vector2d>> holes = new ArrayList<>();

        for (Shape hole : holder.holes()) {
            holes.add(sortPolygon(hole.points(), false));
        }

        List<Vector2d> currentPolygon = new ArrayList<>(exterior);
        for (List<Vector2d> hole : holes) {
            currentPolygon = bridgeHole(currentPolygon, hole);
        }

        return new Shape(currentPolygon);
    }

    private static List<Vector2d> bridgeHole(List<Vector2d> exterior, List<Vector2d> hole) {
        BridgePoint bridge = findClosestBridgePoints(exterior, hole);

        return buildBridgedPolygon(exterior, hole, bridge);
    }

    private static BridgePoint findClosestBridgePoints(List<Vector2d> exterior, List<Vector2d> hole) {
        double minDistance = Double.MAX_VALUE;
        BridgePoint bestBridge = null;

        for (int i = 0; i < exterior.size(); i++) {
            Vector2d p1 = exterior.get(i);
            for (int j = 0; j < hole.size(); j++) {
                Vector2d p2 = hole.get(j);
                double dist = p1.distance(p2);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestBridge = new BridgePoint(i, j);
                }
            }
        }

        return bestBridge;
    }

    private static List<Vector2d> buildBridgedPolygon(
            List<Vector2d> exterior,
            List<Vector2d> hole,
            BridgePoint bridge
    ) {

        List<Vector2d> newPolygon = new ArrayList<>(exterior.subList(0, bridge.exteriorIndex + 1));

        newPolygon.add(hole.get(bridge.holeIndex));

        newPolygon.addAll(hole.subList(bridge.holeIndex, hole.size()));
        newPolygon.addAll(hole.subList(0, bridge.holeIndex + 1));

        newPolygon.add(exterior.get(bridge.exteriorIndex));

        newPolygon.addAll(exterior.subList(bridge.exteriorIndex, exterior.size()));

        return newPolygon;
    }

    private static List<Vector2d> sortPolygon(List<Vector2d> points, boolean targetCCW) {
        double area = calculateSignedArea(points);
        boolean isCCW = area > 0;

        if (isCCW != targetCCW) {
            Collections.reverse(points);
        }

        return points;
    }

    private static double calculateSignedArea(List<Vector2d> points) {
        if (points.size() < 3) return 0.0;
        double sum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Vector2d current = points.get(i);
            Vector2d next = points.get((i + 1) % points.size());
            sum += (current.getX() * next.getY()) - (next.getX() * current.getY());
        }
        return sum / 2.0;
    }

    private static class BridgePoint {
        int exteriorIndex;
        int holeIndex;

        BridgePoint(int exteriorIndex, int holeIndex) {
            this.exteriorIndex = exteriorIndex;
            this.holeIndex = holeIndex;
        }
    }
}