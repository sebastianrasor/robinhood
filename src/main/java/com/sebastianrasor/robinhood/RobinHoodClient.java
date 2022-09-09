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

package com.sebastianrasor.robinhood;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sebastianrasor.robinhood.utils.entity.ProjectileEntitySimulator;
import com.sebastianrasor.robinhood.utils.misc.Pool;
import com.sebastianrasor.robinhood.utils.misc.Vec3;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class RobinHoodClient implements ClientModInitializer {
	public static MinecraftClient mc;
	private static final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
	private static final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
	private static final List<Path> paths = new ArrayList<>();

	public static boolean isRendering() {
		for (Path path : paths) {
			if (!path.points.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	@Override
	public void onInitializeClient() {
		mc = MinecraftClient.getInstance();

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register((context) -> {
			for (Path path : paths) path.clear();

			// Get item
			ItemStack itemStack = mc.player.getMainHandStack();
			if (itemStack == null) itemStack = mc.player.getOffHandStack();
			if (itemStack == null) return;
			if (!itemFilter(itemStack.getItem())) return;

			float tickDelta = context.tickDelta();
			// Calculate paths
			if (!simulator.set(mc.player, itemStack, 0, tickDelta)) return;
			getEmptyPath().calculate();

			if (itemStack.getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(Enchantments.MULTISHOT, itemStack) > 0) {
				if (!simulator.set(mc.player, itemStack, -10, tickDelta)) return;
				getEmptyPath().calculate();

				if (!simulator.set(mc.player, itemStack, 10, tickDelta)) return;
				getEmptyPath().calculate();
			}

			for (Path path : paths) path.render(context);
		});
	}

	private Path getEmptyPath() {
		for (Path path : paths) {
			if (path.points.isEmpty()) return path;
		}

		Path path = new Path();
		paths.add(path);
		return path;
	}

	private boolean itemFilter(Item item) {
		return item instanceof BowItem || item instanceof CrossbowItem || item instanceof FishingRodItem || item instanceof TridentItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
	}

	private class Path {
		private final List<Vec3> points = new ArrayList<>();

		public void clear() {
			for (Vec3 point : points) vec3s.free(point);
			points.clear();
		}

		public void calculate() {
			addPoint();

			for (int i = 0; i < 2000; i++) {
				HitResult result = simulator.tick();

				addPoint();

				if (result != null) {
					break;
				}
			}

		}

		private void addPoint() {
			points.add(vec3s.get().set(simulator.pos));
		}

		public void render(WorldRenderContext context) {
			RenderSystem.enableDepthTest();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO); // same blending as crosshair
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			RenderSystem.disableTexture();

			Vec3d cameraPos = context.camera().getPos();
			double cameraX = cameraPos.x;
			double cameraY = cameraPos.y;
			double cameraZ = cameraPos.z;


			RenderSystem.lineWidth(10F); // capped at 2.5 somewhere deep in the minecraft source
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

			for (Vec3 point : points) {
				bufferBuilder.vertex(point.x - cameraX, point.y - cameraY, point.z - cameraZ).color(1F, 1F, 1F, 1F).next();
			}

			tessellator.draw();
			RenderSystem.enableTexture();
		}
	}
}
