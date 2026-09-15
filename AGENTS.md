# Skript contributor guide

## Project purpose and state

Skript is an embeddable scripting engine for Kotlin. Its main reason for existing is that scripts can call Kotlin objects whose APIs include `suspend` functions. The language predates a suitable off-the-shelf option for that use case.

The engine has two source modes:

- regular scripts/modules; and
- page templates, in a PHP/Django-like style, which use the same host-provided API objects to emit HTML or other text. Production users rely on templates for output such as invoices and emails.

The syntax deliberately combines JavaScript, Kotlin, and a little Python—notably `*args` and `**kwargs`. `docs/langref.md` is the current, partial reference for regular Skript syntax, and `docs/templates.md` is the reference for the template sub-language.

This is a small, incomplete language implementation, but it is production software. Preserve existing behavior and embedding APIs unless a change is explicitly intended. In particular, there is runtime support for built-in and native classes, but Skript source cannot declare classes yet: the parser reports `class` as unimplemented.

The template-reference milestone is complete. The intended near-term order of work is now:

1. expand the test coverage; then
2. refactor with that behavioral safety net in place.

Do not start broad cleanup or architectural refactoring while doing the characterization-test work.

## Build and dependencies

- Gradle project using the checked-in wrapper (`./gradlew`).
- Kotlin/JVM library with one Java reflection helper.
- Java and Kotlin toolchains target Java 17.
- Tests use JUnit 5 and `kotlinx.coroutines`; suspend tests generally use `runBlocking`.
- Main dependencies include Kotlin reflection/coroutines, Jackson, and `io.github.mslenc:utilsktx`.
- Maven coordinates are `io.github.mslenc:skript`; consult `build.gradle` for the current version and dependency versions.
- `kotlin.code.style=official` is set, but there is no separate lint/format task configured.

Common commands:

```sh
./gradlew test
./gradlew test --tests 'skript.endtoend.PageTemplatesTest'
./gradlew test --tests 'skript.endtoend.PageTemplatesTest.blocksBasics'
```

The Gradle `test` task currently requests an 8 GiB min/max heap. The benchmark-style tests in `src/test/kotlin/skript/bench` are disabled and are not part of an ordinary test run.

## Architecture

The regular source pipeline is:

```text
source
  -> CharStream/Lexer
  -> Tokens
  -> ModuleParser
  -> AST
  -> VarAllocator (locals, globals, closures, template context lookup)
  -> OpCodeGen
  -> FunctionDef/opcodes
  -> SkriptEnv.executeFunction
```

The interpreter is stack based. `FastOpCode` handles synchronous operations; `SuspendOpCode` marks operations that must enter `executeSuspend`. Calls and module loading follow the suspend path, allowing reflected Kotlin `suspend` functions to be invoked with `callSuspendBy`. Keep this property intact when adding new callable or interop behavior.

Important entry points and responsibilities:

- `skript.io.SkriptEngine`: engine-wide native codecs and reflected class definitions; creates environments and installs standard globals (`String`, `Number`, `Boolean`, `Object`, `List`, `Map`, and `Regex`). Native reflection is denied by default and controlled by `NativeAccessGranter`.
- `skript.io.SkriptEnv`: per-environment globals, module cache, class objects, execution, anonymous scripts/templates, loaded templates, native globals, and host-callable script functions.
- `ModuleSourceSkript`: parses and compiles a regular module.
- `ModuleSourceTemplate`: parses, rewrites, and compiles a page template.
- `ModuleNameResolver`, `ModuleSourceProvider`, and `ModuleProvider`: host extension points for resolving and loading regular modules and templates. Imports are suspendable.
- `skript.values`: Skript's value/object/class model, including distinct `SkNull` and `SkUndefined`, scalars, lists, maps, functions, ranges, regexes, and class definitions.
- `skript.interop`: Kotlin reflection, `SkCodec` conversions, native functions/methods/properties/constructors, collection/array wrappers, and JSON support. `@SkriptIgnore` excludes public native APIs from reflection.
- `skript.ast`, `skript.analysis`, `skript.opcodes`, and `skript.exec`: AST definitions, scope/closure allocation, bytecode-like generation, and interpreter runtime.
- `skript.templates`: output runtime, built-in filters, and escaping functions.

`SkriptEngine` is shared configuration; `SkriptEnv` holds mutable execution state. When changing caching, globals, or module behavior, keep that ownership boundary in mind.

## Templates

Do not confuse page templates with backtick string templates. Backtick templates are ordinary language expressions covered by `StringTemplatesTest`; page templates are whole sources handled by `lexPageTemplate` and `PageTemplateParser`.

The page-template pipeline is:

```text
template text
  -> lexPageTemplate
  -> cleanUpStmtOnlyLines
  -> PageTemplateParser
  -> template AST statements
  -> rewriteTemplate
  -> ordinary Skript module AST
  -> the regular allocation/opcode/execution pipeline
```

