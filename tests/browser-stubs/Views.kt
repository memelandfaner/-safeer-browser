package android.view
import android.content.Context
open class View(val context: Context) {
    var parent: ViewGroup? = null
    var layoutParams: Any? = null
}
open class ViewGroup(context: Context): View(context) {
    class LayoutParams { companion object { const val MATCH_PARENT = -1 } }
    val children = mutableListOf<View>()
    fun removeAllViews() { children.toList().forEach { removeView(it) } }
    fun removeView(view: View) { children.remove(view); view.parent = null }
    fun addView(view: View) { check(view.parent == null); children.add(view); view.parent = this }
}
