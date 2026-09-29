"""Static linkage check: do our compiled classes find every Minecraft/NeoForge member they reference?

Usage: python tools/check_linkage.py <classes dir or jar> <neoforge-...-merged.jar> [more merged jars...]

Compiling against one Minecraft version and running on another only works if every method and field we
call still exists with the exact same descriptor (return type included). javac can't tell, the JVM only
fails when the line runs. This walks the constant pool of each of our classes and looks the references
up in the target jar, following superclasses and interfaces. Mixin targets are not covered; CI checks those.
"""
import io
import struct
import sys
import zipfile
from pathlib import Path

CHECKED = ('net/minecraft/', 'com/mojang/blaze3d/', 'net/neoforged/neoforge/')
JDK_INHERITED = {'equals', 'hashCode', 'toString', 'getClass', 'name', 'ordinal', 'values', 'valueOf', 'compareTo'}


def parse(data):
    s = io.BytesIO(data)

    def rd(fmt):
        return struct.unpack('>' + fmt, s.read(struct.calcsize('>' + fmt)))

    assert rd('I')[0] == 0xCAFEBABE
    rd('HH')
    pool = [None] * rd('H')[0]
    i = 1
    while i < len(pool):
        tag = rd('B')[0]
        if tag == 1:
            pool[i] = (1, s.read(rd('H')[0]).decode('utf-8', 'replace'))
        elif tag in (3, 4):
            pool[i] = (tag, rd('I')[0])
        elif tag in (5, 6):
            pool[i] = (tag, rd('Q')[0])
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[i] = (tag, rd('H')[0])
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[i] = (tag, *rd('HH'))
        elif tag == 15:
            pool[i] = (tag, *rd('BH'))
        else:
            raise ValueError(tag)
        i += 1
    utf = lambda k: pool[k][1]
    cls = lambda k: utf(pool[k][1]) if k else None
    _, this, sup = rd('HHH')
    interfaces = [cls(rd('H')[0]) for _ in range(rd('H')[0])]

    def members():
        out = set()
        for _ in range(rd('H')[0]):
            _, n, d = rd('HHH')
            out.add((utf(n), utf(d)))
            for _ in range(rd('H')[0]):
                rd('H')
                s.read(rd('I')[0])
        return out

    fields = members()
    methods = members()
    refs = []
    for entry in pool:
        if entry and entry[0] in (9, 10, 11):
            owner = cls(entry[1])
            nat = pool[entry[2]]
            refs.append(('field' if entry[0] == 9 else 'method', owner, utf(nat[1]), utf(nat[2])))
        elif entry and entry[0] == 7:
            refs.append(('class', utf(entry[1]), None, None))
    return {'name': cls(this), 'super': cls(sup), 'interfaces': interfaces,
            'fields': fields, 'methods': methods, 'refs': refs}


class Target:
    def __init__(self, jars):
        self.zips = [zipfile.ZipFile(j) for j in jars]
        self.cache = {}

    def get(self, name):
        if name not in self.cache:
            self.cache[name] = None
            for z in self.zips:
                try:
                    self.cache[name] = parse(z.read(name + '.class'))
                    break
                except KeyError:
                    pass
        return self.cache[name]

    def has(self, kind, owner, name, desc, seen=None):
        """True if the member exists on owner or a Minecraft/NeoForge supertype of it."""
        seen = seen or set()
        if owner in seen or owner is None:
            return False
        seen.add(owner)
        c = self.get(owner)
        if c is None:
            # JDK/library supertypes (Object, Record, ...) can't be looked at; only a few members are
            # inherited from them, and none of those change between Minecraft versions.
            return not owner.startswith(CHECKED) and name in JDK_INHERITED
        if (name, desc) in (c['fields'] if kind == 'field' else c['methods']):
            return True
        return any(self.has(kind, p, name, desc, seen) for p in [c['super'], *c['interfaces']])


def our_classes(path):
    p = Path(path)
    if p.is_dir():
        for f in p.rglob('*.class'):
            yield f.read_bytes()
    else:
        with zipfile.ZipFile(p) as z:
            for n in z.namelist():
                if n.endswith('.class'):
                    yield z.read(n)


def main():
    ours, targets = sys.argv[1], sys.argv[2:]
    classes = [parse(b) for b in our_classes(ours)]
    own = {c['name'] for c in classes}
    target = Target(targets)
    missing = set()
    for c in classes:
        for kind, owner, name, desc in c['refs']:
            if owner in own or owner.startswith('[') or not owner.startswith(CHECKED):
                continue
            if kind == 'class':
                if target.get(owner) is None:
                    missing.add(f'class  {owner}')
            elif not target.has(kind, owner, name, desc):
                missing.add(f'{kind:6} {owner}.{name}{desc}')
    for m in sorted(missing):
        print('MISSING', m)
    print(f'{len(missing)} missing reference(s)')
    sys.exit(1 if missing else 0)


if __name__ == '__main__':
    main()
