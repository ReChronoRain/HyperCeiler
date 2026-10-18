#pragma once

namespace home_layout {
// Slot 0 transports slider - 70 for wire compatibility, but the preference
// represents an absolute dp margin. Zero delta does NOT mean disabled here.
inline bool hotseat_margin_value(bool requested, int delta, double &value) {
    if (!requested || delta < -70 || delta > 80) return false;
    value = static_cast<double>(delta + 70);
    return true;
}
} // namespace home_layout
