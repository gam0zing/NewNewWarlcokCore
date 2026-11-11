import { NextFunction, Request, Response } from 'express';
import * as fs from 'node:fs';
import * as path from 'node:path';

const MIME: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.md': 'text/markdown; charset=utf-8'
};

// 只对外暴露编辑器实际需要的静态文件；tree_store 只读浏览，其余一律 404
const ALLOWED_FILES = new Set([
  '/skill_tree_editor.html',
  '/builtin_enums.js',
  '/command_definitions.json'
]);

const LOOPBACK = new Set(['127.0.0.1', '::1', '::ffff:127.0.0.1']);

/** 公网访问必须走 HTTPS（内网穿透的 x-forwarded-proto），本机 127.0.0.1 直连放行 */
export function enforceTransport(req: Request, res: Response, next: NextFunction): void {
  const remote = String(req.socket.remoteAddress || '');
  const forwarded = String(req.headers['x-forwarded-proto'] || '').split(',')[0].trim().toLowerCase();
  if (req.secure || forwarded === 'https' || LOOPBACK.has(remote)) {
    next();
    return;
  }
  res.status(403).json({ error: '必须通过 HTTPS 访问（或从本机 127.0.0.1 访问）' });
}

export function securityHeaders(req: Request, res: Response, next: NextFunction): void {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  next();
}

/**
 * 静态文件托管：只允许编辑器页面、内置 enum、命令定义，以及 tree_store 下的只读文件。
 * 明确拒绝 /server/、node_modules 和点文件，避免源码与审计日志被下载。
 */
export function createStaticHandler(rootDir: string) {
  const root = path.resolve(rootDir);

  return (req: Request, res: Response, next: NextFunction): void => {
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      next();
      return;
    }

    let pathname: string;
    try {
      pathname = decodeURIComponent(new URL(req.originalUrl, 'http://localhost').pathname);
    } catch {
      next();
      return;
    }

    if (pathname === '/' || pathname === '/editor.html') pathname = '/skill_tree_editor.html';
    const isStoreFile = pathname.startsWith('/tree_store/');
    if (!ALLOWED_FILES.has(pathname) && !isStoreFile) {
      next();
      return;
    }

    const segments = pathname.replace(/^\/+/, '').split('/');
    if (segments.some(segment => !segment || segment.startsWith('.') || segment === 'server' || segment === 'node_modules')) {
      res.status(404).json({ error: 'Not Found' });
      return;
    }

    const filePath = path.resolve(root, segments.join(path.sep));
    if (!filePath.startsWith(root + path.sep)) {
      res.status(403).json({ error: 'Forbidden' });
      return;
    }

    fs.stat(filePath, (error, stat) => {
      if (error || !stat.isFile()) {
        res.status(404).json({ error: 'Not Found' });
        return;
      }
      res.setHeader('Content-Type', MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream');
      res.setHeader('Content-Length', String(stat.size));
      res.setHeader('Cache-Control', 'no-cache');
      if (req.method === 'HEAD') {
        res.end();
        return;
      }
      const stream = fs.createReadStream(filePath);
      stream.on('error', () => {
        if (!res.headersSent) res.status(500).json({ error: '读取文件失败' });
        else res.end();
      });
      stream.pipe(res);
    });
  };
}
