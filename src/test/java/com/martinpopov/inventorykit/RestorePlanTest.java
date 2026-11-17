package com.martinpopov.inventorykit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class RestorePlanTest {
	private static final int EMPTY = RestorePlan.EMPTY;

	/** Replays clicks with the game's rules, failing on any click the planner should never make. */
	private static int[][] replay(int[] types, int[] counts, int[] maxOfType, List<RestorePlan.Click> clicks) {
		int[] type = types.clone();
		int[] count = counts.clone();
		int carriedType = EMPTY;
		int carriedCount = 0;

		for (RestorePlan.Click click : clicks) {
			int slot = click.slot();

			if (click.right()) {
				assertTrue(carriedType != EMPTY, "right-click with an empty cursor would split a stack");
				assertTrue(type[slot] == EMPTY || type[slot] == carriedType, "right-click on a different item would swap");
				assertTrue(count[slot] < maxOfType[carriedType], "right-click on a full stack does nothing");
				type[slot] = carriedType;
				count[slot]++;
				carriedCount--;

				if (carriedCount == 0) {
					carriedType = EMPTY;
				}
			} else if (carriedType == EMPTY) {
				carriedType = type[slot];
				carriedCount = count[slot];
				type[slot] = EMPTY;
				count[slot] = 0;
			} else if (type[slot] == EMPTY) {
				type[slot] = carriedType;
				count[slot] = carriedCount;
				carriedType = EMPTY;
				carriedCount = 0;
			} else if (type[slot] == carriedType) {
				int moved = Math.min(carriedCount, maxOfType[carriedType] - count[slot]);
				count[slot] += moved;
				carriedCount -= moved;

				if (carriedCount == 0) {
					carriedType = EMPTY;
				}
			} else {
				int slotType = type[slot];
				int slotCount = count[slot];
				type[slot] = carriedType;
				count[slot] = carriedCount;
				carriedType = slotType;
				carriedCount = slotCount;
			}
		}

		assertEquals(EMPTY, carriedType, "nothing may be left on the cursor");
		return new int[][] {type, count};
	}

	private static void assertRestores(int[] currentTypes, int[] currentCounts, int[] targetTypes, int[] targetCounts, int[] max, String label) {
		List<RestorePlan.Click> clicks = RestorePlan.clicks(currentTypes, currentCounts, targetTypes, targetCounts, max);
		int[][] result = replay(currentTypes, currentCounts, max, clicks);
		assertArrayEquals(targetTypes, result[0], label);
		assertArrayEquals(targetCounts, result[1], label);
	}

	@Test
	void undoesASort() {
		// Before: scattered. After sorting: stone merged and everything packed to the front.
		int[] before = {0, EMPTY, 1, 0, EMPTY, 2};
		int[] beforeCounts = {10, 0, 5, 60, 0, 1};
		int[] after = {0, 0, 1, 2, EMPTY, EMPTY};
		int[] afterCounts = {64, 6, 5, 1, 0, 0};

		assertRestores(after, afterCounts, before, beforeCounts, new int[] {64, 64, 1}, "sort");
	}

	@Test
	void undoesADepositThatMergedIntoAnExistingStack() {
		// Slots 0-1 are the chest, 2-3 the player. The player's 10 stone went onto the chest's 30.
		int[] before = {0, EMPTY, 0, 1};
		int[] beforeCounts = {30, 0, 10, 3};
		int[] after = {0, 1, EMPTY, EMPTY};
		int[] afterCounts = {40, 3, 0, 0};

		assertRestores(after, afterCounts, before, beforeCounts, new int[] {64, 64}, "deposit");
	}

	@Test
	void nothingToDoNeedsNoClicks() {
		int[] types = {0, EMPTY, 1};
		int[] counts = {5, 0, 1};

		assertEquals(List.of(), RestorePlan.clicks(types, counts, types, counts, new int[] {64, 1}));
	}

	@Test
	void refusesWhenTheItemsHaveChanged() {
		int[] current = {0, EMPTY};
		int[] currentCounts = {5, 0};
		int[] target = {0, EMPTY};
		int[] targetCounts = {6, 0};

		assertThrows(IllegalStateException.class, () -> RestorePlan.clicks(current, currentCounts, target, targetCounts, new int[] {64}));
	}

	/** Splits each type's total into random stacks and scatters them over the slots. */
	private static boolean scatter(Random random, int[] totals, int[] max, int[] types, int[] counts) {
		List<int[]> stacks = new ArrayList<>();

		for (int type = 0; type < totals.length; type++) {
			int left = totals[type];

			while (left > 0) {
				int size = random.nextInt(3) == 0 ? Math.min(left, max[type]) : 1 + random.nextInt(Math.min(left, max[type]));
				stacks.add(new int[] {type, size});
				left -= size;
			}
		}

		if (stacks.size() > types.length) {
			return false;
		}

		List<Integer> slots = new ArrayList<>();

		for (int slot = 0; slot < types.length; slot++) {
			slots.add(slot);
			types[slot] = EMPTY;
			counts[slot] = 0;
		}

		Collections.shuffle(slots, random);

		for (int i = 0; i < stacks.size(); i++) {
			types[slots.get(i)] = stacks.get(i)[0];
			counts[slots.get(i)] = stacks.get(i)[1];
		}

		return true;
	}

	@Test
	void randomArrangementsAreAlwaysRestored() {
		Random random = new Random(20261117);
		int[] stackSizes = {64, 16, 1};
		int tested = 0;

		for (int round = 0; round < 6000; round++) {
			int size = 1 + random.nextInt(63);
			int typeCount = 1 + random.nextInt(8);
			int[] max = new int[typeCount];
			int[] totals = new int[typeCount];

			for (int type = 0; type < typeCount; type++) {
				max[type] = stackSizes[random.nextInt(stackSizes.length)];
				totals[type] = random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(max[type] * 4);
			}

			int[] currentTypes = new int[size];
			int[] currentCounts = new int[size];
			int[] targetTypes = new int[size];
			int[] targetCounts = new int[size];

			// Includes completely full grids, where the cursor is the only free space.
			if (!scatter(random, totals, max, currentTypes, currentCounts) || !scatter(random, totals, max, targetTypes, targetCounts)) {
				continue;
			}

			assertRestores(currentTypes, currentCounts, targetTypes, targetCounts, max, "round " + round);
			tested++;
		}

		assertTrue(tested > 2000, "only " + tested + " arrangements were tested");
	}
}
