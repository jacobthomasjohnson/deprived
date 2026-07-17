# Deprived 1.3.1 — Control Room

Fabric 1.20.1 sleep-deprivation horror focused on ambiguity rather than jumpscares.

Version 1.3.1 completes the full Deprived rename: mod ID, Java namespace, commands, configuration, assets, registries and networking now use `deprived`.

## What v1.3 adds

- Press `F6`, or select Config from Mod Menu, to open the live Deprived Control Room.
- Visual toggles and sliders apply immediately while the world remains visible behind the menu.
- The System page controls pacing, audio distance, progression thresholds and extreme-stage damage; Apply sends them through an operator-checked server update.
- The Developer page sets test stages and immediately triggers individual audio, visual and hallucination events.
- A live analog-fault preview makes visual tuning possible without waiting for a random event.

## What v1.2 adds

- A staged analog-signal layer: exposure breathing, rolling interference, chromatic edge bleed, tracking tears, phosphor trails, film burns and short channel-drift episodes.
- Analog faults remain sparse at first and grow less stable with severe deprivation instead of becoming a permanent filter.
- Moving edge noise replaces fixed vignette lines.
- The developer HUD is restyled as a muted cyan/magenta signal monitor and reports the active analog fault.
- Every analog family can be disabled independently. `analogEffectIntensity` scales the complete treatment from 0–100.
- v1.2.1 adds a rebindable developer shortcut: press `F8` to trigger a random event from the current deprivation-stage pool. It uses the same operator permission check as `/deprived trigger random`.
- v1.2.2 makes grain and analog motion resolution-aware and clearly perceptible at deprived stages. Footsteps begin 44–58 blocks away at 0.20 volume; knocks and scratches originate 38–56 blocks away at 0.14–0.18 volume.
- v1.2.3 introduces the new uploaded recordings as four rare positional events: `radio`, `singing`, `riser`, and `deepdrone`. Long files are streamed and normalized quietly so they behave as distant environmental sources rather than soundtrack cues.

## What v1.1 changes

- Minecraft's persistent `time_since_rest` statistic drives six slow stages across seven sleepless days.
- Each UUID receives a stable symptom profile weighted toward auditory, visual, paranoia, or physical effects.
- Darkness, caves, storms, and isolation influence event selection.
- Quiet intervals are randomized, with rare short clusters instead of a predictable timer.
- Visual degradation consists of low-opacity grain, slowly drifting edge-noise shadows, minimal color drift, eyelid shadows, and peripheral figures—no camera warping or fixed vignette lines.
- Watchers and false Creepers are client-only entities. Other players cannot see them, they have no server hitbox, and direct observation makes them disappear.
- Sleeping resets deprivation and clears all active client hallucinations.
- The release contains only the 20 supplied field recordings; procedural audio is excluded.

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
