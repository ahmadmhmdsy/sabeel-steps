from PIL import Image
import collections

def diff(p, bg):
    return abs(p[0]-bg[0])+abs(p[1]-bg[1])+abs(p[2]-bg[2])

def analyze(path):
    im = Image.open(path).convert('RGB')
    px = im.load()
    bg = px[3,1000]
    print(path, 'size', im.size, 'bg', bg)
    print('card interior color:', collections.Counter(px[100,y] for y in range(470,610,5)).most_common(3))
    frac = []
    for y in range(250, 1000):
        n=0
        for x in range(60, 1020, 4):
            if diff(px[x,y], bg) > 9:
                n+=1
        frac.append((y, n/240.0))
    best=None; cur=None
    for y,f in frac:
        if f>0.85:
            cur = [y,y] if cur is None else [cur[0],y]
        else:
            if cur is not None:
                if best is None or (cur[1]-cur[0])>(best[1]-best[0]): best=cur
                cur=None
    if cur is not None and (best is None or (cur[1]-cur[0])>(best[1]-best[0])): best=cur
    print('card band candidate:', best)
    def content_rows(a,b):
        out=[]
        for y in range(a,b):
            n=0
            for x in range(30, 1050, 3):
                if diff(px[x,y], bg) > 9: n+=1
            if n>2: out.append(y)
        return out
    ca,cb = best
    above = content_rows(ca-200, ca-2)
    below = content_rows(cb+3, cb+220)
    print('last content rows above:', above[-8:] if above else None)
    print('first content rows below:', below[:8] if below else None)
    print('edges:')
    for y in [ca-30, ca-20, ca-10, ca-2, ca, ca+2, cb-2, cb, cb+10, cb+20, cb+30]:
        print('  y',y,'x100',px[100,y],'x540',px[540,y])

for p in ['docs/assets/screenshots/home-en.png','docs/assets/screenshots/home-ar.png','docs/assets/screenshots/home-dark-en.png']:
    analyze(p)
