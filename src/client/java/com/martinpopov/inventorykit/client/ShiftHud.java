package com.martinpopov.inventorykit.client;

import com.martinpopov.inventorykit.ShiftPlan;
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
	private static final int LOCK_COLOR = 0xFFFFD040;
	private static final int LOCK_BACKING = 0xE0000000;
	private static final String[] PADLOCK = {
			".###.",
			"#...#",
			"#####",
			"##.##",
			"#####"
	};

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || player.isSpectator()) {
			return;
		}

		InventoryKitConfig config = InventoryKitConfig.get();
		extractLockMarkers(graphics, config);

		if (shouldShowPreview(config)) {
			extractPreview(graphics, minecraft, player.getInventory(), config);
		}
	}

	private static void extractLockMarkers(GuiGraphicsExtractor graphics, InventoryKitConfig config) {
		int hotbarLeft = graphics.guiWidth() / 2 - 88;
		int itemTop = graphics.guiHeight() - 19;

		for (int column = 0; column < ShiftPlan.COLUMNS; column++) {
			if (!config.isLocked(column)) {
				continue;
			}

			int x = hotbarLeft + column * 20;

			// A bar the full width of the slot, under the item.
			graphics.fill(x, itemTop + 16, x + 16, itemTop + 18, LOCK_COLOR);

			// A padlock badge on a dark backing in the slot's top-left corner, so the lock does
			// not depend on colour alone and stays visible over any item.
			int badgeLeft = x - 2;
			int badgeTop = itemTop - 2;
			graphics.fill(badgeLeft, badgeTop, badgeLeft + PADLOCK[0].length() + 2, badgeTop + PADLOCK.length + 2, LOCK_BACKING);

			for (int row = 0; row < PADLOCK.length; row++) {
				for (int pixel = 0; pixel < PADLOCK[row].length(); pixel++) {
					if (PADLOCK[row].charAt(pixel) == '#') {
						graphics.fill(badgeLeft + 1 + pixel, badgeTop + 1 + row, badgeLeft + 2 + pixel, badgeTop + 2 + row, LOCK_COLOR);
					}
				}
			}
		}
	}

	private static boolean shouldShowPreview(InventoryKitConfig config) {
		return switch (config.previewMode) {
			case OFF -> false;
			case ALWAYS -> true;
			case WHEN_SHIFTING -> Shifter.recentlyShifted()
					|| config.columnModifier.isDown()
					|| (config.scrollEnabled && config.scrollModifier.isDown());
		};
	}

	private static void extractPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, Inventory inventory, InventoryKitConfig config) {
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
