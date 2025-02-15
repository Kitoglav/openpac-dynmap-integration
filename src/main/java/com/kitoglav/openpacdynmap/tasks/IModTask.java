package com.kitoglav.openpacdynmap.tasks;

import com.kitoglav.openpacdynmap.IOpenPACDynMapIntegration;
import com.kitoglav.openpacdynmap.OpenPACDynMapIntegration;

public abstract class IModTask implements Runnable {
    protected final IOpenPACDynMapIntegration mod;

    public IModTask(IOpenPACDynMapIntegration mod) {
        this.mod = mod;
    }

    @Override
    public abstract void run();

}
