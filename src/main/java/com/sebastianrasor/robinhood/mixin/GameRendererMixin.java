package com.sebastianrasor.robinhood.mixin;

import com.sebastianrasor.robinhood.RobinHoodClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
  @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
  private void bobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
    if (RobinHoodClient.isRendering()) ci.cancel();
  }
}