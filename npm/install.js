#!/usr/bin/env node
const { execSync, spawnSync } = require('child_process');
const { existsSync, mkdirSync, writeFileSync, chmodSync, readdirSync, statSync, rmSync } = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');

const VERSION = '0.5.0';
const REPO = 'wsaaaqqq/roudan-jdbc-cli';
const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');

const ADOPTIUM_API = 'https://api.adoptium.net/v3/binary/latest/8/ga';

function log(msg) { console.log(`[roudan-jdbc-cli] ${msg}`); }
function fail(msg) { console.error(`[error] ${msg}`); process.exit(1); }

function httpGet(url, dest, label) {
  return new Promise((resolve, reject) => {
    const file = require('fs').createWriteStream(dest);
    let total = 0, downloaded = 0, lastLog = 0;
    const get = (u) => {
      https.get(u, (res) => {
        if (res.statusCode === 302 || res.statusCode === 307) {
          get(res.headers.location);
        } else if (res.statusCode === 200) {
          total = parseInt(res.headers['content-length']) || 0;
          res.on('data', (chunk) => {
            downloaded += chunk.length;
            const now = Date.now();
            if (total > 0 && now - lastLog > 500) {
              const pct = Math.round(downloaded * 100 / total);
              process.stderr.write(`\r[roudan] ${label}... ${pct}%`);
              lastLog = now;
            }
          });
          res.pipe(file);
          file.on('finish', () => {
            if (total > 0) process.stderr.write('\r[roudan] ' + label + ': done    \n');
            file.close();
            resolve();
          });
        } else {
          reject(new Error(`HTTP ${res.statusCode}`));
        }
      }).on('error', reject);
    };
    get(url);
  });
}

function findJavaDir(dir) {
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (statSync(full).isDirectory()) {
      const bin = path.join(full, 'bin');
      if (existsSync(path.join(bin, 'java')) || existsSync(path.join(bin, 'java.exe'))) {
        return full;
      }
      const sub = findJavaDir(full);
      if (sub) return sub;
    }
  }
  return null;
}

(async () => {
  log('Installing roudan-jdbc-cli...');

  const plat = process.platform;
  const platMap = { win32: 'windows', linux: 'linux', darwin: 'mac' };
  const adoptOs = platMap[plat];
  if (!adoptOs) fail(`Unsupported platform: ${plat}`);

  const isWin = plat === 'win32';
  const jreUrl = `${ADOPTIUM_API}/${adoptOs}/x64/jre/hotspot/normal/eclipse`;
  const jarUrl = `https://github.com/${REPO}/releases/download/v${VERSION}/roudan-jdbc-cli.jar`;

  mkdirSync(path.join(INSTALL_DIR, 'lib'), { recursive: true });
  mkdirSync(path.join(INSTALL_DIR, 'jre8'), { recursive: true });

  const tmpDir = path.join(os.tmpdir(), 'roudan-install-' + Date.now());
  mkdirSync(tmpDir, { recursive: true });

  try {
    // Download JRE and jar in parallel
    const jreArchive = path.join(tmpDir, isWin ? 'jre.zip' : 'jre.tar.gz');
    const jarPath = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');
    await Promise.all([
      httpGet(jreUrl, jreArchive, 'JRE 8'),
      httpGet(jarUrl, jarPath, 'CLI jar')
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

    // Copy JRE to INSTALL_DIR/jre8/
    if (isWin) {
      execSync(`xcopy /E /I /Y "${javaHome}\\*" "${INSTALL_DIR}\\jre8\\"`, { stdio: 'pipe' });
    } else {
      execSync(`cp -R "${javaHome}/"* "${INSTALL_DIR}/jre8/" 2>/dev/null`, { stdio: 'pipe' });
    }

    // Create wrapper
    if (isWin) {
      const wrapper = `@echo off\r\nset DIR=%~dp0\r\nset JAVA=%DIR%jre8\\bin\\java.exe\r\nif exist "%JAVA%" (\r\n    "%JAVA%" -jar "%DIR%lib\\roudan-jdbc-cli.jar" %*\r\n) else (\r\n    java -jar "%DIR%lib\\roudan-jdbc-cli.jar" %*\r\n)\r\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan.cmd'), wrapper);
      writeFileSync(path.join(INSTALL_DIR, 'roudan.bat'), wrapper);
    } else {
      const wrapper = `#!/bin/sh\nDIR="$(dirname "$(readlink -f "$0")")"\nif [ -x "$DIR/jre8/bin/java" ]; then\n  exec "$DIR/jre8/bin/java" -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"\nelse\n  exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"\nfi\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan'), wrapper);
      chmodSync(path.join(INSTALL_DIR, 'roudan'), '755');
    }

    log(`Installed to ${INSTALL_DIR}`);
    log('Installation successful!');
  } finally {
    try { rmSync(tmpDir, { recursive: true, force: true }); } catch (_) {}
  }
})();
