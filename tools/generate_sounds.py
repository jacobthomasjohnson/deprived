"""Generate Deprived's original procedural horror sound library.

Requires Python 3 and ffmpeg on PATH. No source samples are used.
"""
from array import array
from pathlib import Path
import math
import random
import subprocess
import wave

RATE = 22050
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/deprived/sounds"
TMP = ROOT / ".tooling/generated-wav"

def env(t, attack, release, duration):
    return min(1.0, t / max(attack, .001), (duration - t) / max(release, .001)) ** 1.35

def lowpass(samples, amount=.08):
    value = 0.0
    out = []
    for sample in samples:
        value += amount * (sample - value)
        out.append(value)
    return out

def echoes(samples, taps=((.12, .3), (.27, .2), (.51, .12))):
    out = samples[:] + [0.0] * int(max(d for d, _ in taps) * RATE)
    for delay, gain in taps:
        offset = int(delay * RATE)
        for i, sample in enumerate(samples):
            out[i + offset] += sample * gain
    return out

def normalize(samples, peak=.86):
    maximum = max(max(abs(x) for x in samples), .0001)
    return [max(-1, min(1, x * peak / maximum)) for x in samples]

def noise_burst(rng, duration, body=65, grit=.45):
    n = int(duration * RATE)
    raw = [rng.uniform(-1, 1) for _ in range(n)]
    dark = lowpass(raw, .025)
    return [(dark[i] * (1-grit) + raw[i] * grit) * env(i/RATE, .003, duration*.72, duration)
            + math.sin(2*math.pi*body*i/RATE) * env(i/RATE, .002, duration*.5, duration) * .5 for i in range(n)]

def footstep(rng, variant):
    hit = noise_burst(rng, .22, 48 + variant*3, .16)
    pre = list(reversed(hit[:int(.07*RATE)]))
    return echoes([x*.25 for x in pre] + hit, ((.16,.28),(.34,.16),(.68,.08)))

def breaking(rng, variant):
    duration = .72
    n = int(duration*RATE)
    raw = lowpass([rng.uniform(-1,1) for _ in range(n)], .035 + variant*.01)
    out=[]
    for i, x in enumerate(raw):
        t=i/RATE
        pulses=sum(max(0, 1-abs(t-p)/.035) for p in (.16,.29,.41,.48))
        ring=math.sin(2*math.pi*(170+variant*43)*t)*math.exp(-t*5)
        out.append((x*pulses*.9 + ring*.22)*env(t,.08,.2,duration))
    return echoes(out, ((.11,.35),(.25,.21),(.57,.13),(.91,.06)))

def placement(rng, variant):
    hit=noise_burst(rng,.28,72+variant*8,.08)
    reverse=list(reversed(hit))
    return echoes([x*.32 for x in reverse] + hit, ((.09,.28),(.22,.16),(.46,.09)))

def knock(rng, variant):
    total=1.25 + variant*.07
    out=[0.0]*int(total*RATE)
    for beat in (0,.19 + variant*.008,.43 + variant*.012):
        for j in range(int(.17*RATE)):
            t=j/RATE
            i=int(beat*RATE)+j
            if i<len(out):
                out[i]+=(math.sin(2*math.pi*(78+variant*4)*t)*math.exp(-t*25)+rng.uniform(-.2,.2)*math.exp(-t*40))
    return echoes(lowpass(out,.2),((.14,.22),(.37,.12),(.72,.07)))

def scratch(rng, variant):
    duration=1.7+variant*.12
    n=int(duration*RATE)
    raw=lowpass([rng.uniform(-1,1) for _ in range(n)],.16)
    out=[]
    for i,x in enumerate(raw):
        t=i/RATE
        motion=max(0,math.sin(2*math.pi*(2.3+variant*.11)*t))**5
        squeal=math.sin(2*math.pi*(1150+180*math.sin(t*7))*t)*.12
        out.append((x*.75+squeal)*motion*env(t,.12,.3,duration))
    return echoes(out,((.08,.25),(.21,.15),(.49,.08)))

