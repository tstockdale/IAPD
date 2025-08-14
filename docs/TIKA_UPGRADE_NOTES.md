# Apache Tika Upgrade to 2.x

This project has been upgraded from Apache Tika 1.14 to Tika 2.x.

Key changes:
- Dependencies switched to `tika-core` and `tika-parsers-standard-package` 2.x.
- Added `log4j-slf4j-impl` to route SLF4J logs (from Tika and parsers) to Log4j 2.
- No code changes were required in `PdfTextExtractor`; existing APIs remain compatible.
- VS Code tasks now use Maven; removed references to local `tika-app-1.14.jar`.

Build and run:
- Build: `mvn -DskipTests package`
- Run: `java -jar target/iapd-1.0.0-SNAPSHOT-all.jar`

Notes:
- The shaded JAR includes Tika's standard parsers and their dependencies.
- Shade plugin warns about overlapping resources; these are safe to ignore for this application.
