from pathlib import Path
import sys,json,zipfile,hashlib
here=Path(__file__).resolve().parent
root=Path(sys.argv[1]).resolve()
assert root.is_dir()

# Hash what git would store, not the raw working-tree bytes. With
# core.autocrlf=true and `* text=auto eol=lf` in .gitattributes, git normalises
# CRLF to LF on the way into the object store, so the worktree copy of a tracked
# file hashes differently from the blob that modified-hashes.json recorded.
# Comparing raw bytes therefore failed on a pristine checkout of the very commit
# the hashes were taken from - a false positive, not the drift this is meant to
# catch. Only the worktree side is normalised: the archives keep whatever bytes
# were captured, and their own hashes were recorded the same way.
def worktree_digest(data:bytes)->str:
    return hashlib.sha256(data.replace(b'\r\n',b'\n')).hexdigest()

def raw_digest(data:bytes)->str:
    return hashlib.sha256(data).hexdigest()

modified=json.loads((here/'modified-hashes.json').read_text())
for n,s in modified.items():
    p=(root/n).resolve();assert p.is_relative_to(root)
    assert p.is_file() and worktree_digest(p.read_bytes())==s, 'target differs: '+n
with zipfile.ZipFile(here/'ORIGINAL_FILE.zip') as z:
    for n in modified:
        p=root/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))
original=json.loads((here/'original-hashes.json').read_text())
assert all(raw_digest((root/n).read_bytes())==s for n,s in original.items())
print('ROLLBACK_HASHES=PASS; restored five original files')
