# noCrates 2.0 — Design Spec (PhoenixCrates-class rebuild)

**Date:** 2026-07-02
**Repo:** github.com/fizzexual/noCrates
**Status:** Approved (user directive: "delete this, and make it advanced like phoenixcrates + their modules, research how and do it for 1.20-26.2") — autonomous session, design decisions researched and recorded here.

---

## 1. Goal

Delete the v1 implementation and rebuild noCrates as a **PhoenixCrates-class** crates
plugin: same feature depth as PhoenixCrates Premium **plus the equivalents of all of its
paid add-ons ("modules") built in**, open-source, one jar spanning **Minecraft 1.20 →
26.2** (the current 2026 release).

Research basis (July 2026, from official PhoenixCrates docs/store + PaperMC/Mojang
sources) is summarized in §2 and §3; feature parity matrix in §4.

---

## 2. Platform & cross-version strategy (1.20 → 26.2)

Verified facts driving the design:

- Mojang moved to **year-based versions** in 2026: `1.21.11` (Dec 2025) was the last
  `1.x`; then `26.1` "Tiny Takeover" (2026-03-24, **Java 25**), `26.2` "Chaos Cubed"
  (2026-06-16, current stable). "1.20–26.2" therefore spans Java 17 → 21 → 25 servers.
- Paper hard-forked from Spigot at 1.21.4 and holds ~85–90% share; `api-version: 1.20`
  jars still load on 26.x (valid range 1.13–26.1.2). MiniMessage is bundled on Paper;
  the Spigot MiniMessage shim (adventure-platform-bukkit) is archived/dead.
- Paper 26.2 bundles **adventure 5.x** (breaking removals of *deprecated* 4.x API), so we
  only use the stable adventure surface.
- Known cross-version traps for a GUI/cosmetics plugin:
  1. **InventoryView class→interface (1.21)** — never reference `InventoryView` in our
     bytecode; use `InventoryEvent#getInventory`, `getClickedInventory`, `getWhoClicked`,
     and `InventoryHolder`-based menu routing.
  2. **1.20.5 enum renames** (`Particle.REDSTONE`→`DUST`, `VILLAGER_HAPPY`→
     `HAPPY_VILLAGER`, …) and **Sound enum→interface (1.21.3)** — all Material/Sound/
     Particle lookups go through a `compat` resolver that tries name chains at runtime
     (binary `valueOf` shims keep old refs linking; we never hard-reference renamed
     constants).
  3. **CustomModelData component (1.21.4+)** — we use the int API (deprecated but
     functional) and additionally set `item_model` via reflection when present (1.21.2+).
- **Display entities** (`TextDisplay`/`ItemDisplay`) are stable 1.19.4→26.2 — the basis
  for holograms and physical reward displays. No NMS, no packets, no ProtocolLib.

**Decisions:**

- **Build:** Maven, single shaded jar. Compile with JDK 21 at `--release 17` (Java 17
  bytecode runs on Java 17/21/25). `io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT`
  provided; shade bStats + HikariCP + MariaDB client (relocated).
- **Supported servers:** **Paper and forks (Purpur, Folia) 1.20 → 26.2.** Spigot is not
  supported (no bundled adventure; consistent with CrazyCrates/ExcellentCrates in 2026).
- **Folia:** `folia-supported: true`. All scheduling goes through a `Scheduling` facade:
  Folia detected → region/entity/async schedulers; otherwise BukkitScheduler.
- **plugin.yml:** classic format, `api-version: 1.20`.
- Rejected: paperweight/NMS multi-module (obsolete post-hardfork; nothing here needs
  packets), PacketEvents (display entities suffice), paper-plugin.yml (still experimental).

---

## 3. What "PhoenixCrates + their modules" means (research summary)

PhoenixCrates (Phoenix Plugins, €17.99 + €~106 of add-ons) distinguishes itself by:

1. **Three-phase opening animations** — independently pick a **Pre-Open**, **Post-Open**
   and **Reward-Display** animation per crate ("120+ combinations"), plus up to 12
   always-on **idle effects** built from **13 parametric particle shapes** (Circle,
   Spiral, Star, Astroid, Deltoid, Flower, Quatrefoil, Diamond, Ninja Star, Square,
   Conical Spiral, Pulse, Default) × any particle × hex color.
