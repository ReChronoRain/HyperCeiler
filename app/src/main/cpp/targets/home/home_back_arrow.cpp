/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_back_arrow.h"
#include "home_back_feedback.h"
#include "home_back_morph.h"
#include <android/log.h>
#include <atomic>
#include <cmath>
#include <chrono>
#include <android/choreographer.h>
#include <mutex>
#include <new>
#include <unistd.h>

void *hc_back_path_draw_original = nullptr;
void *hc_back_bitmap_draw_original = nullptr;
void *hc_back_view_vsync_original = nullptr;
void *hc_back_view_destroy_original = nullptr;
void *hc_back_density_original = nullptr;
namespace {
struct BitmapResult {
    uint64_t words[5];
};
struct Api {
    BitmapResult (*create)(int, int, int, int, bool) = nullptr;
    void (*copy)(void *, const void *) = nullptr;
    void (*destroy)(void *) = nullptr;
    void (*draw)(void *, void *, void *, float, float, float, float, float, float, float, float) = nullptr;
    void *(*paint)() = nullptr;
    void (*alpha)(void *, int) = nullptr;
    void (*drop_paint)(void *) = nullptr;
    void (*filter)(void *, bool) = nullptr;
    int (*width_dp)(void *) = nullptr;
    int (*height_dp)(void *) = nullptr;
    int (*smallest_dp)(void *) = nullptr;
};
Api api;
home_back_arrow::Targets shape;
std::atomic_bool bound{false};
struct ViewState {
    void *view = nullptr;
    AChoreographer *choreographer = nullptr;
    uint64_t generation = 0;
    pid_t thread = 0;
    bool frame_pending = false;
};
std::array<ViewState, 2> views;
std::mutex view_lock;
struct FrameRequest {
    uint64_t generation;
    unsigned side;
};
struct Cache {
    uint64_t session = 0;
    void *bitmap = nullptr;
    void *paint = nullptr;
    ~Cache() {
        if (bitmap)
            api.destroy(bitmap);
        if (paint)
            api.drop_paint(paint);
    }
};
thread_local Cache cache;
thread_local std::array<home_back_arrow::Morph, 2> morphs;
thread_local uint64_t frame_time = 0;
uint64_t now_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::steady_clock::now().time_since_epoch())
        .count();
}
struct Visual {
    BackVisualState state;
    home_back_arrow::MorphFrame frame;
};

