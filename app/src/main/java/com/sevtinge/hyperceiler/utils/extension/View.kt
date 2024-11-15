package com.sevtinge.hyperceiler.utils.extension

import android.view.*

fun View.setPadding(padding: Int) = setPadding(padding, padding, padding, padding)

fun View.setPaddingLeft(paddingLeft: Int) = setPaddingSide(paddingLeft, paddingRight)
fun View.setPaddingRight(paddingRight: Int) = setPaddingSide(paddingLeft, paddingRight)

fun View.setPaddingSide(paddingSide: Int) = setPaddingSide(paddingSide, paddingSide)
fun View.setPaddingSide(
    paddingLeft: Int,
    paddingRight: Int
) = setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)