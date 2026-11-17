package com.martinpopov.inventorykit.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import com.martinpopov.inventorykit.client.mixin.AbstractContainerScreenAccessor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * What the mod adds to container screens: the deposit, take, sort, undo and
 * search buttons on storage screens and the player inventory, and slot
 * locking on every screen that shows the player's inventory.
 */
public final class ContainerButtons {
	/** Distance between the left edges of two neighbouring buttons. */
	private static final int STEP = IconButton.SIZE + 1;
	/** Gap between the window's right edge and the last button, matching the slot grid's margin. */
	private static final int RIGHT_MARGIN = 7;
	/** Where the buttons sit in the player inventory: the empty patch right of the recipe book button. */
	private static final int INVENTORY_BUTTON_Y = 66;

	private ContainerButtons() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			// The creative screen lays the inventory out its own way.
			if (screen instanceof AbstractContainerScreen<?> containerScreen && !(screen instanceof CreativeModeInventoryScreen)) {
				attach(minecraft, containerScreen);
			}
		});
	}

	private static void attach(Minecraft minecraft, AbstractContainerScreen<?> screen) {
		AbstractContainerMenu menu = screen.getMenu();
		AbstractContainerScreenAccessor window = (AbstractContainerScreenAccessor) screen;

		// Registered first, so it is asked first: nothing gets through while clicks are being sent.
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !ClickQueue.isBusy() || event.isEscape());
		ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !ClickQueue.isBusy());

		boolean playerInventory = screen instanceof InventoryScreen;
		ItemSearch search = null;

		if (InventoryKitConfig.get().containerButtons && (playerInventory || InventoryActions.isStorage(menu))) {
			search = ItemSearch.attach(screen, window, Screens.getWidgets(screen));
			addButtons(minecraft, screen, window, search, playerInventory);
		}

		ItemSearch finalSearch = search;
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
			if (finalSearch != null && finalSearch.isTyping()) {
				return true;
			}

			if (InventoryKitClient.lockKey.matches(event)) {
				Slot hovered = window.getHoveredSlot();

				if (hovered != null && InventoryActions.isInventorySlot(hovered)) {
					InventoryKitClient.toggleLock(minecraft, hovered.getContainerSlot());
				}

				return false;
			}

			return true;
		});

		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) -> extractLocks(graphics, menu, window));
		ScreenEvents.remove(screen).register(s -> InventoryActions.forget(menu));
	}

	private static void extractLocks(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, AbstractContainerScreenAccessor window) {
		InventoryKitConfig config = InventoryKitConfig.get();

		for (Slot slot : menu.slots) {
			if (slot.isActive() && InventoryActions.isInventorySlot(slot) && config.isLocked(slot.getContainerSlot())) {
				LockBadge.extract(graphics, window.getLeftPos() + slot.x, window.getTopPos() + slot.y);
			}
		}
	}

	private static void addButtons(
			Minecraft minecraft, AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor window, ItemSearch search, boolean playerInventory
	) {
		AbstractContainerMenu menu = screen.getMenu();
		List<AbstractWidget> widgets = Screens.getWidgets(screen);

		Runnable sortInventory = () -> InventoryActions.sort(minecraft, menu, InventoryActions.storageSlots(menu));
		Runnable undo = () -> InventoryActions.undo(minecraft, menu);
		Map<KeyMapping, Runnable> keys = new LinkedHashMap<>();
		keys.put(InventoryKitClient.sortInventoryKey, sortInventory);
		keys.put(InventoryKitClient.undoKey, undo);

		if (playerInventory) {
			IntSupplier y = () -> window.getTopPos() + INVENTORY_BUTTON_Y;
			widgets.add(undoButton(column(window, 2), y, menu, undo));
			widgets.add(searchButton(column(window, 1), y, search));
			widgets.add(sortButton(minecraft, column(window, 0), y, sortInventory));
			registerKeys(screen, search, keys);
			return;
		}

		Runnable sortContainer = () -> InventoryActions.sort(minecraft, menu, InventoryActions.containerSlots(menu));
		Runnable takeAll = () -> InventoryActions.take(minecraft, menu, false);
		Runnable takeMatching = () -> InventoryActions.take(minecraft, menu, true);
		Runnable depositAll = () -> InventoryActions.deposit(minecraft, menu, false);
		Runnable depositMatching = () -> InventoryActions.deposit(minecraft, menu, true);
		keys.put(InventoryKitClient.sortContainerKey, sortContainer);
		keys.put(InventoryKitClient.takeAllKey, takeAll);
		keys.put(InventoryKitClient.takeMatchingKey, takeMatching);
		keys.put(InventoryKitClient.depositAllKey, depositAll);
		keys.put(InventoryKitClient.depositMatchingKey, depositMatching);
		registerKeys(screen, search, keys);

		IntSupplier containerRow = () -> window.getTopPos() + window.getTitleLabelY() - 2;
		IntSupplier inventoryRow = () -> window.getTopPos() + window.getInventoryLabelY() - 2;

		// Added left to right, so Tab walks through them in the order they appear.
		widgets.add(searchButton(column(window, 3), containerRow, search));
		widgets.add(button(column(window, 2), containerRow, IconButton.TAKE_MATCHING, "take_matching", takeMatching));
		widgets.add(button(column(window, 1), containerRow, IconButton.TAKE_ALL, "take_all", takeAll));
		widgets.add(sortButton(minecraft, column(window, 0), containerRow, sortContainer));

		widgets.add(undoButton(column(window, 3), inventoryRow, menu, undo));
		widgets.add(button(column(window, 2), inventoryRow, IconButton.DEPOSIT_MATCHING, "deposit_matching", depositMatching));
		widgets.add(button(column(window, 1), inventoryRow, IconButton.DEPOSIT_ALL, "deposit_all", depositAll));
		widgets.add(sortButton(minecraft, column(window, 0), inventoryRow, sortInventory));
	}

	/** Lets each button's action be bound to a key. The keys are ignored while the search box is being typed in. */
	private static void registerKeys(Screen screen, ItemSearch search, Map<KeyMapping, Runnable> keys) {
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
			if (search.isTyping()) {
				return true;
			}

			for (Map.Entry<KeyMapping, Runnable> entry : keys.entrySet()) {
				if (entry.getKey().matches(event)) {
					entry.getValue().run();
					return false;
				}
			}

			return true;
		});
	}

	/** X position of a button, counted from the right edge of the window. */
	private static IntSupplier column(AbstractContainerScreenAccessor window, int fromRight) {
		return () -> window.getLeftPos() + window.getImageWidth() - RIGHT_MARGIN - IconButton.SIZE - fromRight * STEP;
	}

	private static IconButton button(IntSupplier x, IntSupplier y, String[] icon, String key, Runnable action) {
		Component name = Component.translatable("button.inventorykit." + key);
		IconButton button = new IconButton(x, y, icon, name, shift -> action.run());
		button.setTooltip(Tooltip.create(Component.translatable("button.inventorykit." + key + ".tooltip")));
		return button;
	}

	/** Greyed out until there is something to undo. */
	private static IconButton undoButton(IntSupplier x, IntSupplier y, AbstractContainerMenu menu, Runnable undo) {
		IconButton button = button(x, y, IconButton.UNDO, "undo", undo);
		button.enabledWhen(() -> InventoryActions.canUndo(menu));
		return button;
	}

	private static IconButton searchButton(IntSupplier x, IntSupplier y, ItemSearch search) {
		Component name = Component.translatable("button.inventorykit.search");
		IconButton button = new IconButton(x, y, IconButton.SEARCH, name, shift -> search.toggle());
		button.setTooltip(Tooltip.create(Component.translatable("button.inventorykit.search.tooltip", InventoryKitClient.searchKeyName())));
		return button;
	}

	/** A click sorts; a Shift-click first switches between sorting by category and by name. */
	private static IconButton sortButton(Minecraft minecraft, IntSupplier x, IntSupplier y, Runnable sort) {
		IconButton button = new IconButton(x, y, IconButton.SORT, Component.translatable("button.inventorykit.sort"), shift -> {
			if (shift) {
				InventoryKitConfig config = InventoryKitConfig.get();
				config.sortMode = config.sortMode == InventoryKitConfig.SortMode.CATEGORY
						? InventoryKitConfig.SortMode.NAME
						: InventoryKitConfig.SortMode.CATEGORY;
				InventoryKitConfig.save();
				refreshSortTooltips(minecraft.gui.screen());
			}

			sort.run();
		});
		button.setTooltip(sortTooltip());
		return button;
	}

	private static Tooltip sortTooltip() {
		String mode = InventoryKitConfig.get().sortMode.name().toLowerCase();
		return Tooltip.create(Component.translatable("button.inventorykit.sort.tooltip." + mode));
	}

	private static void refreshSortTooltips(Screen screen) {
		if (screen == null) {
			return;
		}

		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget instanceof IconButton button && button.isSortButton()) {
				button.setTooltip(sortTooltip());
			}
		}
	}
}
