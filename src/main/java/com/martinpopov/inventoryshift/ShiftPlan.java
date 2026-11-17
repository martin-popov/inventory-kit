package com.martinpopov.inventoryshift;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which slot swaps rotate the player's inventory rows by one step.
 *
 * <p>Slot numbers are those of the player inventory menu: the three storage
 * rows are 9-17 (top), 18-26 (middle) and 27-35 (bottom, just above the
 * hotbar), and the hotbar is 36-44.
 *
 * <p>Each {@link Swap} is one "swap this slot with hotbar slot N" action, the
 * same thing the number keys do while hovering a slot. The hotbar slot of a
 * column acts as the temporary holder while that column's items rotate.
 */
public final class ShiftPlan {
	public static final int COLUMNS = 9;
	public static final int TOP_ROW_START = 9;
	public static final int MIDDLE_ROW_START = 18;
	public static final int BOTTOM_ROW_START = 27;
	public static final int HOTBAR_START = 36;

	private static final int[] FORWARD_ORDER = {TOP_ROW_START, MIDDLE_ROW_START, BOTTOM_ROW_START};
	private static final int[] REVERSE_ORDER = {BOTTOM_ROW_START, MIDDLE_ROW_START, TOP_ROW_START};

	private ShiftPlan() {
	}

	/** One swap between a storage slot and the hotbar slot at {@code hotbarIndex} (0-8). */
	public record Swap(int slot, int hotbarIndex) {
	}

	/**
	 * Forward: every row moves down one step. The bottom row becomes the
	 * hotbar and the old hotbar wraps round to the top row.
	 *
	 * <p>Reverse: every row moves up one step. The top row becomes the hotbar
	 * and the old hotbar becomes the bottom row.
	 */
	public static List<Swap> swaps(boolean reverse) {
		int[] order = reverse ? REVERSE_ORDER : FORWARD_ORDER;
		List<Swap> swaps = new ArrayList<>(COLUMNS * order.length);

		for (int column = 0; column < COLUMNS; column++) {
			for (int rowStart : order) {
				swaps.add(new Swap(rowStart + column, column));
			}
		}

		return swaps;
	}
}
