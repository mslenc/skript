package skript.values

import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

class SkList : SkAbstractList {
    val listEls = ArrayList<SkValue>()

    constructor()

    constructor(elements: List<SkValue>) {
        this.listEls.addAll(elements)
    }

    constructor(elements: SkAbstractList) {
        if (elements is SkList) {
            listEls.addAll(elements.listEls)
        } else {
            for (i in 0 until elements.getSize())
                add(elements.getSlot(i))
        }
    }

    override val klass: SkClassDef
        get() = SkListClassDef

    override fun getSize(): Int {
        return listEls.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return listEls[index]
    }

    override fun setSlot(index: Int, value: SkValue) {
        when {
            index < 0 -> typeError("Can't set values at negative indices")
            index == listEls.size -> listEls.add(value)
            index < listEls.size -> listEls[index] = value
            else -> {
                while (listEls.size < index)
                    listEls.add(SkUndefined)
                listEls.add(value)
            }
        }
    }

    override fun setSize(newSize: Int) {
        when {
            newSize < 0 -> typeError("Can't set length to a negative value")
            newSize == 0 -> listEls.clear()
            listEls.size > newSize -> {
                do {
                    listEls.removeAt(listEls.size - 1)
                } while (listEls.size > newSize)
            }
            listEls.size < newSize -> {
                do {
                    listEls.add(SkUndefined)
                } while (listEls.size < newSize)
            }
        }
    }

    override fun add(value: SkValue) {
        listEls.add(value)
    }

    override fun addAll(values: List<SkValue>) {
        listEls.addAll(values)
    }

    fun addAll(values: SkAbstractList) {
        values.forEach { listEls.add(it) }
    }

    override fun removeLast(): SkValue {
        return when {
            listEls.isNotEmpty() -> listEls.removeAt(listEls.size - 1)
            else -> SkUndefined
        }
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.LIST
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other is SkList -> listEls == other.listEls && entries == other.entries
            else -> false
        }
    }

    override fun hashCode(): Int {
        return listEls.hashCode() * 31 + entries.hashCode()
    }

    override fun unwrap(): List<Any?> {
        return listEls.map { it.unwrap() }
    }
}

object SkListClassDef : SkCustomClass<SkList>("List", SkAbstractListClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        check(args.noKwArgs()) { "List constructor doesn't support named arguments" }
        return args.extractPosVarArgs("")
    }
}

inline fun SkAbstractList.forEach(block: (SkValue)->Unit) {
    for (i in 0 until getSize()) {
        block(getSlot(i))
    }
}