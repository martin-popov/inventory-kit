package com.martinpopov.inventorykit.client;

import java.util.ArrayList;
import java.util.List;

import com.martinpopov.inventorykit.ShiftPlan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Carries out row and column shifts on the player's inventory. */
public final class Shifter {
	/** Ticks the preview stays visible after a shift in "when shifting" mode. */
	private static final int PREVIEW_LINGER_TICKS = 50;
	private static final int[] ROW_STARTS = {0, ShiftPlan.TOP_ROW_START, ShiftPlan.MIDDLE_ROW_START, ShiftPlan.BOTTOM_ROW_START};

	private static int previewLingerTicks;

	private Shifter() {
	}

	public static void tick() {
		if (previewLingerTicks > 0) {
			previewLingerTicks--;
		}
	}

	public static boolean recentlyShifted() {
		return previewLingerTicks > 0;
	}

	/** Shifts every unlocked column. */
	public static void shiftRows(Minecraft minecraft, boolean reverse) {
		shift(minecraft, reverse, unlockedColumns());
	}

	/** Shifts only the column of the selected hotbar slot. */
	public static void shiftSelectedColumn(Minecraft minecraft, boolean reverse) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		int column = player.getInventory().getSelectedSlot();

		if (!isColumnLocked(column)) {
			shift(minecraft, reverse, new int[] {column});
		}
	}

	/** A column stays put when any of its four slots is locked, since shifting moves all four. */
	public static boolean isColumnLocked(int column) {
		InventoryKitConfig config = InventoryKitConfig.get();

		for (int rowStart : ROW_STARTS) {
			if (config.isLocked(rowStart + column)) {
				return true;
			}
		}

		return false;
	}

	public static int[] unlockedColumns() {
		int[] columns = new int[ShiftPlan.COLUMNS];
		int count = 0;

		for (int column = 0; column < ShiftPlan.COLUMNS; column++) {
			if (!isColumnLocked(column)) {
				columns[count++] = column;
			}
		}

		return java.util.Arrays.copyOf(columns, count);
	}

	/**
	 * How many steps a shift of these columns would take right now: 1 normally,
	 * more when empty rows are skipped, 0 when there is nothing to bring in.
	 */
	public static int plannedSteps(Inventory inventory, boolean reverse, int[] columns) {
		if (columns.length == 0) {
			return 0;
		}

		if (!InventoryKitConfig.get().skipEmptyRows) {
			return 1;
		}

		return ShiftPlan.stepsToFilledRow(
				reverse,
				rowHasItems(inventory, ShiftPlan.TOP_ROW_START, columns),
				rowHasItems(inventory, ShiftPlan.MIDDLE_ROW_START, columns),
				rowHasItems(inventory, ShiftPlan.BOTTOM_ROW_START, columns)
		);
	}

	private static boolean rowHasItems(Inventory inventory, int rowStart, int[] columns) {
		for (int column : columns) {
			// Storage slots 9-35 have the same numbers in the inventory and in the inventory menu.
			if (!inventory.getItem(rowStart + column).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private static void shift(Minecraft minecraft, boolean reverse, int[] columns) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || player.isSpectator() || ClickQueue.isBusy()) {
			return;
		}

		// Only act on the player's own inventory, never while a chest or other container is open.
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}

		// An item held on the cursor would get mixed into the swaps.
		if (!player.inventoryMenu.getCarried().isEmpty()) {
			return;
		}

		InventoryKitConfig config = InventoryKitConfig.get();

		// Swapping the item out of the player's hand part-way through eating, drawing a bow,
		// blocking or mining would cancel what they are doing.
		if (config.protectItemInUse && (player.isUsingItem() || minecraft.gameMode.isDestroying())) {
			return;
		}

		Inventory inventory = player.getInventory();
		int steps = plannedSteps(inventory, reverse, columns);
		int containerId = player.inventoryMenu.containerId;
		List<ClickQueue.Click> clicks = new ArrayList<>();

		for (ShiftPlan.Swap swap : ShiftPlan.swaps(reverse, steps, columns)) {
			clicks.add(new ClickQueue.Click(containerId, swap.slot(), swap.hotbarIndex(), ContainerInput.SWAP));
		}

		if (clicks.isEmpty()) {
			return;
		}

		previewLingerTicks = PREVIEW_LINGER_TICKS;

		if (config.sound) {
			float pitch = reverse ? 1.15F : 1.4F;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, pitch, 0.2F));
		}

		ClickQueue.submit(minecraft, clicks, () -> {
			if (!config.animation) {
				return;
			}

			for (int column : columns) {
				ItemStack stack = inventory.getItem(column);

				if (!stack.isEmpty()) {
					// The same little bounce items do when they are picked up.
					stack.setPopTime(5);
				}
			}
		});
	}
}
