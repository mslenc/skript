package skript.interop;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides a method that takes an Enum class and returns the enum members indexed by their names.
 * Useful to bypass the breakage caused by newer Kotlin generics rules.
 */
public class EnumHelper {
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getEnumValues(Class<T> klass) {
        return (Map<String, T>) getEnumValuesImpl(klass);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getEnumValuesImpl(Class klass) {
        Set values = EnumSet.allOf(klass);

        LinkedHashMap result = new LinkedHashMap();
        for (Object value : values) {
            result.put(((Enum)value).name(), value);
        }

        return result;
    }
}
