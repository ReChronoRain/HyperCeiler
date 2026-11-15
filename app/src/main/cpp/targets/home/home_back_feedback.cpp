/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_back_feedback.h"
#include "home_back_arrow.h"
#include "home_back_hold.h"
#include <android/log.h>
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <dlfcn.h>
#include <array>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <deque>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

namespace {
constexpr uint32_t magic = 0x48434248;
struct Reply {
    std::mutex lock;
    std::condition_variable changed;
    bool done = false, accepted = false;
};
struct Request {
    std::array<std::byte, 64> bytes{};
    std::shared_ptr<Reply> reply;
};
std::mutex lock;
std::condition_variable changed;
std::deque<Request> queue;
std::atomic_bool started{false};
std::atomic<uint64_t> active_session{0}, presented_session{0};
std::atomic_bool icon_ready{false};
std::atomic<uint64_t> available_session{0};
std::atomic<float> native_short_side{0};
home_back_feedback::HoldState hold;
std::mutex icon_lock;
std::shared_ptr<const BackIconPreview> icon;

uint64_t uptime() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::steady_clock::now().time_since_epoch())
        .count();
}

template <class T> void put(Request &request, size_t offset, T value) {
    std::memcpy(request.bytes.data() + offset, &value, sizeof(value));
}
uint32_t word(const Request &request, size_t offset) {
    uint32_t value;
    std::memcpy(&value, request.bytes.data() + offset, 4);
    return value;
}
Request command(uint32_t operation) {
    Request request;
    put(request, 0, magic);
    put(request, 4, operation);
    return request;
}

void *on_create(void *) {
    return nullptr;
}
void on_destroy(void *) {}
binder_status_t on_transact(AIBinder *, transaction_code_t, const AParcel *, AParcel *) {
    return STATUS_UNKNOWN_TRANSACTION;
}
AIBinder *window_service() {
    using GetService = AIBinder *(*)(const char *);
    auto get = reinterpret_cast<GetService>(dlsym(RTLD_DEFAULT, "AServiceManager_getService"));
    if (!get)
        return nullptr;
    AIBinder *window = get("window");
    if (!window)
        return nullptr;
    const auto *cls = AIBinder_getClass(window);
    if (cls) {
        const char *descriptor = AIBinder_Class_getDescriptor(cls);
        if (!descriptor || std::strcmp(descriptor, "android.view.IWindowManager")) {
            AIBinder_decStrong(window);
            return nullptr;
        }
    } else {
        static auto *contract =
            AIBinder_Class_define("android.view.IWindowManager", on_create, on_destroy, on_transact);
        if (!contract || !AIBinder_associateClass(window, contract)) {
            AIBinder_decStrong(window);
            return nullptr;
        }
    }
    return window;
}
bool read_bytes(void *object, int32_t length, int8_t **out) {
    if (length < 8 || length > 32 + 128 * 128 * 4)
        return false;
    auto &bytes = *static_cast<std::vector<int8_t> *>(object);
    bytes.resize(size_t(length));
    *out = bytes.data();
    return true;
}

