#!/usr/bin/env node
const { execSync } = require('child_process');
const { existsSync, mkdirSync } = require('fs');
const path = require('path');
const https = require('https');
const os = require('os');

const VERSION = '0.1.0';
const REPO = 'wsaaaqqq/roudan-jdbc-cli';
const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');
const JAR_URL = `https://github.com/${REPO}/releases/download/v${VERSION}/roudan-jdbc-cli.jar`;

function log(msg) { console.log(`[roudan-cli] ${msg}`); }
function warn(msg) { console.warn(`[warn] ${msg}`); }
function fail(msg) { console.error(`[error] ${msg}`); process.exit(1); }

(async () => {
  // Check Java
  try {
    execSync('java -version', { stdio: 'pipe' });
  } catch (e) {
    fail('Java 8+ is required. Install Java and retry.');
  }

  // Create directories
  mkdirSync(path.join(INSTALL_DIR, 'lib'), { recursive: true });

  // Download jar
  log(`Downloading roudan-jdbc-cli v${VERSION}...`);
  const jarPath = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');

  if (existsSync(jarPath)) {
    log(`Already installed at ${jarPath}`);
    return;
  }

  await new Promise((resolve, reject) => {
    const file = require('fs').createWriteStream(jarPath);
    https.get(JAR_URL, (res) => {
      if (res.statusCode === 302) {
        https.get(res.headers.location, (r2) => {
          r2.pipe(file);
          file.on('finish', () => { file.close(); resolve(); });
        });
      } else if (res.statusCode === 200) {
        res.pipe(file);
        file.on('finish', () => { file.close(); resolve(); });
      } else {
        reject(new Error(`HTTP ${res.statusCode}`));
      }
    }).on('error', reject);
  });

  // Create wrapper
  const isWin = process.platform === 'win32';
  const wrapperName = isWin ? 'roudan-jdbc-cli.cmd' : 'roudan-jdbc-cli';
  const wrapperPath = path.join(INSTALL_DIR, wrapperName);

  if (isWin) {
    require('fs').writeFileSync(wrapperPath,
      `@echo off\r\njava -jar "${jarPath}" %*`);
  } else {
    require('fs').writeFileSync(wrapperPath,
      `#!/bin/sh\nexec java -jar "${jarPath}" "$@"`);
    require('fs').chmodSync(wrapperPath, '755');
  }

  log(`Installed to ${INSTALL_DIR}`);
  log(`Run: ${INSTALL_DIR}/${wrapperName} --help`);
  log(`Add to PATH: export PATH="${INSTALL_DIR}:$PATH"`);
})();
