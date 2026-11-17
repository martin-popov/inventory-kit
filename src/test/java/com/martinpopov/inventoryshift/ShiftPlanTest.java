package com.martinpopov.inventoryshift;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ShiftPlanTest {
	private static final int SLOT_COUNT = 46;

	/** A fake inventory where every slot holds its own slot number. */
	private static int[] freshInventory() {
		int[] slots = new int[SLOT_COUNT];

		for (int i = 0; i < SLOT_COUNT; i++) {
			slots[i] = i;
		}

		return slots;
	}

	private static void apply(int[] slots, boolean reverse) {
		for (ShiftPlan.Swap swap : ShiftPlan.swaps(reverse)) {
			int hotbarSlot = ShiftPlan.HOTBAR_START + swap.hotbarIndex();
			int held = slots[hotbarSlot];
			slots[hotbarSlot] = slots[swap.slot()];
			slots[swap.slot()] = held;
		}
	}

	private static int[] row(int[] slots, int rowStart) {
		return Arrays.copyOfRange(slots, rowStart, rowStart + ShiftPlan.COLUMNS);
	}

	@Test
	void forwardMovesEveryRowDownOneStep() {
		int[] before = freshInventory();
		int[] after = freshInventory();
		apply(after, false);

		assertArrayEquals(row(before, ShiftPlan.BOTTOM_ROW_START), row(after, ShiftPlan.HOTBAR_START));
		assertArrayEquals(row(before, ShiftPlan.MIDDLE_ROW_START), row(after, ShiftPlan.BOTTOM_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.TOP_ROW_START), row(after, ShiftPlan.MIDDLE_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.HOTBAR_START), row(after, ShiftPlan.TOP_ROW_START));
	}

	@Test
	void reverseMovesEveryRowUpOneStep() {
		int[] before = freshInventory();
		int[] after = freshInventory();
		apply(after, true);

		assertArrayEquals(row(before, ShiftPlan.TOP_ROW_START), row(after, ShiftPlan.HOTBAR_START));
		assertArrayEquals(row(before, ShiftPlan.MIDDLE_ROW_START), row(after, ShiftPlan.TOP_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.BOTTOM_ROW_START), row(after, ShiftPlan.MIDDLE_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.HOTBAR_START), row(after, ShiftPlan.BOTTOM_ROW_START));
	}

	@Test
	void reverseUndoesForward() {
		int[] slots = freshInventory();
		apply(slots, false);
		apply(slots, true);

		assertArrayEquals(freshInventory(), slots);
	}

	@Test
	void fourStepsComeFullCircle() {
		int[] slots = freshInventory();

		for (int i = 0; i < 4; i++) {
			apply(slots, false);
		}

		assertArrayEquals(freshInventory(), slots);
	}

	@Test
	void onlyStorageAndHotbarSlotsAreTouched() {
		int[] slots = freshInventory();
		apply(slots, false);

		// Crafting grid, armour (0-8) and the offhand slot (45) stay put.
		for (int slot = 0; slot < ShiftPlan.TOP_ROW_START; slot++) {
			assertEquals(slot, slots[slot]);
		}

		assertEquals(45, slots[45]);
		assertEquals(27, ShiftPlan.swaps(false).size());
	}
}
