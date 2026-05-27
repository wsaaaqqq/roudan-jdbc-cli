# Changelog

## v0.5.5 -- 2026-05-27

### Changed
- **JAR bundled in npm package** -- `npm install -g` includes the CLI jar (no separate download)
- `rd.js` simplified: uses bundled jar, no download logic
- Removed `.roudan-cli/` dependency; jar lives in npm global dir

## v0.5.4 -- 2026-05-27

### Removed
- **install.js** (postinstall download) -- npm install only installs wrapper
- **Bundled JRE** -- no longer auto-downloads JRE; user must install Java 8+
- **prepare-jre.bat** / **public/** -- JRE asset preparation scripts

### Changed
- `npm/rd.js`: checks for jar, guides user to download if missing
- `install.sh`: simplified, downloads jar only
- `package.json`: removed `postinstall` script
- Startup scripts: removed `jre8` path, use system `java` only

## v0.5.2 -- 2026-05-26

### Features
- **`ls` command**: list connections with name/url/user/current, flat array output
- **`rename` command**: rename a saved connection
- **`logout --name`**: remove a specific connection by name
- **`rdc` alias**: shorter command alias, shown in `--help`

### Fixes
- `connections` output uses `current` boolean flag instead of `*` suffix
- Added `slf4j-nop` to suppress SLF4J startup warnings

## v0.5.1 -- 2026-05-19

### Changed
- **Simplified install**: No JRE download -- uses system Java 8+
- install.js: added 180s timeout, progress indicator without Content-Length
- Only downloads own jar from GitHub Releases, zero external URLs

## v0.5.0 -- 2026-05-19

### Breaking
- **Renamed command**: `rd` -> `roudan` (no aliases, avoids Windows/powershell conflicts)

### Features
- **Local driver directory**: `~/.roudan/drivers/<name>/` auto-detected on connect
- **Login auto-copies driver** to local dir for subsequent use

### Fixes
- `rd.js` wrapper: `spawnSync` replaces `execSync` (fixes quoted arg passing on Windows)

## v0.4.0 -- 2026-05-17

### Features
- **New commands**: `exec`, `import`, `export`, `gen`, `tail`
- `exec --mode transaction|auto` for multi-statement execution
- `import` CSV/JSON batch insert; `export` query results to CSV/JSON
- `gen --ddl` reverse-generates CREATE TABLE; `gen --insert` generates INSERT
- `tail` table change polling with configurable interval

### Fixes
- Derby multi-JAR auto-download (derbyclient + derbyshared)
- Fuzzy matching threshold tightened (<=2 + prefix priority)
- Update command GitHub release URL corrected

## v0.1.0 -- 2026-05-15

### Features
- **6 core commands**: `query`, `count`, `modify`, `tables`, `describe`, `test`
- **3 transaction commands**: `begin`, `commit`, `rollback`
- **Positional params** (`?`) and **named params** (`:paramName`) SQL styles
- HikariCP connection pool, SQL file execution, output formats, error handling

### Distribution
- GitHub Releases (fat-jar + startup scripts)
- npm: `npm install -g roudan-jdbc-cli`
- Docker: `docker run wsaaaqqq/roudan-jdbc-cli`
- One-line install: `curl -fsSL ... | bash`
- OpenCode skill: `skill/SKILL.md`
