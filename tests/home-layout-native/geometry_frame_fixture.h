/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "targets/home/home_drop_geometry.h"
// This arithmetic fixture explicitly owns these synthetic frame slots. The
// production decoders and relocated frames are tested by runtime-layout-contracts.
inline const home_layout::FolderGeometryContract kFolderFrame=[] {
    home_layout::FolderGeometryContract value;
    value.frame[0]={-8,-16,-24,-32,-40,-48,0,0};
    value.frame[1]={-8,-16,-24,-32,-40,-48,-56,0};
    return value;
}();
inline constexpr home_layout::DropGeometryContract kDropFrame{{0,0},-8,-16,-24,-56,-64};
inline constexpr home_dynamic::GeometryFrame kWorkspaceFrame{
    -8,0,0,-88,-64,-80,-72,-88,-64,-80,-72};
inline constexpr home_dynamic::GeometryFrame kOccupiedFrame{
    -8,-16,0,-80,0,-96,-88,-112,-104,-96,-88};
inline constexpr home_dynamic::GeometryFrame kHotseatFrame{
    -8,0,-80,-128,0,0,0,0,0,0,0};
