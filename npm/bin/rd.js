#!/usr/bin/env node
const { spawnSync } = require('child_process');
const { existsSync } = require('fs');
const path = require('path');
const os = require('os');

const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');
const jar = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');
const args = ['-jar', jar].concat(process.argv.slice(2));

const result = spawnSync('java', args, { stdio: 'inherit' });

if (result.status !== 0) {
  if (!existsSync(jar)) {
    console.error('roudan: not installed. Run: npm install -g roudan-jdbc-cli');
  }
  process.exit(result.status || 1);
}
