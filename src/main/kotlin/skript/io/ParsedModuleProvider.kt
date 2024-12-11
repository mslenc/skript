package skript.io

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import skript.ast.ParsedModule
import skript.io.ModuleType.*
import skript.parser.*
import skript.parser.PageTemplateParser
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed class CachePolicy
object NoCaching : CachePolicy()
class CacheAll(val recheckEvery: Duration? = null) : CachePolicy()
class CacheMostRecent(val entries: Int, val recheckEvery: Duration? = null): CachePolicy()

interface ParsedModuleProvider {
    suspend fun getModule(moduleName: String): ParsedModule?

    companion object {
        fun from(sourceProvider: ModuleSourceProvider, caching: CachePolicy = CacheAll(), clock: Clock = Clock.systemDefaultZone()): ParsedModuleProvider {
            return when (caching) {
                NoCaching -> {
                    NoCachePMP(sourceProvider)
                }

                is CacheAll -> {
                    when {
                        caching.recheckEvery == null -> CacheAllNoCheckPMP(sourceProvider, clock)
                        caching.recheckEvery <= Duration.ZERO -> NoCachePMP(sourceProvider)
                        else -> CacheAllCheckPMP(sourceProvider, clock, caching.recheckEvery)
                    }
                }

                is CacheMostRecent -> {
                    // TODO - implement LRU or something like that
                    when {
                        caching.recheckEvery == null -> CacheAllNoCheckPMP(sourceProvider, clock)
                        caching.recheckEvery <= Duration.ZERO -> NoCachePMP(sourceProvider)
                        else -> CacheAllCheckPMP(sourceProvider, clock, caching.recheckEvery)
                    }
                }
            }
        }
    }
}

object NoModuleProvider : ParsedModuleProvider {
    override suspend fun getModule(moduleName: String): ParsedModule? {
        return null
    }
}

internal fun ModuleSource.parse(): ParsedModule {
    val chars = CharStream(source, fileName)

    return when (type) {
        SKRIPT ->        ModuleParser      (Tokens(chars.lexCodeModule())).  parseModule(moduleName)
        PAGE_TEMPLATE -> PageTemplateParser(Tokens(chars.lexPageTemplate().cleanUpStmtOnlyLines())).parsePageTemplate(moduleName)
    }
}

internal class NoCachePMP(val sourceProvider: ModuleSourceProvider) : ParsedModuleProvider {
    override suspend fun getModule(moduleName: String): ParsedModule? {
        return sourceProvider.getSource(moduleName)?.parse()
    }
}

class CacheEntry(val source: String?, val module: ParsedModule?, val error: Exception?, val timestamp: Instant = Instant.now())

fun CacheEntry.withinTtl(now: Instant, ttl: Duration): Boolean {
    return now <= timestamp + ttl
}

suspend fun parseAndCache(sourceProvider: ModuleSourceProvider, moduleName: String, clock: Clock, mutex: Mutex, cache: MutableMap<String, CacheEntry>): ParsedModule? {
    val source = sourceProvider.getSource(moduleName)
    var error: Exception? = null
    val parsed = try {
        source?.parse()
    } catch (e: Exception) {
        error = e
        null
    }
    val now = clock.instant()!!

    mutex.withLock {
        cache[moduleName] = CacheEntry(source?.source, parsed, error, now)
    }
    return parsed
}

internal class CacheAllNoCheckPMP(val sourceProvider: ModuleSourceProvider, val clock: Clock) : ParsedModuleProvider {
    val cache = HashMap<String, CacheEntry>()
    val mutex = Mutex()
    val parser = NoCachePMP(sourceProvider)

    override suspend fun getModule(moduleName: String): ParsedModule? {
        mutex.withLock {
            cache[moduleName]?.let { return it.module }
        }

        return parseAndCache(sourceProvider, moduleName, clock, mutex, cache)
    }
}

internal class CacheAllCheckPMP(val sourceProvider: ModuleSourceProvider, val clock: Clock, val ttl: Duration) : ParsedModuleProvider {
    val cache = HashMap<String, CacheEntry>()
    val mutex = Mutex()

    override suspend fun getModule(moduleName: String): ParsedModule? {
        mutex.withLock {
            cache[moduleName]?.let { cacheEntry ->
                if (cacheEntry.withinTtl(clock.instant(), ttl)) {
                    return cacheEntry.module
                }
            }
        }

        return parseAndCache(sourceProvider, moduleName, clock, mutex, cache)
    }
}

