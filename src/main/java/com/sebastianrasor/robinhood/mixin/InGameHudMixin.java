package com.sebastianrasor.robinhood.mixin;

import com.sebastianrasor.robinhood.RobinHoodClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
  @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
  private void renderCrosshair(MatrixStack matrices, CallbackInfo ci) {
    if (RobinHoodClient.isRendering()) ci.cancel();
  }
}