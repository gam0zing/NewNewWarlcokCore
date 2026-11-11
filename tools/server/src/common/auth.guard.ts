import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import * as crypto from 'node:crypto';
import { config } from '../config';
import { IS_PUBLIC_KEY } from './public.decorator';

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass()
    ]);
    if (isPublic) return true;

    const request = context.switchToHttp().getRequest();
    const header = String(request.headers.authorization || '');
    const matched = /^Bearer\s+(.+)$/i.exec(header);
    if (!matched) throw new UnauthorizedException('缺少 Authorization: Bearer <令牌>');

    const given = Buffer.from(matched[1].trim(), 'utf8');
    const expected = Buffer.from(config.token, 'utf8');
    if (given.length !== expected.length || !crypto.timingSafeEqual(given, expected)) {
      throw new UnauthorizedException('访问令牌不正确');
    }
    return true;
  }
}
