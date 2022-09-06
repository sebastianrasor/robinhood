/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.sebastianrasor.robinhood.utils.misc;

public interface Producer<T> {
    T create();
}
