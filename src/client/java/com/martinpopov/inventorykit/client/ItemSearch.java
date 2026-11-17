package com.martinpopov.inventorykit.client;

import java.util.List;

import com.martinpopov.inventorykit.SearchMatcher;
import com.martinpopov.inventorykit.client.mixin.AbstractContainerScreenAccessor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * A search box for container screens. While it holds text, slots whose item
 * does not match are dimmed and the ones that do get an outline, so every
 * match is visible at once.
 */
public final class ItemSearch {
	private static final int BOX_WIDTH = 100;
	private static final int BOX_HEIGHT = 12;
	private static final int BOX_GAP = 3;
	private static final int SLOT_SIZE = 16;
	private static final int DIM_COLOR = 0xB0000000;
	private static final int MATCH_COLOR = 0xFFFFE060;

	/** Kept across a window resize, which rebuilds the screen's widgets. */
	private static Screen searchingScreen;
	private static String searchingQuery = "";

	private final AbstractContainerScreen<?> screen;
	private final AbstractContainerScreenAccessor window;
	private final EditBox box;
	/** The key that opened the box also arrives as a typed character; that one is dropped. */
	private boolean swallowNextCharacter;

	private ItemSearch(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor window) {
		this.screen = screen;
		this.window = window;
		this.box = new EditBox(Minecraft.getInstance().font, boxX(), boxY(), BOX_WIDTH, BOX_HEIGHT, Component.translatable("button.inventorykit.search"));
		box.setMaxLength(40);
		box.setHint(Component.translatable("search.inventorykit.hint"));
		box.setVisible(false);

		if (searchingScreen == screen) {
			box.setValue(searchingQuery);
			box.setVisible(true);
		}

		box.setResponder(text -> {
			if (searchingScreen == screen) {
				searchingQuery = text;
			}
		});
	}

	public static ItemSearch attach(AbstractContainerScreen<?> screen, AbstractContainerScreenAccessor window, List<AbstractWidget> widgets) {
		ItemSearch search = new ItemSearch(screen, window);
		widgets.add(search.box);

		ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> search.allowKeyPress(event));
		ScreenKeyboardEvents.allowCharType(screen).register((s, event) -> search.allowCharacter());
		ScreenKeyboardEvents.afterKeyRelease(screen).register((s, event) -> search.swallowNextCharacter = false);
		ScreenMouseEvents.beforeMouseClick(screen).register((s, event) -> search.beforeMouseClick(event.x(), event.y()));
		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) -> search.extract(graphics));
		ScreenEvents.remove(screen).register(s -> {
			if (searchingScreen == s) {
				searchingScreen = null;
				searchingQuery = "";
			}
		});

		return search;
	}

	private int boxX() {
		return window.getLeftPos();
	}

	private int boxY() {
		return window.getTopPos() - BOX_HEIGHT - BOX_GAP;
	}

	public void toggle() {
		if (box.isVisible()) {
			close();
		} else {
			open();
		}
	}

	private void open() {
		searchingScreen = screen;
		searchingQuery = box.getValue();
		box.setVisible(true);
		screen.setFocused(box);
	}

	private void close() {
		box.setValue("");
		box.setVisible(false);
		screen.clearFocus();
		searchingScreen = null;
		searchingQuery = "";
	}

	/** @return false to keep the key away from the screen */
	private boolean allowKeyPress(KeyEvent event) {
		boolean typing = box.isVisible() && box.isFocused();

		if (!typing && InventoryKitClient.isSearchKey(event)) {
			open();
			swallowNextCharacter = true;
			return false;
		}

		if (typing) {
			if (event.isEscape()) {
				close();
			} else if (event.isConfirmation()) {
				// Enter keeps the filter but gives the keyboard back to the inventory.
				screen.clearFocus();
			} else {
				box.keyPressed(event);
			}

			// Otherwise typing "e" would close the screen and digits would swap hotbar slots.
			return false;
		}

		if (box.isVisible() && event.isEscape()) {
			close();
			return false;
		}

		return true;
	}

	private boolean allowCharacter() {
		if (swallowNextCharacter) {
			swallowNextCharacter = false;
			return false;
		}

		return true;
	}

	private void beforeMouseClick(double mouseX, double mouseY) {
		if (box.isVisible() && box.isFocused() && !box.isMouseOver(mouseX, mouseY)) {
			screen.clearFocus();
		}
	}

	private void extract(GuiGraphicsExtractor graphics) {
		// The inventory window slides sideways when the recipe book opens.
		box.setX(boxX());
		box.setY(boxY());

		if (!box.isVisible() || box.getValue().isBlank()) {
			return;
		}

		String query = box.getValue();
		int left = window.getLeftPos();
		int top = window.getTopPos();

		for (Slot slot : screen.getMenu().slots) {
			if (!slot.isActive() || !slot.hasItem()) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;

			if (SearchMatcher.matches(slot.getItem().getHoverName().getString(), query)) {
				graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, MATCH_COLOR);
				graphics.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, MATCH_COLOR);
				graphics.fill(x - 1, y, x, y + SLOT_SIZE, MATCH_COLOR);
				graphics.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, MATCH_COLOR);
			} else {
				graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, DIM_COLOR);
			}
		}
	}
}
