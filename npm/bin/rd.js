#!/usr/bin/env node
const { spawnSync } = require('child_process');
const { existsSync } = require('fs');
const path = require('path');
const os = require('os');

const INSTALL_DIR = path.join(os.homedir(), '.roudan-cli');
const jar = path.join(INSTALL_DIR, 'lib', 'roudan-jdbc-cli.jar');

if (!existsSync(jar)) {
  console.error('roudan: jar not found. Download from https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest');
  console.error('       Place it at ' + jar);
  process.exit(1);
}

const args = ['-jar', jar].concat(process.argv.slice(2));
const result = spawnSync('java', args, { stdio: 'inherit' });
process.exit(result.status || 1);
