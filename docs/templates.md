# Page templates

Page templates mix literal output with Skript expressions and control flow. They use the same values, globals, native objects, functions, and suspend functions as regular Skript modules; see [langref.md](langref.md) for the expression language.

# Quick reference

```text
Literal text                                      Emitted unchanged

{# comment #}                                    Template comment

{{ expression }}                                 Emit using the default escape
{{ expression |> filter }}                       Filter, then emit
{{ expression |> filter(arg) -> escape }}        Filter and explicitly escape
{{ expression -> escape1 -> escape2 }}            Apply explicit escapes in order
{{ expression -> escape(arg) }}                   Pass arguments to a custom escape

{% val name = expression %}                      Initialized local value
{% var name = expression %}                      Local variable
{% expression %}                                 Evaluate for side effects

{% if condition %}...{% end %}                   Conditional
{% if condition %}...{% else %}...{% end if %}
{% if condition %}...{% elif condition %}...{% end %}

{% for value in iterable %}...{% end %}           Iterate values
{% for key, value in iterable %}...{% end for %}  Iterate keys and values
{% break %}
{% continue %}

{% include "template" %}                         Render another template
{% include "template" with { name: value } %}    Render with explicit context

{% extends "parent" %}                           Inherit another template
{% block name %}...{% end %}                     Declare and render a block
{% declare block name %}...{% end block %}        Declare without rendering here
{% include block name %}                          Render a named current block
{% include super block %}                         Render this block's parent version
```

Unlike regular Skript control statements, template `if` and `for` tags do not use parentheses or braces. Do not put a semicolon before `%}`.

Only the statement forms shown in this reference are accepted in template tags. Regular-module forms such as `while`, `do`, function declarations, `return`, `import`, and `export` are not part of the template grammar.

# Text, tags, and comments

Text outside a tag is emitted exactly as written. This includes spaces and line endings.

```html
Hello, world!
```

There are three tag forms:

```text
{{ expression }}     # evaluate and emit an expression
{% statement %}      # execute a template statement
{# comment #}        # discard a template comment
```

A template comment ends at the first `#}` and is not emitted. Inside expression and statement tags, Skript block comments are also accepted:

```text
#* non-nesting block comment *#
#+ nesting #+ block comment +# ... +#
```

Single-line `# comment` syntax is not accepted inside a template tag. Use `{# ... #}` for template comments.

# Expression output

```text
{{ expression }}
```

The expression can use the regular Skript expression syntax documented in [langref.md](langref.md), including property and element access, calls, operators, lists, maps, and ranges.

The value is passed through the runtime's default escape and then emitted. Both `null` and `undefined` become an empty string with the built-in escapes.

```html
<p>{{ customer.name }}</p>
<p>{{ lines[0].description }}</p>
```

Literal template text is never escaped.

## Filters

Use `|>` to pass a value through a filter:

```text
{{ value |> filter }}
{{ value |> filter(arg1, arg2) }}
{{ value |> filter(namedArg = value) }}
{{ value |> first |> second }}
```

The input is inserted as the filter's first positional argument. For example:

```text
{{ amount |> number(maxDigits = 2) }}
```

is equivalent to calling `number(amount, maxDigits = 2)`. A bare filter name in an output expression is resolved from `TemplateRuntime.filters`.

Filters run before output escaping. If no explicit escape follows the expression, the filtered result still receives the default escape:

```text
{{ createdAt |> date("long") }}
{{ createdAt |> dateTime("full") -> html }}
```

## Explicit escaping

Use `->` to select an escape instead of the runtime default:

```text
{{ value -> html }}
{{ value -> js -> html }}
{{ value |> filter -> url }}
```

Escapes are applied from left to right. As soon as an explicit escape is present, the default escape is not applied. Consequently, `-> raw` explicitly bypasses a configured `html` or other non-raw default:

```text
{{ trustedHtml -> raw }}
```

A bare escape name is resolved from `TemplateRuntime.escapes`.

Custom escapes may take additional arguments using `-> escape(arg1, arg2)`. As with a filter, the value on the left is inserted as the escape function's first positional argument. The built-in escapes do not have additional parameters.

# Context and name lookup

The host renders a template with a context map. It is available explicitly as `ctx`, while its entries can normally be referenced directly:

```html
{{ ctx.customerName }}
{{ customerName }}
```

Names in render and block bodies are resolved in this order:

1. local variables;
2. a defined entry in `ctx`; and
3. an environment global.

A context entry whose value is `null` is defined and resolves to `null`. A missing entry, or one containing `undefined`, falls through to the globals.

