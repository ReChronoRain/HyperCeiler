package com.zhenxiang.blur.model

data class CornersRadius(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float,
) {

    companion object {
        fun all(radius: Float): CornersRadius {
            return CornersRadius(radius, radius, radius, radius)
        }

        fun custom(
            topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float
        ): CornersRadius {
            return CornersRadius(topLeft, topRight, bottomLeft, bottomRight)
        }
    }
}
