package com.sevtinge.hyperceiler.utils.api.miuiStringToast.res

class Left {
    private var iconParams: IconParams? = null
    private var textParams: TextParams? = null

    fun setIconParams(iconParams: IconParams?) {
        this.iconParams = iconParams
    }

    fun setTextParams(textParams: TextParams?) {
        this.textParams = textParams
    }

    fun getIconParams(): IconParams? {
        return iconParams
    }

    fun getTextParams(): TextParams? {
        return textParams
    }

    override fun toString(): String {
        return "Left{iconParams=$iconParams, textParams=$textParams}"
    }
}
