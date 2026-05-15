#!/usr/bin/env node
const { execSync } = require('child_process');
const { existsSync } = require('fs');
const path = require('path');
const os = require('os');

const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');
const jar = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');

const isWin = process.platform === 'win32';
const bundledJava = path.join(INSTALL_DIR, 'jre8', 'bin', isWin ? 'java.exe' : 'java');
const cmd = existsSync(bundledJava)
  ? `"${bundledJava}" -jar "${jar}" ${process.argv.slice(2).join(' ')}`
  : `java -jar "${jar}" ${process.argv.slice(2).join(' ')}`;

try {
  execSync(cmd, { stdio: 'inherit', shell: true });
} catch (e) {
  if (!existsSync(jar)) {
    console.error('rd: not installed. Run: npm install -g roudan-jdbc-cli');
  }
  process.exit(1);
}
