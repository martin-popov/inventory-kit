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

	private static KeyMapping shiftKey;
	private static KeyMapping columnKey;
	private static KeyMapping lockKey;
	private static KeyMapping searchKey;

	@Override
	public void onInitializeClient() {
		InventoryKitConfig.load();

		shiftKey = register("key.inventorykit.shift", InputConstants.KEY_TAB);
		columnKey = register("key.inventorykit.shift_column", InputConstants.UNKNOWN.getValue());
		lockKey = register("key.inventorykit.toggle_lock", InputConstants.KEY_Y);
		searchKey = register("key.inventorykit.search", InputConstants.KEY_SLASH);

		ContainerButtons.register();

		ClientTickEvents.END_CLIENT_TICK.register(InventoryKitClient::onEndTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), new ShiftHud());
	}

	private static KeyMapping register(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, defaultKey, KeyMapping.Category.INVENTORY));
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

		while (shiftKey.consumeClick()) {
			if (InventoryKitConfig.get().columnModifier.isDown()) {
				Shifter.shiftSelectedColumn(minecraft, reverseHeld());
			} else {
				Shifter.shiftRows(minecraft, reverseHeld());
			}
		}

		while (columnKey.consumeClick()) {
			Shifter.shiftSelectedColumn(minecraft, reverseHeld());
		}

		while (lockKey.consumeClick()) {
			toggleLock(minecraft);
		}
	}

	private static void toggleLock(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null || player.isSpectator()) {
			return;
		}

		InventoryKitConfig config = InventoryKitConfig.get();
		int column = player.getInventory().getSelectedSlot();
		config.lockedSlots[column] = !config.lockedSlots[column];
		InventoryKitConfig.save();

		String key = config.lockedSlots[column] ? "message.inventorykit.locked" : "message.inventorykit.unlocked";
		minecraft.gui.hud.setOverlayMessage(Component.translatable(key, column + 1), false);
	}
}
