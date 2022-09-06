/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.sebastianrasor.robinhood.mixininterface;

import com.sebastianrasor.robinhood.utils.misc.Vec3;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public interface IVec3d {
    void set(double x, double y, double z);

    default void set(Vec3i vec) {
        set(vec.getX(), vec.getY(), vec.getZ());
    }
    default void set(Vec3 vec) {
        set(vec.x, vec.y, vec.z);
    }
    default void set(Vec3d vec) {
        set(vec.x, vec.y, vec.z);
    }
    void setXZ(double x, double z);

    void setY(double y);
}
