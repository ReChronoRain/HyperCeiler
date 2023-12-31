package com.sevtinge.hyperceiler.utils.api.miuiStringToast.res

class IconParams {
    private var category: String? = null
    private var iconFormat: String? = null
    private var iconResName: String? = null
    private var iconType = 0

    fun setCategory(category: String?) {
        this.category = category
    }

    fun setIconFormat(iconFormat: String?) {
        this.iconFormat = iconFormat
    }

    fun setIconType(iconType: Int) {
        this.iconType = iconType
    }

    fun setIconResName(iconResName: String?) {
        this.iconResName = iconResName
    }

    fun getCategory(): String? {
        return category
    }

    fun getIconFormat(): String? {
        return iconFormat
    }

    fun getIconType(): Int {
        return iconType
    }

    fun getIconResName(): String? {
        return iconResName
    }

    override fun toString(): String {
        return "IconParams{category='$category', iconFormat='$iconFormat', iconResName='$iconResName', iconType=$iconType}"
    }
}
