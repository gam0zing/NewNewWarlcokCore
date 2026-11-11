#!/usr/bin/env node
'use strict';

/**
 * 一键安装器：随服务端 zip 一起分发。
 * 运行后把 payload 展开到目标目录，生成 .env，安装依赖、编译并用 pm2 启动。
 * 用法：node install.js [--target <目录>] [--token <令牌>] [--no-start] [--force]
 */

const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const ROOT = __dirname;
const PAYLOAD = path.join(ROOT, 'payload');
const argv = process.argv.slice(2);

function readArg(name) {
  const index = argv.indexOf(name);
  if (index === -1) return '';
  const value = argv[index + 1];
  return value && !value.startsWith('--') ? value : '';
}

function fail(message) {
  console.error('\n[安装失败] ' + message);
  process.exit(1);
}

function readEnvValue(text, key) {
  const matched = new RegExp('^' + key + '=(.*)$', 'm').exec(text);
  return matched ? matched[1].trim() : '';
}

// Windows 下 npm 是 npm.cmd，必须经过 shell 才能启动（否则 spawnSync 报 EINVAL）
const IS_WINDOWS = process.platform === 'win32';

function spawnNpm(commandArgs, cwd) {
  if (IS_WINDOWS) {
    return spawnSync('npm ' + commandArgs.join(' '), { cwd, stdio: 'inherit', shell: true });
  }
  return spawnSync('npm', commandArgs, { cwd, stdio: 'inherit' });
}

function run(title, commandArgs, cwd) {
  console.log('\n== ' + title + ' ==');
  const result = spawnNpm(commandArgs, cwd);
  if (result.error) fail(title + ' 失败：' + result.error.message);
  if (result.status !== 0) fail(title + ' 失败（退出码 ' + result.status + '）');
}

async function main() {
  const major = Number(String(process.versions.node).split('.')[0]);
  if (!Number.isFinite(major) || major < 18) {
    fail('需要 Node.js 18 或更高版本，当前是 ' + process.versions.node + '。请先升级 Node.js：https://nodejs.org/');
  }

  if (!fs.existsSync(path.join(PAYLOAD, 'server', 'package.json'))) {
    fail('找不到 payload/server。请确认压缩包已完整解压，并且 install.js 与 payload 文件夹在同一目录。');
  }

  const target = path.resolve(readArg('--target') || path.join(process.cwd(), 'SkillTreeServer'));
  const force = argv.includes('--force');
  const noStart = argv.includes('--no-start');
  const serverDir = path.join(target, 'server');

  // 已安装过的目录允许直接覆盖升级（tree_store 等运行数据不在 payload 里，不会被删）
  const marker = path.join(serverDir, 'package.json');
  if (fs.existsSync(target) && fs.readdirSync(target).length > 0 && !fs.existsSync(marker) && !force) {
    fail('目标目录已有内容且不是本服务端：' + target + '\n可以换一个目录，或加 --force 强制覆盖。');
  }

  console.log('安装目录：' + target);
  fs.mkdirSync(target, { recursive: true });
  fs.cpSync(PAYLOAD, target, { recursive: true, force: true });

  // 访问令牌：优先命令行，其次沿用已有 .env，最后随机生成
  const envPath = path.join(serverDir, '.env');
  let token = readArg('--token').trim();
  let envExisted = fs.existsSync(envPath);
  if (!token && envExisted) token = readEnvValue(fs.readFileSync(envPath, 'utf8'), 'EDITOR_TOKEN');
  if (!token) token = crypto.randomBytes(24).toString('base64url');
  token = token.replace(/[\r\n]/g, '').trim();

  if (!envExisted || readArg('--token')) {
    let template = '';
    try {
      template = fs.readFileSync(path.join(serverDir, '.env.example'), 'utf8');
    } catch (error) {
      template = 'EDITOR_TOKEN=change-me\nHOST=127.0.0.1\nPORT=8787\n';
    }
    const content = /^EDITOR_TOKEN=/m.test(template)
      ? template.replace(/^EDITOR_TOKEN=.*$/m, 'EDITOR_TOKEN=' + token)
      : 'EDITOR_TOKEN=' + token + '\n' + template;
    fs.writeFileSync(envPath, content, 'utf8');
    envExisted = true;
    console.log('已生成 server/.env，访问令牌：' + token);
  } else {
    console.log('检测到已有 server/.env，沿用其中的配置');
  }

  run('安装 npm 依赖', ['install', '--no-audit', '--no-fund'], serverDir);
  run('编译服务端', ['run', 'build'], serverDir);

  const envText = fs.readFileSync(envPath, 'utf8');
  const host = readEnvValue(envText, 'HOST') || '127.0.0.1';
  const port = readEnvValue(envText, 'PORT') || '8787';

  if (!noStart) {
    await startWithPm2(serverDir);
    const healthy = await waitForHealth('http://' + host + ':' + port + '/api/health', 20000);
    console.log(healthy
      ? '\n健康检查：服务已就绪'
      : '\n健康检查失败：服务暂未响应，可在 server 目录执行 npm run pm2:logs 查看日志');
  }

  console.log('\n========================================');
  console.log('安装完成');
  console.log('  安装目录：' + target);
  console.log('  编辑器：  http://' + host + ':' + port + '/editor.html');
  console.log('  访问令牌：' + token);
  console.log('  公网共享：ngrok http ' + port + '，把 https 地址发给别人');
  console.log('  PM2 命令：npm run pm2:logs / pm2:restart / pm2:stop');
  console.log('========================================\n');
}

/**
 * 用 pm2 的编程接口启动，而不是 pm2 CLI。
 * pm2 CLI 在部分 Windows（缺少 wmic）上会卡在输出进程列表这一步，导致安装脚本一直不退出。
 */
async function startWithPm2(serverDir) {
  console.log('\n== 用 pm2 启动服务 ==');

  let pm2 = null;
  try {
    pm2 = require(path.join(serverDir, 'node_modules', 'pm2'));
  } catch (error) {
    console.warn('[提示] 找不到 pm2 模块，跳过自动启动');
    return false;
  }

  const disconnect = () => {
    try { pm2.disconnect(); } catch (error) { /* 忽略 */ }
  };

  try {
    await withTimeout(new Promise((resolve, reject) => {
      pm2.connect(error => (error ? reject(error) : resolve()));
    }), 30000, 'pm2 连接');

    const configPath = path.join(serverDir, 'ecosystem.config.js');
    const config = require(configPath);
    const app = Object.assign({}, config.apps[0], { cwd: serverDir });
    await withTimeout(new Promise((resolve, reject) => {
      pm2.start(app, error => (error ? reject(error) : resolve()));
    }), 30000, 'pm2 启动');

    disconnect();
    console.log('pm2 已启动：' + app.name);
    return true;
  } catch (error) {
    disconnect();
    console.warn('\n[提示] pm2 自动启动失败：' + (error && error.message ? error.message : error));
    console.warn('可以稍后在 server 目录手动执行：npm run pm2:start');
    return false;
  }
}

function withTimeout(promise, ms, label) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(label + ' 超时')), ms);
    promise.then(
      value => { clearTimeout(timer); resolve(value); },
      error => { clearTimeout(timer); reject(error); }
    );
  });
}

async function waitForHealth(url, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return true;
    } catch (error) { /* 服务还没起来，继续等 */ }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  return false;
}

main().catch(error => fail(error && error.message ? error.message : String(error)));
