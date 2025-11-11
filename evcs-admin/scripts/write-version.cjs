#!/usr/bin/env node
const { execSync } = require('node:child_process')
const { writeFileSync, mkdirSync } = require('node:fs')
const { resolve } = require('node:path')

function safe(cmd) {
  try { return execSync(cmd, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim() } catch { return '' }
}

const commit = safe('git rev-parse --short HEAD') || 'unknown'
const branch = safe('git rev-parse --abbrev-ref HEAD') || 'unknown'
const date = new Date().toISOString()
let buildNumber = process.env.BUILD_NUMBER
if (!buildNumber || !/^[0-9]+$/.test(buildNumber)) {
  buildNumber = safe('git rev-list --count HEAD') || '0'
}

const content = JSON.stringify({ commit, branch, buildTime: date, buildNumber }, null, 2)

const outDir = resolve(__dirname, '..', 'dist')
try { mkdirSync(outDir, { recursive: true }) } catch {}
writeFileSync(resolve(outDir, 'version.json'), content)
console.log('[build] Wrote version.json:', content)

