package com.martinpopov.inventoryshift;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ShiftPlanTest {
	private static final int SLOT_COUNT = 46;
	private static final int[] ALL_COLUMNS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	/** A fake inventory where every slot holds its own slot number. */
	private static int[] freshInventory() {
		int[] slots = new int[SLOT_COUNT];

		for (int i = 0; i < SLOT_COUNT; i++) {
			slots[i] = i;
		}

		return slots;
	}

	private static void apply(int[] slots, boolean reverse, int steps, int... columns) {
		for (ShiftPlan.Swap swap : ShiftPlan.swaps(reverse, steps, columns)) {
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
		apply(after, false, 1, ALL_COLUMNS);

		assertArrayEquals(row(before, ShiftPlan.BOTTOM_ROW_START), row(after, ShiftPlan.HOTBAR_START));
		assertArrayEquals(row(before, ShiftPlan.MIDDLE_ROW_START), row(after, ShiftPlan.BOTTOM_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.TOP_ROW_START), row(after, ShiftPlan.MIDDLE_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.HOTBAR_START), row(after, ShiftPlan.TOP_ROW_START));
	}

	@Test
	void reverseMovesEveryRowUpOneStep() {
		int[] before = freshInventory();
		int[] after = freshInventory();
		apply(after, true, 1, ALL_COLUMNS);

		assertArrayEquals(row(before, ShiftPlan.TOP_ROW_START), row(after, ShiftPlan.HOTBAR_START));
		assertArrayEquals(row(before, ShiftPlan.MIDDLE_ROW_START), row(after, ShiftPlan.TOP_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.BOTTOM_ROW_START), row(after, ShiftPlan.MIDDLE_ROW_START));
		assertArrayEquals(row(before, ShiftPlan.HOTBAR_START), row(after, ShiftPlan.BOTTOM_ROW_START));
	}

	@Test
	void reverseUndoesForward() {
		int[] slots = freshInventory();
		apply(slots, false, 1, ALL_COLUMNS);
		apply(slots, true, 1, ALL_COLUMNS);

		assertArrayEquals(freshInventory(), slots);
	}

	@Test
	void fourStepsComeFullCircle() {
		int[] slots = freshInventory();

		for (int i = 0; i < 4; i++) {
			apply(slots, false, 1, ALL_COLUMNS);
		}

		assertArrayEquals(freshInventory(), slots);
		assertEquals(0, ShiftPlan.swaps(false, 4, ALL_COLUMNS).size());
	}

	@Test
	void multipleStepsMatchRepeatedSingleSteps() {
		for (boolean reverse : new boolean[] {false, true}) {
			for (int steps = 1; steps <= 3; steps++) {
				int[] repeated = freshInventory();

				for (int i = 0; i < steps; i++) {
					apply(repeated, reverse, 1, ALL_COLUMNS);
				}

				int[] atOnce = freshInventory();
				apply(atOnce, reverse, steps, ALL_COLUMNS);

				assertArrayEquals(repeated, atOnce, "steps=" + steps + " reverse=" + reverse);
			}
		}
	}

	@Test
	void threeStepsUseTheShortWayRound() {
		assertEquals(27, ShiftPlan.swaps(false, 3, ALL_COLUMNS).size());
		assertEquals(54, ShiftPlan.swaps(false, 2, ALL_COLUMNS).size());
	}

	@Test
	void onlyTheGivenColumnsMove() {
		int[] slots = freshInventory();
		apply(slots, false, 1, 4);

		for (int column = 0; column < ShiftPlan.COLUMNS; column++) {
			int hotbar = slots[ShiftPlan.HOTBAR_START + column];

			if (column == 4) {
				assertEquals(ShiftPlan.BOTTOM_ROW_START + column, hotbar);
			} else {
				assertEquals(ShiftPlan.HOTBAR_START + column, hotbar);
			}
		}

		assertEquals(3, ShiftPlan.swaps(false, 1, 4).size());
	}

	@Test
	void armourCraftingAndOffhandAreNeverTouched() {
		int[] slots = freshInventory();
		apply(slots, false, 1, ALL_COLUMNS);

		for (int slot = 0; slot < ShiftPlan.TOP_ROW_START; slot++) {
			assertEquals(slot, slots[slot]);
		}

		assertEquals(45, slots[45]);
	}

	@Test
	void stepsToFilledRowSkipsEmptyRows() {
		// Forward arrival order is bottom, middle, top.
		assertEquals(1, ShiftPlan.stepsToFilledRow(false, true, true, true));
		assertEquals(2, ShiftPlan.stepsToFilledRow(false, true, true, false));
		assertEquals(3, ShiftPlan.stepsToFilledRow(false, true, false, false));
		assertEquals(0, ShiftPlan.stepsToFilledRow(false, false, false, false));

		// Reverse arrival order is top, middle, bottom.
		assertEquals(1, ShiftPlan.stepsToFilledRow(true, true, false, false));
		assertEquals(2, ShiftPlan.stepsToFilledRow(true, false, true, true));
		assertEquals(3, ShiftPlan.stepsToFilledRow(true, false, false, true));
	}

	@Test
	void arrivingRowMatchesWhatTheSwapsDeliver() {
		for (boolean reverse : new boolean[] {false, true}) {
			for (int steps = 1; steps <= 3; steps++) {
				int[] slots = freshInventory();
				apply(slots, reverse, steps, ALL_COLUMNS);

				int expectedRowStart = ShiftPlan.arrivingRowStart(reverse, steps);
				assertArrayEquals(row(freshInventory(), expectedRowStart), row(slots, ShiftPlan.HOTBAR_START),
						"steps=" + steps + " reverse=" + reverse);
			}
		}
	}
}