2. **Engine modes** per crate: vanilla **block** (chest/any material with native lid
   animation) or **model item** (CustomModelData display) — we add both; ModelEngine is
   out of scope (proprietary dep).
3. **Keys as first-class objects**, many-to-many linked to crates with per-link **amount**
   and **consumption priority**; physical + **virtual** keys, `/virtualkeys` menu,
   key payments between players, keyless crates.
4. **Deep reward model:** display-item vs **multiple win-items** + win-commands; weight +
   normalized "your chance"; per-player **and global win limits** with cooldowns;
   **restricted permissions** with **alternative reward** fallback; virtual rewards;
   broadcast; **max-win-rewards N** per opening; **Random or Selective (pick-your-reward,
   non-gambling) mode**.
5. **Guaranteed Win (milestones/pity):** Sequential (one-time thresholds) or Repetitive
   (every N), optional chance+threshold mix.
6. **Rerolls:** free per-open + permission groups + admin-granted balances.
7. **Ops features:** open money cost with confirmation menu, per-crate cooldowns,
   quick-open (sneak-click), simultaneous openings, knockback without key, packet-free
   holograms, action logging + **Discord webhooks**, SQLite/MySQL, PlaceholderAPI,
   19-language editor, **migration importers** (CrazyCrates, ExcellentCrates, …).
8. **Add-on system:** separate jars in `plugins/PhoenixCrates/addons/` with `addon.yml`
   + `Addon` base class. Paid add-ons: **Crate Claim, Mass Opening, LootBoxes, Rarities,
   Last Winner**, and 8 animation add-ons (**Physical CSGO, Orbit Roulette, Black Hole,
   Astro Burst, Rainbow, Cyclone Heart, Froggo Boom, Chicken Jockey**).

---

## 4. Parity matrix → noCrates 2.0 scope

Everything below ships **in the base jar, free**. PhoenixCrates' paid add-ons become
**built-in modules** implemented on a public module API (proving the API works), and the
same loader accepts **external addon jars**.

| PhoenixCrates | noCrates 2.0 |
|---|---|
| 3-phase animations + idle shapes | ✅ identical pipeline, 13 shape presets, effect spec strings |
| Engine: block / vanilla model / ModelEngine | ✅ block + model (ItemDisplay + CustomModelData); ModelEngine ❌ (proprietary) |
| Keys: linked, priority, virtual, pay | ✅ full |
| Rewards incl. limits/alt/selective | ✅ full |
| Guaranteed Win seq/repetitive | ✅ full |
| Rerolls | ✅ full |
| Cost+confirm, cooldown, quick-open, knockback | ✅ full |
| Holograms (built-in) | ✅ TextDisplay-based |
| SQLite/MySQL, logging, Discord webhook | ✅ + YAML fallback |
| Editor (crates/keys/rewards/animations/milestones/rerolls/rarities/tiers/migrations) | ✅ full GUI editor + chat prompts |
| Migrations: CrazyCrates, ExcellentCrates, +4 | ✅ CrazyCrates + ExcellentCrates concrete; importer SPI for more |
| Languages (19) | ✅ language-file system, ships en + a few translations |
| Add-on jars (`addons/`, addon.yml, Addon class) | ✅ same mechanism, open API |
| Add-on: Crate Claim | ✅ built-in module `crate-claim` |
| Add-on: Mass Opening (x1–x100/all tiers) | ✅ module `mass-opening` |
| Add-on: LootBoxes | ✅ module `lootboxes` |
| Add-on: Rarities (+key guaranteed rarity) | ✅ module `rarities` |
| Add-on: Last Winner placeholders | ✅ module `last-winner` |
| Animation add-ons (8) | ✅ module `animations-plus` registering all 8 |
| GeyserMC notes | ✅ nothing Java-only in flows; documented |
| Folia | ✅ `folia-supported: true` via Scheduling facade |

Kept from v1 (features PhoenixCrates lacks — our edge): **Chest Hunt** (pick-N-of-M
spawned chests) as module `chest-hunt`, and **PlaceholderAPI + Vault + custom-item hooks
(ItemsAdder/Oraxen/Nexo/MMOItems)** for reward items *and* key items ("item hooks").

