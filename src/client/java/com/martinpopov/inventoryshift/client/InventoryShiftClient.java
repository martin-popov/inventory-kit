package com.martinpopov.inventoryshift.client;

import com.martinpopov.inventoryshift.ShiftPlan;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;

public class InventoryShiftClient implements ClientModInitializer {
	private static KeyMapping shiftKey;

	@Override
	public void onInitializeClient() {
		shiftKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.inventoryshift.shift",
				InputConstants.KEY_TAB,
				KeyMapping.Category.INVENTORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(InventoryShiftClient::onEndTick);
	}

	private static void onEndTick(Minecraft minecraft) {
		while (shiftKey.consumeClick()) {
			boolean reverse = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
					|| InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
			shiftRows(minecraft, reverse);
		}
	}

	private static void shiftRows(Minecraft minecraft, boolean reverse) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null || player.isSpectator()) {
			return;
		}

		// Only act on the player's own inventory, never while a chest or other container is open.
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}

		// An item held on the cursor would get mixed into the swaps.
		if (!player.inventoryMenu.getCarried().isEmpty()) {
			return;
		}

		int containerId = player.inventoryMenu.containerId;

		for (ShiftPlan.Swap swap : ShiftPlan.swaps(reverse)) {
			minecraft.gameMode.handleContainerInput(containerId, swap.slot(), swap.hotbarIndex(), ContainerInput.SWAP, player);
		}
	}
}
