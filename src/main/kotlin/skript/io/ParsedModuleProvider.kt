package skript.io

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import skript.ast.Module
import skript.lexer.CharStream
import skript.lexer.Tokens
import skript.lexer.lex
import skript.lexer.parseModule
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed class CachePolicy
object NoCaching : CachePolicy()
class CacheAll(val recheckEvery: Duration? = null) : CachePolicy()
class CacheMostRecent(val entries: Int, val recheckEvery: Duration? = null): CachePolicy()

interface ParsedModuleProvider {
    suspend fun getModule(moduleName: String): Module?

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
    override suspend fun getModule(moduleName: String): Module? {
        return null
    }
}

fun ModuleSource.parse(): Module {
    val tokens = CharStream(source, fileName).lex()
    return Tokens(tokens).parseModule(moduleName)
}

internal class NoCachePMP(val sourceProvider: ModuleSourceProvider) : ParsedModuleProvider {
    override suspend fun getModule(moduleName: String): Module? {
        return sourceProvider.getSource(moduleName)?.parse()
    }
}

class CacheEntry(val source: String?, val module: Module?, val error: Exception?, val timestamp: Instant = Instant.now())

fun CacheEntry.withinTtl(now: Instant, ttl: Duration): Boolean {
    return now <= timestamp + ttl
}

suspend fun parseAndCache(sourceProvider: ModuleSourceProvider, moduleName: String, clock: Clock, mutex: Mutex, cache: MutableMap<String, CacheEntry>): Module? {
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

    override suspend fun getModule(moduleName: String): Module? {
        mutex.withLock {
            cache[moduleName]?.let { return it.module }
        }

        return parseAndCache(sourceProvider, moduleName, clock, mutex, cache)
    }
}

internal class CacheAllCheckPMP(val sourceProvider: ModuleSourceProvider, val clock: Clock, val ttl: Duration) : ParsedModuleProvider {
    val cache = HashMap<String, CacheEntry>()
    val mutex = Mutex()

    override suspend fun getModule(moduleName: String): Module? {
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

