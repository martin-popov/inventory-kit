# Inventory Shift

A small client-side Fabric mod for Minecraft Java Edition 26.3 that cycles your inventory rows through the hotbar.

- **Tab** shifts every inventory row down one step: the row just above the hotbar becomes your hotbar, and the old hotbar wraps round to the top row.
- **Shift + Tab** shifts the other way.

Armour, the offhand slot and the crafting grid are never touched.

## Features

| Feature | How to use it | Default |
|---|---|---|
| Shift all rows | Tab, or Shift + Tab to reverse | On |
| Shift one column | Alt + Tab moves only the column of the selected hotbar slot. There is also a separate, unbound "Shift Selected Column" key. | Alt |
| Scroll-wheel shifting | Hold Alt and scroll. Down moves the rows down, up moves them back. Can be set to move only the selected column. | On, all rows |
| Locked hotbar slots | Press Y to lock or unlock the selected hotbar slot. A locked slot's column never moves and shows a small gold line on the hotbar. | Y |
| Next-row preview | A small strip above the hotbar shows the row the next shift brings in. Hold Shift to see the reverse direction. | Always |
| Feedback | A soft pop sound and a bounce on the items that arrive in the hotbar. | On |
| Skip empty rows | Rows that are completely empty are jumped past, so the hotbar never lands on nothing. | On |

With "skip empty rows" off, four presses always bring you back to where you started.

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl) installed, the mod has a settings screen in the Mods list. Both are optional; without them, edit `config/inventoryshift.json` by hand.

The keys are listed under **Options → Controls → Key Binds → Inventory** and can be rebound. Tab is also the vanilla player list key; both fire when they share a key, so rebind one of them if that bothers you on multiplayer. On Windows, Alt + Tab belongs to the system, so pick Ctrl as the column modifier or bind the separate column key.

## How it works

The mod does nothing you could not do by hand. For each column it sends three ordinary "swap with hotbar slot" clicks, the same action as pressing a number key while hovering a slot. A full row shift is 27 clicks, or 54 when one empty row is skipped. It works in single player and on servers without a server-side mod. Servers with strict anti-cheat may not like that many clicks in one tick.

It only runs when no screen is open and nothing is held on the cursor.

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
