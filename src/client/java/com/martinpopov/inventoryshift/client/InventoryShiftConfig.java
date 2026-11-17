package com.martinpopov.inventoryshift.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.martinpopov.inventoryshift.ShiftPlan;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

/** Settings, stored as JSON in the game's config folder. */
public final class InventoryShiftConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("inventoryshift");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("inventoryshift.json");

	private static InventoryShiftConfig instance = new InventoryShiftConfig();

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

	public static InventoryShiftConfig get() {
		return instance;
	}

	public boolean isLocked(int column) {
		return column >= 0 && column < lockedSlots.length && lockedSlots[column];
	}

	public static void load() {
		if (!Files.exists(PATH)) {
			instance = new InventoryShiftConfig();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(PATH)) {
			InventoryShiftConfig loaded = GSON.fromJson(reader, InventoryShiftConfig.class);
			instance = loaded != null ? loaded : new InventoryShiftConfig();
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("Could not read {}, using default settings", PATH, e);
			instance = new InventoryShiftConfig();
		}

		instance.repair();
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
		InventoryShiftConfig defaults = new InventoryShiftConfig();

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

		if (lockedSlots == null || lockedSlots.length != ShiftPlan.COLUMNS) {
			boolean[] fixed = new boolean[ShiftPlan.COLUMNS];

			if (lockedSlots != null) {
				System.arraycopy(lockedSlots, 0, fixed, 0, Math.min(lockedSlots.length, fixed.length));
			}

			lockedSlots = fixed;
		}
	}
}
