/**
 * PM2 启动配置：pm2 start ecosystem.config.js
 * 先执行 npm run build，再启动。令牌等敏感配置放在同目录 .env 中，不要写进本文件。
 */
module.exports = {
  apps: [
    {
      name: 'skill-tree-editor',
      script: 'dist/main.js',
      cwd: __dirname,
      instances: 1,
      exec_mode: 'fork', // 版本目录是文件存储，必须单进程，避免并发写入竞争
      autorestart: true,
      max_restarts: 20,
      max_memory_restart: '300M',
      watch: false,
      time: true,
      env: {
        NODE_ENV: 'production'
      }
    }
  ]
};
