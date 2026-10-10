package android.widget
class ImageView {
    var listener: ((ImageView) -> Unit)? = null
    fun setOnClickListener(callback: (ImageView) -> Unit) { listener=callback }
    fun click() { checkNotNull(listener).invoke(this) }
}
