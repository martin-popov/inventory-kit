package com.martinpopov.inventorykit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SortPlanTest {
	private static final int EMPTY = SortPlan.EMPTY;

	/** Replays clicks on a copy of the slots, with the same rules the game uses for a left-click. */
	private static int[][] replay(int[] types, int[] counts, int[] maxOfType, List<Integer> clicks) {
		int[] type = types.clone();
		int[] count = counts.clone();
		int carriedType = EMPTY;
		int carriedCount = 0;

		for (int slot : clicks) {
			if (carriedType == EMPTY) {
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

	private static int[] totals(int[] types, int[] counts, int typeCount) {
		int[] totals = new int[typeCount];

		for (int slot = 0; slot < types.length; slot++) {
			if (types[slot] != EMPTY) {
				totals[types[slot]] += counts[slot];
			}
		}

		return totals;
	}

	private static void assertSorted(int[] types, int[] counts, int[] maxOfType, int[] rankOfType) {
		boolean seenEmpty = false;
		boolean[] partialSeen = new boolean[maxOfType.length];

		for (int slot = 0; slot < types.length; slot++) {
			if (types[slot] == EMPTY) {
				seenEmpty = true;
				continue;
			}

			assertTrue(!seenEmpty, "empty slots must all be at the end");

			if (counts[slot] < maxOfType[types[slot]]) {
				assertTrue(!partialSeen[types[slot]], "at most one half-full stack per item");
				partialSeen[types[slot]] = true;
			}

			if (slot > 0) {
				int previousRank = rankOfType[types[slot - 1]];
				int rank = rankOfType[types[slot]];
				assertTrue(previousRank <= rank, "items must be in rank order");

				if (previousRank == rank) {
					assertTrue(counts[slot - 1] >= counts[slot], "fuller stacks come first");
				}
			}
		}
	}

	@Test
	void sortsASmallMixedGroup() {
		// Types: 0 = stone (rank 1), 1 = dirt (rank 0), 2 = sword (rank 2, unstackable).
		int[] types = {0, EMPTY, 1, 2, 0, 1, EMPTY, 2, 0};
		int[] counts = {10, 0, 64, 1, 60, 5, 0, 1, 64};
		int[] max = {64, 64, 1};
		int[] rank = {1, 0, 2};

		int[][] result = replay(types, counts, max, SortPlan.clicks(types, counts, max, rank));

		assertArrayEquals(new int[] {1, 1, 0, 0, 0, 2, 2, EMPTY, EMPTY}, result[0]);
		assertArrayEquals(new int[] {64, 5, 64, 64, 6, 1, 1, 0, 0}, result[1]);
	}

	@Test
	void alreadySortedNeedsNoClicks() {
		int[] types = {0, 0, 1, EMPTY};
		int[] counts = {64, 3, 1, 0};
		int[] max = {64, 1};
		int[] rank = {0, 1};

		assertEquals(List.of(), SortPlan.clicks(types, counts, max, rank));
	}

	@Test
	void emptyGroupNeedsNoClicks() {
		int[] types = {EMPTY, EMPTY, EMPTY};
		int[] counts = {0, 0, 0};

		assertEquals(List.of(), SortPlan.clicks(types, counts, new int[0], new int[0]));
	}

	@Test
	void randomGroupsAlwaysEndUpSortedWithNothingLost() {
		Random random = new Random(20261117);
		int[] stackSizes = {64, 16, 1};

		for (int round = 0; round < 3000; round++) {
			int size = 1 + random.nextInt(54);
			int typeCount = 1 + random.nextInt(8);
			int[] max = new int[typeCount];
			int[] rank = new int[typeCount];

			for (int type = 0; type < typeCount; type++) {
				max[type] = stackSizes[random.nextInt(stackSizes.length)];
				rank[type] = type;
			}

			// Shuffle the ranks so type numbers and sort order are unrelated.
			for (int type = typeCount - 1; type > 0; type--) {
				int other = random.nextInt(type + 1);
				int held = rank[type];
				rank[type] = rank[other];
				rank[other] = held;
			}

			int[] types = new int[size];
			int[] counts = new int[size];

			for (int slot = 0; slot < size; slot++) {
				if (random.nextInt(4) == 0) {
					types[slot] = EMPTY;
				} else {
					types[slot] = random.nextInt(typeCount);
					counts[slot] = random.nextBoolean() ? max[types[slot]] : 1 + random.nextInt(max[types[slot]]);
				}
			}

			List<Integer> clicks = SortPlan.clicks(types, counts, max, rank);
			int[][] result = replay(types, counts, max, clicks);

			assertSorted(result[0], result[1], max, rank);
			assertArrayEquals(totals(types, counts, typeCount), totals(result[0], result[1], typeCount), "round " + round);
		}
	}
}
