#!/usr/bin/env node
const { execSync, spawnSync } = require('child_process');
const { existsSync, mkdirSync, writeFileSync, chmodSync, readdirSync, statSync, rmSync } = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');

const VERSION = '0.5.2';
const REPO = 'wsaaaqqq/roudan-jdbc-cli';
const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');

function log(msg) { console.log(`[roudan-jdbc-cli] ${msg}`); }
function fail(msg) { console.error(`[error] ${msg}`); process.exit(1); }

function httpGet(url, dest, label, timeoutMs) {
  return new Promise((resolve, reject) => {
    const file = require('fs').createWriteStream(dest);
    let total = 0, downloaded = 0, lastLog = 0, ticks = 0;
    let resolved = false;
    const done = (err) => { if (!resolved) { resolved = true; file.close(); err ? reject(err) : resolve(); } };

    const timer = setTimeout(() => done(new Error(label + ': download timeout')), timeoutMs || 120000);

    const progress = () => {
      const now = Date.now();
      if (now - lastLog > 500) {
        if (total > 0) {
          process.stderr.write(`\r[roudan] ${label}... ${Math.round(downloaded * 100 / total)}%`);
        } else {
          process.stderr.write(`\r[roudan] ${label}... ${'.'.repeat((++ticks % 4) + 1)}  `);
        }
        lastLog = now;
      }
    };

    const get = (u) => {
      https.get(u, (res) => {
        if (res.statusCode === 302 || res.statusCode === 307) {
          get(res.headers.location);
        } else if (res.statusCode === 200) {
          total = parseInt(res.headers['content-length']) || 0;
          res.on('data', (chunk) => {
            downloaded += chunk.length;
            progress();
          });
          res.pipe(file);
          file.on('finish', () => {
            clearTimeout(timer);
            if (total > 0) process.stderr.write('\r[roudan] ' + label + ': done    \n');
            else process.stderr.write('\r[roudan] ' + label + ': done    \n');
            done(null);
          });
        } else {
          done(new Error(`${label}: HTTP ${res.statusCode}`));
        }
      }).on('error', (e) => { clearTimeout(timer); done(e); });
    };
    get(url);
  });
}

function hasJava() {
  const r = spawnSync('java', ['-version'], { stdio: 'ignore' });
  return r.status === 0;
}

function findJavaDir(dir) {
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (statSync(full).isDirectory()) {
      const bin = path.join(full, 'bin');
      if (existsSync(path.join(bin, 'java')) || existsSync(path.join(bin, 'java.exe'))) return full;
      const sub = findJavaDir(full);
      if (sub) return sub;
    }
  }
  return null;
}

(async () => {
  log('Installing roudan-jdbc-cli...');

  const isWin = process.platform === 'win32';
  const platMap = { win32: 'windows', linux: 'linux', darwin: 'mac' };
  const osName = platMap[process.platform];
  if (!osName) fail(`Unsupported platform: ${process.platform}`);

  mkdirSync(path.join(INSTALL_DIR, 'lib'), { recursive: true });

  const jarPath = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');
  const jarUrl = `https://github.com/${REPO}/releases/download/v${VERSION}/roudan-jdbc-cli.jar`;

  let bundledJava = false;
  const needJre = !hasJava();

  try {
    if (needJre) {
      log('Java not found, downloading bundled JRE 8...');
      const jreExt = isWin ? 'zip' : 'tar.gz';
      const jreName = `roudan-jre8-${osName}-x64.${jreExt}`;
      const jreUrl = `https://github.com/${REPO}/releases/download/v${VERSION}/${jreName}`;
      const tmpDir = path.join(os.tmpdir(), 'roudan-install-' + Date.now());
      mkdirSync(tmpDir, { recursive: true });
      const jreArchive = path.join(tmpDir, jreName);

      try {
        await Promise.all([
          httpGet(jarUrl, jarPath, 'CLI jar', 180000),
          httpGet(jreUrl, jreArchive, 'JRE 8', 300000)
        ]);

        log('Extracting JRE...');
        const extractDir = path.join(tmpDir, 'extract');
        mkdirSync(extractDir, { recursive: true });

        if (isWin) {
          execSync(`powershell -Command "Expand-Archive -Path '${jreArchive}' -DestinationPath '${extractDir}' -Force"`, { stdio: 'pipe' });
        } else {
          execSync(`tar xzf "${jreArchive}" -C "${extractDir}" 2>/dev/null`, { stdio: 'pipe' });
        }

        const javaHome = findJavaDir(extractDir);
        if (!javaHome) fail('JRE extraction failed: java binary not found.');

        mkdirSync(path.join(INSTALL_DIR, 'jre8'), { recursive: true });
        if (isWin) {
          execSync(`xcopy /E /I /Y "${javaHome}\\*" "${INSTALL_DIR}\\jre8\\"`, { stdio: 'pipe' });
        } else {
          execSync(`cp -R "${javaHome}/"* "${INSTALL_DIR}/jre8/" 2>/dev/null`, { stdio: 'pipe' });
        }

        bundledJava = true;
        log('JRE installed.');
      } finally {
        try { rmSync(tmpDir, { recursive: true, force: true }); } catch (_) {}
      }
    } else {
      await httpGet(jarUrl, jarPath, 'CLI jar', 180000);
    }

    // Create wrapper (prefers bundled JRE, falls back to system java)
    const javaCmd = bundledJava ? (
      isWin ? '%DIR%jre8\\bin\\java.exe' : '"$DIR/jre8/bin/java"'
    ) : 'java';

    if (isWin) {
      const wrapper = `@echo off\r\nset DIR=%~dp0\r\nif exist "%DIR%jre8\\bin\\java.exe" (\r\n    "%DIR%jre8\\bin\\java.exe" -jar "%DIR%lib\\roudan-jdbc-cli.jar" %*\r\n) else if exist "%DIR%lib\\roudan-jdbc-cli.jar" (\r\n    java -jar "%DIR%lib\\roudan-jdbc-cli.jar" %*\r\n) else (\r\n    echo roudan: not installed. Run: npm install -g roudan-jdbc-cli\r\n    exit /b 1\r\n)\r\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan.cmd'), wrapper);
      writeFileSync(path.join(INSTALL_DIR, 'roudan.bat'), wrapper);
    } else {
      const wrapper = `#!/bin/sh\nDIR="$(dirname "$(readlink -f "$0")")"\nif [ -x "$DIR/jre8/bin/java" ]; then\n  exec "$DIR/jre8/bin/java" -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"\nelif [ -f "$DIR/lib/roudan-jdbc-cli.jar" ]; then\n  exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"\nelse\n  echo "roudan: not installed. Run: npm install -g roudan-jdbc-cli"\n  exit 1\nfi\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan'), wrapper);
      chmodSync(path.join(INSTALL_DIR, 'roudan'), '755');
    }

    log(`Installed to ${INSTALL_DIR}`);
    log('Run: roudan --help');
  } catch (e) {
    try { rmSync(jarPath, { force: true }); } catch (_) {}
    fail(e.message);
  }
})();
