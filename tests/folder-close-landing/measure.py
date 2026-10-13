"""Track the small red YouTube preview in original screenrecord frames."""
import sys, json
from pathlib import Path
sys.path.insert(0, 'C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/folder-margin-bounce/analysis-deps')
import av
import numpy as np

p = Path(sys.argv[1]); records = []
for i, f in enumerate(av.open(str(p)).decode(video=0)):
    t = float(f.pts * f.time_base)
    if t < 2: continue
    a = f.to_ndarray(format='rgb24')[200:1300,650:780]
    ys,xs = np.where((a[:,:,0]>210)&(a[:,:,1]<90)&(a[:,:,2]<100))
    pts = set(zip(ys,xs)); components = []
    while pts:
        q=pts.pop(); todo=[q]; cs=[q]
        while todo:
            y,x=todo.pop()
            for n in [(y-1,x),(y+1,x),(y,x-1),(y,x+1)]:
                if n in pts: pts.remove(n); todo.append(n); cs.append(n)
        xy=np.array(cs); h=np.ptp(xy[:,0])+1; w=np.ptp(xy[:,1])+1
        if len(cs)>30 and 1.2<w/h<3 and h<25 and w<35:
            components.append([650+float(np.mean(xy[:,1])),200+float(np.mean(xy[:,0])),int(w)])
    components=[c for c in components if 675<c[0]<710]
    if components:
        c=min(components,key=lambda c:abs(c[0]-692))
        records.append(dict(i=i,t=t,x=c[0],y=c[1],w=c[2]))
jumps=[dict(dy=abs(b['y']-a['y']),before=a,after=b) for a,b in zip(records,records[1:]) if b['t']-a['t']<.025 and abs(b['x']-a['x'])<5]
assert len(jumps)>5, 'insufficient consecutive compact-preview frames'
result=dict(max_jump=max(jumps,key=lambda j:j['dy']),records=records)
p.with_suffix('.measurement.json').write_text(json.dumps(result,indent=2))
print(p.name, json.dumps(result['max_jump']), 'EXIT=0')
