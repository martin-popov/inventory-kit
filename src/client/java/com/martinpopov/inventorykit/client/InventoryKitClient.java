package com.martinpopov.inventorykit.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class InventoryKitClient implements ClientModInitializer {
	public static final String MOD_ID = "inventorykit";

	private static final int UNBOUND = InputConstants.UNKNOWN.getValue();
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	// Used while playing.
	private static KeyMapping shiftKey;
	private static KeyMapping shiftBackKey;
	private static KeyMapping columnKey;
	private static KeyMapping columnBackKey;
	/** Also works on the slot under the mouse in inventory screens. */
	public static KeyMapping lockKey;

	// Used inside chests and the inventory. Every button has one, so nothing needs the mouse.
	public static KeyMapping searchKey;
	public static KeyMapping sortContainerKey;
	public static KeyMapping sortInventoryKey;
	public static KeyMapping depositMatchingKey;
	public static KeyMapping depositAllKey;
	public static KeyMapping takeMatchingKey;
	public static KeyMapping takeAllKey;
	public static KeyMapping undoKey;

	@Override
	public void onInitializeClient() {
		InventoryKitConfig.load();

		shiftKey = register("shift", InputConstants.KEY_TAB);
		shiftBackKey = register("shift_back", UNBOUND);
		columnKey = register("shift_column", UNBOUND);
		columnBackKey = register("shift_column_back", UNBOUND);
		lockKey = register("toggle_lock", InputConstants.KEY_Y);

		searchKey = register("search", InputConstants.KEY_SLASH);
		sortContainerKey = register("sort_container", UNBOUND);
		sortInventoryKey = register("sort_inventory", UNBOUND);
		depositMatchingKey = register("deposit_matching", UNBOUND);
		depositAllKey = register("deposit_all", UNBOUND);
		takeMatchingKey = register("take_matching", UNBOUND);
		takeAllKey = register("take_all", UNBOUND);
		undoKey = register("undo", UNBOUND);

		ContainerButtons.register();
		ClientTickEvents.END_CLIENT_TICK.register(InventoryKitClient::onEndTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), new ShiftHud());
	}

	private static KeyMapping register(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.inventorykit." + name, defaultKey, CATEGORY));
	}

	/** Whether this key press is the "search items" key. It only does anything inside container screens. */
	public static boolean isSearchKey(KeyEvent event) {
		return searchKey != null && searchKey.matches(event);
	}

	public static Component searchKeyName() {
		return searchKey.getTranslatedKeyMessage();
	}

	private static boolean reverseHeld() {
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}

	private static void onEndTick(Minecraft minecraft) {
		Shifter.tick();
		ClickQueue.tick(minecraft);

		while (shiftKey.consumeClick()) {
			if (InventoryKitConfig.get().columnModifier.isDown()) {
				Shifter.shiftSelectedColumn(minecraft, reverseHeld());
			} else {
				Shifter.shiftRows(minecraft, reverseHeld());
			}
		}

		// The "back" keys do with one key what Shift plus the forward key does.
		while (shiftBackKey.consumeClick()) {
			Shifter.shiftRows(minecraft, true);
		}

		while (columnKey.consumeClick()) {
			Shifter.shiftSelectedColumn(minecraft, reverseHeld());
		}

		while (columnBackKey.consumeClick()) {
			Shifter.shiftSelectedColumn(minecraft, true);
		}

		while (lockKey.consumeClick()) {
			LocalPlayer player = minecraft.player;

			if (player != null) {
				toggleLock(minecraft, player.getInventory().getSelectedSlot());
			}
		}
	}

	/** @param slot an inventory slot number: 0-8 hotbar, 9-35 storage */
	public static void toggleLock(Minecraft minecraft, int slot) {
		LocalPlayer player = minecraft.player;
		InventoryKitConfig config = InventoryKitConfig.get();

		if (player == null || player.isSpectator() || slot < 0 || slot >= config.lockedSlots.length) {
			return;
		}

		config.lockedSlots[slot] = !config.lockedSlots[slot];
		InventoryKitConfig.save();

		boolean hotbar = slot < 9;
		String key = "message.inventorykit." + (config.lockedSlots[slot] ? "locked" : "unlocked") + (hotbar ? "" : "_storage");
		Component message = hotbar ? Component.translatable(key, slot + 1) : Component.translatable(key);
		minecraft.gui.hud.setOverlayMessage(message, false);
		// The message above the hotbar is not read out, so say it for narrator users.
		minecraft.getNarrator().saySystemNow(message);
	}
}
