## CHANGELOG

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