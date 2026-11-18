/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_channel.h"
#include "home_native_input_dispatch.h"
namespace home_native_input {
struct RuntimeContract {
    MonitorContract monitor;
    ChannelContract channel;
    ReceiverResultContract receiver;
    CallbackDropContract carrier;
    std::vector<CallbackEventContract> methods;
    DispatchContract dispatch;
    PackedStatusContract finish;
};
inline std::optional<RuntimeContract> runtime_contract(const sdk::Graph& graph) {
    const auto monitor=resolve_monitor(graph);const auto channel=channel_contract(graph);
    const auto receiver=receiver_result(graph);const auto carrier=callback_drop(graph);
    const auto dispatch=input_dispatch(graph);const auto finish=finish_status(graph);const auto batch=batch_consumer(graph);
    if(!monitor||!channel||!receiver||!carrier||!dispatch||!finish||!batch||dispatch->consume!=*batch)return {};
    const auto methods=callback_methods(graph,*carrier);if(!methods)return {};
    const std::set<uint32_t> roles={dispatch->key_slot,dispatch->motion_slot,dispatch->batch_slot,
        dispatch->automatic_slots[0],dispatch->automatic_slots[1]};
    if(roles.size()!=methods->size())return {};
    for(const auto& method:*methods)if(!roles.contains(method.slot)
        ||method.owns_event!=(method.slot==dispatch->key_slot||method.slot==dispatch->motion_slot))return {};
    return RuntimeContract{*monitor,*channel,*receiver,*carrier,*methods,*dispatch,*finish};
}
}
