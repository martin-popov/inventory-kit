package com.martinpopov.inventorykit;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out the clicks that turn one arrangement of items into another that
 * holds exactly the same items. This is what "undo" runs: the arrangement
 * before an action is the target, and the arrangement now is the start.
 *
 * <p>Like {@link SortPlan}, the plan is built against a model of what clicks
 * do, and items are described by a type number. Two kinds of click are used:
 * a left-click picks up, puts down, tops up or swaps a whole stack, and a
 * right-click with a stack on the cursor puts down a single item.
 *
 * <p>It runs in two passes. The first clears every slot that holds the wrong
 * item, following the chain of displaced stacks from slot to slot. After it,
 * every item is in a slot that wants its type. The second evens out the
 * counts, moving the surplus of one stack into the shortfall of another.
 */
public final class RestorePlan {
	public static final int EMPTY = SortPlan.EMPTY;

	/** A guard against a planning loop; real plans need far fewer clicks. */
	private static final int MAX_CLICKS = 20000;

	private RestorePlan() {
	}

	/** One click on a slot; a right-click puts down a single item. */
	public record Click(int slot, boolean right) {
	}

	/**
	 * @throws IllegalStateException if the two arrangements do not hold the same items,
	 *                               or the target cannot be reached with plain clicks
	 */
	public static List<Click> clicks(int[] currentTypes, int[] currentCounts, int[] targetTypes, int[] targetCounts, int[] maxOfType) {
		int size = currentTypes.length;
		int[] totals = new int[maxOfType.length];

		for (int slot = 0; slot < size; slot++) {
			if (currentTypes[slot] != EMPTY) {
				totals[currentTypes[slot]] += currentCounts[slot];
			}

			if (targetTypes[slot] != EMPTY) {
				totals[targetTypes[slot]] -= targetCounts[slot];
			}
		}

		for (int total : totals) {
			if (total != 0) {
				throw new IllegalStateException("The items are not the same as before");
			}
		}

		Model model = new Model(currentTypes.clone(), currentCounts.clone(), targetTypes, targetCounts, maxOfType);

		// Pass 1: empty every slot that holds the wrong item.
		for (int slot = 0; slot < size; slot++) {
			if (model.type[slot] != EMPTY && model.type[slot] != targetTypes[slot]) {
				model.left(slot);
				model.placeCarried();
			}
		}

		// Pass 2: every item now sits in a slot that wants its type. Even out the counts.
		for (int slot = 0; slot < size; slot++) {
			while (model.shortfall(slot) > 0) {
				int donor = model.findSurplus(targetTypes[slot], slot);

				if (donor < 0) {
					throw new IllegalStateException("No spare items for slot " + slot);
				}

				int moved = Math.min(model.shortfall(slot), model.surplus(donor));
				model.left(donor);

				for (int i = 0; i < moved; i++) {
					model.right(slot);
				}

				// Whatever is still on the cursor goes back where it came from.
				if (model.carriedType != EMPTY) {
					model.left(donor);
				}
			}
		}

		for (int slot = 0; slot < size; slot++) {
			if (model.type[slot] != targetTypes[slot] || model.count[slot] != (targetTypes[slot] == EMPTY ? 0 : targetCounts[slot])) {
				throw new IllegalStateException("Slot " + slot + " did not end up as planned");
			}
		}

		if (model.carriedType != EMPTY) {
			throw new IllegalStateException("A stack was left on the cursor");
		}

		return model.clicks;
	}

	private static final class Model {
		final int[] type;
		final int[] count;
		final int[] targetType;
		final int[] targetCount;
		final int[] maxOfType;
		final List<Click> clicks = new ArrayList<>();
		int carriedType = EMPTY;
		int carriedCount;

		Model(int[] type, int[] count, int[] targetType, int[] targetCount, int[] maxOfType) {
			this.type = type;
			this.count = count;
			this.targetType = targetType;
			this.targetCount = targetCount;
			this.maxOfType = maxOfType;
		}

		/** How many more items of its target type this slot needs. */
		int shortfall(int slot) {
			if (targetType[slot] == EMPTY || (type[slot] != EMPTY && type[slot] != targetType[slot])) {
				return 0;
			}

			return targetCount[slot] - count[slot];
		}

		/** How many items this slot holds beyond what it should. */
		int surplus(int slot) {
			if (type[slot] == EMPTY) {
				return 0;
			}

			return type[slot] == targetType[slot] ? count[slot] - targetCount[slot] : count[slot];
		}

		int findSurplus(int wantedType, int except) {
			for (int slot = 0; slot < type.length; slot++) {
				if (slot != except && type[slot] == wantedType && surplus(slot) > 0) {
					return slot;
				}
			}

			return -1;
		}

		/** Puts down the stack on the cursor, following the chain of stacks it displaces. */
		void placeCarried() {
			while (carriedType != EMPTY) {
				int destination = -1;

				// First choice: a slot that wants this item and has room for some of it.
				for (int slot = 0; slot < type.length; slot++) {
					if (targetType[slot] == carriedType && shortfall(slot) > 0) {
						destination = slot;
						break;
					}
				}

				if (destination >= 0) {
					int needed = shortfall(destination);

					if (carriedCount <= needed) {
						left(destination);
					} else {
						for (int i = 0; i < needed; i++) {
							right(destination);
						}
					}

					continue;
				}

				// Otherwise: a slot that wants this item but holds something else. Swapping puts
				// this stack where it belongs and picks up the next one to place.
				for (int slot = 0; slot < type.length; slot++) {
					if (targetType[slot] == carriedType && type[slot] != EMPTY && type[slot] != carriedType) {
						destination = slot;
						break;
					}
				}

				if (destination < 0) {
					throw new IllegalStateException("Nowhere to put the stack on the cursor");
				}

				left(destination);
			}
		}

		private void record(int slot, boolean right) {
			if (clicks.size() >= MAX_CLICKS) {
				throw new IllegalStateException("The plan needs too many clicks");
			}

			clicks.add(new Click(slot, right));
		}

		void left(int slot) {
			record(slot, false);

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

		/** Puts one item from the cursor into an empty slot or a matching stack with room. */
		void right(int slot) {
			if (carriedType == EMPTY || (type[slot] != EMPTY && type[slot] != carriedType) || count[slot] >= maxOfType[carriedType]) {
				throw new IllegalStateException("Cannot put a single item into slot " + slot);
			}

			record(slot, true);
			type[slot] = carriedType;
			count[slot]++;
			carriedCount--;

			if (carriedCount == 0) {
				carriedType = EMPTY;
			}
		}
	}
}
