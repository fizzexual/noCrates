<h1 align="center">noCrates</h1>

<p align="center">
  <b>An advanced, free, open-source crates plugin for Minecraft — built to do everything the community wants.</b><br>
  In-game editor · weighted rewards · animated openings · physical &amp; virtual keys · one jar for MC&nbsp;1.20&nbsp;→&nbsp;latest.
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/Minecraft-1.20%20%E2%86%92%2026.x-7b5cff">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-orange">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue">
</p>

---

## Highlights

- **One jar, every modern version.** Compiled to Java 17 bytecode against the stable Paper API, with a `VersionCompat` layer that resolves materials/sounds/particles by name. The same build loads on **MC 1.20 (Java 17), 1.21 (Java 21) and 26.x (Java 25)** — no NMS, no per-version downloads.
- **In-game GUI editor.** `/crates editor` — create crates, pick animations, add the item in your hand as a reward, set chances/rarities, keys, cooldowns and pity, all through menus with live save. No file editing required.
- **Six animations + two standout mechanics.** CS:GO spinner, GUI reveal, roulette, cascade, in-world physical and instant — plus a **Lootbox** (right-click a block, no key, instant reward) and the novel **Chest Hunt** (spawns a grid of chests around you; open only a few). Pluggable `Animation` interface.
- **Physical *and* virtual keys.** Place crate blocks with floating holograms, or run fully virtual crates openable from a command. Physical keys are tagged with persistent data so they survive renames and stacking.
- **Deep reward system.** Weighted chances, colour-coded rarities, per-player win limits & cooldowns, and a **pity/milestone** system that guarantees a tier every N opens.
- **In-game friendly.** Live `/crates reload`, bind a crate to the block you're looking at with `/crates setblock`, preview every reward's real drop chance.
- **Integrations, all optional.** Vault (money/permission rewards), PlaceholderAPI, custom items (ItemsAdder / Oraxen / Nexo / MMOItems), and bStats. **YAML storage by default, optional MySQL/MariaDB** for cross-server networks. The plugin runs perfectly with none of these installed.

## Cross-version support

| Minecraft | Java runtime | Status |
|---|---|---|
| 1.20 – 1.20.6 | 17+ | ✅ |
| 1.21 – 1.21.x | 21+ | ✅ |
| 26.1+ | 25+ | ✅ |

> Built against `paper-api 1.20.1`. Material/sound/particle names that changed between versions are resolved with fallbacks; you can override any of them in config if a future version renames something.

## Installation

1. Download `noCrates-x.y.z.jar` from [Releases](https://github.com/fizzexual/noCrates/releases) (or build it — see below).
2. Drop it in your server's `plugins/` folder and restart.
3. (Optional) Install **Vault** + an economy plugin and **PlaceholderAPI**.
4. Edit `plugins/noCrates/crates/example.yml` or make new crate files, then `/crates reload`.

## Quick start

```text
/crates key give vote <you> 5      # give yourself 5 Vote keys
/crate vote                        # open it (CS:GO animation)
/crate preview vote                # see every reward and its chance
/crates setblock vote              # bind the block you're looking at as a Vote Crate
```

## Commands

| Command | Permission | Description |
|---|---|---|
| `/crate [name]` | `nocrates.open` | Open a crate (or list your crates with no args) |
| `/crate preview <name>` | `nocrates.open` | Preview a crate's rewards & chances |
| `/crates editor [crate]` | `nocrates.editor` | Open the in-game crate editor |
| `/crates reload` | `nocrates.admin` | Reload all configs |
| `/crates list` | `nocrates.admin` | List configured crates |
| `/crates give <crate> <player> [n]` | `nocrates.admin` | Give a **physical** key item |
| `/crates key give\|giveall\|take <crate> [player] [n]` | `nocrates.admin` | Manage **virtual** keys |
| `/crates open <crate> [player]` | `nocrates.admin` | Force-open a crate |
| `/crates setblock <crate>` | `nocrates.admin` | Bind the block you're looking at |

## Crate configuration

Each crate is one file: `plugins/noCrates/crates/<name>.yml`. All text uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html).

```yaml
display-name: "<gradient:#7b5cff:#ff5ca8><bold>Vote Crate</bold></gradient>"
animation: csgo                 # csgo | reveal | roulette | cascade | physical | instant | chesthunt
key:
  type: both                    # virtual | physical | both
  id: vote
  item: { material: TRIPWIRE_HOOK, name: "<yellow>Vote Key", glow: true }
pity: { enabled: true, every: 25, tier: legendary }
settings: { broadcast: true, cooldown-seconds: 0 }
rewards:
  diamonds:
    rarity: common
    chance: 50.0                # relative weight, normalised across the crate
    display: { material: DIAMOND, name: "<aqua>5 Diamonds", amount: 5 }
    actions:
      - "item: DIAMOND 5"
      - "message: <green>You won 5 diamonds!"
```

### Reward actions

`item: MATERIAL [amount]` · `command: <console cmd>` · `playercommand: <cmd>` ·
`message: <text>` · `broadcast: <text>` · `money: <amount>` (Vault) · `xp: <amount>` ·
`permission: <node>` (Vault) · `sound: <key> [vol] [pitch]` · `firework: true` ·
`customitem: <itemsadder|oraxen|nexo|mmoitems> <id>` (give an item from a custom-item plugin)

`%player%` is replaced with the winner's name in commands and messages.

## Placeholders (PlaceholderAPI)

`%nocrates_keys_<crate>%` · `%nocrates_opens_<crate>%` · `%nocrates_pity_<crate>%` (opens until the next milestone).

## Building from source

Requires JDK 17+ and Maven.

```bash
mvn clean package
# -> target/noCrates-x.y.z.jar
```

## Roadmap

The in-game editor, six animations, lootbox, chest hunt, MySQL storage and custom-item rewards are all **live**. Still planned:

- **"Pick your reward" GUI** — a no-gambling, choose-one-of-N opening.
- **Quick / mass open** — open many keys at once with a summary screen.
- **Win history GUI** and per-crate leaderboards.
- **Config import** from CrazyCrates / ExcellentCrates.

## License

[MIT](LICENSE) © 2026 fizzexual