template <class T> T field(const void *object, size_t offset) {
    T result;
    std::memcpy(&result, static_cast<const std::byte *>(object) + offset, sizeof result);
    return result;
}
Visual visual(void *view, const float *params) {
    if (!bound.load(std::memory_order_acquire))
        return {};
    unsigned side = field<uint8_t>(view, shape.right);
    if (side >= morphs.size() || !std::isfinite(params[0]))
        return {};
    auto state = hc_back_feedback_visual(side == 0);
    auto &morph = morphs[side];
    bool ready = state.ready && bool(state.icon);
    bool changed = ready != (morph.session == state.session && morph.recent);
    auto frame = morph.sample(state.session, ready, frame_time ? frame_time : now_ms());
    if (changed && state.session)
        __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow",
                            "arrow transition session=%llu target=%s iconMs=100",
                            static_cast<unsigned long long>(state.session), ready ? "recent" : "back");
    return {std::move(state), frame};
}
bool draw_icon(void *view, void *canvas, const float *rect, const float *params, const Visual &value) {
    const auto &icon = value.state.icon;
    if (!value.state.ready || !icon)
        return false;
    float center = (rect[2] + rect[3]) * .5f, cy = params[2], scale = params[3];
    int alpha = field<int>(params, 16);
    float height = field<float>(view, shape.height) * scale;
    int window_width = field<int>(view, shape.rect + 8) - field<int>(view, shape.rect);
    if (!std::isfinite(center) || !std::isfinite(cy) || !std::isfinite(height) || height <= 0 ||
        window_width <= 0 || window_width > 1000 || alpha <= 0 || alpha > 255)
        return false;
    if (cache.session != icon->session) {
        if (cache.bitmap) {
            api.destroy(cache.bitmap);
            cache.bitmap = nullptr;
        }
        BitmapResult result = api.create(int(icon->width), int(icon->height), 5, 160, true);
        cache.session = icon->session;
        if ((result.words[0] & 255) != 0 || !result.words[1])
            return false;
        cache.bitmap = reinterpret_cast<void *>(result.words[1]);
        api.copy(cache.bitmap, icon->pixels.data());
    }
    if (!cache.bitmap)
        return false;
    if (!cache.paint) {
        cache.paint = api.paint();
        if (!cache.paint)
            return false;
        api.filter(cache.paint, true);
    }
    float size = std::min(std::max(height * 1.4f, std::abs(rect[3] - rect[2])), window_width * .88f) *
                 value.frame.icon;
    if (size < 1)
        return true;
    api.alpha(cache.paint, alpha);
    api.draw(canvas, cache.bitmap, cache.paint, 0, 0, float(icon->width), float(icon->height),
             center - size * .5f, cy - size * .5f, center + size * .5f, cy + size * .5f);
    if (value.frame.icon >= .15f && hc_back_feedback_presented(icon->session))
        __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow",
                            "native icon shown side=%s size=%.1f session=%llu", icon->left ? "left" : "right",
                            size, static_cast<unsigned long long>(icon->session));
    return true;
}
using Draw = void (*)(void *, void *, void *, const float *, const float *);
void draw(void *original, void *self, void *view, void *canvas, const float *rect, const float *params) {
    auto value = visual(view, params);
    if (draw_icon(view, canvas, rect, params, value))
        return;
    std::array<float, 4> bounds;
    std::array<float, 6> animated;
    std::memcpy(bounds.data(), rect, sizeof(bounds));
    std::memcpy(animated.data(), params, sizeof(animated));
    float center = (rect[2] + rect[3]) * .5f;
    bounds[2] = center + (rect[2] - center) * value.frame.icon;
    bounds[3] = center + (rect[3] - center) * value.frame.icon;
    animated[3] *= value.frame.icon;
    reinterpret_cast<Draw>(original)(self, view, canvas, bounds.data(), animated.data());
}
void refresh_frame(int64_t, void *data) {
    std::unique_ptr<FrameRequest> request(static_cast<FrameRequest *>(data));
    void *view = nullptr;
    uint64_t *arc = nullptr;
    {
        std::lock_guard guard(view_lock);
        auto &state = views[request->side];
        if (!state.view || state.generation != request->generation || state.thread != gettid())
            return;
        state.frame_pending = false;
        if (!hc_back_feedback_visual(request->side == 0).session || !hc_back_view_vsync_original)
            return;
        view = state.view;
        arc = reinterpret_cast<uint64_t *>(static_cast<std::byte *>(view) - shape.arc_offset);
        uint64_t count = __atomic_load_n(arc, __ATOMIC_ACQUIRE);
        // Pin the verified Arc<OnceCell<View>> while outside the cache lock.
        // The destructor hook clears cached pointers before the allocation is freed.
        do {
            if (!count || count > 0x7fffffff || !__atomic_load_n(arc + 1, __ATOMIC_ACQUIRE))
                return;
        } while (
            !__atomic_compare_exchange_n(arc, &count, count + 1, true, __ATOMIC_ACQUIRE, __ATOMIC_RELAXED));
    }
    hc_back_view_vsync(view);
    if (__atomic_fetch_sub(arc, 1, __ATOMIC_RELEASE) == 1) {
        __atomic_thread_fence(__ATOMIC_ACQUIRE);
        reinterpret_cast<void (*)(void *)>(shape.arc_drop)(arc);
    }
}
bool post_frame(AChoreographer *scheduler, FrameRequest *request) {
    if (!request)
        return false;
    // refresh_frame takes ownership when the choreographer delivers the callback.
    AChoreographer_postFrameCallback64(scheduler, refresh_frame, request);
    return true;
}
} // namespace

