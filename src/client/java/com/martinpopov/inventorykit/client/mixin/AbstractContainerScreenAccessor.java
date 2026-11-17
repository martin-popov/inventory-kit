package com.martinpopov.inventorykit.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Reads where a container window sits on screen, so buttons can be placed on it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int getLeftPos();

	@Accessor("topPos")
	int getTopPos();

	@Accessor("imageWidth")
	int getImageWidth();

	@Accessor("titleLabelY")
	int getTitleLabelY();

	@Accessor("inventoryLabelY")
	int getInventoryLabelY();
}
