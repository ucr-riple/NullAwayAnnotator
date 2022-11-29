## CHANGELOG

### Version 1.3.4
- Added inference deactivation mode.
- Updated injector infrastructure.
- Added infrastructure to support backward compatibility with previous versions of NullAway.
- Updated SuppressWarnings injections logic.
- Renamed maven group id and annotator core and scanner modules.
- Added release scripts to cut a release in maven central.
- Updated region computation for field declarations. 
- Bug fixes and refactorings.

### VERSION 1.3.3
- Enabled detection of flow of @Nullable back to upstream from downstream.
- Renamed `MethodInheritanceTree` to `MethodDeclarationTree`.
- Added configuration modes for downstream dependency analysis (`strict`|`lower bound` | `upper bound` | `default`).
- Updated fix tree construction with triggered fixes from downstream dependencies.
- Added report cache.
- Added extra cycle for better reduction of errors.
- Support `@NullUnmarked` and `@SuppressWarnings` injection to resolve all remaining errors.
- Bug fix in retrieving methods inserted in `MethodDeclarationTree` that are not declared in target module.
- Bug fixes and refactorings.

### VERSION 1.3.2

- Added downstream dependency analysis.
- Added Error Prone checkers to all modules.
- Enhanced analysis of downstream dependencies to execute in parallel.
- Renamed `Scanner` module to `Type Annotator Scanner`.
- Added `Library Models Loader` module to interact with `NullAway` when analyzing downstream dependencies.
- Updated `Core` module installation by forcing Maven Publish to use shadowJar output.
- Banned mutable declaration of static fields in Annotator code base.
- Added `UUID` to every generated config file by annotator to ensure re-run of the analysis.
- Updated testing infrastructure.
- Fixed bug in `Injector` when the target element is in generated code.
- Enhanced error report/handling in `Injector` when target class is not located.
- Removed parameter names and uri for method serializations.
- Added guards for wrong configurations.
- Fixed bug when `Scanner` modules crashes when not activated.
- Added javadoc to most parts of `Core` module.
- Updated installation guide and scripts.
- Added `CI` jobs.

---
### VERSION 1.1.0

Our Base Version