---

## 5. Architecture

Single Maven project, base package `com.nocrates`. **Kernel + modules**:

```
com.nocrates
  NoCratesPlugin            bootstrap; wires Services; loads modules last
  core/       Services (DI locator), MainConfig, ReloadManager
  compat/     ServerVersion, Compat (material/sound/particle resolvers),
              Scheduling (Bukkit vs Folia), ItemModelCompat (reflection)
  text/       Text (MiniMessage + legacy-& in, Component out), Lang (languages/*.yml)
  item/       ItemBuilder, ItemSpec (YAML<->ItemStack incl. custom-item hooks, PDC tags)
  menu/       Menu framework (InventoryHolder routing, pages, fill, configurable
              MenuConfig loaded from menus/*.yml), ChatPrompt
  action/     ActionType SPI + parser: [MESSAGE][TITLE][ACTION_BAR][CLOSE_INVENTORY]
              [COMMAND][GAMEMODE][POTION_EFFECT][SOUND][MENU][DELAY][OPEN]
  key/        Key model (first-class), KeyRegistry (keys.yml), KeyService
              (balances, physical PDC tagging, links, priority consumption, pay)
  crate/      Crate model, CrateSerializer (YAML round-trip), CrateRegistry,
              placement (locations, attach/detach/placeCrate), engine modes
              (BlockEngine, ModelEngine[ItemDisplay]), Hologram, click routing
  reward/     Reward model, RollEngine (weighted, seedable), WinLimits,
              GuaranteedWin (sequential/repetitive), RewardGrant (win-items/commands/
              broadcast/alternative), SelectiveMode
  open/       OpenService: full pipeline checks→cost confirm→consume→roll→animate→
              grant→record; OpenSession; QuickOpen; simultaneity guard
  animation/  AnimationService; phase SPIs: PreOpenAnimation, PostOpenAnimation,
              RewardDisplayAnimation; IdleEffect engine: ShapeRenderer (13 curves),
              EffectSpec parser ("SHAPE;{PARTICLE;#hex;ox;oy;oz;r;vel;n}");
              built-in phases (§6); GuiRoulette (menu-based opening)
  reroll/     RerollService (free/groups/granted balances), reroll menu wiring
  stats/      StatsService (opens, per-reward wins), placeholders data
  storage/    DataStore SPI: YamlDataStore, SqliteDataStore, MySqlDataStore(HikariCP);
              PlayerData (keys, cooldowns, opens, win counts, milestones, rerolls, claims)
  logging/    ActionLogger (file), DiscordWebhook (async embeds)
  module/     ModuleManager: loads built-in modules (modules.yml toggles) and external
              jars from addons/ (addon.yml, URLClassLoader); Addon base class
              (onLoad/onEnable/onDisable), NoCratesApi facade, custom events
  modules/    crateclaim/ massopen/ lootboxes/ rarities/ lastwinner/
              animationsplus/ chesthunt/          (each only touches public API)
  hook/       Vault, PlaceholderAPI expansion, custom items (ItemsAdder/Oraxen/Nexo/
              MMOItems reflective providers), bStats
  command/    /crates command tree + tab completion (mirrors PhoenixCrates verbs)
  editor/     Editor hub + sub-editors (crates, rewards, keys, animations, milestones,
              rerolls, rarities, mass-open tiers, migrations, language)
  migrate/    Importer SPI; CrazyCratesImporter, ExcellentCratesImporter
```

**Public extension surface** (what addon jars can use): `Addon`, `NoCratesApi`
(registries + services), SPIs `PreOpenAnimation/PostOpenAnimation/RewardDisplayAnimation/
IdleShape/ActionType/DataStore/Importer/CustomItemProvider`, and events
(`CrateOpenEvent`, `RewardWinEvent`, `KeyChangeEvent`, `CrateRegisterEvent`).

**Data flow (open):** click crate → `OpenService.attempt` → checks (enabled, perm, cooldown,
cost→confirm menu, keys by priority) → consume → `RollEngine` (+GuaranteedWin override,
WinLimits filter→alternative) → `OpenSession` → `AnimationService.play(pre→post→display)`
(or Selective/GUI path) → `RewardGrant.grant` (idempotent) → stats/log/webhook/events.

