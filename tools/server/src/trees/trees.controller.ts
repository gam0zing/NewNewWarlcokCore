import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  Post,
  Req,
  Res
} from '@nestjs/common';
import { Request, Response } from 'express';
import { TreesService } from './trees.service';
import { Public } from '../common/public.decorator';
import { config } from '../config';

function parseVersion(value: string): number {
  const version = Number(value);
  if (!Number.isInteger(version) || version < 1 || version > 9999) {
    throw new BadRequestException('版本号必须是 1-9999 的整数');
  }
  return version;
}

@Controller('api')
export class TreesController {
  constructor(private readonly trees: TreesService) {}

  @Public()
  @Get('health')
  health() {
    return {
      ok: true,
      service: 'skill-tree-editor',
      version: 1,
      time: new Date().toISOString(),
      upload_limit: config.maxUploadBytes
    };
  }

  @Get('trees')
  listTrees() {
    return this.trees.listTrees();
  }

  @Get('trees/:treeId/versions')
  listVersions(@Param('treeId') treeId: string) {
    return this.trees.listVersions(treeId);
  }

  @Get('trees/:treeId/versions/:version/download')
  async download(
    @Param('treeId') treeId: string,
    @Param('version') version: string,
    @Res() res: Response
  ): Promise<void> {
    const parsed = parseVersion(version);
    const result = await this.trees.download(treeId, parsed);
    const asciiName = `skill_tree_v${String(parsed).padStart(4, '0')}.zip`;
    res.setHeader('Content-Type', 'application/zip');
    res.setHeader(
      'Content-Disposition',
      `attachment; filename="${asciiName}"; filename*=UTF-8''${encodeURIComponent(result.filename)}`
    );
    res.setHeader('Content-Length', String(result.buffer.length));
    res.setHeader('Cache-Control', 'no-store');
    res.end(result.buffer);
  }

  @Get('trees/:treeId/versions/:version')
  getVersion(@Param('treeId') treeId: string, @Param('version') version: string) {
    return this.trees.getVersion(treeId, parseVersion(version));
  }

  @Post('trees')
  createTree(@Body() body: any, @Req() request: Request) {
    return this.trees.createTree(body, request.ip);
  }

  @Post('trees/:treeId/versions')
  addVersion(@Param('treeId') treeId: string, @Body() body: any, @Req() request: Request) {
    return this.trees.addVersion(treeId, body, request.ip);
  }
}
