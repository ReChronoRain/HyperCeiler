/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include <algorithm>
#include <cstring>
#include <cmath>

namespace home_layout {
struct BackGestureConfig { int height = 60; int width = 100; };
constexpr int32_t kBackGestureMagic = 0x48434231;
template<class Read> bool read_back_gesture(Read read, BackGestureConfig &out, bool optional = true) {
    out = {};
    int32_t magic = 0, height = 0, width = 0;
    if (!read(magic)) return optional;
    if (magic != kBackGestureMagic || !read(height) || !read(width)
        || height < 10 || height > 100 || width < 100 || width > 400) return false;
    out = {height, width}; return true;
}
struct BackGestureShape { uint64_t entry = 0; uint32_t left = 0, right = 0, screen = 0; uint64_t splice = 0; };
// STP Wt,Wt2,[X19,#imm]: offsets come from the running image, not a ROM profile.
inline std::optional<uint32_t> back_pair(uint32_t word, unsigned first, unsigned second) {
    if ((word & 0xffc003ffu) != (0x29000260u | first) || ((word >> 10) & 31) != second) return {};
    const unsigned offset = (word >> 15) & 127;
    if (offset & 64) return {};
    return offset * 4;
}
inline std::optional<BackGestureShape> back_shape(const elf_targets::Image &image,
    const unwind::FunctionBounds &function) {
    const auto size = function.end - function.begin;
    if (size < 0x300 || size > 0x1800) return {};
    bool receiver = false;
    std::optional<uint32_t> screen;
    std::optional<BackGestureShape> result;
    for (auto at = function.begin; at + 4 <= function.end; at += 4) {
        const auto w = image.word(at); if (!w) return {};
        if (at - function.begin < 0x60 && *w == 0xaa0003f3u) receiver = true;
        if (auto p = back_pair(*w,24,28)) { if (screen) return {}; screen = p; }
        // Native width computation and the four stores defining two Rects.
        if (at + 36 > function.end || *w != 0x1e2202c0u) continue; // SCVTF S0,W22
        std::array<uint32_t,9> ws{};
        for (unsigned i=0;i<9;++i) {auto v=image.word(at+i*4); if(!v)return {}; ws[i]=*v;}
        auto left = back_pair(ws[2],31,8), rb = back_pair(ws[4],24,21);
        auto lb = back_pair(ws[8],9,21);
        // Last right-top store lies two instructions beyond this window.
        auto sub=image.word(at+36), store=image.word(at+40);
        auto right=store ? back_pair(*store,9,8) : std::nullopt;
        if (!left || !right || !rb || !lb || !sub || *sub != 0x4b090309u
            || *rb != *right+8 || *lb != *left+8 || *right != *left+16
            || ws[1]!=0x52a86409u || ws[3]!=0x1e270121u
            || ws[5]!=0x1e200900u || ws[6]!=0x1e211800u || ws[7]!=0x1e380009u) continue;
        if(result) return {};
        result=BackGestureShape{function.begin,*left,*right,0,at};
    }
    if (!receiver || !screen || !result || *screen + 8 > result->left) return {};
    result->screen = *screen; return result;
}
inline std::optional<BackGestureShape> resolve_back_gesture(std::span<const std::byte> bytes) {
    const auto image=elf_targets::parse(bytes); if(!image)return {};
    std::optional<BackGestureShape> result;
    for (const char *label : {"update_home_region home_region: ", "update_region is_fold_or_pad: "}) {
        const auto literal=elf_targets::unique_literal(*image,label);
        if(!literal) continue;
        const auto fn=elf_targets::unique_function(*image,*literal);
        if(!fn) return {};
        const auto shape=back_shape(*image,*fn); if(!shape)return {};
        if(result && result->entry!=shape->entry)return {};
        result=shape;
    }
    return result;
}

struct BackWindowShape { uint64_t apply=0,update=0,splice=0;uint32_t rect_sp=0,side_field=0; };
struct BackWindowRect { int32_t left,top,right,bottom; bool operator==(const BackWindowRect &)const=default; };
inline std::optional<BackWindowShape> resolve_back_window(std::span<const std::byte> bytes) {
    const auto image=elf_targets::parse(bytes);if(!image)return {};
    auto owner=[&](const char *text){auto l=elf_targets::unique_literal(*image,text);
        return l?elf_targets::unique_function(*image,*l):std::optional<unwind::FunctionBounds>{};};
    const auto apply=owner(" apply_touchable_region (local coords): left="),update=owner(" update_valid_gesture_region: ");
    if(!apply||!update)return {};
    bool input=false,copy=false,width=false,call=false;
    for(auto at=update->begin;at+8<=update->end;at+=4){auto a=image->word(at),b=image->word(at+4);if(!a||!b)return {};
        if(at-update->begin<0x40&&*a==0xaa0103f4u)input=true;
        if(*a==0x3dc00280u&&(*b&0xffe00fffu)==0x3c8002c0u)copy=true;
        if(*a==0xb9400a89u&&*b==0xb940028au)width=true;
        if((*a&0xfc000000u)==0x94000000u){int64_t delta=int64_t(int32_t(*a<<6))>>4;
            if(uint64_t(int64_t(at)+delta)==apply->begin)call=true;}
    }
    if(!input||!copy||!width||!call)return {};
    bool receiver=false,param=false;std::optional<BackWindowShape> result;
    for(auto at=apply->begin;at+40<=apply->end;at+=4){auto a=image->word(at);if(!a)return {};
        if(at-apply->begin<0x60&&*a==0xaa0003f4u)receiver=true;
        if(at-apply->begin<0x60&&*a==0x2a0103f3u)param=true;
        if(*a!=0xb9401be8u)continue; // LDR W8,[SP,#24], right of copied Rect
        std::array<uint32_t,10> w{};for(unsigned i=0;i<10;++i){auto v=image->word(at+i*4);if(!v)return {};w[i]=*v;}
        if(w[1]!=0xb94013e9u||w[2]!=0x4b090108u||w[3]!=0x7100051fu
            ||w[4]!=0xb9002fe8u||w[6]!=0x6b080269u||(w[8]&0xffc003ffu)!=0x3940028au)continue;
        if(result){return {};}result=BackWindowShape{apply->begin,update->begin,at,16,(w[8]>>10)&4095};
    }
    return receiver&&param?result:std::nullopt;
}
struct BackGestureInputs {
    int32_t top, bottom, screen_width, screen_height, density;
    float width_dp;
};

inline std::optional<int32_t> back_width_pixels(const BackGestureInputs &input) {
    if (input.density <= 0 || input.density > 4096) return {};
    const float pixels = input.width_dp * static_cast<float>(input.density) / 160.0f;
    if (!(pixels >= 1.0f && pixels < 32769.0f)) return {};
    return static_cast<int32_t>(pixels);
}

inline std::optional<BackGestureInputs> back_gesture_geometry(BackGestureInputs input,
                                                             BackGestureConfig config) {
    const auto width = back_width_pixels(input);
    const auto w = input.screen_width;
    const auto h = input.screen_height;
    if (w <= 0 || h <= 0 || w > 32768 || h > 32768 || input.top < 0
        || input.bottom > h || input.top >= input.bottom || !width || *width > w / 2
        || config.height < 10 || config.height > 100
        || config.width < 100 || config.width > 400) return {};
    if (config.width != 100) {
        const float cap = (w / 2) * 160.0f / input.density;
        input.width_dp = std::min(input.width_dp * (config.width / 100.0f), cap);
        if (const auto pixels = back_width_pixels(input); pixels && *pixels > w / 2) {
            input.width_dp = std::nextafter(input.width_dp, 0.0f);
        }
    }
    if (config.height != 60) {
        const auto length = std::max(1, h * config.height / 100);
        if (h >= w) {
            input.top = std::max(0, input.bottom - length);
        } else {
            const auto center = (input.top + input.bottom) / 2;
            input.top = std::clamp(center - length / 2, 0, h - length);
            input.bottom = input.top + length;
        }
    }
    return input;
}

inline std::optional<BackWindowRect> back_window_rect(const BackGestureInputs &stock,
                                                     unsigned side, BackGestureConfig config) {
    const auto target = back_gesture_geometry(stock, config);
    if (!target || side > 1) return {};
    const auto span = back_width_pixels(*target);
    if (!span) return {};
    const auto w = target->screen_width;
    return side == 0 ? BackWindowRect{0, target->top, *span, target->bottom}
                     : BackWindowRect{w - *span, target->top, w, target->bottom};
}

inline bool adjust_back_gesture_inputs(BackGestureInputs &input, BackGestureConfig config) {
    if (config.height == 60 && config.width == 100) return false;
    const auto target = back_gesture_geometry(input, config);
    if (!target) return false;
    input = *target;
    return true;
}
} // namespace home_layout
