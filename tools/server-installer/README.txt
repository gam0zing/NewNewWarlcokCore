NewNew 技能树远程仓库 · 一键安装包
====================================

需要：Node.js 18 或更高版本（https://nodejs.org/）。
依赖、NestJS、pm2 都会通过 npm 自动下载，不需要提前安装。

【Windows】
  双击 install.bat

【macOS / Linux】
  node install.js

默认安装到当前目录下的 SkillTreeServer 文件夹，并自动完成：
  1. 复制服务端与编辑器页面
  2. 生成随机访问令牌，写入 server/.env
  3. npm install 下载依赖
  4. npm run build 编译
  5. pm2 启动服务

安装完成后：
  编辑器      http://127.0.0.1:8787/editor.html
  访问令牌    安装结束时打印，也保存在 SkillTreeServer/server/.env
  公网共享    ngrok http 8787

常用参数：
  node install.js --target D:\my-server     指定安装目录
  node install.js --token 你的令牌           指定访问令牌
  node install.js --no-start                只安装，不启动
  node install.js --force                   目标目录已有其它内容时强制覆盖

再次运行 install.js 会覆盖程序文件并保留 server/.env 与 tree_store，可用于升级。

安全提醒：
  - server/.env 里的 EDITOR_TOKEN 相当于管理员密码，不要发给别人。
  - 服务默认只监听 127.0.0.1，公网访问请用 ngrok / frp 等内网穿透。
