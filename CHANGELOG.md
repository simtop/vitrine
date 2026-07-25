# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] — unreleased

Initial release. Extracted and generalised from a multi-module production-shaped Android app.

### Added

- `@VitrineComponent` annotation with `tab`, `name`, and `demoContainer`.
- KSP processor generating an interactive Material 3 wrapper per annotated composable: `TextField`
  for `String`, `Checkbox` for `Boolean`, numeric `TextField` for `Int`, `Slider` for `Float`.
- One aggregating `VitrineProvider` per module, registered via `ServiceLoader`, plus
  `loadVitrineItems()` to collect every module's items with a stable (tab, name) ordering.
- Configuration through KSP arguments: `vitrine.generatedPackage`, `vitrine.providerName`,
  `vitrine.visibility`.

### Notes on what changed during extraction

- The generated package, provider class name, and visibility were hardcoded; all three are now
  KSP arguments. The annotation FQNs were hardcoded to the originating project and are now owned by
  this library, so they are fixed by definition rather than configurable.
- Provider generation is latched to a single KSP round. The original wrote the `ServiceLoader` entry
  on every round, which can produce duplicate service entries.
- The provider's `Dependencies` are now correctly aggregating over every annotated source file, so
  editing any component invalidates the provider.
- `vitrine.visibility=internal` no longer emits a `ServiceLoader` entry, which would have failed at
  runtime; the processor warns and tells you to reference the class directly.
- An unrecognised `vitrine.visibility` now fails the build instead of silently defaulting.

[Unreleased]: https://github.com/simtop/vitrine/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/simtop/vitrine/releases/tag/v0.1.0
