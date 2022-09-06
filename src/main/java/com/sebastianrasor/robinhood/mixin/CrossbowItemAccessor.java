/*
 * Robin Hood
 *
 * Copyright (C) 2022 Sebastian Rasor <https://www.sebastianrasor.com>
 * Copyright (C) 2021 Meteor Development <https://github.com/MeteorDevelopment>
 *
 * The following code is a derivative work of the code from the Meteor Client
 * distribution (https://github.com/MeteorDevelopment/meteor-client), which is
 * licensed GPLv3. This code therefore is also licensed under the terms of the
 * GNU Public License, version 3.
 */

package com.sebastianrasor.robinhood.mixin;

import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {
    @Invoker("getSpeed")
    static float getSpeed(ItemStack itemStack) {
        return CrossbowItem.hasProjectile(itemStack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }
}
