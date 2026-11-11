#!/usr/bin/env node
'use strict';

/**
 * 打包纯净的服务端分发包。
 * 用法：node tools/build-server-package.js [--out <zip 路径>]
 * 产物默认在 tools/release/NewNewSkillTreeServer-v<版本>.zip。
 *
 * 分发包结构：
 *   install.js / install.bat / README.txt
 *   payload/
 *     skill_tree_editor.html
 *     builtin_enums.js
 *     server/...
 */

const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');

const TOOLS_DIR = __dirname;
const SERVER_DIR = path.join(TOOLS_DIR, 'server');
const INSTALLER_DIR = path.join(TOOLS_DIR, 'server-installer');
const RELEASE_DIR = path.join(TOOLS_DIR, 'release');

const argv = process.argv.slice(2);
const outIndex = argv.indexOf('--out');
const outArg = outIndex !== -1 ? argv[outIndex + 1] : '';

// 服务端根部需要随包分发的固定文件
const SERVER_ROOT_FILES = [
  'package.json',
  'package-lock.json',
  'tsconfig.json',
  'tsconfig.build.json',
  'nest-cli.json',
  'ecosystem.config.js',
  '.env.example',
  '.gitignore'
];

const INSTALLER_FILES = ['install.js', 'install.bat', 'README.txt'];

const EDITOR_FILES = ['skill_tree_editor.html', 'builtin_enums.js'];
const EDITOR_OPTIONAL = ['command_definitions.json', '技能树编辑器使用说明.md'];

function fail(message) {
  console.error('\n[打包失败] ' + message);
  process.exit(1);
}

function readFile(filePath, label) {
  if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    fail('缺少文件：' + (label || filePath));
  }
  return fs.readFileSync(filePath);
}

function walk(dir, prefix, output) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name.startsWith('.')) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, prefix + '/' + entry.name, output);
    } else if (entry.isFile()) {
      output.push({ name: prefix + '/' + entry.name, data: fs.readFileSync(full) });
    }
  }
}

function collect() {
  const files = [];

  for (const name of INSTALLER_FILES) {
    files.push({ name, data: readFile(path.join(INSTALLER_DIR, name), 'server-installer/' + name) });
  }

  for (const name of EDITOR_FILES) {
    files.push({ name: 'payload/' + name, data: readFile(path.join(TOOLS_DIR, name)) });
  }
  for (const name of EDITOR_OPTIONAL) {
    const full = path.join(TOOLS_DIR, name);
    if (fs.existsSync(full)) files.push({ name: 'payload/' + name, data: fs.readFileSync(full) });
  }

  for (const name of SERVER_ROOT_FILES) {
    files.push({ name: 'payload/server/' + name, data: readFile(path.join(SERVER_DIR, name), 'server/' + name) });
  }

  const srcDir = path.join(SERVER_DIR, 'src');
  if (!fs.existsSync(srcDir)) fail('缺少 server/src');
  walk(srcDir, 'payload/server/src', files);

  files.sort((left, right) => left.name.localeCompare(right.name));
  return files;
}

// ------------------------------------------------------------------ ZIP（deflate，零第三方依赖）

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let index = 0; index < 256; index += 1) {
    let value = index;
    for (let bit = 0; bit < 8; bit += 1) {
      value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
    }
    table[index] = value >>> 0;
  }
  return table;
})();

function crc32(buffer) {
  let value = 0xffffffff;
  for (const byte of buffer) {
    value = CRC_TABLE[(value ^ byte) & 0xff] ^ (value >>> 8);
  }
  return (value ^ 0xffffffff) >>> 0;
}

function dosDateTime(date) {
  const time = ((date.getHours() & 31) << 11) | ((date.getMinutes() & 63) << 5) | (Math.floor(date.getSeconds() / 2) & 31);
  const day = (((date.getFullYear() - 1980) & 127) << 9) | (((date.getMonth() + 1) & 15) << 5) | (date.getDate() & 31);
  return { time, date: day };
}

function buildZip(files) {
  const { time, date } = dosDateTime(new Date());
  const localChunks = [];
  const centralChunks = [];
  let offset = 0;

  for (const file of files) {
    const nameBuffer = Buffer.from(file.name, 'utf8');
    const compressed = zlib.deflateRawSync(file.data, { level: 9 });
    const crc = crc32(file.data);

    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x0800, 6); // UTF-8 文件名
    local.writeUInt16LE(8, 8);      // deflate
    local.writeUInt16LE(time, 10);
    local.writeUInt16LE(date, 12);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(compressed.length, 18);
    local.writeUInt32LE(file.data.length, 22);
    local.writeUInt16LE(nameBuffer.length, 26);
    local.writeUInt16LE(0, 28);
    localChunks.push(local, nameBuffer, compressed);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0x0800, 8);
    central.writeUInt16LE(8, 10);
    central.writeUInt16LE(time, 12);
    central.writeUInt16LE(date, 14);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(compressed.length, 20);
    central.writeUInt32LE(file.data.length, 24);
    central.writeUInt16LE(nameBuffer.length, 28);
    central.writeUInt16LE(0, 30);
    central.writeUInt16LE(0, 32);
    central.writeUInt16LE(0, 34);
    central.writeUInt16LE(0, 36);
    central.writeUInt32LE(0, 38);
    central.writeUInt32LE(offset, 42);
    centralChunks.push(central, nameBuffer);

    offset += local.length + nameBuffer.length + compressed.length;
  }

  const centralBuffer = Buffer.concat(centralChunks);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(files.length, 8);
  end.writeUInt16LE(files.length, 10);
  end.writeUInt32LE(centralBuffer.length, 12);
  end.writeUInt32LE(offset, 16);
  end.writeUInt16LE(0, 20);

  return Buffer.concat([...localChunks, centralBuffer, end]);
}

// ------------------------------------------------------------------ 主流程

function main() {
  const packageJson = JSON.parse(readFile(path.join(SERVER_DIR, 'package.json'), 'server/package.json').toString('utf8'));
  const version = packageJson.version || '0.0.0';
  const files = collect();
  const zip = buildZip(files);

  const outPath = outArg ? path.resolve(outArg) : path.join(RELEASE_DIR, 'NewNewSkillTreeServer-v' + version + '.zip');
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, zip);

  console.log('已打包 ' + files.length + ' 个文件，版本 v' + version);
  console.log('产物：' + outPath);
  console.log('大小：' + (zip.length / 1024).toFixed(1) + ' KB');
  console.log('\n分发包内容：install.js / install.bat / README.txt + payload/（编辑器与 server）');
  console.log('对方解压后双击 install.bat 或运行 node install.js 即可。');
}

try {
  main();
} catch (error) {
  fail(error && error.message ? error.message : String(error));
}
