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
import com.sebastianrasor.robinhood.utils.FakeWorld;
import com.sebastianrasor.robinhood.utils.entity.ProjectileEntitySimulator;
import com.sebastianrasor.robinhood.utils.misc.Pool;
import com.sebastianrasor.robinhood.utils.misc.Vec3;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
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
		AutoConfig.register(RobinHoodConfig.class, JanksonConfigSerializer::new);

		mc = MinecraftClient.getInstance();

		WorldRenderEvents.LAST.register((context) -> {
			for (Path path : paths) path.clear();

			// Get item
			ClientPlayerEntity player = mc.player;
			HitResult hit = mc.crosshairTarget;
			ItemStack itemStack = player.getMainHandStack();

			// see if useOnBlock is overwritten from Item
			boolean useOnBlockOverwritten;
			try {
				Class<?> itemClass = itemStack.getItem().getClass();
				Method method = itemClass.getMethod("useOnBlock", ItemUsageContext.class);
				Class<?> declaringClass = method.getDeclaringClass();
				useOnBlockOverwritten = declaringClass != Item.class;
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}

			// simulate usage in a fake world to see if it's successful
			ActionResult result = ActionResult.FAIL;
			if (useOnBlockOverwritten && hit.getType() == Type.BLOCK) {
				FakeWorld fakeWorld = new FakeWorld();
				ItemUsageContext itemUsageContext = new ItemUsageContext(fakeWorld, player, Hand.MAIN_HAND, itemStack, (BlockHitResult) hit);
				result = itemStack.useOnBlock(itemUsageContext);
			}

			boolean isBucket = itemStack.getItem() instanceof FluidModificationItem;
			if (itemStack.isEmpty()) itemStack = player.getOffHandStack();
			if (itemStack.getUseAction() == UseAction.NONE && !useOnBlockOverwritten && !isBucket) itemStack = player.getOffHandStack();
			if (useOnBlockOverwritten && !result.isAccepted()) itemStack = player.getOffHandStack();
			if (isBucket && hit.getType() != Type.BLOCK) itemStack = player.getOffHandStack();

			if (!itemFilter(itemStack.getItem()));

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
			RobinHoodConfig config = AutoConfig.getConfigHolder(RobinHoodConfig.class).getConfig();

			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
			RenderSystem.enableDepthTest();
			if (config.useBlending) {
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO); // same blending as crosshair
			}
			RenderSystem.lineWidth(config.lineWidth);

			Vec3d cameraPosition = context.camera().getPos();

			bufferBuilder.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.LINES);

			Vec3 normal = null;
			int i = 0;
			for (Vec3 point : points) {
				i++;

				point.subtract(cameraPosition);

				if (i < points.size()) {
					Vec3 nextPoint = points.get(i);
					nextPoint = new Vec3(nextPoint.x, nextPoint.y, nextPoint.z);
					nextPoint.subtract(cameraPosition);
					normal = nextPoint.subtract(point).normalize();
				}

				bufferBuilder
						.vertex((float)point.x, (float)point.y, (float)point.z)
						.color(config.lineColor)
						.normal((float)normal.x, (float)normal.y, (float)normal.z)
						.next();
			}
			tessellator.draw();
		}
	}
}
