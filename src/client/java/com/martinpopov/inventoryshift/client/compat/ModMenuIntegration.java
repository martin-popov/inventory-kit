package com.martinpopov.inventoryshift.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;

/** Adds the settings button to the Mod Menu list. Only loaded when Mod Menu is installed. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// The settings screen is built with YACL, so there is nothing to show without it.
		if (!FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
			return parent -> null;
		}

		return YaclConfigScreen::create;
	}
}
