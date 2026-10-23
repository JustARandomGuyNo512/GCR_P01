---
name: gcr-animation-and-sounds
description: Add or edit gun animations, animation-driven sounds, and gameplay-to-animation events in GunsCraft Reforged. Explains the Bedrock clip pipeline, controller subscriptions, event dispatch, timeline commands, and why keyframes must never be written in Java.
whenToUse: Use when the task involves an animation playing (or not playing), a sound that should fire at an animation moment, a new gameplay event that should trigger a clip (aim, inspect, jam), or when someone proposes hardcoding keyframes/camera shake in a render class.
---

# Animation, sound cues, and events

GCR animations are **data**, not code. A clip is a Bedrock `.animation.json`; the clip's own timeline can
carry sound cues and commands; controllers subscribe to gameplay events and play clips by name. If you
find yourself writing frame arrays or a timer in Java, you have taken the wrong path — see the
"rejected pattern" at the end.

Read `AGENTS.md` §3.1–3.2 first.

---

## The pipeline in one diagram

```
gameplay code decides something happened
  (fire mode, reload task, draw/holster, stuck removal, …)
        │  Client.getGunRenderer().dispatchAnimationEvent(EventType.X, Map.of(k, v))
        ▼
DefaultGunRenderer.dispatchAnimationEvent          (queues if the render context isn't built yet)
        ▼
AnimationEventBus.dispatch(type, context, params)  (walks registered controllers by priority)
        ▼
controller callback (subscribed in `GunController`, `ARMainController`, `AKController`, …)
        │  if (isTrackClear(MAIN)) MAIN.play(anim("mag_reload").coverState());
        ▼
Track → AnimationInstance → KeyframeAnimator
        ▼
bones move; sound_effects fire; timeline commands run
```

Files that own each layer:

| Layer | File |
|---|---|
| Event types | `client/model/modular/animation/eventSys/EventType.java` |
| Bus | `client/model/modular/animation/eventSys/AnimationEventBus.java` |
| Dispatch entry | `client/render/DefaultGunRenderer.java` (`dispatchAnimationEvent`) |
| Base subscriptions | `client/model/modular/animation/controllers/GunController.java` |
| Per-gun subscriptions | `.../controllers/ARMainController.java`, `AKController.java`, … |
| Clip loading | `client/animation/io/BedrockAnimationLoader.java` |
| Clip registry | `client/animation/AnimationRegister.java` |
| Timeline commands | `client/animation/command/CommandFactory.java` |
| Wiring (god file) | `ClientTestingResources.java` — `loadAndRegisterAnimations`, `registerAnimations` |

---

## Existing event types

`EventType` currently defines:

`SHOOT`, `SWITCH_FIRE_MODE`, `RELOAD`, `CHECK_MAG`, `CHECK_CHAMBER`, `REMOVE_STUCK`,
`CHECK_SUB_WEAPON`, `RELOAD_SUB_WEAPON`, `CLEAR_TRACK`, `DRAW`, `HOLSTER`.

There is **no ADS/aim event yet**. If your task is "the gun should react to aiming", the correct
change is to add an event (see §4), not to animate from the renderer.

Some events carry parameters, e.g. `RELOAD` with `"animation_name"`, `REMOVE_STUCK` with `"name"`,
`SHOOT` with `"stuck"` / `"last_round"`. Parameters are read in the callback via
`context.getParam("…")`. The `boolean` in the `EventType` constructor (`dispatchPerNodeInstance`) is
currently not consumed anywhere; copy the value used by the closest sibling event.

---

## 1. Add or edit a clip

Clips live in `src/main/resources/assets/gcr/model_assets/animation/<gun>.animation.json`, Bedrock
format (`format_version` 1.8.0):

```json
{
  "format_version": "1.8.0",
  "animations": {
    "my_clip": {
      "animation_length": 0.6,
      "bones": {
        "root": {
          "position": { "0.0": [0, 0, 0], "0.11": [0, -1.195, -3.005], "0.6": [0, 0, 0] },
          "rotation": { "0.0": [0, 0, 0], "0.11": [0, 0, 2.05], "0.6": [0, 0, 0] }
        }
      },
      "sound_effects": { "0.11": { "effect": "m4new_smallmove3" } },
      "timeline": { "0.3": "show_msg(clip_start);" }
    }
  }
}
```

