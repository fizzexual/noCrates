# noCrates — Design Spec

**Date:** 2026-06-29
**Repo:** github.com/fizzexual/noCrates
**Status:** Approved — ready for implementation planning

---

## 1. Goal

An advanced, free, open-source Minecraft **crates plugin** that ships "everything the
community wants" plus two standout mechanics (a right-click **Lootbox** and a novel
**pick-N-of-M Chest Hunt**), fronted by an **easy-to-use in-game GUI editor** so server
owners can build and edit crates without touching files.

Quality bar = match or beat ExcellentCrates / CrazyCrates on features, then exceed them.

---

## 2. Platform & cross-version strategy

- **Build:** Maven, single shaded jar. `javac 21` at `--release 17` → **Java 17 bytecode**.
- **API target:** Paper API `1.20.1-R0.1-SNAPSHOT` (stable subset of Bukkit/Paper).
- **Runtime range:** the one jar loads on **MC 1.20 (Java 17) → 1.21 (Java 21) → 26.1.2
  (Java 25)**. Bytecode and the stable API are backward-compatible; **no NMS** is used.
- **`VersionCompat`** layer absorbs the only things that drift across the range —
  `Material` / `Sound` / `Particle` identifiers — by resolving them from config by key
  with safe fallbacks. Owners on unusual versions can override identifiers in config.
- **Holograms:** native **TextDisplay** entities (stable since 1.19.4). No hologram
  dependency required; optional hooks for DecentHolograms/HolographicDisplays.
- **Rejected alternative:** NMS multi-module version abstraction — heavy, brittle, and
  unnecessary because all animations are Bukkit-inventory / entity based, not packet-level.

---

## 3. Architecture — modular monolith

One Maven project, clean package boundaries under `com.nocrates`:

| Package | Responsibility |
|---|---|
| `core` | Plugin bootstrap, lifecycle, config loading, service registry |
| `compat` | `VersionCompat` (materials/sounds/particles), soft-dependency detection |
| `crate` | Crate model, YAML (de)serialization, crate registry |
| `reward` | Reward model + **weighted roll engine**, rarities, pity/milestone |
| `key` | Physical + virtual keys, key balances, give/take/all, timed keys |
| `animation` | `Animation` interface + all open styles |
| `editor` | In-game GUI editor |
| `gui` | Reusable menu framework (paged menus, click handlers, items) |
| `storage` | `DataStore` interface → `YamlDataStore`, `MySqlDataStore` (HikariCP) |
| `hook` | Vault, PlaceholderAPI, custom-item providers, holograms, bStats |
| `command` | Command + tab-completion tree |
| `message` | `messages.yml`, MiniMessage (Adventure) formatting |
| `util` | Items, locations, scheduling, math helpers |

**Three extension interfaces** keep the core closed for modification, open for extension:
- `Animation` — `play(player, crate, rolledReward, callback)`; new open styles drop in.
- `DataStore` — load/save player data; new backends drop in.
- `RewardAction` — `execute(player, ctx)`; new reward kinds drop in (item/command/money/…).

---

## 4. Crate model & YAML schema

Each crate is one file `crates/<name>.yml`. The in-game editor reads and writes this exact
schema; power users may hand-edit and `/crates reload`.

```yaml
name: vote
display-name: "<gradient:#f0c=>#f60>Vote Crate</gradient>"
animation: csgo            # csgo | roulette | reveal | cascade | physical | instant | quick | pick | lootbox | chesthunt
key:
  type: virtual            # virtual | physical | both
  item:                    # for physical keys
    material: TRIPWIRE_HOOK
    name: "<yellow>Vote Key"
    glow: true
block:                     # optional physical crate
  enabled: true
  locations: ["world,100,65,200"]
  hologram: ["<bold>VOTE CRATE", "<gray>Right-click to open", "<gray>Keys: %nocrates_keys_vote%"]
  particles: VILLAGER_HAPPY
preview:
  enabled: true
  title: "Vote Crate — Rewards"
pity:                      # guaranteed reward every N opens
  enabled: true
  every: 25
  tier: legendary
settings:
  broadcast: true
  cooldown-seconds: 0
rewards:
  - id: diamonds
    rarity: common         # references rarities.yml (color, weight floor, broadcast)
    chance: 50.0           # weight; normalized across the crate
    display:               # shown in preview & some animations
      material: DIAMOND
      name: "<aqua>5 Diamonds"
    actions:
      - "item: DIAMOND 5"
      - "message: <green>You won 5 diamonds!"
    limits:
      max-per-player: -1   # -1 = unlimited
      cooldown-seconds: 0
  - id: rank-vip
    rarity: legendary
    chance: 2.0
    display: { material: NETHER_STAR, name: "<gold>VIP Rank", glow: true }
    actions:
      - "command: lp user %player% parent add vip"
      - "broadcast: <gold>%player% won VIP from the Vote Crate!"
      - "firework: true"
```

**Reward actions** (string mini-DSL, each maps to a `RewardAction`):
`item:`, `command:` (console), `playercommand:`, `message:`, `broadcast:`, `money:`
(Vault), `xp:`, `permission:` (temp/perm), `sound:`, `firework:`, plus custom-item refs
`itemsadder:`, `oraxen:`, `nexo:`, `mmoitems:`, `executableitem:`.

---

## 5. Animations / open styles ("everything")

Implemented behind the `Animation` interface:

1. **CS:GO spinner** — horizontal scrolling row decelerating onto the won reward.
2. **Roulette / wheel** — GUI cells cycle and settle.
3. **GUI reveal** — slots cycle, then settle on the reward.
4. **Cascade / cosmic** — multi-row tiered reveal.
5. **Physical** — crate block pops open, item displays eject, firework.
6. **Instant** — no animation, immediate grant.
7. **QuickOpen / mass-open** — open many keys at once, summary GUI.
8. **Pick-your-reward** — no-gambling: player clicks 1 of several offered rewards.

