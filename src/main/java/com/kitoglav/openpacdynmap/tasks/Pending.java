package com.kitoglav.openpacdynmap.tasks;

import com.kitoglav.openpacdynmap.shapes.ShapeHolder;
import net.minecraft.util.Identifier;

public record Pending(String id, ShapeHolder shape, String claimName, Identifier dimension, int color, String owner, boolean isGlobal) {

}
