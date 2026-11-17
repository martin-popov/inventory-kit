package com.martinpopov.inventorykit.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.martinpopov.inventorykit.client.InventoryKitConfig;
import com.martinpopov.inventorykit.client.Shifter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	/**
	 * Wraps the spot where the scroll wheel picks the next hotbar slot. With
	 * the scroll modifier held, the wheel shifts the inventory and the
	 * selected slot stays where it is.
	 */
	@WrapOperation(
			method = "onScroll",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I")
	)
	private int inventorykit$shiftOnScroll(double wheel, int currentSelected, int limit, Operation<Integer> original) {
		InventoryKitConfig config = InventoryKitConfig.get();

		if (!config.scrollEnabled || !config.scrollModifier.isDown() || wheel == 0.0) {
			return original.call(wheel, currentSelected, limit);
		}

		// Scrolling down moves the rows down; scrolling up moves them back up.
		boolean reverse = wheel > 0.0;
		Minecraft minecraft = Minecraft.getInstance();

		if (config.scrollTarget == InventoryKitConfig.ScrollTarget.COLUMN) {
			Shifter.shiftSelectedColumn(minecraft, reverse);
		} else {
			Shifter.shiftRows(minecraft, reverse);
		}

		return currentSelected;
	}
}
