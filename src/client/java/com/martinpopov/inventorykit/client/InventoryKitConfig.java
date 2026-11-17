package com.martinpopov.inventorykit.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.martinpopov.inventorykit.ShiftPlan;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

/** Settings, stored as JSON in the game's config folder. */
public final class InventoryKitConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventorykit");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("inventorykit.json");
	/** Settings file from when the mod was called Inventory Shift. */
	private static final Path OLD_PATH = FabricLoader.getInstance().getConfigDir().resolve("inventoryshift.json");

	private static InventoryKitConfig instance = new InventoryKitConfig();

	public enum Modifier {
		NONE,
		ALT,
		CTRL;

		public boolean isDown() {
			return switch (this) {
				case NONE -> false;
				case ALT -> InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT);
				case CTRL -> InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
			};
		}
	}

	public enum ScrollTarget {
		ROWS,
		COLUMN
	}

	public enum PreviewMode {
		OFF,
		ALWAYS,
		WHEN_SHIFTING
	}

	public enum SortMode {
		/** The order of the creative menu, tab by tab. */
		CATEGORY,
		NAME
	}

	/** Skip past rows that are completely empty so the hotbar never lands on nothing. */
	public boolean skipEmptyRows = true;

	/** Holding this while pressing the shift key moves only the selected column. */
	public Modifier columnModifier = Modifier.ALT;

	public boolean scrollEnabled = true;
	public Modifier scrollModifier = Modifier.ALT;
	public ScrollTarget scrollTarget = ScrollTarget.ROWS;

	/** Hotbar slots (0-8) whose column never moves. */
	public boolean[] lockedSlots = new boolean[ShiftPlan.COLUMNS];

	public PreviewMode previewMode = PreviewMode.OFF;
	/** Extra pixels to raise the preview strip by; negative lowers it. */
	public int previewOffsetY = 0;

	public boolean sound = true;
	public boolean animation = true;

	/** Show the deposit, take, sort and search buttons on storage screens and the inventory. */
	public boolean containerButtons = true;
	public SortMode sortMode = SortMode.CATEGORY;
	/** Whether the deposit buttons also empty the hotbar. Locked hotbar slots are never emptied. */
	public boolean depositFromHotbar = true;

	public static InventoryKitConfig get() {
		return instance;
	}

	public boolean isLocked(int column) {
		return column >= 0 && column < lockedSlots.length && lockedSlots[column];
	}

	public static void load() {
		Path source = Files.exists(PATH) ? PATH : OLD_PATH;

		if (!Files.exists(source)) {
			instance = new InventoryKitConfig();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(source)) {
			InventoryKitConfig loaded = GSON.fromJson(reader, InventoryKitConfig.class);
			instance = loaded != null ? loaded : new InventoryKitConfig();
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("Could not read {}, using default settings", source, e);
			instance = new InventoryKitConfig();
		}

		instance.repair();

		if (source != PATH) {
			save();
		}
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(PATH)) {
			GSON.toJson(instance, writer);
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", PATH, e);
		}
	}

	/** Fills in anything a hand-edited or older file left missing. */
	private void repair() {
		InventoryKitConfig defaults = new InventoryKitConfig();

		if (columnModifier == null) {
			columnModifier = defaults.columnModifier;
		}

		if (scrollModifier == null) {
			scrollModifier = defaults.scrollModifier;
		}

		if (scrollTarget == null) {
			scrollTarget = defaults.scrollTarget;
		}

		if (previewMode == null) {
			previewMode = defaults.previewMode;
		}

		if (sortMode == null) {
			sortMode = defaults.sortMode;
		}

		if (lockedSlots == null || lockedSlots.length != ShiftPlan.COLUMNS) {
			boolean[] fixed = new boolean[ShiftPlan.COLUMNS];

			if (lockedSlots != null) {
				System.arraycopy(lockedSlots, 0, fixed, 0, Math.min(lockedSlots.length, fixed.length));
			}

			lockedSlots = fixed;
		}
	}
}