---

## 6. Animation system (the headline)

- **Idle effects** (per crate, list): parsed from `"SPIRAL;{DUST;#7b5cff;0;0.2;0;1.2;0.05;2}"`.
  `ShapeRenderer` computes parametric points per tick for: CIRCLE, SPIRAL, CONICAL_SPIRAL,
  STAR, NINJA_STAR, SQUARE, DIAMOND, ASTROID, DELTOID, FLOWER, QUATREFOIL, PULSE, DEFAULT.
  Colorable particles use DUST options; others use velocity/offset. Runs on a single
  repeating task per world batch; only within render-radius of players.
- **Opening = 3 chained phases** with per-phase configurable delays:
  - **Pre-Open** (at the crate, before reveal): DEFAULT, CRACK, LIGHTNING, KEY_OPENER,
    FIRE, BLASTING, SONIC_BOOM
  - **Post-Open** (burst): BALL, SWIRL, FIRE, COMPACT, ROTATING_HEAD
  - **Reward-Display** (shows won item): DEFAULT (hover ItemDisplay + name), HELIX,
    SMOKE_SPIRAL, FIRE_SPIRAL, PHYSICAL_ITEM, FIREWORK
  - Module `animations-plus` adds: PHYSICAL_CSGO (vertical roulette above crate),
    ORBIT_ROULETTE, ASTRO_BURST, BLACK_HOLE (pull + effects), RAINBOW (arcs),
    CYCLONE_HEART, FROGGO_BOOM (frog dive), CHICKEN_JOCKEY (charging jockey) — split
    across pre/post phases as in PhoenixCrates.
- **GUI animations:** GUI_CSGO (horizontal spinner menu) usable instead of world phases;
  also the Selective-mode chooser menu. Block engine uses native chest lid open/close
  where the material supports it.
- Animations are **visual only**; grant logic lives in `OpenSession.finish()` (idempotent,
  runs even if visuals fail). `simultaneous-openings: false` queues/locks the crate.

---

## 7. Config surface (files)

```
config.yml         general: render-radius, knockback, quick-open, database{type,...},
                   logging{file, discord{enabled,url}}, language
modules.yml        enable/disable each built-in module
keys.yml           first-class keys: item spec, virtual, glow, links
crates/<id>.yml    per crate: enabled, display-name, engine{type,block-material,model},
                   locations[], permission{}, open{cost,cooldown,quick-open,
                   simultaneous,knockback}, keys[{id,amount,priority}]|none,
                   hologram{lines,offset}, preview{enabled,menu}, rewards-mode,
                   max-win-rewards, animation{idle[],pre-open,post-open,reward-display,
                   delays{}}, rewards{id:{display-item,win-items[],win-commands[],
                   percentage,broadcast,virtual,share-online,win-limits{player,global,
                   player-cooldown,global-cooldown},restricted-permissions[],
                   alternative-reward{},rarity}}, guaranteed{mode,milestones[]},
                   reroll{enabled,free,groups[]}
menus/*.yml        preview/confirmation/selective/reroll/virtual-keys/mass-open menus
languages/*.yml    all player-facing text (MiniMessage; en_US shipped + de_DE, fr_FR,
                   es_ES, pt_BR, ru_RU, zh_CN, pl_PL, tr_TR, nl_NL starter translations)
logger.yml         what to log + webhook formats
rarities.yml       (module) rarity tiers: display, sync-percentage, key guarantees
```

ItemStacks in YAML use a readable `ItemSpec` (material/amount/name/lore/glow/
custom-model-data/item-model/head-texture/enchants/flags/custom-item ref) — *not* raw
Bukkit serialization — so files stay hand-editable and diff-able.

---

## 8. Commands, permissions, placeholders

