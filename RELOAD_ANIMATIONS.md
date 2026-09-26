# Animation variants

Extra clips go in the **same JSON** as the base animation, not a new file.

- M4A1: `src/main/resources/assets/gcr/model_assets/animation/m4a1_main.animation.json`
- AK74M: `src/main/resources/assets/gcr/model_assets/animation/ak74m.animation.json`
- Shared draw / holster: `src/main/resources/assets/gcr/model_assets/animation/ar.global.animation.json`

## How naming works

Keep the existing clip (`shoot`, `mag_reload`, `check_mag`, …). Add variants by appending `_something`.

The game groups by the **longest** matching prefix, then picks one at random when that action plays.

| Clip name | Pool |
|---|---|
| `shoot`, `shoot_one`, `shoot_two` | Fire |
| `shoot_last`, `shoot_last_one` | Last round |
| `shoot_stuck`, `shoot_stuck_one` | Jam |
| `mag_reload`, `mag_reload_one` | Tactical reload (ammo left) |
| `mag_reload_empty`, `mag_reload_empty_one` | Empty reload |
| `mag_reload_charge`, `mag_reload_charge_one` | AR empty, bolt not locked |
| `check_mag`, `check_mag_one` | Check mag |
| `check_chamber`, `check_chamber_one` | Check chamber |
| `check_chamber_simple`, `check_chamber_simple_one` | Simple chamber check |
| `draw`, `draw_one` | Draw |
| `holster`, `holster_one` | Holster |
| `remove_stuck`, `remove_stuck_one` | Clear jam |
| `remove_stuck_empty`, `remove_stuck_empty_one` | Clear jam, empty |
| `to_semi`, `to_semi_one` | Switch to semi |
| `to_auto`, `to_auto_one` | Switch to auto |

`mag_reload_empty_one` is empty, not tactical. `shoot_last_one` is last-round, not a normal shoot.

## Example

Copy a working clip and rename it:

```json
"shoot_one": {
  "animation_length": 0.1,
  "bones": { }
}
```

Bone names must stay the same as the gun model.

## How to test

1. Add the clip(s), restart the client (startup scan only).
2. Do that action several times. If a category has more than one clip, it should roll randomly.
3. One clip in a category → always that clip.

No extra Java mapping is needed.
