package com.martinpopov.inventoryshift.client;

import com.martinpopov.inventoryshift.ShiftPlan;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Draws the lock markers on the hotbar and the preview of the row that comes next. */
public final class ShiftHud implements HudElement {
	private static final int SLOT_SIZE = 18;
	private static final float PREVIEW_SCALE = 0.75F;
	private static final int PREVIEW_BACKGROUND = 0x66000000;
	private static final int LOCK_COLOR = 0xFFE0B040;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || player.isSpectator()) {
			return;
		}

		InventoryShiftConfig config = InventoryShiftConfig.get();
		extractLockMarkers(graphics, config);

		if (shouldShowPreview(config)) {
			extractPreview(graphics, minecraft, player.getInventory(), config);
		}
	}

	private static void extractLockMarkers(GuiGraphicsExtractor graphics, InventoryShiftConfig config) {
		int hotbarLeft = graphics.guiWidth() / 2 - 88;
		int y = graphics.guiHeight() - 3;

		for (int column = 0; column < ShiftPlan.COLUMNS; column++) {
			if (config.isLocked(column)) {
				int x = hotbarLeft + column * 20;
				graphics.fill(x + 4, y, x + 12, y + 1, LOCK_COLOR);
			}
		}
	}

	private static boolean shouldShowPreview(InventoryShiftConfig config) {
		return switch (config.previewMode) {
			case OFF -> false;
			case ALWAYS -> true;
			case WHEN_SHIFTING -> Shifter.recentlyShifted()
					|| config.columnModifier.isDown()
					|| (config.scrollEnabled && config.scrollModifier.isDown());
		};
	}

	private static void extractPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, Inventory inventory, InventoryShiftConfig config) {
		boolean reverse = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
		int[] columns = Shifter.unlockedColumns();
		int steps = Shifter.plannedSteps(inventory, reverse, columns);

		if (steps == 0) {
			return;
		}

		int rowStart = ShiftPlan.arrivingRowStart(reverse, steps);
		int width = ShiftPlan.COLUMNS * SLOT_SIZE;

		// Sit above the hearts, armour and the held item name, which are lower in creative mode.
		int clearance = minecraft.gameMode.canHurtPlayer() ? 62 : 48;
		float left = graphics.guiWidth() / 2.0F - width * PREVIEW_SCALE / 2.0F;
		float top = graphics.guiHeight() - clearance - config.previewOffsetY - SLOT_SIZE * PREVIEW_SCALE;

		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE);
		graphics.fill(0, 0, width, SLOT_SIZE, PREVIEW_BACKGROUND);

		for (int column : columns) {
			ItemStack stack = inventory.getItem(rowStart + column);

			if (!stack.isEmpty()) {
				int x = column * SLOT_SIZE + 1;
				graphics.item(stack, x, 1);
				graphics.itemDecorations(minecraft.font, stack, x, 1);
			}
		}

		graphics.pose().popMatrix();
	}
}
