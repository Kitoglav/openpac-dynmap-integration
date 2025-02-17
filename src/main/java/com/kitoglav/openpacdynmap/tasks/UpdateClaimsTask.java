package com.kitoglav.openpacdynmap.tasks;

import com.flowpowered.math.vector.Vector2d;
import com.kitoglav.openpacdynmap.*;
import com.kitoglav.openpacdynmap.shapes.Shape;
import com.kitoglav.openpacdynmap.shapes.ShapeHolder;
import com.kitoglav.openpacdynmap.util.DynMapUtils;
import com.kitoglav.openpacdynmap.util.OpenPACClaimsUtils;
import org.dynmap.markers.*;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.player.config.PlayerConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpdateClaimsTask extends IModTask {
    private final BlockingQueue<IServerPlayerClaimInfoAPI> playersToDo = new LinkedBlockingQueue<>();
    private final ExecutorService executor;
    private volatile CompletableFuture<Void> loadingFuture;


    public UpdateClaimsTask(IOpenPACDynMapIntegration mod) {
        super(mod);
        this.executor = mod.getExecutor();
    }

    @Override
    public void run() {
        try {
            clearMarkers();
            start();
            loadingFuture.get(30, TimeUnit.SECONDS);
            processQueue();
        } catch (TimeoutException e) {
            OpenPACDynMapIntegration.error("Timeout loading player claims", e);
        } catch (InterruptedException e) {
            OpenPACDynMapIntegration.warn("Processing interrupted", e);
        } catch (ExecutionException e) {
            OpenPACDynMapIntegration.error("Error loading player claims", e.getCause());
        } finally {
            resetState();
        }
    }

    private void clearMarkers() {
        MarkerSet set = mod.getMarkerSet();
        Stream.of(set.getMarkers(), set.getAreaMarkers(), set.getCircleMarkers(), set.getPolyLineMarkers()).filter(Objects::nonNull).flatMap(Collection::stream).forEach(GenericMarker::deleteMarker);
    }

    private void processQueue() throws InterruptedException {
        IServerPlayerClaimInfoAPI player;
        while ((player = playersToDo.poll(1, TimeUnit.SECONDS)) != null) {
            if (player.getClaimCount() > 0) {
                executor.execute(new CollectPendingsForPlayer(player));
            }
        }
    }

    private void resetState() {
        playersToDo.clear();
        if (loadingFuture != null) {
            loadingFuture.cancel(true);
        }
        loadingFuture = null;
    }

    public void start() {
        loadingFuture = CompletableFuture.runAsync(() -> {
            mod.getOpenPACApi().getServerClaimsManager().getPlayerInfoStream().forEach(this::safeAddToQueue);
        }, mod.getServer());
    }

    private void safeAddToQueue(IServerPlayerClaimInfoAPI player) {
        try {
            if (!playersToDo.offer(player, 1, TimeUnit.SECONDS)) {
                OpenPACDynMapIntegration.warn("Queue is full, skipping player {}", player);
            }
        } catch (InterruptedException e) {
            OpenPACDynMapIntegration.warn("Processing interrupted", e);
        }
    }

    class CollectPendingsForPlayer extends IModTask {
        private final IServerPlayerClaimInfoAPI player;

        CollectPendingsForPlayer(IServerPlayerClaimInfoAPI player) {
            super(UpdateClaimsTask.this.mod);
            this.player = player;
        }

        @Override
        public void run() {
            String name = OpenPACClaimsUtils.getClaimName(player);
            Queue<Pending> pendings = new ArrayDeque<>();
            player.getStream().forEach(dimensionClaims -> {
                List<ShapeHolder> shapes = OpenPACClaimsUtils.createShapes(dimensionClaims.getValue().getStream().flatMap(IPlayerClaimPosListAPI::getStream).collect(Collectors.toSet()));
                for (int i = 0; i < shapes.size(); i++) {
                    ShapeHolder shape = shapes.get(i);
                    String id = name + "_" + i;
                    pendings.add(new Pending(id, shape, name, dimensionClaims.getKey(), player.getClaimsColor(), player.getPlayerUsername(), player.getPlayerId().equals(PlayerConfig.SERVER_CLAIM_UUID)));
                }
            });
            mod.getExecutor().execute(new IteratePendingsForPlayer(pendings));
        }

        class IteratePendingsForPlayer extends IModTask {
            private final Queue<Pending> pendings;

            IteratePendingsForPlayer(Queue<Pending> pendings) {
                super(UpdateClaimsTask.this.mod);
                this.pendings = pendings;
            }

            @Override
            public void run() {
                Pending pending;
                while ((pending = pendings.poll()) != null) {
                    handleRegion(pending, mod.getMarkerSet());
                }
            }

            private void handleRegion(Pending pending, MarkerSet markerSet) {
                String dim = DynMapUtils.getDynMapWorldName(pending.dimension(), mod.getServer());
                ShapeHolder shapeHolder = pending.shape();
                Shape base = shapeHolder.baseShape();
                Shape cutout = shapeHolder.cutoutShape();
                Shape[] holes = shapeHolder.holes();
                drawShape(base, markerSet, "outline", pending, dim, false);
                for (int i = 0; i < holes.length; i++) {
                    Shape hole = holes[i];
                    drawShape(hole, markerSet, "holeline_" + i, pending, dim, false);
                }
                drawShape(cutout, markerSet, "fill", pending, dim, true);
            }

            private void drawShape(Shape shape, MarkerSet markerSet, String markerPostfix, Pending pending, String dimName, boolean fillOrLine) {
                double[] x = shape.points().stream().mapToDouble(Vector2d::getX).toArray();
                double[] y = shape.points().stream().mapToDouble(Vector2d::getY).toArray();
                var m = markerSet.createAreaMarker(pending.id() + "_" + markerPostfix, pending.claimName(), false, dimName, x, y, false);
                addStyle(m, pending, fillOrLine);
                String desc = formatInfoWindow(m, pending);
                m.setDescription(desc);
            }

            private static final String DEF_INFOWINDOW_PLAYER = "<div class=\"infowindow\">" + "<span style=\"font-size:120%;\">" + "%regionname%</span><br/> " + "Owner: <span style=\"font-weight:bold;\">%playerowner%</span></div>";
            private static final String DEF_INFOWINDOW_SERVER = "<div class=\"infowindow\">" + "<span style=\"font-size:120%;\">" + "%regionname%</span><br/> " + "<span style=\"font-weight:bold;color:gold;\">GLOBAL</span></div>";

            private String formatInfoWindow(AreaMarker m, Pending pending) {
                String v = "<div class=\"regioninfo\">" + (pending.isGlobal() ? DEF_INFOWINDOW_SERVER : DEF_INFOWINDOW_PLAYER) + "</div>";
                v = v.replace("%regionname%", m.getLabel());
                if (!pending.isGlobal()) {
                    v = v.replace("%playerowner%", pending.owner());
                }
                return v;
            }

            private void addStyle(AreaMarker m, Pending pending, boolean isFilled) {
                int color = pending.color();
                if (isFilled) {
                    m.setFillStyle(0.35F, color);
                    m.setLineStyle(0, 0, 0);
                } else {
                    m.setFillStyle(0, 0);
                    m.setLineStyle(2, 1F, color);
                }
            }
        }
    }
}
