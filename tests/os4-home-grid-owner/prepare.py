#!/usr/bin/env python3
"""Derive a reviewable owner candidate without changing production inputs."""
import hashlib
from pathlib import Path

SOURCE=Path(__file__).resolve().parents[2]
CPP=SOURCE/'app/src/main/cpp'
TEST=SOURCE/'tests/home-startup'

def once(text,old,new):
    assert text.count(old)==1, (old,text.count(old))
    return text.replace(old,new)

def prepare(out):
    production=CPP/'targets/home/home_startup.cpp'
    text=production.read_text()
    text=once(text,'alignas(8) uint64_t hc_layout_startup_epoch_gate = 0;',
        '''alignas(8) uint64_t hc_layout_startup_epoch_gate = 0;
// One release-published feature key. Consumers must first acquire an odd
// shared epoch, then acquire this key once. A late publisher cannot reopen
// a revoked epoch. Nonzero dimensions are pinned at the first snapshot.
alignas(8) uint64_t hc_home_grid_config_key = 0;''')
    text=once(text,'''// Phone, Pad and Fold grids also feed the Rust GridConfig/model. Do not create
// a Dart/Rust disagreement while that first-use deadline remains unproven.
// Keep their generated Dart plans as evidence but decline the composite grids.
constexpr uint64_t kUnsupported = PhoneGridDart | PhoneGridRust | PadGrid | FoldGrid | RecentsAnimationRust;''',
        '''// Candidate phone route uses the typed Dart producer/serializer path.
// It does not patch the legacy global Rust getters. Pad/Fold and the unrelated
// earlier Rust animation deadline remain outside this phone candidate.
constexpr uint64_t kUnsupported = PadGrid | FoldGrid | RecentsAnimationRust;''')
    text=once(text,'static_assert(kHookCount == 15);','static_assert(kHookCount == 18);')
    text=once(text,'uint64_t eligible = 0;','uint64_t eligible = 0, initial_grid_key = 0;')
    text=once(text,'bool attempted = false, installed = false, terminal = false;',
        'bool attempted = false, installed = false, terminal = false, ever_activated = false;')
    text=once(text,'''    return __atomic_compare_exchange_n(&hc_layout_startup_epoch_gate, &expected, desired,
        false, __ATOMIC_RELEASE, __ATOMIC_ACQUIRE);''',
        '''    const bool opened = __atomic_compare_exchange_n(&hc_layout_startup_epoch_gate, &expected, desired,
        false, __ATOMIC_RELEASE, __ATOMIC_ACQUIRE);
    // The first admitted frame may exist before the final authorization
    // readback. Its resident downstream hooks cannot be uninstalled on error.
    if (opened) g_state.ever_activated = true;
    return opened;''')
    text=once(text,'Result failure(Error error, uint64_t mask, bool unhook) noexcept {',
        '''Result failure(Error error, uint64_t mask, bool unhook) noexcept {
    unhook = unhook && !g_state.ever_activated;''')
    text=once(text,'if (config.grid_enabled) mask |= PhoneGridDart | PhoneGridRust;',
        'if (config.grid_enabled) mask |= PhoneGridDart;')
    text=once(text,'bool valid_config(const home_layout::Config &config) noexcept {',
        '''bool valid_config(const home_layout::Config &config) noexcept {
    if (config.grid_enabled && (config.cell_x < 3 || config.cell_x > 9
        || config.cell_y < 4 || config.cell_y > 13)) return false;''')
    text=once(text,'bool set_desired(const plan::Word &word) noexcept {',
        '''uint64_t grid_key(const home_layout::Config &config) noexcept {
    return (uint64_t(config.cell_x) << 8) | uint64_t(config.cell_y);
}
void clear_consumers() noexcept {
    __atomic_store_n(&hc_home_grid_config_key, uint64_t{0}, __ATOMIC_RELEASE);
    detail::clear_consumers();
}
void publish_consumers(const home_layout::Config &config, uint64_t applied) noexcept {
    detail::publish_consumers(config, applied);
    const uint64_t key = (applied & PhoneGridDart) && config.grid_enabled ? grid_key(config) : 0;
    __atomic_store_n(&hc_home_grid_config_key, key, __ATOMIC_RELEASE);
}
bool set_desired(const plan::Word &word) noexcept {''')
    # Rewrite owner call sites, retaining the two bridge calls above.
    text=text.replace('    detail::clear_consumers();','    clear_consumers();')
    text=once(text,'''void clear_consumers() noexcept {
    __atomic_store_n(&hc_home_grid_config_key, uint64_t{0}, __ATOMIC_RELEASE);
    clear_consumers();''','''void clear_consumers() noexcept {
    __atomic_store_n(&hc_home_grid_config_key, uint64_t{0}, __ATOMIC_RELEASE);
    detail::clear_consumers();''')
    text=once(text,'    detail::publish_consumers(config, mask & ~kUnsupported);',
                   '    publish_consumers(config, mask & ~kUnsupported);')
    text=once(text,'    detail::publish_consumers(config, supported);',
                   '    publish_consumers(config, supported);')
    text=once(text,'    const auto installed = install_hooks(mask & ~kUnsupported, permit);',
        '''    g_state.initial_grid_key = (mask & PhoneGridDart) ? grid_key(config) : 0;
    const auto installed = install_hooks(mask & ~kUnsupported, permit);''')
    text=once(text,'''    if (!valid_config(config) || !make_plan(config, supported)) return failure(Error::UnsupportedConfiguration, mask, false);''',
        '''    if (!valid_config(config) || !make_plan(config, supported)) return failure(Error::UnsupportedConfiguration, mask, false);
    // Row values now live in the shared key, so comparing code words alone
    // would silently allow a late row change while Dart/Rust caches stay old.
    if ((supported & PhoneGridDart) && grid_key(config) != g_state.initial_grid_key)
        return failure(Error::LateConfiguration, mask, false);''')
    candidate=out/'home_startup.cpp'; candidate.write_text(text)

    fixture=(TEST/'executor_test.cpp').read_text()
    fixture=fixture.replace('"targets/home/home_startup_plan.generated.h"','"home_startup_plan.generated.h"')
    fixture=fixture.replace('originals[15]','originals[18]').replace('i<15','i<18')
    fixture=fixture.replace('hook_calls==15','hook_calls==18').replace('position<=15','position<=18')
    fixture=fixture.replace('position<=30','position<=43')
    fixture=fixture.replace('(PhoneGridDart|PhoneGridRust|PadGrid|FoldGrid|RecentsAnimationRust)',
                            '(PadGrid|FoldGrid|RecentsAnimationRust)')
    fixture=once(fixture,'''if(v.family==0)assert(r.applied==0&&r.declined==(PhoneGridDart|PhoneGridRust)&&!write_calls);
            else if(v.family==2||v.family==3)''','''if(v.family==2||v.family==3)''')
    fixture=once(fixture,'extern "C" uint64_t hc_layout_startup_epoch_gate;',
        'extern "C" uint64_t hc_layout_startup_epoch_gate, hc_home_grid_config_key;')
    fixture=once(fixture,'static unsigned hook_calls, unhook_calls, write_calls, read_calls;',
        'static unsigned hook_calls, unhook_calls, write_calls, read_calls;\nstatic uint32_t installed_vas[18]{};\nstatic bool reject_after_activation = false;')
    fixture=once(fixture,'static bool auth(void *) { return authorized.load(); }',
        '''static bool auth(void *) {
    if (reject_after_activation && (__atomic_load_n(&hc_layout_startup_epoch_gate,__ATOMIC_ACQUIRE)&1))
        authorized.store(false);
    return authorized.load();
}''')
    fixture=once(fixture,'    ++hook_calls;',
        '    installed_vas[hook_calls] = uintptr_t(address)-base;\n    ++hook_calls;')
    fixture=once(fixture,'    for(unsigned family: {2u,3u})test(',
        (Path(__file__).parent/'extra_cases.inc').read_text()+'\n    for(unsigned family: {2u,3u})test(')
    target=out/'executor_test.cpp'; target.write_text(fixture)
    return {str(p.relative_to(SOURCE)):hashlib.sha256(p.read_bytes()).hexdigest()
            for p in (production, TEST/'executor_test.cpp', Path(__file__), Path(__file__).parent/'extra_cases.inc')}
