package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkString

object SetMember : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value, state)
        }
    }
}

object SetMemberKeepValue : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value, state)

            stack.push(value)
        }
    }
}

class SetKnownMember(key: String) : SuspendOpCode() {
    private val key = SkString(key)

    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value, state)
        }
    }
}

class SetKnownMemberKeepValue(key: String) : SuspendOpCode() {
    private val key = SkString(key)

    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value, state)

            stack.push(value)
        }
    }
}

object GetMember : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.findMember(key, state))
        }
    }
}

class GetKnownMember(key: String) : SuspendOpCode() {
    val key = SkString(key)

    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val obj = stack.pop()

            stack.push(obj.findMember(key, state))
        }
    }
}