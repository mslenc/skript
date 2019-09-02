package skript.opcodes

import skript.exec.RuntimeState

object SetMember : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value)
        }
    }
}

object SetMemberKeepValue : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value)

            stack.push(value)
        }
    }
}

class SetKnownMember(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value)
        }
    }
}

class SetKnownMemberKeepValue(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.setMember(key, value)

            stack.push(value)
        }
    }
}

object GetMember : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.findMember(key))
        }
    }
}

class GetKnownMember(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val obj = stack.pop()

            stack.push(obj.findMember(key))
        }
    }
}