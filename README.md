# Deprived

[![build](https://github.com/jacobthomasjohnson/deprived/actions/workflows/build.yml/badge.svg)](https://github.com/jacobthomasjohnson/deprived/actions/workflows/build.yml)

A sleep-deprivation horror mod for Minecraft 1.20.1 (Fabric) built on ambiguity rather than jumpscares. The longer a player avoids sleep, the more unreliable the world becomes: distant footsteps, knocks from empty rooms, figures at the edge of vision, and an image that slowly loses its signal. Sleeping resets it.

Current release: **1.3.1, Control Room**. See the [changelog](CHANGELOG.md) for version history.

## How it's built

- **Java on Fabric**, split into server-side logic (`src/main`) and client rendering and UI (`src/client`), with custom network packets for configuration, debug, visual, and hallucination events.
- **Stage-based progression** driven by Minecraft's persistent time-since-rest statistic, with a stable per-player symptom profile and event selection weighted by darkness, caves, storms, and isolation.
- **Positional audio** from original field recordings, processed for game use by a PowerShell script in `tools/`, with distance and volume ranges tuned per event type.
- **Live Control Room** (`F6` or Mod Menu): visual changes apply instantly, and system settings go to the server through an operator permission check.
- **Deterministic testing**: commands to set the deprivation stage and fire any individual event, plus a developer HUD that reports the active state.
- **CI**: GitHub Actions builds the mod with Gradle on every push and pull request.

## Default progression

1. **Uneasy** (day 1) — distant doors, knocks, whispers and ambience.
2. **Disturbed** — scratches, impacts and environmental inconsistencies.
3. **Delirious** — sparse grain, peripheral movement, breathing, footsteps and watchers.
4. **Fractured** — deeper corners, darkness, false impacts and mild fatigue.
5. **Unraveling** — false Creepers, multiple figures, weakness and occasional clusters.
6. **Lost** (day 7) — rare collapse events, survival pressure and uncertain real damage.

## Playtest commands

- `/deprived status` — exact awake days and stage.
- `/deprived profile` — the player's stable symptom weights.
- `/deprived debug` — toggle the compact developer HUD for the invoking player.
- `/deprived setdeprivation <hours>` — set test state; 24 hours is one Minecraft day and triggers one immediate sample event.
- `/deprived trigger <event>` — deterministic individual event testing.
- `/deprived reload` — reload configuration.

Suggested stage hours: `24`, `53`, `82`, `111`, `140`, `168`.

The developer HUD shows awake time, stage, symptom weights, live context, next-event countdown, last event, pool size, server sequences, client hallucination count, peripheral/visual state, and configured grain/shadow strength. It is disabled by default and never appears for other players.

Long footsteps and disturbance sounds are anchored to fixed world coordinates. They do not follow the player, and their custom 56–80 block attenuation ranges allow quiet distant playback without artificially raising source volume.

World hallucination tests:

```mcfunction
/deprived trigger watcher
/deprived trigger watchers
/deprived trigger falsecreeper
```

## Configuration

`config/deprived.json` controls progression, event pacing, audio distance, damage, grain, edge noise, analog effects, peripheral figures and world hallucinations. `eventPacingPercent` above 100 creates longer silences; below 100 increases activity.

## Assets and build

- Untouched recordings: `custom-sound-input/`
- Game-audio conversion: `tools/process_custom_sounds.ps1`
- Art sources: `art_sources/`
- Build and install: `build-local.ps1`
