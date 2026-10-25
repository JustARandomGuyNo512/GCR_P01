---
name: gcr-add-gun
description: Add a new firearm to GunsCraft Reforged end-to-end — module definition, stats, slots, recoil, display transforms, item + creative tab, GLTF mesh, animation clips and controller wiring, sounds, and both-language strings.
whenToUse: Use when the task is "add a gun/weapon/firearm", "port this gun into GCR", or when an existing gun is missing from the creative tab, has no animation, wrong recoil, or wrong sound.
---

# Adding a gun

A gun is a special `IModular` with slots, stats, recoil, display transforms, a mesh, a controller and
clips. Expect to touch **ten** files. Do not shortcut the model/animation wiring — a gun without a
`registerModel` entry exists but is invisible, and a gun without controller mappings throws
`Animation not found: shoot` the first time it fires.

Read `AGENTS.md` §3 first; the same "no content data in logic classes" rules apply.

---

## What a gun is made of

| Piece | Type | Where it lives |
|---|---|---|
| Identity + slots + default modules | `AR` / `AK` constructor chain | `GCRModules.java` |
| Stats | `BaseProperties` | same call |
| Transform in hand / inventory / GUI | `DisplayData` | same call |
| Recoil | `RecoilData` (`RecoilImpulse`, `RecoilController`, `VisualRecoilMix`) | same call |
| Fire modes | `List<IFireMode<?>>` (`ARSemi`, `ARFullAuto`, `AKSemi`, `AKFullAuto`) | same call |
| Item | `GunItem` | `GCR.java` |
| Mesh | `.gltf` + `.png` | `assets/gcr/model_assets/gltf/` |
| Clips | `.animation.json` | `assets/gcr/model_assets/animation/` |
| Controller wiring | `registerModel(...)` lambda | `ClientTestingResources.java` |
| Sounds | `.ogg` + `sounds.json` + `ModSounds` | `assets/gcr/sounds/`, `sound/ModSounds.java` |

---

## 1. Definition in `GCRModules.java`

Copy the nearest sibling (`M4A1` for AR, `AK74M`/`AKMN` for AK) and change every value. The block is
long on purpose — it is the gun's data sheet:

```java
public static final IModular MY_GUN = new AR(
        RL("my_gun"),
        RL("common/pivot_maps/my_gun_main.pivot.geo.json"),   // slot geometry for attachments

        new BaseProperties(
                850, 1.15f, 0.18f, 3.5f,          // rpm, weight, spread, agility
                0.00075f, 0.1f,                   // stuckRate, maxStuckRate
                1.3f, 4f,                         // aimingSpeed, soundRange
                30f,                              // bulletVelocity
                0.005f, 0.05f / 60,               // shootHeatInc, heatDecSpeed
                6,                                // baseDamage
                RL("my_gun_fire"), RL("my_gun_fire_suppressed"),
                Map.of("mag_reload_length", 1.8f, "mag_reload_empty_length", 2.45f,
                       "mag_reload_charge_length", 2.8f,
                       "remove_stuck_empty_length", 0.85f, "remove_stuck_length", 0.55f)),
        new DisplayData()
                .setTranslation(0, 0, 8.775f, -7.175875f, -23.0375f, 0, 0, 0, 0.625f, 0.625f, 0.625f)
                /* contexts 0..5: GUI, ground, fixed, third person, first person, … */
                .setAimingTranslation(0, 0, 0, 0, 0, 0),
        new RecoilData(
                new RecoilImpulse(/* … */),
                new RecoilController(/* … */),
                new VisualRecoilMix(/* … */)),
        List.of(ARSemi.SEMI, ARFullAuto.FULL_AUTO))
        .addSlot(new ReplaceOnlySlot("BARREL").setFilter(SlotFilters.hasAllTags("barrel", "5.56x45", "ar")))
        .addSlot(new SingleFixedSlot("HANDGUARD").setFilter(SlotFilters.hasAllTags("handguard", "ar")))
        .addSlot(new ReplaceOnlySlot("REAR_GRIP").setFilter(SlotFilters.hasAllTags("rear_grip", "ar")))
        .addSlot(new SingleFixedSlot("STOCK").setFilter(SlotFilters.hasAllTags("stock", "ar")))
        .addSlot(new ReplaceOnlySlot("MAG").setFilter(SlotFilters.hasAllTags("mag", "ar", "5.56x45")))
        .addSlot(new Rail("SCOPE", Direction.UPPER, 10f, -1.802f, -14.4f, "ar_sight",
                "allow_ignore_rear_IS_collide")
                .setFilter(SlotFilters.hasAllTags("sight", "upper", "on_rail")))
        .setDefaultModuleInitHandler((workspace, accessor) -> {
            Unit root = workspace.getRootUnit();
            workspace.addChild(root, "BARREL", M4_PROFILE_FSB_BARREL.getID());
            workspace.addChild(root, "REAR_GRIP", A2_PISTOL_GRIP.getID());
            /* … one default per slot, or the gun ships incomplete … */
        });
```

Every slot needs a **filter** (an unfiltered slot accepts anything) and a **default module** — without
the defaults the gun spawns with empty slots, which usually means no barrel, no sights and no mag. The
pivot map needs a bone per slot name, or that slot's attachments render detached / skip collision.

Do not create a new gun class unless the firing behaviour genuinely differs; `AR` and `AK` already
cover the families and provide reload/stuck tasks.

---

## 2. Item + creative tab

`GCR.java`:

```java
public static final DeferredItem<Item> MY_GUN_ITEM =
        ITEMS.register(GCRModules.MY_GUN.getSimpleID(), () -> new GunItem((AR) GCRModules.MY_GUN));
```

