package com.martinpopov.inventorykit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Works out the left-clicks that sort a group of slots.
 *
 * <p>The plan is built against a small model of what a left-click does in a
 * container: pick up a stack, put one down, top up a matching stack, or swap
 * with a different one. Sorting happens in two passes. The first merges
 * half-full stacks of the same item. The second walks the slots in order and
 * brings the right stack into each one.
 *
 * <p>Items are described by a type number so this class needs nothing from
 * the game: two stacks have the same type when they can stack together.
 */
public final class SortPlan {
	public static final int EMPTY = -1;

	private SortPlan() {
	}

	/**
	 * @param slotTypes  type number of the stack in each slot, or {@link #EMPTY}
	 * @param slotCounts how many items each slot holds
	 * @param maxOfType  largest stack size for each type
	 * @param rankOfType sort position of each type; lower comes first
	 * @return the slots to left-click, in order
	 * @throws IllegalStateException if the slots cannot be sorted with plain clicks
	 */
	public static List<Integer> clicks(int[] slotTypes, int[] slotCounts, int[] maxOfType, int[] rankOfType) {
		Model model = new Model(slotTypes.clone(), slotCounts.clone(), maxOfType);
		int size = slotTypes.length;

		// Pass 1: merge half-full stacks of the same type, working from the back.
		for (int from = size - 1; from > 0; from--) {
			for (int into = 0; into < from && model.isPartial(from); into++) {
				if (model.type[into] == model.type[from] && model.isPartial(into)) {
					model.click(from);
					model.click(into);

					if (model.carriedType != EMPTY) {
						model.click(from);
					}
				}
			}
		}

		// Pass 2: put the stacks in order. Within a type, fuller stacks go first.
		List<int[]> wanted = new ArrayList<>();

		for (int slot = 0; slot < size; slot++) {
			if (model.type[slot] != EMPTY) {
				wanted.add(new int[] {model.type[slot], model.count[slot]});
			}
		}

		wanted.sort(Comparator.<int[]>comparingInt(stack -> rankOfType[stack[0]]).thenComparingInt(stack -> -stack[1]));

		for (int slot = 0; slot < wanted.size(); slot++) {
			int type = wanted.get(slot)[0];
			int count = wanted.get(slot)[1];

			if (model.holds(slot, type, count)) {
				continue;
			}

			int source = -1;

			for (int candidate = slot + 1; candidate < size; candidate++) {
				if (model.holds(candidate, type, count)) {
					source = candidate;
					break;
				}
			}

			if (source < 0) {
				throw new IllegalStateException("No stack left for slot " + slot);
			}

			model.click(source);
			model.click(slot);

			if (model.carriedType != EMPTY) {
				model.click(source);
			}

			if (!model.holds(slot, type, count) || model.carriedType != EMPTY) {
				throw new IllegalStateException("Slot " + slot + " did not end up as planned");
			}
		}

		return model.clicks;
	}

	/** What a left-click does to a slot and the stack on the cursor. */
	private static final class Model {
		final int[] type;
		final int[] count;
		final int[] maxOfType;
		final List<Integer> clicks = new ArrayList<>();
		int carriedType = EMPTY;
		int carriedCount;

		Model(int[] type, int[] count, int[] maxOfType) {
			this.type = type;
			this.count = count;
			this.maxOfType = maxOfType;
		}

		boolean isPartial(int slot) {
			return type[slot] != EMPTY && count[slot] < maxOfType[type[slot]];
		}

		boolean holds(int slot, int wantedType, int wantedCount) {
			return type[slot] == wantedType && count[slot] == wantedCount;
		}

		void click(int slot) {
			clicks.add(slot);

			if (carriedType == EMPTY) {
				// Pick up whatever is there.
				carriedType = type[slot];
				carriedCount = count[slot];
				type[slot] = EMPTY;
				count[slot] = 0;
			} else if (type[slot] == EMPTY) {
				// Put the carried stack down.
				type[slot] = carriedType;
				count[slot] = carriedCount;
				carriedType = EMPTY;
				carriedCount = 0;
			} else if (type[slot] == carriedType) {
				// Top the slot up; whatever does not fit stays on the cursor.
				int moved = Math.min(carriedCount, maxOfType[carriedType] - count[slot]);
				count[slot] += moved;
				carriedCount -= moved;

				if (carriedCount == 0) {
					carriedType = EMPTY;
				}
			} else {
				// Different items swap places.
				int slotType = type[slot];
				int slotCount = count[slot];
				type[slot] = carriedType;
				count[slot] = carriedCount;
				carriedType = slotType;
				carriedCount = slotCount;
			}
		}
	}
}
