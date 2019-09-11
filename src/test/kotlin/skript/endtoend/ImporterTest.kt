package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import skript.interop.SkCodec
import skript.interop.SkCodecString
import skript.io.NativeAccessGranter
import skript.io.SkriptEngine
import java.lang.UnsupportedOperationException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.createType

object ImporterNativeObjects : NativeAccessGranter {
    override fun isAccessAllowed(klass: KClass<*>): Boolean {
        return when (klass) {
            LocalDate::class,
            LocalDateTime::class,
            ContactInfoUpdater::class -> true

            else -> false
        }
    }
}

class ImporterTest {
    @Test
    fun testReflectionBasics() = runBlocking {
        val engine = SkriptEngine(nativeAccessGranter = ImporterNativeObjects)
        val env = engine.createEnv()

        val updater = ContactInfoUpdater()
        env.setNativeGlobal("updater", updater)
        env.runAnonymousScript("""
            if (!updater.loadById("def"))
                updater.createNew();
                
            updater.firstName = "Janez";
            updater.lastName = "Novak";
        """.trimIndent())

        assertTrue(updater.firstNameProperty.holder is UpdatedValue)
        assertEquals("Janez", updater.firstName)
        assertEquals(EntityState.NEW, updater.state)
    }

    @Test
    fun testFunctionMode() = runBlocking {
        val engine = SkriptEngine(nativeAccessGranter = ImporterNativeObjects)
        val env = engine.createEnv()

        val stringType = String::class.createType()
        val stringsType = List::class.createType(listOf(invariant(stringType)))

        val skriptFunc = env.createCallable(
            listOf<Pair<String, SkCodec<*>>>(
                "updater" to engine.getNativeCodec(ContactInfoUpdater::class)!!,
                "row" to engine.getNativeCodec(stringsType)!!
            ),
            SkCodecString,
            """
                updater.createNew();
                updater.firstName = `${'$'}{ row[1] } : ${'$'}{ row[2] }`;
                updater.lastName = row[0];
                return "OK";
            """)

        repeat(100000) { idx ->
            val updater = ContactInfoUpdater()
            val row = listOf("Mimi", "Rogers", idx.toString())
            val skriptResult = skriptFunc(updater, row)

            assertEquals("OK", skriptResult)
            assertEquals("Rogers : $idx", updater.firstName)
            assertEquals("Mimi", updater.lastName)
        }
    }
}


sealed class ValueHolder<out T>
object NoValue : ValueHolder<Nothing>()
data class InitialValue<T>(val value: T, val readOnly: Boolean) : ValueHolder<T>()
data class UpdatedValue<T>(val value: T) : ValueHolder<T>()

class UpdaterValue<T> {
    var holder: ValueHolder<T> = NoValue

    fun initValue(value: T, readOnly: Boolean = false) {
        holder = InitialValue(value, readOnly)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when (val holder = this.holder) {
            is NoValue -> throw IllegalStateException("There is no value")
            is InitialValue -> holder.value
            is UpdatedValue -> holder.value
        }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        when (val holder = this.holder) {
            is InitialValue -> {
                if (holder.readOnly)
                    throw UnsupportedOperationException("Can't change ${property.name}")
                this.holder = UpdatedValue(value)
            }

            is NoValue,
            is UpdatedValue -> {
                this.holder = UpdatedValue(value)
            }
        }
    }
}

enum class EntityState {
    UNINITIALIZED,
    LOADED,
    NEW
}

class ContactInfoUpdater {
    internal var state = EntityState.UNINITIALIZED

    internal val firstNameProperty = UpdaterValue<String>()
    internal val lastNameProperty = UpdaterValue<String>()
    internal val dateCreatedProperty = UpdaterValue<LocalDateTime>()

    var firstName by firstNameProperty
    var lastName by lastNameProperty
    val dateCreated by dateCreatedProperty

    fun loadById(id: String): Boolean {
        check(state == EntityState.UNINITIALIZED) { "The entity was already loaded or created, can't load it (again)" }

        if (id == "abc") {
            state = EntityState.LOADED
            firstNameProperty.initValue("Mitja")
            lastNameProperty.initValue("Šlenc")
            dateCreatedProperty.initValue(LocalDateTime.of(2019, 9, 10, 13, 55, 22), readOnly = true)
            return true
        } else {
            return false
        }
    }

    fun createNew() {
        check(state == EntityState.UNINITIALIZED) { "The entity was already loaded or created, can't create a new one (again)" }
        state = EntityState.NEW
        dateCreatedProperty.initValue(LocalDateTime.now(), readOnly = true)
    }


}