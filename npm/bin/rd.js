#!/usr/bin/env node
const { spawnSync } = require('child_process');
const { existsSync } = require('fs');
const path = require('path');

const jar = path.join(__dirname, '..', 'lib', 'roudan-jdbc-cli.jar');

if (!existsSync(jar)) {
  console.error('roudan: internal error - jar not found. Reinstall: npm install -g roudan-jdbc-cli');
  process.exit(1);
}

const args = ['-jar', jar].concat(process.argv.slice(2));
const result = spawnSync('java', args, { stdio: 'inherit' });
process.exit(result.status || 1);