`/crates` (aliases `/crate`, `/nocrates`, `/nc`) subcommands mirroring PhoenixCrates:
`open <crate> [player]`, `preview <crate> [player]`, `claim`, `virtualkeys`,
`create|delete|clone|edit <crate>`, `editor`, `enable|disable <crate>`,
`givecrate <crate> <player> [n]`, `placecrate <crate> <world> <x> <y> <z>`,
`attach <crate>` / `detach`, `givereward <crate> <reward> <player|all>`,
`giverandomreward <crate> <player|all>`, `key give|giveall|take|set|check|pay|reset …`,
`reroll give|take …`, `massopen <crate> <tier>`, `migrate <plugin>`,
`resetcooldown <crate> <player>`, `resetwinlimit player|global …`, `reload`, `list`,
`stats [player]`.

Permissions under `nocrates.*`: `nocrates.admin`, `nocrates.command.<sub>`,
`nocrates.crate.<id>`, `nocrates.editor`, `nocrates.massopen.<tier|all>`,
`nocrates.reroll.<group>`, wildcard parents in plugin.yml.

Placeholders (PAPI): `%nocrates_keys_<key>%`, `%nocrates_cooldown_<crate>%`,
`%nocrates_opened_<crate>%`, `%nocrates_opened_total%`, `%nocrates_rerolls_<crate>%`,
`%nocrates_guaranteed_amount_<crate>%`, `%nocrates_guaranteed_reward_<crate>%`,
`%nocrates_winlimit_<crate>_<reward>%`, `%nocrates_lastwinner_<field>_<crate>_<n>%` (module).

---

## 9. Storage, logging, hooks

- `DataStore` SPI. Default **YAML** (zero-setup), **SQLite** if the server-bundled driver
  is present (it is on Paper), **MySQL/MariaDB** via shaded HikariCP for networks.
  Async writes, in-memory cache, save-on-quit + periodic flush + onDisable flush.
- `ActionLogger`: opens/wins to rotating file; **Discord webhook** posts embeds async
  (Java 11 HttpClient), fire-and-forget with rate-limit backoff.
- Hooks (all optional, reflection-guarded): Vault (open cost, money rewards),
  PlaceholderAPI, ItemsAdder/Oraxen/Nexo/MMOItems (reward items + key "item hooks"),
  bStats (id kept from v1).

---

## 10. Editor & migrations

- `/crates editor`: hub → Crates (create/clone/delete/enable, per-crate: engine, keys,
  animation pickers listing every registered phase animation, idle-effect builder
  (shape→particle→color→params), rewards (drag-drop item to add; edit percentage via
  chat prompt; limits; restricted perms; alternative reward; reorder), milestones,
  reroll, settings) → Keys → Rarities → Mass-open tiers → Migrations → Language.
- All editor writes go through `CrateSerializer`/registries → files stay canonical;
  `/crates reload` re-reads everything.
- Migrations menu runs `Importer`s: **CrazyCrates** and **ExcellentCrates** map their
  crate YAMLs (rewards/chances/keys/preview) best-effort into ours, report a summary.

---

## 11. Testing & verification

- JUnit 5 (no MockBukkit — pure-logic tests only): EffectSpec parser, ShapeRenderer math
  (point counts/bounds), action parser, RollEngine distribution (seeded), GuaranteedWin
  sequential/repetitive, WinLimits, key link priority consumption order, CrateSerializer
  and ItemSpec YAML round-trips, importer mapping fixtures.
- **Build gate at every phase:** `mvn -q clean package` green (tests + shaded jar).
- Manual smoke on a Paper server is out of scope for this session; compat layer logs
  clear warnings for unresolvable identifiers instead of failing.

---

## 12. Delivery plan (phases)

1. **Platform:** delete v1; scaffold pom/plugin.yml/bootstrap; compat+scheduling+text+
   item+menu+action+storage foundations; module loader + public API. *(gate)*
2. **Crates engine:** keys, crate model/serializer/registry, placement+holograms+click,
   rewards+limits+guaranteed+roll, open pipeline, selective, reroll, stats, logging,
   placeholders, commands. *(gate)*
3. **Animations:** idle shape engine, 3-phase framework, all built-in phases, GUI CS:GO,
   engine modes. *(gate)*
4. **Modules:** crate-claim, mass-opening, lootboxes, rarities, last-winner,
   animations-plus, chest-hunt — all via the public API; external addon loading. *(gate)*
5. **Editor + migrations + polish:** full editor, importers, languages, README, final
   verification. *(gate)*

Commits are conventional-commit style per feature; push at phase gates.
