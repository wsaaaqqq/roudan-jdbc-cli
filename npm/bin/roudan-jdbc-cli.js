#!/usr/bin/env node
const { execSync } = require('child_process');
const path = require('path');
const os = require('os');

const jar = path.join(os.homedir(), '.roudan-cli', 'lib', 'roudan-jdbc-cli.jar');
try {
  execSync(`java -jar "${jar}" ${process.argv.slice(2).join(' ')}`, { stdio: 'inherit' });
} catch (e) {
  if (!require('fs').existsSync(jar)) {
    console.error('[roudan-cli] Not installed. Run: npx @roudan/jdbc-cli install');
  }
  process.exit(1);
}
