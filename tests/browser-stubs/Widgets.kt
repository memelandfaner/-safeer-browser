package android.widget
import android.content.Context
import android.view.View
import android.view.ViewGroup
class FrameLayout(context: Context): ViewGroup(context) { class LayoutParams(width: Int, height: Int) }
class Button(context: Context): View(context) {
    var action: (() -> Unit)? = null
    fun setText(id: Int) {}
    fun setOnClickListener(listener: (View) -> Unit) { action = { listener(this) } }
}
