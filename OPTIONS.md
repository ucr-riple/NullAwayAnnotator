Here are all __optional__ arguments that can alter Annotator's default configuration:

| Flag                                                   | Description |
|--------------------------------------------------------|-------------|
| `-ch,--chain`                                          | Injects the complete tree of fixes associated with the fix. |
| `-db,--disable-bailout`                                | Annotator will not bail out from the search tree as soon as its effectiveness hits zero or less and will completely traverse the tree until no new fix is suggested. |
| `-depth,--depth <arg>`                                 | Sets the depth of the analysis search. |
| `-n,--nullable <arg>`                                  | Sets custom `@Nullable` annotation. |
| `-dc,--disable-cache`                                  | Disables cache usage. |
| `-dpp,--disable-parallel-processing`                   | Disables parallel processing of fixes within an iteration. |
| `-rboserr, --redirect-build-output-stderr`             | Redirects build outputs to `STD Err`. |
| `-exs, --exhaustive-search`                            | Annotator will perform an exhaustive search, injecting `@Nullable` on all elements involved in an error regardless of their overall effectiveness. (This feature is used mostly in experiments and may not have a practical use.) |
| `-dol, --disable-outer-loop`                           | Disables outer loop (This feature is used mostly in experiments and may not have a practical use.) |
| `-adda, --activate-downstream-dependencies-analysis`   | Activates downstream dependency analysis. |
| `-ddbc, --downstream-dependencies-build-command <arg>` | Command to build all downstream dependencies at once; this command must include changing the directory from root to the target project. |
| `-nlmlp, --nullaway-library-model-loader-path <arg>`   | NullAway Library Model loader path. |
| `-sre, --suppress-remaining-errors <arg>`              | Forces remaining unresolved errors to be silenced using suppression annotations. Fully qualified annotation name for `@NullUnmarked` must be passed. |
| `-am, --analysis-mode <arg>`                           | Analysis mode. Can be [default|upper_bound|lower_bound|strict] |
| `-di, --deactivate-infere`                             | Disables inference of `@Nullable` annotation. |
| `-drdl, --deactivate-region-detection-lombok`          | Deactivates region detection for Lombok. |
| `-nna, --nonnull-annotations <arg>`                    | Adds a list of non-null annotations separated by a comma to be acknowledged by Annotator (e.g., com.example1.Nonnull,com.example2.Nonnull) |
| `eic, enable-impact-cache`                             | Enables fixes impacts caching for next cycles. |
