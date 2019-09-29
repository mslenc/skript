package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame

object SetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val obj = stack.pop()

            obj.entrySet(key, value, frame.env)
        }
        return null
    }
    override fun toString() = "SetElementOp"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-3)
    }
}

object SetElementKeepValueOp : SuspendOpCode() {
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
    override fun toString() = "SetElementKeepValueOp"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-2)
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-2)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}

object GetElementOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val key = stack.pop()
            val obj = stack.pop()

            stack.push(obj.entryGet(key, frame.env))
        }
        return null
    }
    override fun toString() = "GetElementOp"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
    }
}