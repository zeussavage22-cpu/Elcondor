package dev.darknet.service

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Small reflection helpers for the hidden and system APIs DarkNet needs. They
 * run in the privileged process, where hidden-API checks are lifted at start.
 */
internal object Reflect {

    /** Calls the first public-or-hidden method named [name] whose parameters accept [args]. */
    fun call(target: Any, name: String, vararg args: Any?): Any? {
        val method = findMethod(target.javaClass, name, args)
            ?: throw NoSuchMethodException("${target.javaClass.name}.$name(${args.size} args)")
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    fun has(target: Any, name: String, argCount: Int): Boolean =
        allMethods(target.javaClass).any { it.name == name && it.parameterCount == argCount }

    fun newInstance(className: String, vararg args: Any?): Any {
        val cls = Class.forName(className)
        val ctor = cls.declaredConstructors.firstOrNull { c ->
            c.parameterCount == args.size && c.parameterTypes.withIndex().all { (i, t) -> accepts(t, args[i]) }
        } ?: throw NoSuchMethodException("$className.<init>(${args.size} args)")
        ctor.isAccessible = true
        return ctor.newInstance(*args)
    }

    /** Implements a callback interface by name; [onCall] receives the method name and arguments. */
    fun proxy(interfaceName: String, onCall: (String, Array<out Any?>) -> Unit): Any {
        val iface = Class.forName(interfaceName)
        val handler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "DarkNet:${iface.simpleName}"
                else -> {
                    runCatching { onCall(method.name, args ?: emptyArray()) }
                    defaultReturn(method)
                }
            }
        }
        return Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), handler)
    }

    private fun defaultReturn(method: Method): Any? = when (method.returnType) {
        java.lang.Boolean.TYPE -> false
        Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        else -> null
    }

    private fun findMethod(cls: Class<*>, name: String, args: Array<out Any?>): Method? =
        allMethods(cls).firstOrNull { m ->
            m.name == name && m.parameterCount == args.size &&
                m.parameterTypes.withIndex().all { (i, t) -> accepts(t, args[i]) }
        }

    private fun allMethods(cls: Class<*>): List<Method> {
        val out = mutableListOf<Method>()
        var c: Class<*>? = cls
        while (c != null) {
            out += c.declaredMethods
            c = c.superclass
        }
        out += cls.methods
        return out
    }

    private fun accepts(type: Class<*>, value: Any?): Boolean {
        if (value == null) return !type.isPrimitive
        val boxed = when (type) {
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            Integer.TYPE -> Integer::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            else -> type
        }
        return boxed.isInstance(value)
    }
}
