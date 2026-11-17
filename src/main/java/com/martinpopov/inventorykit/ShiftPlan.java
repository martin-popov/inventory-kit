package com.martinpopov.inventorykit;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which slot swaps rotate the player's inventory rows.
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
	public static final int ROWS = 4;
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
	 * Swaps that rotate the given columns by {@code steps} steps.
	 *
	 * <p>Forward: every row moves down one step per step. The bottom row
	 * becomes the hotbar and the old hotbar wraps round to the top row.
	 *
	 * <p>Reverse: every row moves up. The top row becomes the hotbar and the
	 * old hotbar becomes the bottom row.
	 *
	 * <p>Three steps one way are sent as a single step the other way, since
	 * the result is the same and it takes a third of the clicks.
	 */
	public static List<Swap> swaps(boolean reverse, int steps, int... columns) {
		int normalized = Math.floorMod(steps, ROWS);

		if (normalized == 3) {
			reverse = !reverse;
			normalized = 1;
		}

		int[] order = reverse ? REVERSE_ORDER : FORWARD_ORDER;
		List<Swap> swaps = new ArrayList<>(normalized * columns.length * order.length);

		for (int step = 0; step < normalized; step++) {
			for (int column : columns) {
				for (int rowStart : order) {
					swaps.add(new Swap(rowStart + column, column));
				}
			}
		}

		return swaps;
	}

	/**
	 * How many steps it takes until a row with items lands in the hotbar.
	 *
	 * @return 1 to 3, or 0 when all three storage rows are empty
	 */
	public static int stepsToFilledRow(boolean reverse, boolean topFilled, boolean middleFilled, boolean bottomFilled) {
		boolean[] arrivalOrder = reverse
				? new boolean[] {topFilled, middleFilled, bottomFilled}
				: new boolean[] {bottomFilled, middleFilled, topFilled};

		for (int i = 0; i < arrivalOrder.length; i++) {
			if (arrivalOrder[i]) {
				return i + 1;
			}
		}

		return 0;
	}

	/** First slot of the storage row that lands in the hotbar after {@code steps} (1-3) steps. */
	public static int arrivingRowStart(boolean reverse, int steps) {
		int[] order = reverse ? FORWARD_ORDER : REVERSE_ORDER;
		return order[steps - 1];
	}
}
