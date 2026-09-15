package android.content

open class Context {
    companion object { const val MODE_PRIVATE = 0 }
    private val stores = HashMap<String, SharedPreferences>()
    @Synchronized fun getSharedPreferences(name: String, mode: Int): SharedPreferences = stores.getOrPut(name) { SharedPreferences() }
}
class SharedPreferences {
    private val values = java.util.concurrent.ConcurrentHashMap<String, Any>()
    fun getString(key: String, fallback: String?) = values[key] as? String ?: fallback
    fun getLong(key: String, fallback: Long) = values[key] as? Long ?: fallback
    fun getInt(key: String, fallback: Int) = values[key] as? Int ?: fallback
    fun getBoolean(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback
    fun edit() = Editor()
    inner class Editor {
        private val pending = HashMap<String, Any>()
        fun putString(key: String, value: String) = apply { pending[key] = value }
        fun putLong(key: String, value: Long) = apply { pending[key] = value }
        fun putInt(key: String, value: Int) = apply { pending[key] = value }
        fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        fun apply() { values.putAll(pending) }
    }
}