extern "C" void hc_back_view_vsync(void *view) {
    {
        std::lock_guard guard(view_lock);
        unsigned side = field<uint8_t>(view, shape.right);
        if (side < views.size()) {
            auto &state = views[side];
            if (state.view != view) {
                state.view = view;
                state.generation++;
                state.choreographer = AChoreographer_getInstance();
                state.thread = gettid();
                state.frame_pending = false;
                morphs[side] = {};
                __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow",
                                    "arrow view attached side=%u frameScheduler=%d", side,
                                    state.choreographer != nullptr);
            }
        }
    }
    uint64_t previous = frame_time;
    frame_time = now_ms();
    reinterpret_cast<void (*)(void *)>(hc_back_view_vsync_original)(view);
    unsigned side = field<uint8_t>(view, shape.right);
    if (side < morphs.size() && morphs[side].animating)
        hc_back_arrow_refresh();
    frame_time = previous;
}
extern "C" void hc_back_view_destroy(void *view) {
    {
        std::lock_guard guard(view_lock);
        for (auto &state : views)
            if (state.view == view) {
                state.view = nullptr;
                state.choreographer = nullptr;
                state.generation++;
                state.frame_pending = false;
            }
    }
    reinterpret_cast<void (*)(void *)>(hc_back_view_destroy_original)(view);
}
void hc_back_arrow_refresh() {
    std::lock_guard guard(view_lock);
    if (!hc_back_view_vsync_original || !hc_back_view_destroy_original)
        return;
    for (unsigned side = 0; side < views.size(); side++) {
        auto &state = views[side];
        auto visual = hc_back_feedback_visual(side == 0);
        if (!state.view || !state.choreographer || state.frame_pending || !visual.session)
            continue;
        state.frame_pending = post_frame(state.choreographer,
            new (std::nothrow) FrameRequest{state.generation, side});
    }
}

extern "C" void hc_back_path_draw(void *self, void *view, void *canvas, const float *rect,
                                  const float *params) {
    draw(hc_back_path_draw_original, self, view, canvas, rect, params);
}
extern "C" void hc_back_bitmap_draw(void *self, void *view, void *canvas, const float *rect,
                                    const float *params) {
    draw(hc_back_bitmap_draw_original, self, view, canvas, rect, params);
}
extern "C" int hc_back_density(void *configuration) {
    int dpi = reinterpret_cast<int (*)(void *)>(hc_back_density_original)(configuration);
    if (bound.load(std::memory_order_acquire) && dpi >= 80 && dpi <= 1280) {
        int smallest = api.smallest_dp(configuration);
        if (smallest <= 0)
            smallest = std::min(api.width_dp(configuration), api.height_dp(configuration));
        if (smallest > 0 && smallest <= 8192)
            hc_back_feedback_short_side(smallest * dpi / 160.f);
    }
    return dpi;
}
void hc_back_arrow_bind(uintptr_t base, const home_back_arrow::Targets &targets) {
    if (bound.load())
        return;
    shape = targets;
    shape.arc_drop += base;
    api.create = reinterpret_cast<decltype(api.create)>(base + targets.imports[0]);
    api.copy = reinterpret_cast<decltype(api.copy)>(base + targets.imports[1]);
    api.destroy = reinterpret_cast<decltype(api.destroy)>(base + targets.imports[2]);
    api.draw = reinterpret_cast<decltype(api.draw)>(base + targets.imports[3]);
    api.paint = reinterpret_cast<decltype(api.paint)>(base + targets.imports[4]);
    api.alpha = reinterpret_cast<decltype(api.alpha)>(base + targets.imports[5]);
    api.drop_paint = reinterpret_cast<decltype(api.drop_paint)>(base + targets.imports[6]);
    api.filter = reinterpret_cast<decltype(api.filter)>(base + targets.imports[7]);
    api.width_dp = reinterpret_cast<decltype(api.width_dp)>(base + targets.imports[11]);
    api.height_dp = reinterpret_cast<decltype(api.height_dp)>(base + targets.imports[12]);
    api.smallest_dp = reinterpret_cast<decltype(api.smallest_dp)>(base + targets.imports[13]);
    bound.store(true, std::memory_order_release);
}
