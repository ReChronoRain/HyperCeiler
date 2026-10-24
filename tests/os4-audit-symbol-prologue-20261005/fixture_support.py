from pathlib import Path

def function(s,key):
    a=s.index(key);b=s.index('{',a);j=b+1;n=1
    while n:n+=(s[j]=='{')-(s[j]=='}');j+=1
    return s[a:j]

def codeview_fixture(root,read_body,global_image=False):
    p=Path(root)/'app/src/main/cpp/targets/home/tweaks'
    header='\n'.join(l for l in (p/'scanner.h').read_text(encoding='utf8').splitlines() if not l.startswith(('#include','#pragma')))
    s=(p/'scanner.cpp').read_text(encoding='utf8')
    alias='using Image=::Image;using Segment=::Segment;' if global_image else ''
    pre='#include <span>\n#include <array>\nnamespace hometweaks {'+alias+'struct FunctionSignature {};}\n'
    pre+='namespace nhk { constexpr size_t kMaxExecutableCodeBytes=256u*1024u*1024u;bool safe_read(uintptr_t address,std::span<std::byte> out){'+read_body+'}}\n'
    pre+=header+'\nnamespace hometweaks {\n'
    for key in ['bool CodeView::ExecutableRangesOk()', 'bool CodeView::RangeOk(', 'bool CodeView::ReadWords(']:pre+=function(s,key)+'\n'
    pre+='}\n'
    if global_image:pre+='using hometweaks::CodeView;\n'
    return pre
