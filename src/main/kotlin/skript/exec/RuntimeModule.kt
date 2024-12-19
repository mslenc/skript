package skript.exec

import skript.io.ModuleName
import skript.values.SkMap
import skript.values.SkValue

class RuntimeModule(val moduleName: ModuleName, val exports: SkMap = SkMap(), val moduleVars: Array<SkValue> = emptyArray())
