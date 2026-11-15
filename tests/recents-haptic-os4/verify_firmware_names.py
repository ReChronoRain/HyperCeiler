"""Check runtime filename indexing and localized coverage of the device fixture."""
from pathlib import Path
import os, subprocess, tempfile, xml.etree.ElementTree as ET, re
here=Path(__file__).resolve().parent
root=here.parents[1]
resources=root/'library/core/src/main/res'
arrays={n.attrib['name']:[x.text for x in n] for n in ET.parse(resources/'values/home_haptic_effect_names.xml').getroot()}
chinese=ET.parse(resources/'values-zh-rCN/home_haptic_effect_names.xml').getroot()[0]
keys=arrays['home_haptic_firmware_keys']
assert len(keys)==len(set(keys))==len(arrays['home_haptic_firmware_groups'])==len(arrays['home_haptic_firmware_names'])==len(chinese)
fixture=here/'firmware-names-24129PN74C.txt'
tokens={m[2].lower() for line in fixture.read_text(encoding='utf8').splitlines()
    if (m:=re.fullmatch(r'(\d+)_(.+?)(?:_P)?_RTP\.bin',line,re.I)) and int(m[1])>21}
assert tokens<=set(keys),tokens-set(keys)
jdk=Path(os.environ['JAVA_HOME'])/'bin'
suffix='.exe' if os.name=='nt' else ''
with tempfile.TemporaryDirectory(prefix='hc-firmware-names-') as work:
    subprocess.run([str(jdk/('javac'+suffix)),'-d',work,
        str(root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/recent/HomeHapticFirmwareNames.java'),
        str(here/'FirmwareNamesTest.java')],check=True)
    subprocess.run([str(jdk/('java'+suffix)),'-cp',work,'FirmwareNamesTest',str(fixture)],check=True)
print(f'Localized coverage PASS: {len(tokens)} current device stems, aligned English/Chinese/group resources')
