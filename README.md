<h1 align="center">noCrates 2.0</h1>

<p align="center">
  <b>A PhoenixCrates-class crates plugin — free and open source.</b><br>
  3-phase animations · idle particle shapes · linked keys · selective rewards · guaranteed win · rerolls ·
  a real module/addon system — one jar for <b>Minecraft 1.20 → 26.2</b>.
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/Minecraft-1.20%20%E2%86%92%2026.2-7b5cff">
  <img alt="Server" src="https://img.shields.io/badge/Paper%20%7C%20Purpur%20%7C%20Folia-supported-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B%20bytecode-orange">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue">
</p>

---

## Why it's "advanced"

noCrates 2.0 is a ground-up rebuild modeled on the feature set of **PhoenixCrates Premium
plus all of its paid add-ons** — implemented openly, with every "add-on" shipped as a free
built-in module on a public addon API.

### Three-phase opening animations
Every opening chains three independently configurable phases — mix them freely:

| Pre-Open | Post-Open | Reward Display |
|---|---|---|
| DEFAULT, CRACK, LIGHTNING, KEY_OPENER, FIRE, BLASTING, SONIC_BOOM, RAINBOW*, CYCLONE_HEART*, FROGGO_BOOM*, CHICKEN_JOCKEY* | BALL, SWIRL, FIRE, COMPACT, ROTATING_HEAD, PHYSICAL_CSGO*, ORBIT_ROULETTE*, ASTRO_BURST*, BLACK_HOLE* | DEFAULT, HELIX, SMOKE_SPIRAL, FIRE_SPIRAL, PHYSICAL_ITEM, FIREWORK, GUI_CSGO, CHEST_HUNT* |

\* from built-in modules — **26 animations, hundreds of combinations**, all selectable in the in-game editor.
Everything is entity/particle based (display entities — no NMS, no packet library), with per-phase timings and a watchdog so a broken animation can never eat a reward.

### Idle effects: 13 parametric particle shapes
Always-on crate auras built from `SHAPE;{PARTICLE;#hex;offX;offY;offZ;radius;velocity;amount}` specs:
`CIRCLE, SPIRAL, CONICAL_SPIRAL, STAR, NINJA_STAR, SQUARE, DIAMOND, ASTROID, DELTOID, FLOWER, QUATREFOIL, PULSE, DEFAULT` × any particle × any color. Rendered per-region (Folia-safe), only near players, with a GUI wizard in the editor.

### Keys are first-class
- Keys live in `keys.yml` and are **linked** to crates many-to-many: per link **amount** and **consumption priority**, virtual-only keys (dupe-proof), physical keys tagged with PDC.
- Consumption is **all-or-nothing** (virtual balance before physical items).
- `/crates virtualkeys` menu, `/crates key pay` between players, key **rarity guarantees** (a key that always drops epic+).

### A deep reward model
- **Display item** vs. **multiple win-items** + **win-commands** (PAPI-aware).
- Weights with normalized "real chance" shown in previews.
- **Per-player and global win limits** with cooldowns, **restricted permissions** with **alternative rewards** (already-VIP players get money instead), virtual rewards, share-with-online, broadcast per reward.
- **`max-win-rewards`** — up to 30 rewards per opening.
- **Rewards mode per crate:** `RANDOM` (weighted) or **`SELECTIVE`** — a non-gambling mode where players *choose* their reward for a per-reward key cost.

### Guaranteed win (milestones/pity)
`REPETITIVE` (every N opens) or `SEQUENTIAL` (one-time thresholds), optional chance mixing, per-player progress, placeholders for "opens until guaranteed".

### Rerolls
Stacking sources, PhoenixCrates-style: free-per-open + permission groups (`nocrates.reroll.<group>`) + admin-granted balances. Claim-or-reroll menu after the animation; closing always claims — a reroll can never void a reward.

### Ops features
Open **money cost** with confirmation menu (Vault) · per-crate **cooldowns** · **quick-open** (sneak-click) · simultaneous-opening lock · **knockback** for keyless clicks · built-in **TextDisplay holograms** (no hologram plugin) · engine modes: **BLOCK** (native chest lid animation) or **MODEL** (floating item display + CustomModelData/item_model) · placeable crate items · file + **Discord webhook** logging · YAML / SQLite / **MySQL** storage · full **PlaceholderAPI** suite · 10 languages.

## Modules (the "PhoenixCrates add-ons", built in and free)

| Module | PhoenixCrates equivalent | What it does |
|---|---|---|
| `rarities` | Rarities (€7.99) | Rarity tiers, synchronized drop rates, preview labels, key rarity guarantees |
| `last-winner` | Last Winner (€3.99) | Rolling winner history placeholders per crate |
| `crate-claim` | Crate Claim (€9.99) | Full-inventory rewards stored; `/crates claim` menu with claim-all |
| `mass-opening` | Mass Opening (€9.99) | x1/x6/x12/x32/x64/ALL tiers with per-tier permissions + summary |
| `lootboxes` | LootBoxes (€9.99) | Give players placeable one-shot crates — place, open, gone |
| `animations-plus` | 8 animation add-ons (~€55) | Physical CSGO, Orbit Roulette, Black Hole, Astro Burst, Rainbow, Cyclone Heart, Froggo Boom, Chicken Jockey |
| `chest-hunt` | — (noCrates exclusive) | Opening spawns M chests around you; open K, each grants its own roll |

Toggle any of them in `modules.yml`.

