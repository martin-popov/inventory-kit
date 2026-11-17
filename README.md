# Inventory Shift

A small client-side Fabric mod for Minecraft Java Edition 26.3.

- **Tab** shifts every inventory row down one step: the row just above the hotbar becomes your hotbar, and the old hotbar wraps round to the top row.
- **Shift + Tab** shifts the other way.

Four presses bring you back to where you started. Armour, the offhand slot and the crafting grid are never touched.

## How it works

The mod does nothing you could not do by hand. For each of the nine columns it sends three ordinary "swap with hotbar slot" clicks, the same action as pressing a number key while hovering a slot. That is 27 clicks per press, so it works in single player and on servers without a server-side mod. Servers with strict anti-cheat may not like that many clicks in one tick.

It only runs when no screen is open and nothing is held on the cursor.

## Key binding

The key is listed under **Options → Controls → Key Binds → Inventory** as "Shift Inventory Rows" and can be rebound. Tab is also the vanilla player list key; both fire when they share a key, so rebind one of them if that bothers you on multiplayer.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API

## Building

Needs a Java 25 JDK.

```
./gradlew build
```

The mod jar is written to `build/libs/`. Copy `inventoryshift-<version>.jar` into your `mods` folder.

## License

MIT
