#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
src=root/'src/main/java'
java=list(src.rglob('*.java'))
txt={str(f.relative_to(root)):f.read_text('utf8') for f in java}
checks={
 '434_java_files_preserved':len(java)==434,
 'central_renderer_uses_1_20_1_buffers': 'ByteBufferBuilder' not in txt['src/main/java/com/sheridan/gcr/client/render/DefaultGunRenderer.java'] and 'SequencedMap' not in txt['src/main/java/com/sheridan/gcr/client/render/DefaultGunRenderer.java'],
 'java21_frame_delta_removed':all('.getTimer().getGameTimeDeltaPartialTick(' not in v for v in txt.values()),
 'core_model_buffers_ported':all('ByteBufferBuilder' not in txt['src/main/java/com/sheridan/gcr/client/model/'+n] for n in ['BufferedBatchSingleMeshModel.java','BufferedBoneMeshModel.java']),
 'particle_camera_1_21_removed':all('getFacingCameraMode()' not in txt['src/main/java/com/sheridan/gcr/client/render/fx/particles/'+n] for n in ['ember/EmberParticle.java','explosion/FragmentParticle.java','explosion/SparkParticle.java']),
 'forge_tick_migration':all('PlayerTickEvent.Pre' not in v and 'PlayerTickEvent.Post' not in v for k,v in txt.items() if k.endswith(('GunHeatHandler.java','TestEvents.java'))),
 'single_heat_subscriber':txt['src/main/java/com/sheridan/gcr/common/GunHeatHandler.java'].count('void onServerPlayerTickPre(')==1,
 'mixin_iris_disabled':'IrisShaderCompatMixin' not in (root/'src/main/resources/gcr.mixins.json').read_text('utf8'),
 'oculus_not_required': 'oculus' not in (root/'build.gradle').read_text('utf8').lower().replace('// no oculus','').replace('// no oculus, iris or sodium dependency',''),
 'forge_1_20_1_metadata':'[1.20.1,1.20.2)' in (root/'src/main/resources/META-INF/mods.toml').read_text('utf8'),
 'source_resources_preserved':len(list((root/'src/main/resources').rglob('*')))>500,
}
if len(java)!=434: print('Unexpected Java source count',len(java),file=sys.stderr)
for k,v in checks.items(): print(('PASS' if v else 'FAIL')+' '+k)
print(json.dumps(checks,ensure_ascii=False,indent=2))
if not all(checks.values()): sys.exit(1)
