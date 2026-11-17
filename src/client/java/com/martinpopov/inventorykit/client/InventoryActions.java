package com.martinpopov.inventorykit.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.martinpopov.inventorykit.RestorePlan;
import com.martinpopov.inventorykit.SortPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Moving items between the player and an open container, sorting either side, and undoing that. */
public final class InventoryActions {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventorykit");
	private static final int HOTBAR_SIZE = 9;
	public static final int INVENTORY_SIZE = 36;

	/** Where everything was before the last deposit, take or sort. */
	private record Snapshot(AbstractContainerMenu menu, List<Slot> slots, List<ItemStack> stacks) {
	}

	private static Snapshot lastAction;

	private InventoryActions() {
	}

	/** Chests, barrels, shulker boxes, hoppers, dispensers and droppers. */
	public static boolean isStorage(AbstractContainerMenu menu) {
		return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu || menu instanceof HopperMenu;
	}

	/** Whether this slot is one of the player's 36 inventory slots (not armour or the offhand). */
	public static boolean isInventorySlot(Slot slot) {
		return slot.container instanceof Inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < INVENTORY_SIZE;
	}

	/** The slots of the open chest, barrel, shulker box and so on. */
	public static List<Slot> containerSlots(AbstractContainerMenu menu) {
		List<Slot> slots = new ArrayList<>();

		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof Inventory)) {
				slots.add(slot);
			}
		}

		return slots;
	}

	/** The player's 27 storage slots, top-left to bottom-right. */
	public static List<Slot> storageSlots(AbstractContainerMenu menu) {
		return playerSlots(menu, HOTBAR_SIZE, INVENTORY_SIZE);
	}

	public static List<Slot> hotbarSlots(AbstractContainerMenu menu) {
		return playerSlots(menu, 0, HOTBAR_SIZE);
	}

	private static List<Slot> playerSlots(AbstractContainerMenu menu, int from, int to) {
		List<Slot> slots = new ArrayList<>();

		for (Slot slot : menu.slots) {
			if (slot.container instanceof Inventory && slot.getContainerSlot() >= from && slot.getContainerSlot() < to) {
				slots.add(slot);
			}
		}

		slots.sort(Comparator.comparingInt(Slot::getContainerSlot));
		return slots;
	}

	private static boolean isLocked(Slot slot) {
		return isInventorySlot(slot) && InventoryKitConfig.get().isLocked(slot.getContainerSlot());
	}

	/**
	 * Moves items from the player into the container. Locked slots stay put.
	 *
	 * @param matchingOnly only move items the container already holds
	 */
	public static void deposit(Minecraft minecraft, AbstractContainerMenu menu, boolean matchingOnly) {
		List<Slot> sources = new ArrayList<>(storageSlots(menu));

		if (InventoryKitConfig.get().depositFromHotbar) {
			sources.addAll(hotbarSlots(menu));
		}

		sources.removeIf(InventoryActions::isLocked);
		quickMove(minecraft, menu, sources, matchingOnly ? containerSlots(menu) : null);
	}

	/**
	 * Moves items from the container to the player.
	 *
	 * @param matchingOnly only take items the player already carries
	 */
	public static void take(Minecraft minecraft, AbstractContainerMenu menu, boolean matchingOnly) {
		List<Slot> playerSide = new ArrayList<>(storageSlots(menu));
		playerSide.addAll(hotbarSlots(menu));
		quickMove(minecraft, menu, containerSlots(menu), matchingOnly ? playerSide : null);
	}

	/** Shift-clicks each source slot. With {@code mustMatch} set, only items already present there are moved. */
	private static void quickMove(Minecraft minecraft, AbstractContainerMenu menu, List<Slot> sources, List<Slot> mustMatch) {
		LocalPlayer player = minecraft.player;

		if (!canAct(minecraft, menu)) {
			return;
		}

		List<ItemStack> wanted = null;

		if (mustMatch != null) {
			wanted = new ArrayList<>();

			for (Slot slot : mustMatch) {
				if (slot.hasItem()) {
					wanted.add(slot.getItem());
				}
			}
		}

		List<ClickQueue.Click> clicks = new ArrayList<>();

		for (Slot slot : sources) {
			ItemStack stack = slot.getItem();

			if (stack.isEmpty() || !slot.mayPickup(player)) {
				continue;
			}

			if (wanted != null && !containsSame(wanted, stack)) {
				continue;
			}

			clicks.add(new ClickQueue.Click(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE));
		}

		run(minecraft, menu, clicks, null);
	}

	private static boolean containsSame(List<ItemStack> stacks, ItemStack stack) {
		for (ItemStack other : stacks) {
			if (ItemStack.isSameItemSameComponents(other, stack)) {
				return true;
			}
		}

		return false;
	}

	/** Sorts the given slots in place, merging half-full stacks first. Locked slots are left alone. */
	public static void sort(Minecraft minecraft, AbstractContainerMenu menu, List<Slot> candidates) {
		LocalPlayer player = minecraft.player;

		if (!canAct(minecraft, menu)) {
			return;
		}

		List<Slot> slots = new ArrayList<>();

		for (Slot slot : candidates) {
			ItemStack stack = slot.getItem();

			// Bundles react to being clicked with an item (they swallow it), so they stay exactly
			// where they are, like locked slots, and the other items are sorted around them.
			if (isLocked(slot) || stack.has(DataComponents.BUNDLE_CONTENTS)) {
				continue;
			}

			if (!stack.isEmpty() && !slot.mayPickup(player)) {
				return;
			}

			slots.add(slot);
		}

		Kinds kinds = new Kinds();
		int[] slotTypes = new int[slots.size()];
		int[] slotCounts = new int[slots.size()];

		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getItem();
			slotTypes[i] = kinds.of(stack, slots.get(i));
			slotCounts[i] = stack.getCount();
		}

		Comparator<ItemStack> order = ItemOrder.comparator(minecraft, InventoryKitConfig.get().sortMode);
		List<Integer> kindsInOrder = new ArrayList<>();

		for (int kind = 0; kind < kinds.stacks.size(); kind++) {
			kindsInOrder.add(kind);
		}

		kindsInOrder.sort((a, b) -> order.compare(kinds.stacks.get(a), kinds.stacks.get(b)));
		int[] rankOfKind = new int[kinds.stacks.size()];

		for (int rank = 0; rank < kindsInOrder.size(); rank++) {
			rankOfKind[kindsInOrder.get(rank)] = rank;
		}

		List<ClickQueue.Click> clicks = new ArrayList<>();

		try {
			for (int index : SortPlan.clicks(slotTypes, slotCounts, kinds.maxSizes(), rankOfKind)) {
				clicks.add(new ClickQueue.Click(menu.containerId, slots.get(index).index, 0, ContainerInput.PICKUP));
			}
		} catch (IllegalStateException e) {
			// Nothing has been clicked yet, so the inventory is untouched.
			LOGGER.warn("Could not plan a sort: {}", e.getMessage());
			return;
		}

		run(minecraft, menu, clicks, () -> dropCarriedInto(minecraft, menu, slots));
	}

	public static boolean canUndo(AbstractContainerMenu menu) {
		return lastAction != null && lastAction.menu == menu && !ClickQueue.isBusy();
	}

	/** Puts every item back where it was before the last deposit, take or sort in this screen. */
	public static void undo(Minecraft minecraft, AbstractContainerMenu menu) {
		if (!canUndo(menu) || !canAct(minecraft, menu)) {
			return;
		}

		Snapshot snapshot = lastAction;
		lastAction = null;

		List<Slot> slots = new ArrayList<>();
		List<ItemStack> targets = new ArrayList<>();

		for (int i = 0; i < snapshot.slots.size(); i++) {
			Slot slot = snapshot.slots.get(i);
			ItemStack now = slot.getItem();
			ItemStack before = snapshot.stacks.get(i);

			// Bundles must never be clicked with an item. They are skipped, which is only safe
			// if they have not moved.
			if (now.has(DataComponents.BUNDLE_CONTENTS) || before.has(DataComponents.BUNDLE_CONTENTS)) {
				if (!ItemStack.matches(now, before)) {
					return;
				}

				continue;
			}

			slots.add(slot);
			targets.add(before);
		}

		Kinds kinds = new Kinds();
		int size = slots.size();
		int[] currentTypes = new int[size];
		int[] currentCounts = new int[size];
		int[] targetTypes = new int[size];
		int[] targetCounts = new int[size];

		for (int i = 0; i < size; i++) {
			ItemStack now = slots.get(i).getItem();
			currentTypes[i] = kinds.of(now, slots.get(i));
			currentCounts[i] = now.getCount();
			targetTypes[i] = kinds.of(targets.get(i), slots.get(i));
			targetCounts[i] = targets.get(i).getCount();
		}

		List<ClickQueue.Click> clicks = new ArrayList<>();

		try {
			for (RestorePlan.Click click : RestorePlan.clicks(currentTypes, currentCounts, targetTypes, targetCounts, kinds.maxSizes())) {
				clicks.add(new ClickQueue.Click(menu.containerId, slots.get(click.slot()).index, click.right() ? 1 : 0, ContainerInput.PICKUP));
			}
		} catch (IllegalStateException e) {
			// Items were added or removed since, so there is nothing sensible to go back to.
			LOGGER.info("Could not undo: {}", e.getMessage());
			return;
		}

		ClickQueue.submit(minecraft, clicks, () -> dropCarriedInto(minecraft, menu, slots));
	}

	/** Forgets the last action, for example when its screen closes. */
	public static void forget(AbstractContainerMenu menu) {
		if (lastAction != null && lastAction.menu == menu) {
			lastAction = null;
		}
	}

	/** Remembers where everything is, then sends the clicks. */
	private static void run(Minecraft minecraft, AbstractContainerMenu menu, List<ClickQueue.Click> clicks, Runnable whenDone) {
		if (clicks.isEmpty()) {
			return;
		}

		List<Slot> slots = new ArrayList<>();

		if (isStorage(menu)) {
			slots.addAll(containerSlots(menu));
		}

		slots.addAll(storageSlots(menu));
		slots.addAll(hotbarSlots(menu));
		List<ItemStack> stacks = new ArrayList<>();

		for (Slot slot : slots) {
			stacks.add(slot.getItem().copy());
		}

		lastAction = new Snapshot(menu, slots, stacks);
		ClickQueue.submit(minecraft, clicks, whenDone);
	}

	/** Should never be needed, but never leave the player holding a stack they did not pick up. */
	private static void dropCarriedInto(Minecraft minecraft, AbstractContainerMenu menu, List<Slot> slots) {
		LocalPlayer player = minecraft.player;

		if (player == null || player.containerMenu != menu || menu.getCarried().isEmpty()) {
			return;
		}

		for (Slot slot : slots) {
			if (!slot.hasItem()) {
				minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, player);
				return;
			}
		}
	}

	private static boolean canAct(Minecraft minecraft, AbstractContainerMenu menu) {
		LocalPlayer player = minecraft.player;
		return player != null
				&& minecraft.gameMode != null
				&& !player.isSpectator()
				&& !ClickQueue.isBusy()
				&& player.containerMenu == menu
				&& menu.getCarried().isEmpty();
	}

	/** Gives every distinct kind of stack a number, for the planners. */
	private static final class Kinds {
		final List<ItemStack> stacks = new ArrayList<>();
		final List<Integer> maxSizes = new ArrayList<>();

		int of(ItemStack stack, Slot slot) {
			if (stack.isEmpty()) {
				return SortPlan.EMPTY;
			}

			for (int kind = 0; kind < stacks.size(); kind++) {
				if (ItemStack.isSameItemSameComponents(stacks.get(kind), stack)) {
					return kind;
				}
			}

			stacks.add(stack);
			maxSizes.add(Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack)));
			return stacks.size() - 1;
		}

		int[] maxSizes() {
			return maxSizes.stream().mapToInt(Integer::intValue).toArray();
		}
	}
}
