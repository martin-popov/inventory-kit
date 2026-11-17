package com.martinpopov.inventorykit.client;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The orders items can be sorted in. */
public final class ItemOrder {
	private static Map<Item, Integer> creativeOrder;

	private ItemOrder() {
	}

	public static Comparator<ItemStack> comparator(Minecraft minecraft, InventoryKitConfig.SortMode mode) {
		Comparator<ItemStack> byName = Comparator
				.<ItemStack, String>comparing(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT))
				.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());

		if (mode == InventoryKitConfig.SortMode.NAME) {
			return byName;
		}

		Map<Item, Integer> order = creativeOrder(minecraft);
		return Comparator
				.<ItemStack>comparingInt(stack -> order.getOrDefault(stack.getItem(), Integer.MAX_VALUE))
				.thenComparing(byName);
	}

	/** Position of every item in the creative menu, tab by tab. */
	private static Map<Item, Integer> creativeOrder(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return creativeOrder != null ? creativeOrder : Map.of();
		}

		// The creative tabs are only filled in once something asks for them, normally the
		// creative screen. This does the same as that screen, including its search index.
		boolean hasPermissions = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
		HolderLookup.Provider holders = player.level().registryAccess();
		boolean rebuilt = CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), hasPermissions, holders);

		if (rebuilt) {
			SessionSearchTrees searchTrees = player.connection.searchTrees();
			List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
			searchTrees.updateCreativeTooltips(holders, searchItems);
			searchTrees.updateCreativeTags(searchItems);
		}

		if (rebuilt || creativeOrder == null) {
			Map<Item, Integer> order = new HashMap<>();

			for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
				if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
					continue;
				}

				for (ItemStack stack : tab.getDisplayItems()) {
					order.putIfAbsent(stack.getItem(), order.size());
				}
			}

			creativeOrder = order;
		}

		return creativeOrder;
	}
}