Assigning to a bare name updates the context when that key already exists. Otherwise, it writes an environment global, which can fail when that global is protected. Prefer local `val` or `var` declarations unless context/global mutation is intentional.

# Variables and expression statements

## Initialized values

`val` requires an initializer. Multiple declarations can share a tag.

```text
{% val subtotal = quantity * unitPrice %}
{% val first = values[0], second = values[1] %}
```

## Variables

`var` may omit its initializer, in which case its initial value is `undefined`. As with `val`, multiple declarations can share a tag.

```text
{% var count = 0 %}
{% var result %}
{% count += 1 %}
```

Declarations are lexically scoped to their template block, conditional branch, or loop body. The variables declared by a `for` loop are scoped to its body.

## Expression statements

Any regular Skript expression can be evaluated in a statement tag. Its result is discarded. This is useful for assignments and calls made for side effects, including calls to host-provided suspend functions.

```text
{% total += line.amount %}
{% audit.recordInvoice(invoice.id) %}
```

# Conditionals

```text
{% if condition %}
    ...
{% end %}
```

An `else` branch is optional:

```text
{% if condition %}
    ...
{% else %}
    ...
{% end if %}
```

Use `elif` for additional conditions:

```text
{% if total < 0 %}
    Credit
{% elif total == 0 %}
    Settled
{% else %}
    Amount due
{% end %}
```

The closing tag may be either `{% end %}` or `{% end if %}`.

# Loops

Template `for` accepts one or two loop variables. With one variable, it receives each value:

```text
{% for line in lines %}
    {{ line.description }}
{% end %}
```

With two loop variables, the first receives the key/index and the second receives the value:

```text
{% for index, line in lines %}
    {{ index }}: {{ line.description }}
{% end for %}
```

For lists and strings, the key is a zero-based numeric index. For maps, it is the map key. Other host values can provide their own Skript iterator behavior.

The closing tag may be either `{% end %}` or `{% end for %}`.

Use `break` and `continue` inside loops:

```text
{% for line in lines %}
    {% if line.hidden %}{% continue %}{% end %}
    {% if line.stop %}{% break %}{% end %}
    {{ line.description }}
{% end %}
```

# Including templates

```text
{% include "templateName" %}
```

The string is resolved through the environment's `ModuleNameResolver` and loaded through its module provider. Rendering is suspendable.

By default, an included template receives a shallow copy of the current context:

```text
{% include "address" %}
```

Use `with` and a map literal to replace that context:

```text
{% include "address" with { address: billingAddress } %}
```

An explicit context does not inherit the current context automatically. Spread `ctx` when both inherited and overridden values are wanted:

```text
{% include "address" with { **ctx, address: shippingAddress } %}
```

The copy is shallow: nested objects and collections remain the same values.

# Inheritance and blocks

## Extending a template

```text
{% extends "parentTemplate" %}
```

A template can contain at most one `extends` tag. The parent is resolved and loaded like an included template.

The parent renders with the child's complete block map, so blocks declared by the child override blocks with the same name inherited from the parent. In multi-level inheritance, the block map accumulates through the chain.

Conventionally, put `extends` at the start and put child output inside blocks. Current behavior renders the parent first and then evaluates the remainder of the child template. Therefore, literal or expression output outside child block declarations is emitted after the parent output rather than being ignored.

## Declaring and rendering a block

```text
{% block body %}
    ...
{% end %}
```

`block` both declares a named block and renders the currently selected implementation at that location. A child declaration with the same name overrides its parent. Block names must be unique within one template.

All of these closing forms are accepted:

```text
{% end %}
{% end block %}
{% end block body %}
```

## Declaring without rendering

```text
{% declare block sidebar %}
    ...
{% end block %}
```

`declare block` adds the block to the template's block map but emits nothing at the declaration site. It can later be rendered with `include block`.

## Rendering a named block

```text
{% include block sidebar %}
{% include block sidebar with { **ctx, compact: true } %}
```

This renders the named block selected from the current, fully overridden block map. As with template includes, omitting `with` passes a shallow copy of the current context; an explicit map replaces it.

## Rendering the parent block

Inside a block, use `include super block` to render the immediate parent's implementation of the same block:

```text
{% block body %}
    <header>Before parent</header>
    {% include super block %}
    <footer>After parent</footer>
{% end %}
```

It also accepts an explicit context:

```text
{% include super block with { **ctx, compact: true } %}
```

`include super block` is invalid outside a block. The parent must provide that block when it is executed.

# Whitespace

A line containing one or more statement tags and otherwise only spaces/tabs has its indentation and line ending removed. This keeps control-flow tags from creating blank lines:

