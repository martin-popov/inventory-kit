package com.martinpopov.inventorykit.client;

import java.util.List;
import java.util.function.IntSupplier;

import com.martinpopov.inventorykit.client.mixin.AbstractContainerScreenAccessor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Adds the deposit, take, sort and search buttons to storage screens and the player inventory. */
public final class ContainerButtons {
	/** Distance between the left edges of two neighbouring buttons. */
	private static final int STEP = IconButton.SIZE + 1;
	/** Gap between the window's right edge and the last button, matching the slot grid's margin. */
	private static final int RIGHT_MARGIN = 7;
	/** Where the sort button sits in the player inventory: the empty patch right of the recipe book button. */
	private static final int INVENTORY_BUTTON_Y = 66;

	private ContainerButtons() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (InventoryKitConfig.get().containerButtons && screen instanceof AbstractContainerScreen<?> containerScreen) {
				addButtons(minecraft, containerScreen);
			}
		});
	}

	private static boolean isStorage(AbstractContainerMenu menu) {
		return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu || menu instanceof HopperMenu;
	}

	private static void addButtons(Minecraft minecraft, AbstractContainerScreen<?> screen) {
		AbstractContainerMenu menu = screen.getMenu();
		boolean playerInventory = screen instanceof InventoryScreen;

		if (!playerInventory && !isStorage(menu)) {
			return;
		}

		AbstractContainerScreenAccessor window = (AbstractContainerScreenAccessor) screen;
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		ItemSearch search = ItemSearch.attach(screen, window, widgets);

		if (playerInventory) {
			IntSupplier y = () -> window.getTopPos() + INVENTORY_BUTTON_Y;
			widgets.add(sortButton(minecraft, column(window, 0), y, () -> InventoryActions.sort(minecraft, menu, InventoryActions.storageSlots(menu))));
			widgets.add(searchButton(column(window, 1), y, search));
			return;
		}

		IntSupplier containerRow = () -> window.getTopPos() + window.getTitleLabelY() - 1;
		IntSupplier inventoryRow = () -> window.getTopPos() + window.getInventoryLabelY() - 1;

		widgets.add(sortButton(minecraft, column(window, 0), containerRow, () -> InventoryActions.sort(minecraft, menu, InventoryActions.containerSlots(menu))));
		widgets.add(button(column(window, 1), containerRow, IconButton.TAKE_ALL, "take_all", () -> InventoryActions.take(minecraft, menu, false)));
		widgets.add(button(column(window, 2), containerRow, IconButton.TAKE_MATCHING, "take_matching", () -> InventoryActions.take(minecraft, menu, true)));
		widgets.add(searchButton(column(window, 3), containerRow, search));

		widgets.add(sortButton(minecraft, column(window, 0), inventoryRow, () -> InventoryActions.sort(minecraft, menu, InventoryActions.storageSlots(menu))));
		widgets.add(button(column(window, 1), inventoryRow, IconButton.DEPOSIT_ALL, "deposit_all", () -> InventoryActions.deposit(minecraft, menu, false)));
		widgets.add(button(column(window, 2), inventoryRow, IconButton.DEPOSIT_MATCHING, "deposit_matching", () -> InventoryActions.deposit(minecraft, menu, true)));
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
