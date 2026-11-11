import * as path from 'node:path';
import * as dotenv from 'dotenv';

const serverDir = path.resolve(__dirname, '..');

// 优先读取 tools/server/.env（PM2 的 cwd 也是这里）
dotenv.config({ path: path.join(serverDir, '.env') });

const toolsDir = path.resolve(process.env.TOOLS_DIR || path.join(serverDir, '..'));

export const config = {
  serverDir,
  toolsDir,
  storeDir: path.resolve(process.env.TREE_STORE_DIR || path.join(toolsDir, 'tree_store')),
  auditLog: path.resolve(process.env.AUDIT_LOG || path.join(serverDir, 'audit.log')),
  host: (process.env.HOST || '127.0.0.1').trim(),
  port: Number(process.env.PORT || 8787),
  token: (process.env.EDITOR_TOKEN || '').trim(),
  maxUploadBytes: Number(process.env.MAX_UPLOAD_BYTES || 5 * 1024 * 1024),
  rateLimitWindowMs: 60 * 1000,
  rateLimitPosts: 30
};

/** 启动前检查关键配置，缺失时给出明确提示而不是让服务带病运行 */
export function assertConfig(): void {
  if (!config.token) {
    console.error('[配置错误] 未设置 EDITOR_TOKEN。请在 tools/server/.env 中填写一个足够长的随机字符串（可复制 .env.example）。');
    process.exit(1);
  }
  if (!Number.isInteger(config.port) || config.port <= 0 || config.port > 65535) {
    console.error('[配置错误] PORT 不是合法端口：' + process.env.PORT);
    process.exit(1);
  }
  if (!Number.isFinite(config.maxUploadBytes) || config.maxUploadBytes <= 0) {
    console.error('[配置错误] MAX_UPLOAD_BYTES 必须是正整数');
    process.exit(1);
  }
}
