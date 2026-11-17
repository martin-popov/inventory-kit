package com.martinpopov.inventorykit.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * A small square button in the vanilla button style, with a pixel icon drawn
 * on it instead of text. Its position is re-read every frame, because the
 * inventory window moves when the recipe book opens.
 */
public class IconButton extends Button {
	public static final int SIZE = 11;

	/** Seven rows of seven characters; '#' is a lit pixel. */
	public static final String[] DEPOSIT_MATCHING = {
			"...#...",
			"..###..",
			".#####.",
			"...#...",
			"...#...",
			".......",
			"#######"
	};
	public static final String[] DEPOSIT_ALL = {
			"...#...",
			"..#.#..",
			".#...#.",
			"...#...",
			"..#.#..",
			".#...#.",
			"......."
	};
	public static final String[] TAKE_MATCHING = {
			"#######",
			".......",
			"...#...",
			"...#...",
			".#####.",
			"..###..",
			"...#..."
	};
	public static final String[] TAKE_ALL = {
			".......",
			".#...#.",
			"..#.#..",
			"...#...",
			".#...#.",
			"..#.#..",
			"...#..."
	};
	public static final String[] SORT = {
			"#######",
			".......",
			"#####..",
			".......",
			"###....",
			".......",
			"#......"
	};
	public static final String[] SEARCH = {
			".###...",
			"#...#..",
			"#...#..",
			"#...#..",
			".###...",
			"....##.",
			".....##"
	};

	public static final String[] UNDO = {
			"..#....",
			".##....",
			"#####..",
			".##..#.",
			"..#..#.",
			".....#.",
			"..###.."
	};

	private static final int ICON_COLOR = 0xFFFFFFFF;
	private static final int ICON_COLOR_DISABLED = 0xFFA0A0A0;
	private static final int ICON_SHADOW = 0xFF3F3F3F;

	private final String[] icon;
	private final IntSupplier x;
	private final IntSupplier y;
	private final Consumer<Boolean> onPress;
	private BooleanSupplier enabled = () -> true;

	/** @param onPress called with whether Shift was held */
	public IconButton(IntSupplier x, IntSupplier y, String[] icon, Component name, Consumer<Boolean> onPress) {
		super(x.getAsInt(), y.getAsInt(), SIZE, SIZE, name, button -> { }, DEFAULT_NARRATION);
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.onPress = onPress;
	}

	/** Greys the button out whenever the condition is false. */
	public void enabledWhen(BooleanSupplier enabled) {
		this.enabled = enabled;
		this.active = enabled.getAsBoolean();
	}

	public boolean isSortButton() {
		return icon == SORT;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		onPress.accept(input.hasShiftDown());
	}

	/**
	 * A mouse click does not leave the button focused, so it does not stay highlighted and
	 * Space or Enter cannot press it again by accident. It can still be reached with Tab
	 * and pressed from the keyboard.
	 */
	@Override
	public boolean shouldTakeFocusAfterInteraction() {
		return false;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		setX(x.getAsInt());
		setY(y.getAsInt());
		active = enabled.getAsBoolean();
		extractDefaultSprite(graphics);
		int color = active ? ICON_COLOR : ICON_COLOR_DISABLED;

		int left = getX() + 2;
		int top = getY() + 2;

		for (int row = 0; row < icon.length; row++) {
			for (int column = 0; column < icon[row].length(); column++) {
				if (icon[row].charAt(column) == '#') {
					graphics.fill(left + column + 1, top + row + 1, left + column + 2, top + row + 2, ICON_SHADOW);
				}
			}
		}

		for (int row = 0; row < icon.length; row++) {
			for (int column = 0; column < icon[row].length(); column++) {
				if (icon[row].charAt(column) == '#') {
					graphics.fill(left + column, top + row, left + column + 1, top + row + 1, color);
				}
			}
		}
	}
}
