# GCR Forge 1.20.1 — Upstream-ready feature branch

**Base:** `forge-1.20.1-port` (`c777cfa4667190514ec085e4d03e243195f7451a`)  
**Updated from:** `gcr-aftermatch-1.20.1` (`9f7e63d0f4acffad53faf4ebb745932eef48f850`)  
**Status:** Upstream candidate built and checked by its GitHub Actions workflow. In-game testing and release approval are still required.

## Physical magazine and chamber mechanics

- Persistent physical magazine stacks with distinct ammunition quantities, calibers and round-type information; 5.56×45, 5.45×39, 6.8×51 Fury and 7.62×39.
- Actual provided 32×32 transparent cartridge textures for all four calibers, used by canonical and legacy item model IDs.
- Chamber and magazine are independent: double R detaches a magazine, another double R ejects the chambered round into inventory. When inventory is full, the round is dropped on the ground.
- Short hold-and-release R quick reloads; long R hold opens the wheel.
- Adaptive radial petals according to the number of magazine variants, one petal for physical magazines with identical type, amount, capacity and cartridge composition, showing `×N` for repeats. Magazine choices are ordered deterministically.
- Hovering the wheel center highlights **Cancel**. Release R over a petal to select or over the center to cancel; ESC does not close it. Movement (WASD, sprint, jump and sneak) works while the wheel is open, but mouse-look is blocked so mouse movement selects magazines.
- Last manually chosen magazine type persists per firearm; quick reload prioritizes it. Fallback never overwrites this preference. Existing magazine is returned on swap and chambered round retained.
- The server verifies exact selected inventory magazine stacks and performs authoritative transfers.
- Loaded magazines append `(+)` to their localized name; empty ones show the bare name. Tooltips show remaining cartridges, compatible GCR firearms, and **hold R to load / hold RMB to unload**.
- Physically loading **one cartridge every 16 game ticks (0.8 seconds)**; unloading **one every 10 game ticks (0.5 seconds)**. Limits are imposed client-side and server-side.
- When a gun is jammed, a **single press of R** starts clearing; the animation continues on key release. Existing server-authoritative jam/timing checks and gun state sync remain in place.
- Existing fallback animations are used where the dedicated `remove_mag`, `install_mag` and `eject_round` clips are still unavailable.

## Upstream portability and safeguards

- Base port's AKMN animation, other pre-existing source and locale entries preserved.
- New code uses neutral `PhysicalMagazineInput`, `PhysicalMagazinePacket`, and `MagazineWheelScreen` class names.
- Legacy `aftermatch_*` NBT keys remain to preserve compatibility with existing weapon and magazine data; they do not require the AftermatchDayZ mod.
- The radial magazine search uses normal Minecraft inventory slots; no custom Aftermatch inventory dependency.
- **Excluded:** Aftermatch detailed/multipart hitbox adjustments, `OwnMultipartHitFilter.java`, `BulletEntity.java` and `LaserEffectRenderer.java` changes, plus server-specific systems.
- This branch has a dedicated independent verification workflow and downloadable test JAR artifact. In-game verification and maintainer approval are still required before publication. Do not merge into `forge-1.20.1-port` without official approval.

## Additional upstream updates — 2026-10-26

- Quick-reload magazine priority: fullest loaded copy of the last manually chosen compatible type, otherwise fullest loaded compatible magazine of any type, stable inventory-order tie breaker.
- Three new per-round sound clips: two equally weighted loading variants and one unloading sound, triggered only on authoritative server transfers.
- Middle/extra mouse button binding fixes and registration of the chamber-check mapping.
- Redesigned charcoal-and-amber radial magazine menu, with **no Luxium dependency**.
- Optional spring-damped hipfire weapon/arm bank on turning and strafing (client config; ADS and ballistics unchanged).
- Slightly enlarged hipfire reticle point, animated laser impact dot with dedicated sprite, and cosmetic server-announced bullet-hole decals (90s lifetime, bounded to 256 on each client).
- **Preserved:** player-only anatomy and damage in PlayerAnatomy and PlayerHitZoneConfig. No AftermatchDayZ integration is required.

Before publishing: validate in-game client/server behavior, shader compatibility for laser rendering, and distribution rights for the included three audio recordings.
