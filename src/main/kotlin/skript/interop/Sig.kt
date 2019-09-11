package skript.interop

abstract class Sig<T: Function<R>, R>: Comparable<Sig<T, R>> {
    val funType = this::class.supertypes[0].arguments[0].type
    val allTypes = funType!!.arguments.map { it.type!! }
    val argTypes = allTypes.subList(0, allTypes.size - 1)
    val retType = this::class.supertypes[0].arguments[1].type

    override fun compareTo(other: Sig<T, R>): Int {
        return 0
    }
}
