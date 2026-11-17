package com.martinpopov.inventorykit.client.compat;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.martinpopov.inventorykit.ShiftPlan;
import com.martinpopov.inventorykit.client.InventoryKitConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The settings screen. Only loaded when YACL is installed. */
public final class YaclConfigScreen {
	private YaclConfigScreen() {
	}

	public static Screen create(Screen parent) {
		InventoryKitConfig config = InventoryKitConfig.get();
		InventoryKitConfig defaults = new InventoryKitConfig();

		OptionGroup.Builder lockedSlots = OptionGroup.createBuilder()
				.name(text("group.locked"))
				.description(OptionDescription.of(text("group.locked.description")));

		for (int column = 0; column < ShiftPlan.COLUMNS; column++) {
			int slot = column;
			lockedSlots.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("config.inventorykit.locked_slot", slot + 1))
					.binding(false, () -> config.lockedSlots[slot], value -> config.lockedSlots[slot] = value)
					.controller(TickBoxControllerBuilder::create)
					.build());
		}

		return YetAnotherConfigLib.createBuilder()
				.title(text("title"))
				.category(ConfigCategory.createBuilder()
						.name(text("title"))
						.group(OptionGroup.createBuilder()
								.name(text("group.shifting"))
								.option(toggle("skip_empty", defaults.skipEmptyRows, () -> config.skipEmptyRows, value -> config.skipEmptyRows = value))
								.option(choice("column_modifier", InventoryKitConfig.Modifier.class, "modifier",
										defaults.columnModifier, () -> config.columnModifier, value -> config.columnModifier = value))
								.build())
						.group(OptionGroup.createBuilder()
								.name(text("group.scroll"))
								.option(toggle("scroll_enabled", defaults.scrollEnabled, () -> config.scrollEnabled, value -> config.scrollEnabled = value))
								.option(choice("scroll_modifier", InventoryKitConfig.Modifier.class, "modifier",
										defaults.scrollModifier, () -> config.scrollModifier, value -> config.scrollModifier = value))
								.option(choice("scroll_target", InventoryKitConfig.ScrollTarget.class, "scroll_target",
										defaults.scrollTarget, () -> config.scrollTarget, value -> config.scrollTarget = value))
								.build())
						.group(OptionGroup.createBuilder()
								.name(text("group.preview"))
								.option(choice("preview_mode", InventoryKitConfig.PreviewMode.class, "preview_mode",
										defaults.previewMode, () -> config.previewMode, value -> config.previewMode = value))
								.option(Option.<Integer>createBuilder()
										.name(text("preview_offset"))
										.description(OptionDescription.of(text("preview_offset.description")))
										.binding(defaults.previewOffsetY, () -> config.previewOffsetY, value -> config.previewOffsetY = value)
										.controller(option -> IntegerSliderControllerBuilder.create(option).range(-40, 120).step(1))
										.build())
								.build())
						.group(OptionGroup.createBuilder()
								.name(text("group.feedback"))
								.option(toggle("sound", defaults.sound, () -> config.sound, value -> config.sound = value))
								.option(toggle("animation", defaults.animation, () -> config.animation, value -> config.animation = value))
								.build())
						.group(OptionGroup.createBuilder()
								.name(text("group.containers"))
								.option(toggle("container_buttons", defaults.containerButtons, () -> config.containerButtons, value -> config.containerButtons = value))
								.option(choice("sort_mode", InventoryKitConfig.SortMode.class, "sort_mode",
										defaults.sortMode, () -> config.sortMode, value -> config.sortMode = value))
								.option(toggle("deposit_from_hotbar", defaults.depositFromHotbar, () -> config.depositFromHotbar, value -> config.depositFromHotbar = value))
								.build())
						.group(lockedSlots.build())
						.build())
				.save(InventoryKitConfig::save)
				.build()
				.generateScreen(parent);
	}

	private static Component text(String key) {
		return Component.translatable("config.inventorykit." + key);
	}

	private static Option<Boolean> toggle(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		return Option.<Boolean>createBuilder()
				.name(text(key))
				.description(OptionDescription.of(text(key + ".description")))
				.binding(defaultValue, getter, setter)
				.controller(TickBoxControllerBuilder::create)
				.build();
	}

	private static <E extends Enum<E>> Option<E> choice(String key, Class<E> type, String valuePrefix, E defaultValue, Supplier<E> getter, Consumer<E> setter) {
		return Option.<E>createBuilder()
				.name(text(key))
				.description(OptionDescription.of(text(key + ".description")))
				.binding(defaultValue, getter, setter)
				.controller(option -> EnumControllerBuilder.create(option)
						.enumClass(type)
						.formatValue(value -> text(valuePrefix + "." + value.name().toLowerCase())))
				.build();
	}
}
