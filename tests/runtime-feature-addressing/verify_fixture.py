#!/usr/bin/env python3
"""Independently check pinned instruction evidence, not production discovery."""
import hashlib
import json
import lzma
from pathlib import Path
import struct
import sys

HERE = Path(__file__).resolve().parent
raw = Path(sys.argv[1]).read_bytes()
fixture = json.loads((HERE/'features-7751.json').read_text())
allocator = json.loads((HERE/'folder-allocators.json').read_text())
grid = json.loads((HERE/'grid-7751.json').read_text())
scalars = json.loads((HERE/'scalars-7751.json').read_text())
columns = json.loads((HERE/'columns-7751.json').read_text())
indicator = json.loads((HERE/'indicator-7751.json').read_text())
widget_move = json.loads((HERE/'widget-move-7751.json').read_text())
assert hashlib.sha256(raw).hexdigest() == fixture['source_sha256'] == allocator['source_sha256'] == grid['source_sha256'] == scalars['source_sha256'] == columns['source_sha256'] == indicator['source_sha256'] == widget_move['source_sha256']
phoff = struct.unpack_from('<Q', raw, 32)[0]
phsize, phcount = struct.unpack_from('<HH', raw, 54)
loads = [struct.unpack_from('<IIQQQQQQ', raw, phoff+i*phsize) for i in range(phcount)]
def read(va, size):
    for p in loads:
        if p[0] == 1 and p[3] <= va and va+size <= p[3]+p[5]:
            return raw[p[2]+va-p[3]:p[2]+va-p[3]+size]
    raise AssertionError('Non-file-backed evidence: '+hex(va))

for row in fixture['functions']+grid['functions']+scalars['functions']+columns['functions']+indicator['functions']+widget_move['functions']+[allocator['rectangle_allocator']]:
    words = [int(w,16) if isinstance(w,str) else w for w in row['words']]
    assert list(struct.unpack('<'+'I'*len(words),read(row['va'],len(words)*4))) == words

# Names are an independent test oracle only. Production Code reads no section,
# string or symbol table, as the executable-only repackaging test demonstrates.
def sections(data):
    at = struct.unpack_from('<Q',data,40)[0]
    size,count,strings = struct.unpack_from('<HHH',data,58)
    rows = [struct.unpack_from('<IIQQQQIIQQ',data,at+i*size) for i in range(count)]
    st = rows[strings]; names = data[st[4]:st[4]+st[5]]
    return rows,{names[r[0]:names.find(b'\0',r[0])].decode():i for i,r in enumerate(rows)}

rows,names = sections(raw)
s = rows[names['.gnu_debugdata']]
mini = lzma.decompress(raw[s[4]:s[4]+s[5]])
rows,names = sections(mini)
s = rows[names['.symtab']]; st = rows[s[6]]; strings = mini[st[4]:st[4]+st[5]]
addresses = {}
symbol_sizes = {}
for at in range(s[4],s[4]+s[5],s[9]):
    name,info,other,section,va,size = struct.unpack_from('<IBBHQQ',mini,at)
    if name and va and (info & 15) == 2:
        label = strings[name:strings.find(b'\0',name)].decode()
        addresses.setdefault(label,set()).add(va)
        symbol_sizes.setdefault(label,{})[va]=size
oracle = {
    'app':'AppIcon.build', 'current':'GridController.currentConfig',
    'desktop':'ShortcutIconWidget._buildNormalDrawable',
    'desktop_width':'ShortCutCommonMixin.launcherIconWidth',
    'desktop_height':'ShortCutCommonMixin.launcherIconHeight',
    'progress_width':'ShortCutCommonMixin.launcherIconWidth',
    'progress_height':'ShortCutCommonMixin.launcherIconHeight',
    'close':'FolderAnimController._initScale', 'cache':'FolderAnimController._refreshCachedGridParams',
    'stable':'FolderClingGetxController.getStableIconRectInFolder',
    'caller':'CheckRectHelper.deterministicFolderLandingRect', 'ratio':'getAnimDurationRatio',
    'flight':'FlightCohort.animDurationRatio',
    'api':'RustLibApiImpl.crateApiSceneAnimationTransitionAnimDurationApiGetAnimDurationRatio',
    'magic':'Utilities.getDefaultGestureAnimMagicSpeed',
    'high':'DeviceLevelUtils.isHighAnimationRate', 'low':'DeviceLevelUtils.isLowAnimationRate',
    'widget':'_TransformedWidgetContainer.build',
    'dock_scope':'DockScope.isInDock',
    'dock_title0':'ShortcutIconWidget._buildTextWidget',
    'dock_title1':'ProgressShortCutIconWidget._buildTextWidget',
}
for role,label in oracle.items():
    assert fixture['roots'][role] in addresses[label],(role,label)
