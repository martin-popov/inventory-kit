package com.martinpopov.inventorykit.client;

import java.util.ArrayDeque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Sends the mod's clicks to the game. In single player they all go at once.
 * On a server they are spread over several ticks, because a hundred clicks
 * arriving together is exactly what anti-cheat plugins look for.
 *
 * <p>Only one batch runs at a time. While it does, the mod starts nothing
 * new and the container screen ignores input, so nothing can move items out
 * from under a half-finished sort.
 */
public final class ClickQueue {
	/** One click on a slot of the open container. */
	public record Click(int containerId, int slot, int button, ContainerInput input) {
	}

	private static final ArrayDeque<Click> PENDING = new ArrayDeque<>();
	private static Runnable whenDone;

	private ClickQueue() {
	}

	public static boolean isBusy() {
		return !PENDING.isEmpty();
	}

	/**
	 * @param whenDone run once every click has been sent; skipped if the batch is abandoned
	 */
	public static void submit(Minecraft minecraft, List<Click> clicks, Runnable whenDone) {
		if (clicks.isEmpty() || isBusy()) {
			return;
		}

		PENDING.addAll(clicks);
		ClickQueue.whenDone = whenDone;

		// With no limit there is no reason to wait for the next tick.
		if (clicksPerTick(minecraft) == Integer.MAX_VALUE) {
			run(minecraft);
		}
	}

	public static void tick(Minecraft minecraft) {
		if (isBusy()) {
			run(minecraft);
		}
	}

	private static int clicksPerTick(Minecraft minecraft) {
		int limit = InventoryKitConfig.get().multiplayerClicksPerTick;
		return minecraft.hasSingleplayerServer() || limit <= 0 ? Integer.MAX_VALUE : limit;
	}

	private static void run(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.gameMode == null) {
			abandon();
			return;
		}

		int budget = clicksPerTick(minecraft);

		while (budget-- > 0 && !PENDING.isEmpty()) {
			Click click = PENDING.peek();

			// The screen was closed or swapped for another one part-way through.
			if (player.containerMenu.containerId != click.containerId()) {
				abandon();
				return;
			}

			PENDING.poll();
			minecraft.gameMode.handleContainerInput(click.containerId(), click.slot(), click.button(), click.input(), player);
		}

		if (PENDING.isEmpty() && whenDone != null) {
			Runnable finished = whenDone;
			whenDone = null;
			finished.run();
		}
	}

	private static void abandon() {
		PENDING.clear();
		whenDone = null;
	}
}
