package android.util

/** Minimalen nadomestek za android.util.Log v testih na namizju (brez Androida). */
object Log {
    @JvmStatic fun v(tag: String, msg: String): Int = 0
    @JvmStatic fun d(tag: String, msg: String): Int = 0
    @JvmStatic fun i(tag: String, msg: String): Int = 0
    @JvmStatic fun w(tag: String, msg: String): Int = 0
    @JvmStatic fun e(tag: String, msg: String): Int = 0
}
