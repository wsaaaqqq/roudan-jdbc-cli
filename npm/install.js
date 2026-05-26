#!/usr/bin/env node
const { existsSync, mkdirSync, writeFileSync, chmodSync, rmSync } = require('fs');
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

    const timer = setTimeout(() => {
      done(new Error(label + ': download timeout'));
    }, timeoutMs || 120000);

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
      }).on('error', (e) => {
        clearTimeout(timer);
        done(e);
      });
    };
    get(url);
  });
}

(async () => {
  log('Installing roudan-jdbc-cli...');

  const isWin = process.platform === 'win32';
  const jarUrl = `https://github.com/${REPO}/releases/download/v${VERSION}/roudan-jdbc-cli.jar`;

  mkdirSync(path.join(INSTALL_DIR, 'lib'), { recursive: true });

  const jarPath = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');

  try {
    await httpGet(jarUrl, jarPath, 'CLI jar', 180000);

    // Create wrapper
    if (isWin) {
      const wrapper = `@echo off\r\nset DIR=%~dp0\r\nif exist "%DIR%lib\\roudan-jdbc-cli.jar" (\r\n    java -jar "%DIR%lib\\roudan-jdbc-cli.jar" %*\r\n) else (\r\n    echo roudan: not installed. Run: npm install -g roudan-jdbc-cli\r\n    exit /b 1\r\n)\r\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan.cmd'), wrapper);
      writeFileSync(path.join(INSTALL_DIR, 'roudan.bat'), wrapper);
    } else {
      const wrapper = `#!/bin/sh\nDIR="$(dirname "$(readlink -f "$0")")"\nif [ -f "$DIR/lib/roudan-jdbc-cli.jar" ]; then\n  exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"\nelse\n  echo "roudan: not installed. Run: npm install -g roudan-jdbc-cli"\n  exit 1\nfi\n`;
      writeFileSync(path.join(INSTALL_DIR, 'roudan'), wrapper);
      chmodSync(path.join(INSTALL_DIR, 'roudan'), '755');
    }

    log(`Installed to ${INSTALL_DIR}`);
    log('Run: roudan --help');
  } catch (e) {
    // Clean up on failure
    try { rmSync(jarPath, { force: true }); } catch (_) {}
    fail(e.message);
  }
})();
