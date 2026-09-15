package android.os
class Bundle { var url = "" }
class Looper { companion object { fun getMainLooper() = Looper() } }
object SystemClock { var time = 0L; fun elapsedRealtime() = time }
class Handler(looper: Looper) {
    fun postDelayed(task: Runnable, delay: Long) {}
    fun removeCallbacks(task: Runnable) {}
    fun removeCallbacksAndMessages(token: Any?) {}
}
