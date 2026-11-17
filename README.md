# Inventory Kit

A client-side Fabric mod for Minecraft Java Edition 26.3 with a handful of inventory tools:

- cycle your inventory rows through the hotbar with **Tab**
- deposit, take and sort items in chests with small buttons on the chest screen
- sort your own inventory
- search a chest or your inventory by typing

It was first published as Inventory Shift.

## Shifting rows

- **Tab** shifts every inventory row down one step: the row just above the hotbar becomes your hotbar, and the old hotbar wraps round to the top row.
- **Shift + Tab** shifts the other way.

Armour, the offhand slot and the crafting grid are never touched.

| Feature | How to use it | Default |
|---|---|---|
| Shift all rows | Tab, or Shift + Tab to reverse | On |
| Shift one column | Alt + Tab moves only the column of the selected hotbar slot. There is also a separate, unbound "Shift Selected Column" key. | Alt |
| Scroll-wheel shifting | Hold Alt and scroll. Down moves the rows down, up moves them back. Can be set to move only the selected column. | On, all rows |
| Locked hotbar slots | Press Y to lock or unlock the selected hotbar slot. A locked slot's column never moves, and the deposit buttons leave it alone. It shows a small gold line on the hotbar. | Y |
| Next-row preview | A small strip above the hotbar shows the row the next shift brings in. Hold Shift to see the reverse direction. Turn it on in the settings. | Off |
| Feedback | A soft pop sound and a bounce on the items that arrive in the hotbar. | On |
| Skip empty rows | Rows that are completely empty are jumped past, so the hotbar never lands on nothing. | On |

With "skip empty rows" off, four presses always bring you back to where you started.

## Chest buttons

Chests, barrels, shulker boxes, hoppers, dispensers and droppers get two rows of small buttons, one beside the container's title and one beside the "Inventory" label.

| Row | Button | What it does |
|---|---|---|
| Container | Search | Opens the search box (see below) |
| Container | Take matching | Takes the items you already carry |
| Container | Take everything | Empties the container into your inventory |
| Container | Sort | Sorts the container |
| Inventory | Deposit matching | Moves the items the container already holds |
| Inventory | Deposit everything | Empties your inventory into the container |
| Inventory | Sort | Sorts your 27 storage slots |

Both deposit buttons leave locked hotbar slots alone. The player inventory screen has its own sort and search buttons next to the recipe book button.

## Sorting

Sorting merges half-full stacks, then puts everything in order with the gaps at the end. There are two orders:

- **Category** (default): the order of the creative menu, tab by tab.
- **Name**: alphabetical.

Shift-click a sort button to switch between them, or change it in the settings. Your hotbar is never sorted. Bundles stay exactly where they are and the other items are sorted around them.

## Search

Press **/** in a chest or your inventory, or click the magnifier button, and start typing. Items that do not match are dimmed and the ones that do get an outline, in the container and in your own inventory at once.

- One or two letters match the start of any word: `r` finds Rotten Flesh, Raw Iron and Block of Redstone.
- Three or more letters also match in the middle of a word: `ston` finds Cobblestone.
- **Enter** keeps the filter and gives the keyboard back to the inventory. **Esc** clears the search.

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl) installed, the mod has a settings screen in the Mods list. Both are optional; without them, edit `config/inventorykit.json` by hand.

The keys are listed under **Options → Controls → Key Binds → Inventory** and can be rebound. Tab is also the vanilla player list key; both fire when they share a key, so rebind one of them if that bothers you on multiplayer. On Windows, Alt + Tab belongs to the system, so pick Ctrl as the column modifier or bind the separate column key.

## How it works

The mod does nothing you could not do by hand. Shifting sends ordinary "swap with hotbar slot" clicks, depositing and taking send shift-clicks, and sorting sends left-clicks. It works in single player and on servers without a server-side mod.

Everything happens in one go: a row shift is 27 clicks and sorting a double chest can be well over a hundred. Servers with strict anti-cheat may not like that many clicks in one tick.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API

## Building

Needs a Java 25 JDK.

```
./gradlew build
```

The mod jar is written to `build/libs/`. Copy `inventorykit-<version>.jar` into your `mods` folder.

## License

MIT