grid_oracle = {
    'current':'GridController.currentConfig', 'rx':'RxObjectMixin.value',
    'cell':'GridCellDelegate.performLayout', 'occupied':'GridOccupiedCellDelegate.performLayout',
    'hotseat':'HotSeatLayoutDelegate.cellLayout', 'drop':'CellLayoutGetxController.calculateCenterGlobalPosition',
    'local':'CellLayoutGetxController.cellToCenterOffset', 'counts':'CellLayoutGetxController.isItemPosEmpty',
    'legacy':'GridSizeCalRules._calVariableHeight', 'cap':'GridSizeCalRules.calVarCellHeight',
    'handler':'PhoneCellSizeHandler.calVariableValues',
}
for role,label in grid_oracle.items(): assert grid['roots'][role] in addresses[label],(role,label)
scalar_oracle = {'current':'GridController.currentConfig','margin':'GridController.hotSeatsMarginBottom',
    'row':'FolderGridViewGetxController.folderCellHeight','row_consumer':'FolderGridView._buildScrollableGrid',
    'screen':'GridController.screenMarginBottom'}
for role,label in scalar_oracle.items(): assert scalars['roots'][role] in addresses[label],(role,label)
columns_oracle = {'current':'GridController.currentConfig','counts':'CellLayoutGetxController.isItemPosEmpty',
    'owner':'PhoneRowColHandler.handle','copy':'ComputeContext.copyWith',
    'persist':'GridController._persistCellCountIfEligible','save':'LauncherLayoutType.saveAndUpdateLayoutType',
    'put':'PreferenceUtils.putInt'}
for role,label in columns_oracle.items(): assert columns['roots'][role] in addresses[label],(role,label)
indicator_oracle = {'deflate':'EdgeInsets.deflateRect','editing':'LauncherIndicatorState.isInEditing','comparison':'_LinkedHashSetMixin.contains',
    'show':'LauncherIndicatorState._showIndicator','animate':'LauncherIndicatorState._animateIndicator',
    'refresh':'LauncherIndicatorState._refreshIndicator','type':'LauncherIndicatorState._getCurrentIndicatorType'}
for role,label in indicator_oracle.items(): assert indicator['roots'][role] in addresses[label],(role,label)
# This closure has no named STT_FUNC. It begins immediately after the actual
# parent's reported body, with its own original frame and editing-state BL.
policy = indicator['roots']['policy']
assert not any(policy in values for values in addresses.values())
assert not any(indicator['roots']['margin'] in values for values in addresses.values())
assert any(va+size==policy for va,size in symbol_sizes['LauncherIndicatorState._buildScreenIndicator'].items())
assert struct.unpack('<II',read(policy,8)) == (0xa9bf79fd,0xaa0f03fd)
widget_move_oracle = {'owner':'AssistantDragToPAHandler.canDragToPA',
    'span':'AssistantDragToPAHandler._isSpanSupportedByPa','rx':'RxObjectMixin.value'}
for role,label in widget_move_oracle.items(): assert widget_move['roots'][role] in addresses[label],(role,label)
font_call = fixture['roots']['font_site']-4
word = struct.unpack('<I',read(font_call,4))[0]
assert word & 0xfc000000 == 0x94000000
delta = word & 0x3ffffff
if delta & (1 << 25): delta -= 1 << 26
assert font_call+delta*4 in addresses['GridConfig.getTitleTextSize']
print('Fixture provenance PASS: pinned ELF hash, every original instruction, independent named-owner oracle (test only)')
