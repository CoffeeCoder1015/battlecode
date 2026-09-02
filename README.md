# Battlecode 2025 Bot

A collection of Java bots developed for MIT Battlecode 2025.

Team bytebyte: 91/504 placement in the [leaderboards](https://play.battlecode.org/bc25java/rankings).

Battlecode is a strategy programming competition in which every robot runs autonomously. The 2025 game centers on spreading paint, constructing towers on ruins, managing resources, and fighting for control of the map.

This repository preserves several generations of the bot. The latest generation is [`s2rd`](java/src/s2rd/).

## Highlights

Battlecode robots have limited vision, separate memory, and a fixed computation budget each turn. The bot therefore uses local algorithms instead of maintaining a shared map or planning the entire game ahead.

### Diffusion Movement

Battlecode robots have limited vision and do not know the map beforehand, so diffusion is used to explore when there is no clear target.

Each robot keeps moving in one direction until it is blocked by a wall or another robot, then changes direction and continues exploring. When it finds a ruin or enemy tower, the relevant target behavior takes priority.

Implementation: [diffusion movement](java/src/s2rd/Pathing.java#L46).

### Finite-State Control

Each robot selects between a small number of behaviors using its current observations and stored state.

A soldier prioritizes tower combat, active construction, new construction, and finally exploration. Flags and counters preserve unfinished work between turns, while higher-priority behaviors stop lower-priority behaviors from running that turn.

Implementation: [soldier controller](java/src/s2rd/Soldier.java#L33).

### Online Greedy Decisions

Robots must act before they know the rest of the map, so they repeatedly choose the best option visible during the current turn.

Soldiers select the nearest unfinished ruin, attackers select the nearest enemy tower, and towers prefer low-health targets. Moppers search every possible swing direction and choose the one that hits the most enemies.

Implementations: [ruin selection](java/src/s2rd/Soldier.java#L82), [tower selection](java/src/s2rd/TowerEngager.java#L25), [tower target selection](java/src/s2rd/Tower.java#L128), and [mop swing search](java/src/s2rd/Mopper.java#L16).

### Grid Packing

Resource patterns occupy fixed areas of the map, so placing them is a grid-packing problem.

Before starting a pattern, a soldier checks for unfinished ruins and conflicting resource patterns. It permits the cardinal spacing where neighboring patterns fit, then converts map positions into coordinates within the pattern to paint the correct cells.

Implementation: [resource pattern placement](java/src/s2rd/Soldier.java#L158).

## Bot Evolution

| Package | Role |
| --- | --- |
| [`s1`](java/src/s1/) | First integrated bot with construction, combat, and economy logic in one controller. |
| [`s1d`](java/src/s1d/), [`s1d2`](java/src/s1d2/), [`s1dMixed`](java/src/s1dMixed/) | Movement experiments combining diffusion, paint awareness, and different robot behaviors. |
| [`s2`](java/src/s2/) | Refactor into separate controllers for soldiers, moppers, splashers, towers, pathing, and tower engagement. |
| [`s2rd`](java/src/s2rd/) | Latest generation, adding energy-based diffusion, crowd awareness, reflected spawn directions, and further economy tuning. |

The older packages are useful snapshots of how individual ideas changed during development rather than merely obsolete copies.

## Project Structure

- `java/src/` — Java bot implementations.
- `java/maps/` — custom Battlecode maps.
- `java/test/` — Java test sources.

## Running a Match

The project uses the official [Battlecode 2025 scaffold](https://github.com/battlecode/battlecode25-scaffold).

From the `java` directory:

```sh
./gradlew build
./gradlew run -PteamA=s2rd -PteamB=s2 -Pmaps=DefaultSmall
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

The bot names and map can also be changed in [`java/gradle.properties`](java/gradle.properties#L1).
