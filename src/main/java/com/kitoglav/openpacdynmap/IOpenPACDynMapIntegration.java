package com.kitoglav.openpacdynmap;

import com.coreoz.wisp.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerSet;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.concurrent.ThreadPoolExecutor;

public interface IOpenPACDynMapIntegration {
    MinecraftServer getServer();

    OpenPACServerAPI getOpenPACApi();

    DynmapCommonAPI getDynMapApi();

    Scheduler getScheduler();

    ThreadPoolExecutor getExecutor();

    MarkerSet getMarkerSet();
}