### External addons
The same loader accepts third-party jars in `plugins/noCrates/addons/`:

```yaml
# addon.yml inside your jar
name: MyAddon
main: com.example.MyAddon
version: 1.0
author: you
```

```java
public final class MyAddon extends Addon {
    @Override
    public void onEnable() {
        api().registerPre(new MyPreAnimation());     // shows up in the editor
        api().registerAction(new MyActionType());    // usable as [MY_ACTION] lines
        api().registerCommand("mycmd", (sender, args) -> { ... });
        api().registerPlaceholder("my", (player, rest) -> "...");
    }
}
```

Events: `CrateOpenEvent` (cancellable, mutable outcome), `RewardWinEvent`, `KeyChangeEvent`.

## Cross-version: one jar, 1.20 → 26.2

| Minecraft | Java | Server |
|---|---|---|
| 1.20 – 1.20.4 | 17+ | Paper/Purpur |
| 1.20.5 – 1.21.11 | 21 | Paper/Purpur/Folia |
| 26.1 – 26.2 | 25 | Paper/Purpur/Folia |

How: Java-17 bytecode against the stable Paper 1.20.1 API (`api-version: 1.20`), and **no
fragile references** — materials/sounds/particles/potion-effects resolve by name with
rename chains (the 1.20.5 renames, the 1.21.3 `Sound` interface change), entities spawn
via `World#spawn(Class)` (immune to the `EntityType.FIREWORK` rename), menus never touch
`InventoryView` (the 1.21 interface change), and everything schedules through a Folia-aware
facade (`folia-supported: true`). Spigot without Adventure/MiniMessage is not supported —
use Paper or a fork, like every modern crates plugin.

## Quick start

1. Drop `noCrates-2.0.0.jar` into `plugins/` (Paper 1.20+), restart.
2. `/crates editor` → **Crates → Create** — or edit `crates/example.yml`.
3. `/crates key give vote <you> 10`, then right-click your crate (left-click previews).
4. Bind a block: look at it and `/crates attach <crate>`.

## Commands

Aliases: `/crates`, `/crate`, `/nocrates`, `/nc` — permissions `nocrates.command.<verb>` / `nocrates.admin`.

```
open <crate> [player]      preview <crate> [player]     virtualkeys        claim
create/delete/clone        edit [crate] | editor        enable/disable     list | stats [player]
givecrate <crate> <p> [n]  lootbox give <crate> <p> [n] placecrate <crate> <world> <x> <y> <z>
attach <crate> | detach    givereward <crate> <reward> <p|all>             giverandomreward <crate> <p|all>
key give|giveall|take|set|check|pay ...                 reroll give|take <crate> <p> <n>
massopen <crate>           migrate <crazycrates|excellentcrates>           resetcooldown <crate> <p>
resetwinlimit player|global <crate> [p]                 reload
```

## Placeholders (PlaceholderAPI)

`%nocrates_keys_<key>%` · `%nocrates_cooldown_<crate>%` · `%nocrates_opened_<crate>%` ·
`%nocrates_opened_total%` · `%nocrates_rerolls_<crate>%` · `%nocrates_guaranteed_amount_<crate>%` ·
`%nocrates_guaranteed_reward_<crate>%` · `%nocrates_winlimit_<crate>_<reward>%` ·
`%nocrates_globalwinlimit_<crate>_<reward>%` · `%nocrates_lastwinner_<player|reward|second|minute|hour|day|month|year>_<crate>_<n>%`

## Configuration layout

```
plugins/noCrates/
  config.yml        language, render radius, database (YAML/SQLITE/MYSQL), logging + Discord webhook
  modules.yml       toggle built-in modules + their settings
  keys.yml          key items, virtual flags, rarity guarantees
  rarities.yml      rarity tiers (rarities module)
  crates/<id>.yml   one file per crate (see crates/example.yml — fully commented)
  menus/*.yml       restyle every GUI (preview, confirmation, selective, reroll, virtualkeys, massopen)
  languages/*.yml   en_US, de_DE, fr_FR, es_ES, pt_BR, ru_RU, zh_CN, pl_PL, tr_TR, nl_NL
  addons/           drop external addon jars here
  logs/             daily action logs
```

Reward actions inside menus use the `[ACTION]` system:
`[MESSAGE] [BROADCAST] [TITLE] [ACTION_BAR] [CLOSE_INVENTORY] [COMMAND] [GAMEMODE] [POTION_EFFECT] [SOUND] [MENU] [OPEN] [DELAY]`.

## Integrations

Vault (open costs) · PlaceholderAPI · ItemsAdder / Oraxen / Nexo / MMOItems (reward items,
key items and crate models via `custom-item: "itemsadder:my_item"` in any ItemSpec) ·
GeyserMC-friendly flows · optional MySQL for networks. All optional — the plugin is fully
functional with none installed.

## Migrating from other crate plugins

`/crates migrate crazycrates` and `/crates migrate excellentcrates` import crates,
rewards, chances, commands and key items best-effort (a report lists anything that needs
a manual touch). More importers can be registered by addons.

## Building from source

JDK 21+ and Maven: `mvn clean package` → `target/noCrates-2.0.0.jar` (37 unit tests cover
the roll engine, guaranteed-win logic, win limits, key-link planning, effect/action
parsers, shape math and YAML round-trips).

Metrics (bStats) are disabled by default in source (`bstatsId = 0`) — register your own id
on bstats.org if you fork.

## License

[MIT](LICENSE) © 2026 fizzexual
