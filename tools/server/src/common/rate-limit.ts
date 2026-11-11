import { NextFunction, Request, Response } from 'express';
import { config } from '../config';

const hits = new Map<string, number[]>();

/** 简单的写请求限流：同一 IP 每分钟最多 30 次 POST */
export function rateLimit(req: Request, res: Response, next: NextFunction): void {
  if (!req.path.startsWith('/api/') || req.method === 'GET' || req.method === 'HEAD') {
    next();
    return;
  }

  const now = Date.now();
  const key = req.ip || 'unknown';
  const recent = (hits.get(key) || []).filter(time => now - time < config.rateLimitWindowMs);
  if (recent.length >= config.rateLimitPosts) {
    res.status(429).json({ error: '请求过于频繁，请稍后再试' });
    return;
  }

  recent.push(now);
  hits.set(key, recent);

  if (hits.size > 2000) {
    for (const [ip, times] of hits) {
      if (!times.length || now - times[times.length - 1] > config.rateLimitWindowMs) hits.delete(ip);
    }
  }
  next();
}
