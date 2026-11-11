import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';

/** 标记不需要访问令牌的路由（目前只有 /api/health） */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
