package com.kitoglav.openpacdynmap.shapes;

import com.flowpowered.math.vector.Vector2d;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public record Shape(List<Vector2d> points) {

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("points", points)
                .toString();
    }

}
