package com.martinpopov.inventorykit.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.martinpopov.inventorykit.SortPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Moving items between the player and an open container, and sorting either side. */
public final class InventoryActions {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventorykit");
	private static final int HOTBAR_SIZE = 9;
	private static final int INVENTORY_SIZE = 36;

	private InventoryActions() {
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

	/**
	 * Moves items from the player into the container. Locked hotbar slots stay put.
	 *
	 * @param matchingOnly only move items the container already holds
	 */
	public static void deposit(Minecraft minecraft, AbstractContainerMenu menu, boolean matchingOnly) {
		InventoryKitConfig config = InventoryKitConfig.get();
		List<Slot> sources = new ArrayList<>(storageSlots(menu));

		if (config.depositFromHotbar) {
			for (Slot slot : hotbarSlots(menu)) {
				if (!config.isLocked(slot.getContainerSlot())) {
					sources.add(slot);
				}
			}
		}

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
			// Taken before anything moves, so items do not start matching halfway through.
			wanted = new ArrayList<>();

			for (Slot slot : mustMatch) {
				if (slot.hasItem()) {
					wanted.add(slot.getItem().copy());
				}
			}
		}

		for (Slot slot : sources) {
			ItemStack stack = slot.getItem();

			if (stack.isEmpty() || !slot.mayPickup(player)) {
				continue;
			}

			if (wanted != null && !containsSame(wanted, stack)) {
				continue;
			}

			minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, player);
		}
	}

	private static boolean containsSame(List<ItemStack> stacks, ItemStack stack) {
		for (ItemStack other : stacks) {
			if (ItemStack.isSameItemSameComponents(other, stack)) {
				return true;
			}
		}

		return false;
	}

	/** Sorts the given slots in place, merging half-full stacks first. */
	public static void sort(Minecraft minecraft, AbstractContainerMenu menu, List<Slot> candidates) {
		LocalPlayer player = minecraft.player;

		if (!canAct(minecraft, menu)) {
			return;
		}

		// Bundles react to being clicked with an item (they swallow it), so they are left exactly
		// where they are and the other items are sorted around them.
		List<Slot> slots = new ArrayList<>();

		for (Slot slot : candidates) {
			ItemStack stack = slot.getItem();

			if (stack.has(DataComponents.BUNDLE_CONTENTS)) {
				continue;
			}

			if (!stack.isEmpty() && !slot.mayPickup(player)) {
				return;
			}

			slots.add(slot);
		}

		// Give every distinct kind of stack a number.
		List<ItemStack> kinds = new ArrayList<>();
		List<Integer> maxOfKind = new ArrayList<>();
		int[] slotTypes = new int[slots.size()];
		int[] slotCounts = new int[slots.size()];

		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getItem();

			if (stack.isEmpty()) {
				slotTypes[i] = SortPlan.EMPTY;
				continue;
			}

			int kind = -1;

			for (int k = 0; k < kinds.size(); k++) {
				if (ItemStack.isSameItemSameComponents(kinds.get(k), stack)) {
					kind = k;
					break;
				}
			}

			if (kind < 0) {
				kind = kinds.size();
				kinds.add(stack);
				maxOfKind.add(Math.min(stack.getMaxStackSize(), slots.get(i).getMaxStackSize(stack)));
			}

			slotTypes[i] = kind;
			slotCounts[i] = stack.getCount();
		}

		Comparator<ItemStack> order = ItemOrder.comparator(minecraft, InventoryKitConfig.get().sortMode);
		List<Integer> kindsInOrder = new ArrayList<>();

		for (int k = 0; k < kinds.size(); k++) {
			kindsInOrder.add(k);
		}

		kindsInOrder.sort((a, b) -> order.compare(kinds.get(a), kinds.get(b)));
		int[] rankOfKind = new int[kinds.size()];

		for (int rank = 0; rank < kindsInOrder.size(); rank++) {
			rankOfKind[kindsInOrder.get(rank)] = rank;
		}

		List<Integer> clicks;

		try {
			clicks = SortPlan.clicks(slotTypes, slotCounts, maxOfKind.stream().mapToInt(Integer::intValue).toArray(), rankOfKind);
		} catch (IllegalStateException e) {
			// Nothing has been clicked yet, so the inventory is untouched.
			LOGGER.warn("Could not plan a sort: {}", e.getMessage());
			return;
		}

		for (int index : clicks) {
			minecraft.gameMode.handleContainerInput(menu.containerId, slots.get(index).index, 0, ContainerInput.PICKUP, player);
		}

		// Should never happen, but never leave the player holding a stack they did not pick up.
		if (!menu.getCarried().isEmpty()) {
			for (Slot slot : slots) {
				if (!slot.hasItem()) {
					minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, player);
					break;
				}
			}
		}
	}

	private static boolean canAct(Minecraft minecraft, AbstractContainerMenu menu) {
		LocalPlayer player = minecraft.player;
		return player != null
				&& minecraft.gameMode != null
				&& !player.isSpectator()
				&& player.containerMenu == menu
				&& menu.getCarried().isEmpty();
	}
}
