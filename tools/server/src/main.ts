import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import * as express from 'express';
import { NextFunction, Request, Response } from 'express';
import { AppModule } from './app.module';
import { assertConfig, config } from './config';
import { createStaticHandler, enforceTransport, securityHeaders } from './common/static';
import { rateLimit } from './common/rate-limit';

async function bootstrap(): Promise<void> {
  assertConfig();

  const app = await NestFactory.create<NestExpressApplication>(AppModule, { bodyParser: false });
  app.set('trust proxy', true);
  app.disable('x-powered-by');

  // 中间件按顺序在 Nest 初始化前注册，保证静态资源也受安全头与 HTTPS 约束
  app.use(enforceTransport);
  app.use(securityHeaders);
  app.use(createStaticHandler(config.toolsDir));
  app.use(rateLimit);
  app.use(express.json({ limit: config.maxUploadBytes }));
  app.use(express.urlencoded({ extended: false, limit: '1mb' }));

  app.enableShutdownHooks();
  await app.init();

  // 放在路由之后，把请求体过大等中间件异常也统一成 JSON
  app.use((error: any, req: Request, res: Response, next: NextFunction) => {
    if (res.headersSent) {
      next(error);
      return;
    }
    const status = Number(error && (error.status || error.statusCode)) || 500;
    const message = status === 413
      ? '请求体超过 ' + Math.round(config.maxUploadBytes / 1024 / 1024) + 'MB 限制'
      : (error && error.message) || '服务器内部错误';
    if (status >= 500) console.error(error);
    res.status(status).json({ error: message });
  });

  await app.listen(config.port, config.host);

  const address = `http://${config.host}:${config.port}`;
  console.log(`技能树远程仓库服务已启动：${address}/editor.html`);
  console.log(`仓库目录：${config.storeDir}`);
  console.log('接口需要访问令牌（EDITOR_TOKEN）；/api/health 无需令牌。');
}

bootstrap();
