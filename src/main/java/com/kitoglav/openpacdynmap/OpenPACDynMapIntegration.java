package com.kitoglav.openpacdynmap;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.SchedulerConfig;
import com.coreoz.wisp.schedule.Schedules;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kitoglav.openpacdynmap.tasks.UpdateClaimsTask;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.dynmap.*;
import org.dynmap.markers.MarkerSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpenPACDynMapIntegration extends DynmapCommonAPIListener implements ModInitializer, IOpenPACDynMapIntegration {
    public static final String MOD_ID = "openpac-dynmap-integration";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private volatile MinecraftServer server;
    private volatile OpenPACServerAPI openPacApi;
    private volatile DynmapCommonAPI dynmapApi;
    private volatile Scheduler scheduler;
    private volatile ThreadPoolExecutor executor;
    private volatile MarkerSet claimMarkerSet;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::serverStopped);
    }
    private void serverStarted(MinecraftServer server) {
        info("{} status: STARTING...", MOD_ID);
        this.server = server;
        DynmapCommonAPIListener.register(this);
        info("{} status: STARTED", MOD_ID);
    }

    private void serverStopped(MinecraftServer server) {
        info("{} status: STOPPING...", MOD_ID);
        this.server = null;
        DynmapCommonAPIListener.unregister(this);
        info("{} status: STOPPED", MOD_ID);

    }

    @Override
    public MinecraftServer getServer() {
        return this.server;
    }

    @Override
    public OpenPACServerAPI getOpenPACApi() {
        return this.openPacApi;
    }

    @Override
    public DynmapCommonAPI getDynMapApi() {
        return this.dynmapApi;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return this.executor;
    }

    @Override
    public MarkerSet getMarkerSet() {
        return this.claimMarkerSet;
    }

    @Override
    public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
        this.dynmapApi = dynmapCommonAPI;
        MarkerSet set = dynmapCommonAPI.getMarkerAPI().getMarkerSet(OpenPACDynMapIntegration.MOD_ID + ":claim");
        if (set != null) {
            set.deleteMarkerSet();
        }
        this.claimMarkerSet = dynmapCommonAPI.getMarkerAPI().createMarkerSet(OpenPACDynMapIntegration.MOD_ID + ":claim", "Claims", null, false);
        info("{} connected to DynMap API!", MOD_ID);
        this.openPacApi = OpenPACServerAPI.get(server);
        info("{} connected to OpenPartiesAndClaims API!", MOD_ID);
        this.scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).minThreads(1).threadFactory(() -> new ThreadFactoryBuilder().setNameFormat(MOD_ID + "-scheduler-thread-%d").build()).build());
        this.executor = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors() * 8, 500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat(MOD_ID + "-executor-thread-%d").build());
        this.scheduler.schedule(new UpdateClaimsTask(this), Schedules.afterInitialDelay(Schedules.fixedFrequencySchedule(Duration.ofSeconds(60)), Duration.ofSeconds(2)));
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        this.dynmapApi = null;
        this.claimMarkerSet = null;
        info("{} disconnected from DynMap API", MOD_ID);
        this.openPacApi = null;
        info("{} disconnected from OpenPartiesAndClaims API", MOD_ID);
        this.scheduler.gracefullyShutdown();
        this.executor.shutdownNow();
        info("{} shouted down executors", MOD_ID);


    }

    public static void error(Throwable e, Object... args) {
        error("Error occurred!", e, args);
    }
    public static void error(String message, Throwable e, Object... args) {
        LOGGER.error(message, e, args);
    }
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }
}