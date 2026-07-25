# Vitrine

[![Maven Central](https://img.shields.io/maven-central/v/io.github.simtop/vitrine-processor)](https://central.sonatype.com/artifact/io.github.simtop/vitrine-processor)
[![CI](https://github.com/simtop/vitrine/actions/workflows/ci.yml/badge.svg)](https://github.com/simtop/vitrine/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**A KSP-generated, interactive component catalog for Jetpack Compose.**

Annotate a composable and Vitrine generates a playground for it: a Material 3 control for every
supported parameter, wired to state, rendered above a live instance of the component. Change the
text, tick the checkbox, drag the slider — the component recomposes. No catalog screen to maintain,
no registration step, and it works across a multi-module project.

## Quick start

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.3.9"
}

dependencies {
    implementation("io.github.simtop:vitrine-annotations:0.1.0")
    ksp("io.github.simtop:vitrine-processor:0.1.0")
}
```

Annotate your composables:

```kotlin
@VitrineComponent(tab = "Buttons", name = "Primary button")
@Composable
fun PrimaryButton(label: String, enabled: Boolean) {
    Button(onClick = {}, enabled = enabled) { Text(label) }
}
```

Render the catalog:

```kotlin
@Composable
fun CatalogScreen() {
    val items = remember { loadVitrineItems() }
    LazyColumn {
        items(items) { item ->
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            item.content()
        }
    }
}
```

`loadVitrineItems()` collects every module's generated provider through `ServiceLoader`, so adding a
component to any module makes it appear — no central list to update.

## What gets generated

For the annotation above, Vitrine writes a wrapper next to your component:

```kotlin
@Composable
public fun PrimaryButton_VitrineWrapper() {
  var label by remember { mutableStateOf("Default Value") }
  var enabled by remember { mutableStateOf(false) }

  Column(modifier = Modifier.padding(16.dp)) {
    TextField(value = label, onValueChange = { label = it }, label = { Text("label") })
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("enabled")
      Checkbox(checked = enabled, onCheckedChange = { enabled = it })
    }
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))
    PrimaryButton(label = label, enabled = enabled)
  }
}
```

…plus one `VitrineProvider` per module, registered as a `ServiceLoader` service.

## Supported parameter types

| Kotlin type | Control | Initial value |
|---|---|---|
| `String` | `TextField` | `"Default Value"` |
| `Boolean` | `Checkbox` | `false` |
| `Int` | `TextField` (numeric keyboard) | `0` |
| `Float` | `Slider` | `0f` |

Parameters of any other type — `Modifier`, your own models, lambdas — get no control and are left
out of the generated call, so **they must have default values**. Vitrine will not invent a value for
a type it doesn't understand. It tells you either way: a warning when it falls back to a default, and
a **build error, pointed at your component**, when an unsupported parameter has no default and the
generated wrapper therefore couldn't call it.

> **This four-type limit is the main thing to know before adopting Vitrine.** Enums, nullables,
> `Color`, `Dp`, `Long` and `Double` have no controls yet, and there is no escape hatch for supplying
> your own value for an arbitrary type. Expanding this is the next release's headline.

## Configuration

All options are KSP arguments:

```kotlin
ksp {
    // Package for the generated provider. Default: io.github.simtop.vitrine.generated
    arg("vitrine.generatedPackage", "com.example.catalog.generated")

    // Provider class name. Default: derived from the module name,
    // e.g. :feature:beerslist -> FeatureBeerslistVitrineProvider
    arg("vitrine.providerName", "MyCatalogProvider")

    // "public" (default) or "internal".
    arg("vitrine.visibility", "public")
}
```

| Argument | Type | Default | Effect |
|---|---|---|---|
| `vitrine.generatedPackage` | String | `io.github.simtop.vitrine.generated` | Package of the generated provider |
| `vitrine.providerName` | String | `<ModuleName>VitrineProvider` | Class name of the generated provider |
| `vitrine.visibility` | `public` \| `internal` | `public` | Provider visibility. **`internal` suppresses the `ServiceLoader` entry** — an internal class can't be loaded reflectively from another module, so you must reference it directly. An unrecognised value fails the build rather than falling back silently |

### `demoContainer`

Some components need scaffolding to look right in isolation. Point the annotation at a sibling
function that provides it:

```kotlin
@VitrineComponent(tab = "Text", name = "Framed label", demoContainer = "FramedLabelDemo")
@Composable
fun FramedLabel(caption: String, modifier: Modifier = Modifier) { /* … */ }

@Composable
fun FramedLabelDemo(caption: String) {
    Surface { Box(Modifier.padding(12.dp)) { FramedLabel(caption) } }
}
```

Vitrine calls `FramedLabelDemo` instead. The container must be in the same package and accept the
same supported parameters.

## How it compares to Showkase

[Showkase](https://github.com/airbnb/Showkase) is the established tool in this space, it is actively
maintained, and for many projects it is the better choice. It reuses your existing `@Preview`
functions, browses colors and typography as well as components, and ships a full browser UI with
search and permutation support.

**The difference is interactivity.** Showkase renders each `@Preview` as its author wrote it — a
fixed set of pre-baked states. Vitrine generates *live controls* bound to component parameters, so
one annotation covers the whole input space instead of one point in it.

Pick Showkase if you want a broad browser over previews you already have. Pick Vitrine if you want
to *manipulate* components — checking how a button behaves as a label grows, or what a progress bar
looks like at 0.37 — without writing a preview per case. They are not mutually exclusive; the
annotations are independent.

## Compatibility

Verified on exactly one configuration — the one [CI](.github/workflows/ci.yml) builds. These are
the versions this library has actually been run against, not a claimed support range:

| | Version |
|---|---|
| Kotlin | 2.4.0 |
| KSP | 2.3.9 (KSP2) |
| AGP | 9.2.1 |
| Gradle | 9.6.1 |
| compileSdk | 37 |
| minSdk | 21 (`vitrine-annotations`) |

Older KSP and AGP versions may well work; they have not been tested, so this table does not claim
them. A KSP/AGP version matrix is the next thing to add here, and the table will grow to match it.

## How it works

A KSP `SymbolProcessor` collects `@VitrineComponent` functions, maps each supported parameter type
to a Material 3 control, and emits the wrappers with [KotlinPoet](https://square.github.io/kotlinpoet/).
It then writes one aggregating `VitrineProvider` per module and a `META-INF/services` entry for it.
Generation is latched to a single KSP round, so the service file is written exactly once — writing
it per-round and swallowing the resulting `FileAlreadyExistsException` is a known way to produce
duplicate service entries that break packaging.

## Contributing

Issues and pull requests welcome. `make check` runs everything CI runs.

## License

[Apache 2.0](LICENSE)