- Times are **seconds**; `animation_length` is the clip length. Keyframes are read in JSON order and
  the sampler assumes ascending timestamps, so keep them sorted.
- Smooth keys are `{"post": [x,y,z], "lerp_mode": "catmullrom"}`. Only `post` is read (`pre` is
  ignored), and `lerp_mode` other than `catmullrom` is treated as linear.
- **Sound cues belong in `sound_effects`** — a code timer that fires the sound at "0.11 s" will drift
  the moment frame rate or tick rate changes. Cues are quantised to the 20 Hz client tick.
- **Logic belongs in `timeline`** — commands are parsed by `CommandFactory`; registered names today:
  `c_left_arm`, `c_right_arm`, `mask_mag_ammo`, `ads_pose_limit`, `exit_ads`, `show_msg`,
  `show_mag_ammo_left`, `shadow_node_render`, `show_gp25_grenade_left`.
  A new command type is a code change (`Command` subclass + `CommandFactory` registration), not an
  inline lambda. Syntax is `name(arg1,arg2);` with **bare arguments** — quotes are not stripped, and
  `onFrame` runs every frame, so each command gates itself.
- Bones named `foo@G` go into a separate sub-clip (`<clipKey>.G`) with no `sound_effects`/`timeline`;
  that is how a grenade-launcher sub-model animates alongside the rifle.

Source `.bbmodel` files live under `src/animation_project/` — remember the third-party-asset rule.

---

## 2. Register the clip with the gun

Two steps, both in `ClientTestingResources.java`:

1. `ModelRegistrationManager.loadAndRegisterAnimations("model_assets/animation/<gun>.animation.json", Map.ofEntries(...))`
   maps **json key → registered name**, e.g. `"shoot" → "m4a1_shoot"`, which registers `gcr:m4a1_shoot`
   in `AnimationRegister`.
2. In the gun's `registerModel(...)` lambda, the controller maps **controller key → registered name**:

```java
IAnimationController<?> controller = new ARMainController(c -> c.registerAnimations(
        "shoot", "gcr:m4a1_shoot",
        "mag_reload", "gcr:m4a1_mag_reload",
        /* … */));
```

`anim("shoot")` in a controller looks up the controller key and throws
`IllegalStateException("Animation not found: shoot")` if you forgot either step. That exception is
the usual reason a newly added clip "does nothing".

`.states.json` files are the state layer: they map gun states (`empty`, `full`, `fired`, `prepare`, …)
to pose overrides (bone scale/rotation) rather than time-based clips. Use them for "hide the shell
when the chamber is empty", not for animation playback.

---

## 3. Subscribe to an existing event

In the base `GunController` (all guns) or a specific controller (one gun family):

```java
subscribe(EventType.CHECK_MAG, 0, (context) -> {
    if (isTrackClear(MAIN)) {
        CHECK.play(anim("check_mag").coverState());
    }
});
```

Rules of the road:

- Use `isTrackClear(track)` before interrupting; `coverState()` / `coverStateExclude("ammo")` declares
  which bone state the clip takes over — omitting it causes the "gun snaps back" bug.
- `MAIN` is the full-body track; `SHOOT`, `DRAW`, `CHECK` are independent; track priorities live in
  `initTrack`.
- Do not read item NBT from the render thread inside a callback to decide state — the weapon thread
  writes it, so the read can be stale. That is exactly why `SHOOT` carries `stuck` / `last_round` as
  parameters. Follow the same pattern for new events.

---

## 4. Add a new event type (only when none fits)

Needed when gameplay gains a moment no existing event describes (e.g. ADS start/end, inspect).

1. `EventType.java`: `public static final EventType AIM_START = new EventType(false, "aim_start");`
2. Publish from the gameplay site with the established API:
   `Client.getGunRenderer().dispatchAnimationEvent(EventType.AIM_START, Map.of("…", "…"));`
   Dispatch from the logic that *decides* the state (e.g. `WeaponStatus.handleAds`), once per
   transition — not from `render()` per frame, which would restart the clip every frame.
