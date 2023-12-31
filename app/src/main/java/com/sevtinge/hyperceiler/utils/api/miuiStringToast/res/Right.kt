package com.sevtinge.hyperceiler.utils.api.miuiStringToast.res

class Right {
    private var iconParams: IconParams? = null
    private var textParams: TextParams? = null

    fun setIconParams(iconParams: IconParams?) {
        this.iconParams = iconParams
    }

    fun setTextParams(textParams: TextParams?) {
        this.textParams = textParams
    }

    fun getIconParams(): IconParams {
        return iconParams!!
    }

    fun getTextParams(): TextParams {
        return textParams!!
    }

    override fun toString(): String {
        return "Right{iconParams=$iconParams, textParams=$textParams}"
    }
}
