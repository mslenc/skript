package skript.opcodes

import skript.exec.Frame

data object SetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.entrySet(key, value, frame.env)
        }
        return null
    }
}

data object SetElementKeepValueOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.entrySet(key, value, frame.env)

            stack.push(value)
        }
        return null
    }
}

class SetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propertySet(key, value, frame.env)
        }
        return null
    }
    override fun toString() = "SetPropertyOp key=$key"
}

class SetPropertyKeepValueOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val obj = stack.pop()

            obj.propertySet(key, value, frame.env)

            stack.push(value)
        }
        return null
    }
    override fun toString() = "SetPropertyKeepValueOp key=$key"
}

data object GetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.entryGet(key, frame.env))
        }
        return null
    }
}

class GetPropertyOp(val key: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val obj = stack.pop()

            stack.push(obj.propertyGet(key, frame.env))
        }
        return null
    }
    override fun toString() = "GetPropertyOp"
}