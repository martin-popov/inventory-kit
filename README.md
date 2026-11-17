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
| Locked slots | Press Y to lock or unlock the selected hotbar slot, or point at any inventory slot in a chest or inventory screen and press Y. A locked slot is never shifted, deposited or sorted, and shows a padlock. | Y |
| Next-row preview | A small strip above the hotbar shows the row the next shift brings in. Hold Shift to see the reverse direction. Turn it on in the settings. | Off |
| Feedback | A soft pop sound and a bounce on the items that arrive in the hotbar. | On |
| Skip empty rows | Rows that are completely empty are jumped past, so the hotbar never lands on nothing. | On |

With "skip empty rows" off, four presses always bring you back to where you started.

Shifting moves a whole column at a time, so a lock on any slot keeps its entire column in place. The shift keys are also ignored while you are eating, aiming a bow, blocking or mining, so the item is not swapped out of your hand; this can be turned off in the settings.

## Chest buttons

Chests, barrels, shulker boxes, hoppers, dispensers and droppers get two rows of small buttons, one beside the container's title and one beside the "Inventory" label.

| Row | Button | What it does |
|---|---|---|
| Container | Search | Opens the search box (see below) |
| Container | Take matching | Takes the items you already carry |
| Container | Take everything | Empties the container into your inventory |
| Container | Sort | Sorts the container |
| Inventory | Undo | Puts everything back where it was before the last deposit, take or sort |
| Inventory | Deposit matching | Moves the items the container already holds |
| Inventory | Deposit everything | Empties your inventory into the container |
| Inventory | Sort | Sorts your 27 storage slots |

Both deposit buttons leave locked slots alone. The player inventory screen has its own undo, search and sort buttons next to the recipe book button.

Undo works while the screen stays open and as long as the same items are still there. If something was added or removed since, for example by a hopper, it does nothing.

## Sorting

Sorting merges half-full stacks, then puts everything in order with the gaps at the end. There are two orders:

- **Category** (default): the order of the creative menu, tab by tab.
- **Name**: alphabetical.

Shift-click a sort button to switch between them, or change it in the settings. Your hotbar is never sorted. Locked slots and bundles stay exactly where they are and the other items are sorted around them.

## Search

Press **/** in a chest or your inventory, or click the magnifier button, and start typing. Items that do not match are dimmed and the ones that do get an outline, in the container and in your own inventory at once.

- One or two letters match the start of any word: `r` finds Rotten Flesh, Raw Iron and Block of Redstone.
- Three or more letters also match in the middle of a word: `ston` finds Cobblestone.
- **Enter** keeps the filter and gives the keyboard back to the inventory. **Esc** clears the search.

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl) installed, the mod has a settings screen in the Mods list. Both are optional; without them, edit `config/inventorykit.json` by hand.

The keys are listed under **Options → Controls → Key Binds → Inventory Kit** and can be rebound. Tab is also the vanilla player list key; both fire when they share a key, so rebind one of them if that bothers you on multiplayer. On Windows, Alt + Tab belongs to the system, so pick Ctrl as the column modifier or bind the separate column key.

## Keyboard and accessibility

- **No mouse needed.** Every chest button can be reached with Tab and pressed with Enter or Space, and each one also has its own key binding (unbound by default): sort container, sort inventory, deposit matching, deposit everything, take matching, take everything and undo.
- **No held keys needed.** Shift + Tab, Alt + Tab and Alt + scroll are shortcuts. "Shift Inventory Rows Back", "Shift Selected Column" and "Shift Selected Column Back" are separate single keys you can bind instead.
- **Narrator.** Buttons and the search box have spoken names, and locking or unlocking a slot is read out.
- **Not colour-only.** Search dims the items that do not match as well as outlining the ones that do, and locked slots are marked with a padlock shape as well as a gold bar.
- **Motion and sound.** The hotbar bounce and the pop sound can each be turned off.
- **Languages.** English, Bulgarian, German, Spanish and French. The translations were written by the author's tooling, not by native speakers, so corrections are welcome.

## How it works

The mod does nothing you could not do by hand. Shifting sends ordinary "swap with hotbar slot" clicks, depositing and taking send shift-clicks, and sorting sends left-clicks. It works in single player and on servers without a server-side mod.

In single player everything happens at once. On a server the clicks are spread out, 12 per tick by default, because a hundred clicks arriving in one tick is what anti-cheat plugins look for. A full sort of a double chest then takes under a second, and the screen ignores input until it finishes. The rate can be changed in the settings; 0 sends everything at once.

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
