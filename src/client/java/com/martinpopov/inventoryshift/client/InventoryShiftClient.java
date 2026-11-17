package com.martinpopov.inventoryshift.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class InventoryShiftClient implements ClientModInitializer {
	public static final String MOD_ID = "inventoryshift";

	private static KeyMapping shiftKey;
	private static KeyMapping columnKey;
	private static KeyMapping lockKey;

	@Override
	public void onInitializeClient() {
		InventoryShiftConfig.load();

		shiftKey = register("key.inventoryshift.shift", InputConstants.KEY_TAB);
		columnKey = register("key.inventoryshift.shift_column", InputConstants.UNKNOWN.getValue());
		lockKey = register("key.inventoryshift.toggle_lock", InputConstants.KEY_Y);

		ClientTickEvents.END_CLIENT_TICK.register(InventoryShiftClient::onEndTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), new ShiftHud());
	}

	private static KeyMapping register(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, defaultKey, KeyMapping.Category.INVENTORY));
	}

	private static boolean reverseHeld() {
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}

	private static void onEndTick(Minecraft minecraft) {
		Shifter.tick();

		while (shiftKey.consumeClick()) {
			if (InventoryShiftConfig.get().columnModifier.isDown()) {
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

		InventoryShiftConfig config = InventoryShiftConfig.get();
		int column = player.getInventory().getSelectedSlot();
		config.lockedSlots[column] = !config.lockedSlots[column];
		InventoryShiftConfig.save();

		String key = config.lockedSlots[column] ? "message.inventoryshift.locked" : "message.inventoryshift.unlocked";
		minecraft.gui.hud.setOverlayMessage(Component.translatable(key, column + 1), false);
	}
}
