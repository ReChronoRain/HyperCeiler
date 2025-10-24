package com.sevtinge.hyperceiler.main.page.about.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import com.hyperfocus.api.FocusApi
import com.hyperfocus.api.IslandApi
import com.sevtinge.hyperceiler.R
import org.json.JSONObject

object EggHelp {
    fun focusBuild(string: String, context: Context): Bundle {

        val icon =
            Icon.createWithResource(context, R.drawable.ic_hyperceiler)
                .apply { setTint(Color.WHITE) }
        val darkIcon =
            Icon.createWithResource(context, R.drawable.ic_hyperceiler)
                .apply { setTint(Color.BLACK) }
        val baseinfo = FocusApi.baseinfo(basetype = 1, title = string)


        return FocusApi.sendFocus(
            ticker = string,
            aodTitle = string,
            island = buildIslandTemplate(string),
            aodPic = icon,
            baseInfo = baseinfo,
            updatable = true,
            enableFloat = false,
            timeout = 120,
            picticker = icon,
            pictickerdark = darkIcon
        )

    }

    private fun buildIslandTemplate(title: String): JSONObject {
        val picInfo = IslandApi.picInfo(pic = "miui.focus.pic_ticker")
        val left = IslandApi.imageTextInfo(
            textInfo = IslandApi.TextInfo(title = title),
            picInfo = picInfo
        )
        val right = IslandApi.imageTextInfo(
            textInfo = IslandApi.TextInfo(title = " "),
            type = 2
        )
        val bigIsland =
            IslandApi.bigIslandArea(imageTextInfoLeft = left, imageTextInfoRight = right)
        val smallIsland =
            IslandApi.SmallIslandArea(picInfo = IslandApi.picInfo(pic = "miui.focus.pic_ticker"))
        return IslandApi.IslandTemplate(
            bigIslandArea = bigIsland,
            smallIslandArea = smallIsland
        )
    }
}