def breath(rng, variant):
    duration=2.0+variant*.18
    n=int(duration*RATE)
    air=lowpass([rng.uniform(-1,1) for _ in range(n)],.045)
    out=[]
    for i,x in enumerate(air):
        t=i/RATE
        pulse=(math.sin(math.pi*t/duration)**1.2)*(0.72+.28*math.sin(2*math.pi*.7*t+variant))
        throat=math.sin(2*math.pi*(43+variant*2)*t)*.09
        out.append((x*.92+throat)*pulse)
    return echoes(out,((.17,.12),(.39,.08)))

def scream(rng, variant):
    duration=2.4+variant*.25
    n=int(duration*RATE)
    out=[]
    phase=0.0
    for i in range(n):
        t=i/RATE
        freq=310+variant*55+130*math.sin(2*math.pi*(1.7+variant*.2)*t)+55*math.sin(2*math.pi*7.3*t)
        phase+=2*math.pi*freq/RATE
        voice=math.sin(phase)+.38*math.sin(phase*2.01)+.18*math.sin(phase*.49)
        out.append((voice*.38+rng.uniform(-.12,.12))*env(t,.38,.75,duration))
    return echoes(lowpass(out,.22),((.13,.3),(.31,.22),(.62,.14),(1.08,.08)))

def stinger(rng, variant):
    duration=1.75+variant*.2
    n=int(duration*RATE)
    out=[]
    for i in range(n):
        t=i/RATE
        rise=(t/duration)**2
        f=38+variant*5+90*rise
        tone=math.sin(2*math.pi*f*t)+.35*math.sin(2*math.pi*(f*2.013)*t)
        hiss=lowpass([rng.uniform(-1,1)],.05)[0]
        out.append((tone*.34+hiss*.18)*env(t,.7,.32,duration))
    out=list(reversed(out[:int(.55*RATE)]))+out
    return echoes(out,((.2,.24),(.46,.15),(.88,.08)))

def whisper(rng, variant):
    duration=1.5+variant*.13
    n=int(duration*RATE)
    raw=lowpass([rng.uniform(-1,1) for _ in range(n)],.09)
    out=[]
    for i,x in enumerate(raw):
        t=i/RATE
        syllables=max(0,math.sin(2*math.pi*(3.1+variant*.17)*t+variant))**3
        formant=.55+.45*math.sin(2*math.pi*(170+variant*19)*t)
        out.append(x*syllables*formant*env(t,.22,.3,duration))
    return echoes(out,((.12,.3),(.3,.2),(.63,.11)))

FAMILIES = {
    "haunted_footstep": (footstep, 6), "false_break": (breaking, 4),
    "false_place": (placement, 4), "knock": (knock, 5), "scratch": (scratch, 5),
    "breath": (breath, 4), "scream": (scream, 3), "stinger": (stinger, 4),
    "whisper": (whisper, 4),
}

def write_wav(path, samples):
    pcm=array('h',(int(x*32767) for x in normalize(samples)))
    with wave.open(str(path),'wb') as wav:
        wav.setnchannels(1); wav.setsampwidth(2); wav.setframerate(RATE); wav.writeframes(pcm.tobytes())

def main():
    OUT.mkdir(parents=True,exist_ok=True); TMP.mkdir(parents=True,exist_ok=True)
    for family,(factory,count) in FAMILIES.items():
        folder=OUT/family; folder.mkdir(parents=True,exist_ok=True)
        for index in range(1,count+1):
            rng=random.Random(f"deprived:{family}:{index}")
            wav=TMP/f"{family}_{index}.wav"; ogg=folder/f"{index}.ogg"
            write_wav(wav,factory(rng,index-1))
            subprocess.run(["ffmpeg","-y","-loglevel","error","-i",str(wav),"-c:a","libvorbis","-q:a","5",str(ogg)],check=True)
            print(ogg.relative_to(ROOT))

if __name__ == "__main__": main()