and `output.accept(MY_GUN_ITEM.get());` inside `GUN_TAB`'s `displayItems`.

---

## 3. Names, icon, icon model

- `assets/gcr/lang/en_us.json` + `zh_cn.json`: `"gcr:my_gun": "My Gun"`.
- `assets/gcr/models/item/my_gun.json`: `{"parent":"item/generated","textures":{"layer0":"gcr:item/my_gun"}}`
- `assets/gcr/textures/item/my_gun.png`: 16×16 icon.

---

## 4. Mesh and controller wiring (`ClientTestingResources.java`)

```java
ModelRegistrationManager.registerModel(
        GCRModules.MY_GUN, "model_assets/gltf/my_gun_main.gltf", "model_assets/gltf/my_gun_main.png", true,
        meshData -> {
            ARMainViewer viewer = new ARMainViewer((AR) GCRModules.MY_GUN,
                    "gcr:my_gun_shoot_last", "gcr:my_gun_shoot_stuck");
            ARMainModel model = new ARMainModel(meshData, new BulletShellDisplay(/* copy a sibling */), viewer);

            IAnimationController<?> controller = new ARMainController(c -> c.registerAnimations(
                    "base", "gcr:my_gun_base",
                    "shoot", "gcr:my_gun_shoot",
                    "shoot_last", "gcr:my_gun_shoot_last",
                    "shoot_stuck", "gcr:my_gun_shoot_stuck",
                    "mag_reload", "gcr:my_gun_mag_reload",
                    "mag_reload_empty", "gcr:my_gun_mag_reload_empty",
                    "remove_stuck", "gcr:my_gun_remove_stuck",
                    "remove_stuck_empty", "gcr:my_gun_remove_stuck_empty",
                    "check_mag", "gcr:my_gun_check_mag",
                    "check_chamber", "gcr:my_gun_check_chamber",
                    "check_chamber_simple", "gcr:my_gun_check_chamber_simple",
                    "draw", "gcr:ar_draw", "holster", "gcr:ar_holster"
            ));
            model.bindController(controller);
            model.callInitAnimation();
            model.callInitTrack();
            model.callInitEventSubscriptions();
            return model;
        });
```

The three `callInit*` lines are not optional; without them the controller has no tracks/subscriptions
and nothing animates. Reuse `ARMainController` for an AR-family gun — do not write a controller just
to rename clips.

---

## 5. Animation clips

1. Create `assets/gcr/model_assets/animation/my_gun.animation.json` (Bedrock `1.8.0`) with the clip
   names your controller maps (`base`, `shoot`, `shoot_last`, `shoot_stuck`, `mag_reload`,
   `mag_reload_empty`, `check_mag`, `check_chamber`, `check_chamber_simple`, `remove_stuck`,
   `remove_stuck_empty`, …). `base` is the idle pose and must exist.
2. Register the file:

```java
ModelRegistrationManager.loadAndRegisterAnimations(
        "model_assets/animation/my_gun.animation.json",
        Map.ofEntries(
                Map.entry("base", "my_gun_base"),
                Map.entry("shoot", "my_gun_shoot"),
                /* … */
        ));
```

3. Optional `.states.json` for state-driven bone visibility (see `m203.states.json`).

Sound cues inside the clips (`sound_effects`) are how the gun makes noise during reload/check — do not
add timers for them.

---

## 6. Sounds

For every new sound event:

1. `.ogg` at `assets/gcr/sounds/<family>/<name>.ogg`.
2. Entry in `assets/gcr/sounds.json`: `"my_gun_fire": {"sounds": ["gcr:ar/my_gun_fire"]}`.
3. `public static final DeferredHolder<SoundEvent, SoundEvent> MY_GUN_FIRE = registerSound("my_gun_fire", "gcr");`
   in `sound/ModSounds.java` (only needed if code references it directly — `BaseProperties` resolves by
   `ResourceLocation`).
4. Pass `RL("my_gun_fire")` / `RL("my_gun_fire_suppressed")` into `BaseProperties`.

---

## 7. Recoil and display data

- `RecoilData` is tuning, not logic: `RecoilImpulse` (kick/shake), `RecoilController` (camera recovery
  speeds), `VisualRecoilMix` (gun-side visual response). Copy a sibling of the same family and adjust;
  never hardcode recoil in the renderer.
- `DisplayData.setTranslation(index, x, y, z, rotX, rotY, rotZ, scale…)` covers the item display
  contexts (`0` GUI … `5` first person in the existing guns) and `setAimingTranslation` is the ADS
  offset. Getting these wrong is why a new gun "floats" or hides the hand.

---

## 8. Verify

1. `./gradlew compileJava`.
2. `./gradlew runClient`: gun in the **Guns** tab with icon and name; equip; draw; fire (semi + auto);
   reload (tactical and empty); shoot until stuck and clear it; aim in/out with a scope; switch to a
   second gun and back.
3. Attachments: install every default slot's module; confirm the accepted tags match the intended
   family and caliber (an AK mag must not fit an AR magwell).
4. Third person / other players: `renderOther` path still works.
5. `./gradlew runServer` with a client: firing, reloading and module commits work; no
   `NoClassDefFoundError` for client classes.

Report which of these you actually ran, in the PR body.

## PR checklist

- [ ] Definition with unique id, filtered slots, default modules, pivot map
- [ ] `GCR.java` item + `GUN_TAB`
- [ ] lang (both), item model, icon
- [ ] GLTF/PNG + `registerModel` + controller mappings + `callInit*`
- [ ] `.animation.json` + `loadAndRegisterAnimations`
- [ ] sounds (ogg, sounds.json, `ModSounds` if referenced, `BaseProperties` names)
- [ ] `compileJava` + dev-client fire/reload/ADS test stated in the PR