bool transfer(AIBinder *window, const Request &request, bool &accepted) {
    AParcel *in = nullptr, *out = nullptr;
    bool ok = AIBinder_prepareTransaction(window, &in) == STATUS_OK && in &&
              AParcel_writeInt32(in, 13) == STATUS_OK &&
              AParcel_writeInt64(in, int64_t(uptime())) == STATUS_OK &&
              AParcel_writeByteArray(in, reinterpret_cast<const int8_t *>(request.bytes.data()),
                                     int32_t(request.bytes.size())) == STATUS_OK;
    binder_status_t status = STATUS_FAILED_TRANSACTION;
    if (ok) {
        status = AIBinder_transact(window, 0x00484351, &in, &out, 0);
        in = nullptr;
    }
    std::vector<int8_t> payload;
    ok = status == STATUS_OK && out && AParcel_readByteArray(out, &payload, read_bytes) == STATUS_OK;
    if (in)
        AParcel_delete(in);
    if (out)
        AParcel_delete(out);
    if (!ok)
        return false;
    bool poll = word(request, 4) == 4;
    size_t header = poll ? 32 : 8;
    if (payload.size() < header)
        return false;
    std::array<uint32_t, 8> response{};
    std::memcpy(response.data(), payload.data(), header);
    if (response[0] != magic)
        return false;
    accepted = response[1] == 1;
    if (!poll)
        return payload.size() == 8;
    uint64_t session = uint64_t(response[2]) | (uint64_t(response[3]) << 32);
    uint32_t width = response[4], height = response[5], bytes = response[6];
    bool ready = (response[1] & 1) != 0;
    if (bytes > 128 * 128 * 4 || width > 128 || height > 128 || (!ready && bytes) ||
        payload.size() != 32 + bytes)
        return false;
    if (bytes) {
        if (!width || !height || bytes != width * height * 4)
            return false;
        auto next = std::make_shared<BackIconPreview>();
        next->session = session;
        next->left = (response[1] & 2) != 0;
        next->width = width;
        next->height = height;
        next->pixels.resize(bytes);
        std::memcpy(next->pixels.data(), payload.data() + 32, bytes);
        std::lock_guard guard(icon_lock);
        icon = next;
    }
    {
        std::lock_guard guard(icon_lock);
        ready = ready && icon && icon->session == session;
    }
    available_session.store(ready ? session : 0, std::memory_order_release);
    return true;
}

void run() {
    AIBinder *window = nullptr;
    uint64_t next_poll = 0;
    for (;;) {
        Request request;
        bool have_request = false, refresh = false;
        {
            std::unique_lock guard(lock);
            uint64_t now = uptime();
            hold.advance(now);
            if (hold.active && now - hold.start > 10000) {
                hold.reset();
                active_session.store(0);
                queue.push_back(command(3));
            }
            bool was_ready = icon_ready.load();
            bool ready = hold.active && available_session.load() == hold.start &&
                         (was_ready ? hold.geometry(true) : hold.ready(now));
            if (ready && !was_ready) {
                icon_ready.store(true);
                refresh = true;
                __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow",
                                    "native hold ready session=%llu fullMs=%llu limitPx=%.1f",
                                    static_cast<unsigned long long>(hold.start),
                                    static_cast<unsigned long long>(now - hold.full_since), hold.limit);
            } else if (!ready && was_ready) {
                icon_ready.store(false);
                presented_session.store(0);
                refresh = hold.active;
                if (hold.active)
                    queue.push_back(command(6));
            }
            if (!queue.empty()) {
                request = std::move(queue.front());
                queue.pop_front();
                have_request = true;
            } else if (hold.active && available_session.load() != hold.start && now >= next_poll) {
                request = command(4);
                next_poll = now + 16;
                have_request = true;
                std::lock_guard image_guard(icon_lock);
                put(request, 8, icon ? icon->session : uint64_t(0));
            } else if (!refresh) {
                if (!hold.active)
                    changed.wait(guard, [] { return !queue.empty() || hold.active; });
                else {
                    uint64_t wake = available_session.load() == hold.start ? hold.start + 10001 : next_poll;
                    if (!ready)
                        wake = std::min(wake, hold.deadline());
                    changed.wait_for(guard, std::chrono::milliseconds(wake > now ? wake - now : 1));
                }
                continue;
            }
        }
        if (refresh)
            hc_back_arrow_refresh();
        if (!have_request)
            continue;
        if (!window)
            window = window_service();
        bool accepted = false;
        if (window && !transfer(window, request, accepted)) {
            AIBinder_decStrong(window);
            window = nullptr;
            icon_ready.store(false);
            available_session.store(0);
            presented_session.store(0);
            // Reconnect if the framework endpoint was replaced between gestures.
            if (word(request, 4) == 1 && word(request, 16) == 0) {
                window = window_service();
                if (window && !transfer(window, request, accepted)) {
                    AIBinder_decStrong(window);
                    window = nullptr;
                }
            }
        }
        if (request.reply) {
            std::lock_guard guard(request.reply->lock);
            request.reply->accepted = accepted;
            request.reply->done = true;
            request.reply->changed.notify_one();
        }
    }
}

