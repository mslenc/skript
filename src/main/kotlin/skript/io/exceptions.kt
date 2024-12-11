package skript.io

import skript.parser.Pos

sealed class SkriptException(message: String, cause: Throwable? = null, val pos: Pos? = null) : Exception(message, cause)

class SkImportError(message: String, cause: Throwable? = null, pos: Pos? = null) : SkriptException(message, cause, pos)
class SkTypeError(message: String, cause: Throwable? = null, pos: Pos? = null) : SkriptException(message, cause, pos)
class SkSyntaxError(message: String, cause: Throwable? = null, pos: Pos? = null) : SkriptException(message, cause, pos)
class SkNativeError(cause: Throwable, message: String = cause.message ?: cause.javaClass.simpleName ?: "???", pos: Pos? = null) : SkriptException(message, cause, pos)