3. Subscribe where it belongs: add to `GunController.firstPersonSubscriptions` if every gun should
   react, otherwise to one controller.
4. Add the clip + both registration mappings (§1, §2).

Threading: dispatch is safe before the render context exists (events are queued), but the *decision*
must not depend on NBT read from another thread. Pass what the callback needs as parameters.

The `boolean` in the `EventType` constructor (`dispatchPerNodeInstance`) is written and never read —
every event dispatches to every subscription on every render node. Do not rely on it to suppress
sub-node handling. `cancel()` is global: it stops all remaining subscribers for that dispatch.

---

## 5. Sounds

Two kinds:

- **Animation cues** → `sound_effects` inside the clip. Nothing in Java. The animation system plays it
  locally and tells the server so other players hear it.
- **Event-driven sounds** (fire, mag in/out) → OGG under `assets/gcr/sounds/<category>/`, an entry in
  `assets/gcr/sounds.json`, a `DeferredHolder` in `sound/ModSounds.java`, and the `ResourceLocation`
  passed where the sound is chosen (e.g. `BaseProperties` fire sound names).

Never trigger a clip-timed sound from a `tick()` counter — that is the exact drift the JSON cue exists
to avoid.

---

## 6. Verify

1. `./gradlew compileJava`.
2. `./gradlew runClient`, then perform the action: the clip plays, on the right track, and returns to
   the base pose.
3. The sound fires at the authored moment (not "roughly"), including when FPS is low.
4. Switch guns and attachments mid-clip: no stuck pose, no exception, no NBT read crash.
5. For a new event: check it does not re-fire every frame (add a log line temporarily, remove it).

**Restart, do not reload.** Animation JSON is parsed once at client setup. `F3+T` and
`/gcr:reload_model` do not re-read `.animation.json` (the latter rebuilds models/controllers from the
already-loaded clips). If your clip "did not change", you are looking at the old parse.

---

## Gotchas worth knowing before you debug

- **Held poses are explicit.** `keepOnLastFrame()` never finishes by itself; the clip stays applied
  until you `clear()` the track (or another track covers it). `"loop"` keys in the JSON are effectively
  inert — do not expect them to hold a pose.
- **First person only.** Track-based clips render through the first-person context; third person only
  animates what a controller's `customThirdPersonAnimation` handles (currently the shoot clips).
- **Tracks are an unordered map.** Two tracks touching the same bone apply in unspecified order; put
  the pose in one clip rather than splitting it across tracks.
- **Commands are shared, per clip.** One `Command` object per `AnimationDef` runs `onFrame` on every
  frame for every gun playing that clip — gate on progress and cooldown, and keep no per-gun state in
  a command field.
- **Clip identity is the registry key.** Registering the same `gcr:<name>` twice silently keeps the
  first one; a mapping without a namespace would resolve to `minecraft:`.

---

## Rejected pattern (do not repeat)

A contribution once added the M4 aim clip as Java arrays in `render/GunPoseHandler.java`
(`AIM_POS`, `AIM_ROT`, `AIM_CAM_ROT` + a hand-written `sample()`/`catmull()` and a 0.6 s counter), plus
a hardcoded 0.11 s sound trigger. It was rejected because:

- it duplicated the clip pipeline with an incompatible second implementation;
- the numbers cannot be re-tuned or shared with other guns without a recompile;
- the sound would drift with tick rate;
- it bypassed `EventType`/controllers, so no other gun or addon can hook it.

If a task looks like that, stop and use §1–§4.

## PR checklist

- [ ] Clip in the `.animation.json`, sounds in `sound_effects`, logic in `timeline`
- [ ] Both mappings added (json→registry, controller→registry)
- [ ] Subscription on the right controller/track with `coverState()` where needed
- [ ] No keyframe arrays, timers, or NBT reads added to render code
- [ ] Dev-client test of the actual action, stated in the PR body