```html
<ul>
    {% for item in items %}
    <li>{{ item }}</li>
    {% end %}
</ul>
```

Expression tags count as output, so a line containing `{{ ... }}` is retained. A statement tag on a line that also contains literal non-whitespace text is retained as well:

```html
{% if visible %}<span>{{ label }}</span>{% end %}
```

Template comments do not trigger statement-line removal. Their surrounding whitespace and line ending remain literal output.

# Built-in escapes

`TemplateRuntime.createWithDefaults` provides these escapes:

| Escape | Behavior |
| --- | --- |
| `raw` | Converts the value to a string without escaping. |
| `html` | Escapes `&`, `<`, `>`, `"`, and `'`. |
| `js` | Backslash-escapes quotes, backslash, newline, carriage return, tab, backspace, vertical tab, form feed, and NUL. |
| `url` | UTF-8 percent-encodes characters except letters, digits, and `-_.!~*'(),;:`. |
| `markdown` | Prefixes Markdown punctuation ``\`*_{}[]<>()#+-.!|`` with a backslash. |

All built-in escapes render `null` and `undefined` as an empty string.

The default escape is selected by the host. `createWithDefaults` uses `raw` unless the host chooses another key, such as `html`.

# Built-in filters

The default filters are locale-aware. Unless the host overrides them, `TemplateRuntime.createWithDefaults` uses `Locale.US`, the system time zone, and the locale's currency.

All optional `fallback` arguments default to an empty string. Numeric filters use the fallback when the input is not a Skript number. Date/time filters use it for `null`, `undefined`, and unsupported value types.

## Numbers

```text
{{ value |> perc(fallback = "", minDigits = 0, maxDigits = 1) }}
{{ value |> integer(fallback = "") }}
{{ value |> number(fallback = "", minDigits = 0, maxDigits = 3, preferInt = false) }}
{{ value |> money(fallback = "", minDigits = 2, maxDigits = 2) }}
```

| Filter | Default formatting |
| --- | --- |
| `perc` | Locale percentage, 0 to 1 fractional digits. |
| `integer` | Locale integer formatting. |
| `number` | Locale number, 0 to 3 fractional digits. If `preferInt` is true and the value is integral, uses 0 fractional digits. |
| `money` | Configured locale/currency, exactly 2 fractional digits. |

Only `minDigits` and `maxDigits` values that convert to non-negative integers override the defaults; other values are treated as absent. Supplying only one leaves the other at its default.

## Dates and times

```text
{{ value |> date(format = "long", fallback = "") }}
{{ value |> dateTime(format = "yyyy-MM-dd HH:mm", fallback = "") }}
{{ value |> time(format = "short", fallback = "") }}
```

| Filter | Accepted host values | Default formatting |
| --- | --- | --- |
| `date` | `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `OffsetDateTime` | Localized short date. |
| `dateTime` | `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `OffsetDateTime` | Localized short date and time. |
| `time` | `LocalTime`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `OffsetDateTime` | Localized short time. |

`format` may be one of `short`, `medium`, `long`, or `full` (lowercase or uppercase), or a Java `DateTimeFormatter` pattern. A missing or invalid pattern uses the default formatter. An empty string is a valid empty formatter and therefore produces an empty result.

The configured time zone is used where a value needs a zone: notably for `Instant`, and for local values passed to `dateTime`/`time`. A `LocalDate` formatted by `dateTime` is placed at noon in the configured zone.

# Runtime customization

The host controls the output destination, filters, escapes, default escape, locale, currency, and time zone through `TemplateRuntime`. Custom filter and escape functions use the same `|>` and `->` syntax when placed in the runtime's maps.

Templates can be executed directly with `SkriptEnv.runAnonymousTemplate`, or loaded as modules with `SkriptEnv.loadTemplate`. A loaded `TemplateInstance.execute` call receives an `SkMap` context and a `TemplateRuntime` and is suspendable.

# Complete example

Base template, `layout`:

```html
<!doctype html>
<html>
    <head>
        <title>{% block title %}Invoice{% end %}</title>
    </head>
    <body>
        {% block body %}{% end %}
    </body>
</html>
```

Included template, `line`:

```html
<li>{{ line.description }}: {{ line.amount |> money }}</li>
```

Child template:

```html
{% extends "layout" %}
{% block title %}
    Invoice {{ invoice.number }}
{% end %}
{% block body %}
    <h1>{{ invoice.customer.name }}</h1>

    <ul>
        {% for line in invoice.lines %}
            {% include "line" with { **ctx, line: line } %}
        {% end %}
    </ul>

    <strong>Total: {{ invoice.total |> money -> html }}</strong>
{% end %}
```
