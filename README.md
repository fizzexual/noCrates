<h1 align="center">✦ noCrates ✦</h1>

<p align="center">
  <b>The advanced, free & open-source crates plugin.</b><br>
  Cinematic three-phase openings · always-on particle auras · linked keys · pick-your-reward mode ·
  guaranteed wins · rerolls · a real addon system — <b>one jar for Minecraft 1.20 → 26.2</b>.
</p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20%20%E2%86%92%2026.2-7b5cff?style=for-the-badge">
  <img alt="Server" src="https://img.shields.io/badge/Paper%20·%20Purpur%20·%20Folia-supported-2f6fed?style=for-the-badge">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-3ddc84?style=for-the-badge">
</p>

<p align="center">
  <a href="https://github.com/fizzexual/noCrates/releases/latest"><b>⬇ Download the latest release</b></a>
  ·
  <a href="#-quick-start">Quick start</a>
  ·
  <a href="#-commands--permissions">Commands</a>
  ·
  <a href="#-build-your-own-addons">Addon API</a>
</p>

---

## ✨ Highlights

- 🎬 **Cinematic openings** — every crate chains three independently chosen phases (*pre-open → post-open → reward display*) from a pool of **26 animations**: lightning strikes, sonic booms, black holes, orbiting reward reels, a frog that cannonballs into your crate, a CS:GO-style spinner both in-world *and* as a GUI, and more. Mix them into hundreds of combinations — per crate, in-game.
- 🌀 **Idle auras** — crates glow around the clock with **13 parametric particle shapes** (spirals, stars, astroids, flowers, pulses…) in any particle and any color, built with a point-and-click wizard or a one-line spec.
- 🔑 **Keys done right** — keys are first-class objects linked to crates many-to-many with per-link *amounts* and *consumption priority*. Virtual (dupe-proof) and physical keys, key payments between players, and keys that **guarantee a rarity tier**.
- 🎁 **A reward engine with teeth** — multiple win-items + commands per reward, per-player *and* global win limits with cooldowns, permission-restricted rewards with automatic **alternative rewards**, broadcast/virtual/shared rewards, and up to **30 rewards per opening**.
- 🧭 **Two ways to win** — classic weighted **RANDOM** rolls, or **SELECTIVE** mode where players *choose* their reward for a key cost. No gambling, no chargebacks, P2W-friendly.
- 🛡️ **Guaranteed win** — pity milestones that fire every N opens (*repetitive*) or at one-time thresholds (*sequential*), with optional chance mixing and live placeholders.
- 🎲 **Rerolls** — free-per-open, permission-group and admin-granted reroll balances stack; a claim-or-reroll menu appears after the animation and closing it always claims, so a reroll can never void a reward.
- 🧩 **Seven built-in modules + external addons** — everything below ships in the jar, and the same loader runs *your* addon jars.
- 🖥️ **Edit everything in-game** — `/crates editor` covers crates, rewards (drag your held item in), keys, links, animations, idle effects, milestones and migrations. Files stay clean, hand-editable YAML.
- 🚀 **One jar, every modern version** — Minecraft **1.20 through 26.2**, Paper/Purpur/**Folia**, no NMS, no packet library, no version-specific downloads.

## 🧩 Modules

Toggle any of them in `modules.yml` — no extra downloads, no paywalls.

| Module | What it gives you |
|---|---|
| **rarities** | Rarity tiers with synchronized drop rates across a tier, rarity labels in previews, and keys that guarantee a tier |
| **last-winner** | Rolling winner-history placeholders per crate (`%nocrates_lastwinner_player_<crate>_1%` …) |
| **crate-claim** | Rewards that don't fit the winner's inventory are stored — `/crates claim` menu with claim-all |
| **mass-opening** | Open x1/x6/x12/x32/x64/**ALL** keys at once behind per-tier permissions, with an aggregated summary instead of chat spam |
| **lootboxes** | Give players placeable one-shot crates: place it, watch the animation, box gone |
| **animations-plus** | The showcase pack — Physical CSGO, Orbit Roulette, Black Hole, Astro Burst, Rainbow, Cyclone Heart, Froggo Boom, Chicken Jockey |
| **chest-hunt** | Opening spawns M chests around you — crack open K of them, each with its own roll, then the area restores itself |

## 🎬 The animation system

Every opening is `pre-open → post-open → reward-display`, each phase picked independently:

| Phase | Built-in choices |
|---|---|
| **Pre-open** | `DEFAULT` `CRACK` `LIGHTNING` `KEY_OPENER` `FIRE` `BLASTING` `SONIC_BOOM` `RAINBOW` `CYCLONE_HEART` `FROGGO_BOOM` `CHICKEN_JOCKEY` |
| **Post-open** | `BALL` `SWIRL` `FIRE` `COMPACT` `ROTATING_HEAD` `PHYSICAL_CSGO` `ORBIT_ROULETTE` `ASTRO_BURST` `BLACK_HOLE` |
| **Reward display** | `DEFAULT` `HELIX` `SMOKE_SPIRAL` `FIRE_SPIRAL` `PHYSICAL_ITEM` `FIREWORK` `GUI_CSGO` `CHEST_HUNT` |

Timings are per-phase and per-crate; a watchdog force-completes any stuck phase so **an animation bug can never eat a reward**. Everything is display-entity and particle based — no NMS.

Idle auras use a compact spec you can also hand-write:

```yaml
animation:
  idle:
    - "SPIRAL;{DUST;#7b5cff;0;0.1;0;0.9;0;2}"     # SHAPE;{PARTICLE;#color;offX;offY;offZ;radius;velocity;amount}
    - "CIRCLE;{DUST;#ff5ca8;0;1.4;0;0.5;0;1}"
```

Shapes: `CIRCLE SPIRAL CONICAL_SPIRAL STAR NINJA_STAR SQUARE DIAMOND ASTROID DELTOID FLOWER QUATREFOIL PULSE DEFAULT`.

## 🚀 Quick start

1. Drop the jar from [Releases](https://github.com/fizzexual/noCrates/releases/latest) into `plugins/` on **Paper 1.20+** (Purpur and Folia work too) and restart.
2. `/crates editor` → **Crates → Create** — or copy one of the example crates below.
3. Give yourself keys: `/crates key give vote <you> 10`.
4. Look at a block and `/crates attach <crate>` — right-click opens (sneak-click = quick open), left-click previews.

A placed crate gets its hologram, idle aura and native chest-lid animation automatically; or switch the engine to `MODEL` for a floating item-display crate (CustomModelData / `item_model` supported).

### 📦 Six ready-made example crates

Fresh installs ship one fully-commented example per feature — open them in `crates/`, copy, tweak:

| Crate | Demonstrates | Try it |
|---|---|---|
| `example` | The basics: weighted rewards, alternative reward, repetitive pity | `/crates key give vote <you> 5` |
| `shop` | **SELECTIVE** pick-your-reward mode with virtual token currency | `/crates key give token <you> 20` → `/crates open shop` |
| `legendary` | Paid opens + confirmation, rarities, **sequential milestones**, rerolls, win limits, restricted perms + alternative, 2 rewards/open, legendary-guarantee key | `/crates key give premium <you> 5 physical` |
| `hunt` | **Chest Hunt** — temporary chests spawn around you, pick 4 of 8 | `/crates attach hunt` in an open area |
| `daily` | **Keyless** crate on a 24h cooldown + **lootbox** one-shot items + GUI spinner | `/crates lootbox give daily <you> 3` |
| `showcase` | **MODEL** engine (floating crate) + animations-plus circus (chicken jockey → black hole), 3 rewards per open, layered idle auras | `/crates open showcase` |

## ⌨ Commands & permissions

Aliases `/crates` `/crate` `/nocrates` `/nc`. Admin verbs need `nocrates.command.<verb>` or `nocrates.admin`.

| | |
|---|---|
| **Players** | `open <crate>` · `preview <crate>` · `virtualkeys` · `claim` · `key pay <key> <player> <n>` · `stats` |
| **Crates** | `create` `delete` `clone` `enable` `disable` · `edit [crate]` / `editor` · `list` |
| **Placement** | `attach <crate>` · `detach` · `placecrate <crate> <world> <x> <y> <z>` · `givecrate <crate> <player> [n]` · `lootbox give <crate> <player> [n]` |
| **Rewards** | `givereward <crate> <reward> <player\|all>` · `giverandomreward <crate> <player\|all>` · `resetwinlimit player\|global <crate> [player]` |
| **Keys** | `key give\|giveall\|take\|set\|check <...>` (append `physical` to give the item) |
| **Extras** | `massopen <crate>` · `reroll give\|take <crate> <player> <n>` · `resetcooldown <crate> <player>` · `migrate <source>` · `reload` |

Key permission nodes: `nocrates.open` (default true), `nocrates.crate.<id>`, `nocrates.editor`, `nocrates.massopen.<tier|all>`, `nocrates.reroll.<group>`, `nocrates.claim`, `nocrates.admin`.

## 📊 Placeholders (PlaceholderAPI)

```
%nocrates_keys_<key>%                        %nocrates_cooldown_<crate>%
%nocrates_opened_<crate>%                    %nocrates_opened_total%
%nocrates_rerolls_<crate>%                   %nocrates_winlimit_<crate>_<reward>%
%nocrates_guaranteed_amount_<crate>%         %nocrates_guaranteed_reward_<crate>%
%nocrates_globalwinlimit_<crate>_<reward>%   %nocrates_lastwinner_<field>_<crate>_<n>%
```
(`<field>` = player / reward / second / minute / hour / day / month / year)

## 🗂 Configuration layout

```
plugins/noCrates/
├─ config.yml        language, render radius, storage (YAML/SQLITE/MYSQL), logging + Discord webhook
├─ modules.yml       toggle the built-in modules + their settings
├─ keys.yml          key items, virtual flags, rarity guarantees
├─ rarities.yml      rarity tiers (rarities module)
├─ crates/           one fully-commented YAML per crate
├─ menus/            restyle every GUI (preview, confirmation, selective, reroll, virtualkeys, massopen)
├─ languages/        en_US · de_DE · fr_FR · es_ES · pt_BR · ru_RU · zh_CN · pl_PL · tr_TR · nl_NL
├─ addons/           drop external addon jars here
└─ logs/             daily action logs (plus optional Discord webhooks)
```

Menu buttons run action lines — usable by addons too:
`[MESSAGE] [BROADCAST] [TITLE] [ACTION_BAR] [CLOSE_INVENTORY] [COMMAND] [GAMEMODE] [POTION_EFFECT] [SOUND] [MENU] [OPEN] [DELAY]`

## 🔌 Integrations

**Vault** (paid opens with a confirmation menu) · **PlaceholderAPI** · **ItemsAdder / Oraxen / Nexo / MMOItems** — use `custom-item: "itemsadder:ruby_crown"` in any item (rewards, keys, crate models) · **MySQL/MariaDB** for cross-server networks · **GeyserMC-friendly** flows · **Folia** region threading. Every integration is optional; the plugin runs fully standalone.

Coming from another crates plugin? `/crates migrate crazycrates` or `/crates migrate excellentcrates` imports crates, rewards, chances, commands and keys best-effort and prints a report of anything that needs a manual touch.

## 🛠 Build your own addons

Drop a jar into `plugins/noCrates/addons/` with an `addon.yml`:

```yaml
name: MyAddon
main: com.example.MyAddon
version: 1.0
author: you
```

```java
public final class MyAddon extends Addon {
    @Override
    public void onEnable() {
        api().registerPre(new MeteorShowerAnimation());   // appears in the editor pickers
        api().registerShape(new HeartShape());            // usable in idle-effect specs
        api().registerAction(new DiscordPingAction());    // usable as [DISCORD_PING] lines
        api().registerCommand("meteor", (sender, args) -> { /* /crates meteor */ });
        api().registerPlaceholder("meteor", (player, rest) -> "...");
    }
}
```

Listen to `CrateOpenEvent` (cancellable, outcome mutable), `RewardWinEvent` and `KeyChangeEvent`. The full service surface (crates, keys, rolls, storage, menus, language) is on `api()`.

## 🧱 One jar, 1.20 → 26.2 — how?

| Minecraft | Java runtime | Servers |
|---|---|---|
| 1.20 – 1.20.4 | 17+ | Paper, Purpur |
| 1.20.5 – 1.21.11 | 21 | Paper, Purpur, Folia |
| 26.1 – 26.2 | 25 | Paper, Purpur, Folia |

The jar is Java-17 bytecode built against the stable 1.20 API, and the codebase never
hard-references anything that changed between 1.20 and 26.2: materials, sounds, particles
and potion effects resolve **by name with rename chains**, entities spawn by **class**
instead of enum constants, menus never touch `InventoryView`, and all scheduling flows
through a Folia-aware facade. Requires Paper or a Paper fork (Spigot lacks the bundled
text engine that powers MiniMessage formatting).

## 🧪 Building from source

```bash
git clone https://github.com/fizzexual/noCrates && cd noCrates
mvn clean package        # JDK 21+ → target/noCrates-2.0.0.jar
```

37 unit tests cover the roll engine, guaranteed-win logic, win limits, key-link planning,
the effect/action parsers, shape math and YAML round-trips. Metrics (bStats) are disabled
by default in source — register your own id if you fork.

## 📜 License

[MIT](LICENSE) © 2026 fizzexual — free forever, contributions welcome.
