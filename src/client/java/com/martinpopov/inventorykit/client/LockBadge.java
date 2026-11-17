package com.martinpopov.inventorykit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The padlock drawn on locked slots, on the hotbar and in inventory screens. */
public final class LockBadge {
	public static final int COLOR = 0xFFFFD040;
	private static final int BACKING = 0xE0000000;
	private static final String[] PADLOCK = {
			".###.",
			"#...#",
			"#####",
			"##.##",
			"#####"
	};

	private LockBadge() {
	}

	/**
	 * Draws the padlock over the top-left corner of a slot. A shape on a dark backing, so the
	 * lock does not depend on colour alone and stays visible over any item.
	 *
	 * @param itemX left edge of the slot's 16x16 item area
	 * @param itemY top edge of the slot's item area
	 */
	public static void extract(GuiGraphicsExtractor graphics, int itemX, int itemY) {
		int left = itemX - 2;
		int top = itemY - 2;
		graphics.fill(left, top, left + PADLOCK[0].length() + 2, top + PADLOCK.length + 2, BACKING);

		for (int row = 0; row < PADLOCK.length; row++) {
			for (int pixel = 0; pixel < PADLOCK[row].length(); pixel++) {
				if (PADLOCK[row].charAt(pixel) == '#') {
					graphics.fill(left + 1 + pixel, top + 1 + row, left + 2 + pixel, top + 2 + row, COLOR);
				}
			}
		}
	}
}
