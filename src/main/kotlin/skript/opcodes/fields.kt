package skript.opcodes

import skript.exec.RuntimeState

object SetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.elementSet(key, value, state)
        }
    }
}

object SetElementKeepValueOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.elementSet(key, value, state)

            stack.push(value)
        }
    }
}

class SetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propSet(key, value, state)
        }
    }
}

class SetPropertyKeepValueOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propSet(key, value, state)

            stack.push(value)
        }
    }
}

object GetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.elementGet(key, state))
        }
    }
}

class GetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val obj = stack.pop()

            stack.push(obj.propGet(key, state))
        }
    }
}