`rewriteTemplate` is central. It turns literal output into `templateRuntime.emit(...)`, and inheritance/includes/blocks into imports plus exported `blocks`, `renderWithBlocks`, and `render` values. A loaded template is exposed to the host as a suspendable `TemplateInstance`.

The implementation and existing tests currently establish these features:

- raw text, `{# ... #}` comments, `{{ expression }}` output, and `{% statement %}` tags;
- automatic removal of whitespace-only lines containing statement tags;
- default escaping for expression output, explicit `-> escape` chains, and `|>` filter chains;
- template `val`/`var`, expressions, `if`/`elif`/`else`, `for`, `break`, and `continue`;
- `extends`, `include`, declared/overridden blocks, named block inclusion, and `include super block`;
- `with { ... }` context maps; absent an explicit map, includes/blocks receive a shallow `{ **ctx }` copy;
- implicit name lookup in render/block functions: local/closure names first, then the `ctx` map, then environment globals;
- default escapes: `raw`, `html`, `js`, `url`, and `markdown`;
- default locale-aware filters: `perc`, `integer`, `number`, `money`, `date`, `dateTime`, and `time`.

For template documentation or behavior changes, treat these files together:

- `docs/templates.md`
- `src/main/kotlin/skript/parser/Lexer.kt`
- `src/main/kotlin/skript/parser/parsePageTemplate.kt`
- `src/main/kotlin/skript/io/ModuleSourceTemplate.kt`
- `src/main/kotlin/skript/templates/`
- `src/test/kotlin/skript/endtoend/PageTemplatesTest.kt`
- `src/test/kotlin/skript/testUtils.kt`

`docs/templates.md` is the user-facing template reference. Keep it synchronized with the implementation and tests, which remain authoritative when investigating actual runtime behavior. Distinguish verified current behavior from desired future syntax.

## Repository map

- `docs/langref.md`: partial regular-language reference.
- `docs/templates.md`: page-template syntax and behavior reference.
- `src/main/kotlin/skript/parser`: lexers and recursive-descent parsers.
- `src/main/kotlin/skript/ast`: script and template AST nodes/visitors.
- `src/main/kotlin/skript/analysis`: name allocation, scopes/closures, and opcode generation.
- `src/main/kotlin/skript/opcodes`: stack-machine instructions, grouped by operation.
- `src/main/kotlin/skript/exec`: frames, compiled function definitions, and runtime modules.
- `src/main/kotlin/skript/values`: native Skript values and built-in behaviors.
- `src/main/kotlin/skript/interop`: host reflection, type codecs/wrappers, and JSON.
- `src/main/kotlin/skript/io`: public embedding API, module loading, source preparation, packing/unpacking, and errors.
- `src/main/kotlin/skript/templates`: template runtime, filters, and escaping.
- `src/test/kotlin/skript/endtoend`: behavioral language, module, interop, and template tests.
- `src/test/kotlin/skript/testUtils.kt`: shared helpers for executing scripts/templates and asserting Skript values.

## Testing and change discipline

- Prefer an end-to-end regression test for observable language, template, suspend-call, module, or interop behavior. Add focused lexer/analyzer/value tests when the defect is isolated to that layer.
- Use `runScriptWithEmit` for script behavior and `runTemplate` for standalone template behavior. For inheritance/includes, build a static `ModuleSourceProvider`, wrap it with `ModuleProvider.from`, and load via `SkriptEnv.loadTemplate`, as `PageTemplatesTest` does.
- For any template change, test both rendered output and whitespace behavior. Template-only statement lines are intentionally removed; inline tags retain surrounding literal text.
- For interop changes, cover conversion in both directions where applicable, Kotlin default/named/vararg parameters, nullability, and suspend functions.
- For parser/operator changes, inspect all affected layers: tokenization, AST, allocation, opcode generation, opcode execution, tests, and the relevant reference (`docs/langref.md` and/or `docs/templates.md`).
- Avoid mass formatting, opportunistic renames, or unrelated cleanup. Existing style is not perfectly uniform, and small diffs are easier to validate in this production-used interpreter.
- Preserve the distinction between `null` and `undefined`, loose versus strict equality, protected globals, and context-versus-global lookup unless the task explicitly changes their semantics.
- Never enable `FullNativeAccess` as a convenience default. Native visibility is part of the host's security boundary.
- Run the narrowest relevant tests while iterating, then the full suite before handing off when the environment permits it. If tests cannot run, report the exact environmental blocker rather than claiming validation.

## Known gaps

The regular-language reference is incomplete. Source-level classes, `when`, `try`, `throw`, and annotations are explicitly rejected as unimplemented by the regular parser. There are additional localized `TODO`s in reflection, conversion, comparison, and collection behavior; do not silently interpret those as authorized scope for an unrelated change.
