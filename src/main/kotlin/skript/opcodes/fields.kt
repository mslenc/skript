package skript.opcodes

import skript.exec.RuntimeState

object SetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.entrySet(key, value, state)
        }
    }
    override fun toString() = "SetElementOp"
}

object SetElementKeepValueOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.entrySet(key, value, state)

            stack.push(value)
        }
    }
    override fun toString() = "SetElementKeepValueOp"
}

class SetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propertySet(key, value, state)
        }
    }
    override fun toString() = "SetPropertyOp key=$key"
}

class SetPropertyKeepValueOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propertySet(key, value, state)

            stack.push(value)
        }
    }
    override fun toString() = "SetPropertyKeepValueOp key=$key"
}

object GetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.entryGet(key, state))
        }
    }
    override fun toString() = "GetElementOp"
}

class GetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val obj = stack.pop()

            stack.push(obj.propertyGet(key, state))
        }
    }
    override fun toString() = "GetPropertyOp"
}