bool submit(Request request) {
    auto reply = request.reply;
    {
        std::lock_guard guard(lock);
        if (word(request, 4) == 1 && word(request, 16) == 2 && !queue.empty() && word(queue.back(), 4) == 1 &&
            word(queue.back(), 16) == 2)
            queue.back() = std::move(request);
        else if (queue.size() < 32)
            queue.push_back(std::move(request));
        else
            return false;
        changed.notify_one();
    }
    if (!reply)
        return false;
    std::unique_lock guard(reply->lock);
    return reply->changed.wait_for(guard, std::chrono::milliseconds(80), [&] { return reply->done; }) &&
           reply->accepted;
}
} // namespace

void hc_back_feedback_start() {
    bool expected = false;
    if (started.compare_exchange_strong(expected, true))
        std::thread(run).detach();
}
void hc_back_feedback_short_side(float pixels) {
    if (!std::isfinite(pixels) || pixels <= 0 || pixels > 32768)
        return;
    if (native_short_side.exchange(pixels, std::memory_order_acq_rel) != pixels)
        __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow", "native screen shortSidePx=%.1f",
                            pixels);
}
bool hc_back_feedback_touch(uintptr_t owner, int action, int pointers, float x, float y, int64_t time,
                            float density, float width, uint64_t revision) {
    if (!started.load())
        return false;
    float short_side = std::min(width, native_short_side.load(std::memory_order_acquire));
    bool shown = false, valid = false, refresh = false;
    {
        std::lock_guard guard(lock);
        valid = hold.update(owner, action, pointers, x, y, time, density, width, revision, short_side);
        if (!valid) {
            active_session.store(0);
            icon_ready.store(false);
            presented_session.store(0);
        } else if (action == 0) {
            icon_ready.store(false);
            available_session.store(0);
            presented_session.store(0);
            active_session.store(uint64_t(time));
        } else if (action == 1) {
            shown = icon_ready.load() && presented_session.load() == hold.start && hold.geometry(true);
            hold.reset();
            active_session.store(0);
            icon_ready.store(false);
        } else if (icon_ready.load() && !hold.geometry(true)) {
            icon_ready.store(false);
            presented_session.store(0);
            queue.push_back(command(6));
            refresh = true;
            __android_log_print(ANDROID_LOG_INFO, "HyperCeiler.BackArrow",
                                "native hold cancelled session=%llu retreatDp=%.1f",
                                static_cast<unsigned long long>(hold.start),
                                (hold.peak - hold.inward()) / hold.density);
        }
        changed.notify_one();
    }
    if (refresh)
        hc_back_arrow_refresh();
    if (!valid) {
        (void)submit(command(3));
        return false;
    }
    if (action == 2)
        return false;
    Request request = command(1);
    put(request, 8, uint64_t(owner));
    put(request, 16, int32_t(action));
    put(request, 20, int32_t(pointers));
    put(request, 24, x);
    put(request, 28, y);
    put(request, 32, time);
    put(request, 40, density);
    put(request, 44, width);
    put(request, 48, revision);
    if (action == 1 && shown)
        request.reply = std::make_shared<Reply>();
    return submit(std::move(request));
}
void hc_back_feedback_cancel() {
    {
        std::lock_guard guard(lock);
        hold.reset();
        active_session.store(0);
        icon_ready.store(false);
        presented_session.store(0);
    }
    if (started.load())
        (void)submit(command(3));
}

BackVisualState hc_back_feedback_visual(bool left) {
    std::lock_guard guard(lock);
    if (!hold.active || hold.left() != left)
        return {};
    BackVisualState state{hold.start, icon_ready.load(), {}};
    std::lock_guard image_guard(icon_lock);
    if (icon && icon->session == hold.start && icon->left == left)
        state.icon = icon;
    return state;
}
bool hc_back_feedback_presented(uint64_t session) {
    std::lock_guard guard(lock);
    if (!icon_ready.load() || !session || session != active_session.load())
        return false;
    if (presented_session.load() == session || queue.size() >= 32)
        return false;
    presented_session.store(session);
    Request request = command(5);
    put(request, 8, session);
    queue.push_back(std::move(request));
    changed.notify_one();
    return true;
}
void hc_back_feedback_commit() {
    if (started.load())
        (void)submit(command(2));
}
