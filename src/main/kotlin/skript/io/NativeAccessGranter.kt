package skript.io

import kotlin.reflect.KClass

interface NativeAccessGranter {
    fun isAccessAllowed(klass: KClass<*>): Boolean
}

object NoNativeAccess : NativeAccessGranter {
    override fun isAccessAllowed(klass: KClass<*>): Boolean {
        return false
    }
}

object FullNativeAccess : NativeAccessGranter {
    override fun isAccessAllowed(klass: KClass<*>): Boolean {
        return true
    }
}