### 5a. Lootbox (standout #1)
A placed block or held item that, on **right-click, instantly grants a rolled reward** —
no key step. Optional per-player **cooldown** instead of a key. Configured as
`animation: lootbox`; reuses the reward engine. Great for hourly/free loot.

### 5b. Chest Hunt — "pick 4 of 8" (standout #2, novel)
On open, the plugin spawns **8 temporary chests in a 5×5 area** centered on the player.
The player physically walks up and opens **up to K (default 4)**; each opened chest plays a
particle/sound and grants its independently-rolled reward, then is consumed. After K picks
(or a timeout) the remaining chests **vanish** and the area is restored. Fully configurable:
grid size, M chests, K picks, timeout, whether picks are simultaneous reveals.
**GUI variant** (`chesthunt-gui`): a menu of M panes, pick K — for servers avoiding world
clutter. Anti-grief: chests are virtual/temporary, region snapshot restored, no block drops.

---

## 6. Rewards, rarities, keys, pity

- **Weighted roll engine:** each reward has a weight (`chance`); engine normalizes and rolls.
  Deterministically unit-testable (seedable RNG) to verify long-run distribution.
- **Rarities** (`rarities.yml`): name → color, optional broadcast, sort order. Rewards
  reference a rarity for consistent coloring across preview/animations.
- **Per-player limits & cooldowns:** `max-per-player`, reward cooldown; tracked in player data.
- **Pity / milestone:** every N opens of a crate, force a reward of a configured tier;
  progress saved per player.
- **Preview GUI:** lists every reward with its **real normalized chance**.
- **Keys:** physical (item, identified via PDC tag), virtual (per-player balance), multiple
  key types per crate, `key give/all/take`, **timed/temporary** keys (expire).

---

## 7. In-game editor (headline feature)

`/crates editor` opens a GUI hub:
- **Create / delete / clone** a crate.
- **Choose animation** from a menu.
- **Rewards:** add the **item in hand** as a reward; set chance, rarity, display, and
  actions (items/commands/money/etc.) via menus and chat prompts.
- **Keys:** set key type and physical key item.
- **Block & hologram:** bind to the block you're looking at; edit hologram lines.
- **Messages / sounds / settings:** broadcast, cooldown, pity.
- **Preview** the crate; **live reload**, no restart.

Every change serializes back to `crates/<name>.yml` (schema in §4). Files and editor stay
in sync; `/crates reload` re-reads from disk.

---

## 8. Storage & hooks

- **Storage (`DataStore`):** `YamlDataStore` default (zero setup — virtual keys, open
  counts, pity progress, win history, reward limits). `MySqlDataStore` optional via
  **HikariCP** for networks (cross-server keys/data). Async writes; cached in memory.
- **Hooks (all soft-depend; plugin runs fine without any):**
  - **Vault** — economy (`money:`) + permissions.
  - **PlaceholderAPI** — exposes `%nocrates_keys_<crate>%`, `%nocrates_opens_<crate>%`,
    `%nocrates_pity_<crate>%`, etc.
  - **Custom items** — ItemsAdder, Oraxen, Nexo, MMOItems, ExecutableItems (reflective
    providers behind a `CustomItemProvider` interface).
  - **Holograms** — native TextDisplay; optional DecentHolograms / HolographicDisplays.
  - **bStats** — anonymous metrics.

---

## 9. Commands & permissions

- `/crate` — player: open a crate, `preview`, list owned keys.
- `/crates` — admin: `editor`, `reload`, `give <crate> <player>`, `key <give|all|take> …`,
  `list`, `debug`.
- Permission tree under `nocrates.*` (`nocrates.open.<crate>`, `nocrates.admin`,
  `nocrates.editor`, `nocrates.key.*`, …). Full tab-completion.
- All player-facing text in `messages.yml`, formatted with **MiniMessage** (Adventure).

---

## 10. Build phases (one project, sequenced for an early working jar)

**Phase 1 — Core (compiles & playable):** bootstrap, config, `VersionCompat`, crate model
+ YAML, keys (physical+virtual), reward engine (weighted/rarities/pity), crate blocks +
TextDisplay holograms, 3 animations (reveal, CS:GO, instant), preview GUI, commands/perms,
Vault + PAPI, bStats, messages, `YamlDataStore`.

**Phase 2 — Editor + animations:** full GUI editor, roulette/wheel/cascade/physical/quick/
pick animations, win history + broadcasts.

**Phase 3 — Standout + scale:** **Lootbox**, **Chest-Hunt pick-N-of-M** (physical + GUI),
custom-item hooks, `MySqlDataStore`, config import from other plugins.

Push to GitHub throughout, starting once Phase 1 compiles.

---

## 11. Testing & verification

- **Unit tests** (JUnit + MockBukkit) for pure logic: weighted-roll distribution (seeded),
  pity counter, crate YAML parse/serialize **round-trip**, key balance math.
- **Build gate:** every phase must `mvn package` cleanly and produce a valid plugin jar
  (valid shaded `plugin.yml`, relocated deps) before pushing.
- **Manual smoke:** load on a Paper test server where feasible; document any version-specific
  identifier overrides in `VersionCompat` defaults.

---

## 12. Repo layout

```
noCrates/
  pom.xml
  README.md
  LICENSE                      (MIT)
  src/main/java/com/nocrates/  (packages per §3)
  src/main/resources/
    plugin.yml
    config.yml
    messages.yml
    rarities.yml
    crates/example.yml
  src/test/java/com/nocrates/
  docs/superpowers/specs/2026-06-29-nocrates-design.md
```
