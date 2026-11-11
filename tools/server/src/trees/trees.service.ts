import {
  BadRequestException,
  ConflictException,
  HttpException,
  Injectable,
  NotFoundException
} from '@nestjs/common';
import * as crypto from 'node:crypto';
import * as fsp from 'node:fs/promises';
import * as path from 'node:path';
import JSZip from 'jszip';
import { config } from '../config';
import {
  StoredVersion,
  TreeMetaEmbedded,
  TreeMetaV2,
  TreeSummary,
  VersionDetail,
  VersionSummary
} from './trees.types';

const TREE_ID_PATTERN = /^t[a-f0-9]{12}$/;
const GAME_TREE_ID_PATTERN = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const MAX_VERSION = 9999;

@Injectable()
export class TreesService {
  /** 文件存储的写入串行化，避免同版本并发创建 */
  private writeQueue: Promise<void> = Promise.resolve();

  async listTrees(): Promise<TreeSummary[]> {
    const stored = await this.scanMetas();
    const map = new Map<string, TreeSummary>();

    for (const { meta } of stored) {
      let summary = map.get(meta.tree_id);
      if (!summary) {
        summary = {
          tree_id: meta.tree_id,
          tree_name: meta.tree_name,
          original_author: meta.original_author,
          latest_version: 0,
          upload_count: 0,
          last_uploaded_at: '',
          editors: []
        };
        map.set(meta.tree_id, summary);
      }
      summary.upload_count += 1;
      if (meta.version >= summary.latest_version) {
        summary.latest_version = meta.version;
        summary.tree_name = meta.tree_name;
        summary.original_author = meta.original_author;
      }
      if (meta.uploaded_at > summary.last_uploaded_at) summary.last_uploaded_at = meta.uploaded_at;
      for (const editor of meta.editors || []) {
        if (!summary.editors.includes(editor)) summary.editors.push(editor);
      }
    }

    return Array.from(map.values()).sort((left, right) =>
      right.last_uploaded_at.localeCompare(left.last_uploaded_at)
    );
  }

  async listVersions(treeId: string): Promise<{
    tree_id: string;
    tree_name: string;
    original_author: string;
    latest_version: number;
    versions: VersionSummary[];
  }> {
    this.assertTreeId(treeId);
    const stored = (await this.scanMetas()).filter(item => item.meta.tree_id === treeId);
    if (!stored.length) throw new NotFoundException('找不到该技能树');

    stored.sort((left, right) => right.meta.version - left.meta.version);
    const latest = stored[0].meta;

    return {
      tree_id: treeId,
      tree_name: latest.tree_name,
      original_author: latest.original_author,
      latest_version: latest.version,
      versions: stored.map(({ meta }) => ({
        version: meta.version,
        uploader: meta.uploader,
        message: meta.message,
        edited_at: meta.edited_at,
        uploaded_at: meta.uploaded_at,
        editors: meta.editors || [],
        file_name: meta.file_name,
        game_tree_id: meta.game_tree_id
      }))
    };
  }

  async getVersion(treeId: string, version: number): Promise<VersionDetail> {
    this.assertTreeId(treeId);
    const stored = (await this.scanMetas()).find(
      item => item.meta.tree_id === treeId && item.meta.version === version
    );
    if (!stored) throw new NotFoundException('找不到该版本');

    let tree: Record<string, unknown>;
    try {
      tree = JSON.parse(await fsp.readFile(path.join(stored.dir, 'tree.json'), 'utf8'));
    } catch {
      throw new HttpException({ error: '该版本的 tree.json 已损坏' }, 500);
    }

    return { tree_id: treeId, version, meta: stored.meta, tree };
  }

  /** 推送当前工作区作为一棵新树，版本从 1 开始 */
  async createTree(payload: any, ip: string): Promise<{ tree_id: string; version: number; meta: TreeMetaV2 }> {
    const tree = this.normalizeTree(payload && payload.tree);
    const treeName = this.cleanText(payload?.name, 64) || this.cleanText(tree.display_name, 64) || '未命名技能树';
    const originalAuthor = this.cleanText(payload?.original_author, 64);
    if (!originalAuthor) throw new BadRequestException('original_author 不能为空');
    const uploader = this.cleanText(payload?.uploader, 64) || originalAuthor;
    const message = this.cleanText(payload?.message, 500);
    const editedAt = this.parseIso(payload?.edited_at);
    const fileName = this.fileNameOf(payload?.file_name);
    const gameTreeId = this.gameTreeIdOf(payload?.game_tree_id, '');

    const treeId = 't' + crypto
      .createHash('sha256')
      .update([treeName, originalAuthor, Date.now(), crypto.randomUUID()].join('|'))
      .digest('hex')
      .slice(0, 12);
    const version = 1;
    const now = new Date().toISOString();
    const meta: TreeMetaV2 = {
      schema: 2,
      tree_id: treeId,
      tree_name: treeName,
      original_author: originalAuthor,
      version,
      editors: [uploader],
      uploader,
      message,
      edited_at: editedAt,
      uploaded_at: now,
      file_name: fileName,
      game_tree_id: gameTreeId,
      sha256: this.hashTree(tree)
    };

    await this.withLock(async () => {
      await fsp.mkdir(config.storeDir, { recursive: true });
      const base = this.versionDirName(treeName, version, uploader);
      const dirName = await this.allocateDir(base, treeId);
      await this.writeVersionDir(dirName, this.withEmbeddedMeta(tree, meta), meta);
    });

    await this.audit(ip, 'create_tree', `${treeId} v${version} ${treeName}`);
    return { tree_id: treeId, version, meta };
  }

  /** 推送当前工作区作为已有树的新版本；expected_latest 不匹配时返回 409 */
  async addVersion(
    treeId: string,
    payload: any,
    ip: string
  ): Promise<{ tree_id: string; version: number; meta: TreeMetaV2 }> {
    this.assertTreeId(treeId);
    const tree = this.normalizeTree(payload && payload.tree);
    const expected = Number(payload?.expected_latest);
    if (!Number.isInteger(expected) || expected < 1) {
      throw new BadRequestException('expected_latest 必须是正整数');
    }
    const uploader = this.cleanText(payload?.uploader, 64);
    if (!uploader) throw new BadRequestException('uploader 不能为空');
    const message = this.cleanText(payload?.message, 500);
    const editedAt = this.parseIso(payload?.edited_at);

    const result = await this.withLock(async () => {
      const stored = (await this.scanMetas()).filter(item => item.meta.tree_id === treeId);
      if (!stored.length) throw new NotFoundException('找不到该技能树');

      stored.sort((left, right) => right.meta.version - left.meta.version);
      const latest = stored[0].meta;
      if (latest.version !== expected) {
        throw new HttpException(
          { error: `远端已更新到 v${latest.version}，请先拉取最新版本`, latest_version: latest.version },
          409
        );
      }

      const version = latest.version + 1;
      if (version > MAX_VERSION) throw new ConflictException('版本号已达上限');

      const treeName = this.cleanText(payload?.name, 64) || latest.tree_name;
      const fileName = payload?.file_name ? this.fileNameOf(payload.file_name) : latest.file_name;
      const gameTreeId = this.gameTreeIdOf(payload?.game_tree_id, latest.game_tree_id || '');
      const editors = Array.from(new Set([...(latest.editors || []), uploader]));
      const meta: TreeMetaV2 = {
        schema: 2,
        tree_id: treeId,
        tree_name: treeName,
        original_author: latest.original_author,
        version,
        editors,
        uploader,
        message,
        edited_at: editedAt,
        uploaded_at: new Date().toISOString(),
        file_name: fileName,
        game_tree_id: gameTreeId,
        sha256: this.hashTree(tree)
      };

      const base = this.versionDirName(treeName, version, uploader);
      const dirName = await this.allocateDir(base, treeId);
      await this.writeVersionDir(dirName, this.withEmbeddedMeta(tree, meta), meta);
      return { tree_id: treeId, version, meta };
    });

    await this.audit(ip, 'create_version', `${treeId} v${result.version} ${uploader}`);
    return result;
  }

  /** 生成包含 tree.json + meta.json 的 zip */
  async download(treeId: string, version: number): Promise<{ buffer: Buffer; filename: string }> {
    const detail = await this.getVersion(treeId, version);
    const zip = new JSZip();
    zip.file('tree.json', JSON.stringify(detail.tree, null, 2) + '\n');
    zip.file('meta.json', JSON.stringify(detail.meta, null, 2) + '\n');
    const buffer = await zip.generateAsync({
      type: 'nodebuffer',
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    });
    const filename = `${this.safeSegment(detail.meta.tree_name, 'tree')}_v${this.formatVersion(version)}.zip`;
    return { buffer, filename };
  }

  // ------------------------------------------------------------------ 内部实现

  private assertTreeId(treeId: string): void {
    if (!TREE_ID_PATTERN.test(treeId)) throw new BadRequestException('tree_id 格式不正确');
  }

  private async scanMetas(): Promise<StoredVersion[]> {
    await fsp.mkdir(config.storeDir, { recursive: true });
    const entries = await fsp.readdir(config.storeDir, { withFileTypes: true });
    const result: StoredVersion[] = [];

    for (const entry of entries) {
      if (!entry.isDirectory() || entry.name.startsWith('.')) continue;
      const dir = path.join(config.storeDir, entry.name);
      try {
        const meta = JSON.parse(await fsp.readFile(path.join(dir, 'meta.json'), 'utf8')) as TreeMetaV2;
        if (!meta || meta.schema !== 2) continue;
        if (!TREE_ID_PATTERN.test(String(meta.tree_id))) continue;
        if (!Number.isInteger(meta.version) || meta.version < 1) continue;
        result.push({ dir, meta });
      } catch {
        // 忽略不完整或损坏的版本目录，不影响其它树
      }
    }
    return result;
  }

  private normalizeTree(raw: any): Record<string, any> {
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
      throw new BadRequestException('tree 必须是对象');
    }
    if (raw.schema !== 1) throw new BadRequestException('tree.schema 必须为 1');
    if (!Array.isArray(raw.nodes) || !Array.isArray(raw.edges)) {
      throw new BadRequestException('tree 缺少 nodes / edges 数组');
    }
    if (raw.nodes.length > 5000) throw new BadRequestException('节点数量超过 5000');
    if (raw.edges.length > 20000) throw new BadRequestException('连接数量超过 20000');

    // 深拷贝并丢弃客户端自带的 meta：版本、作者等由服务端重新生成
    const tree = JSON.parse(JSON.stringify(raw));
    delete tree.meta;
    return tree;
  }

  private withEmbeddedMeta(tree: Record<string, any>, meta: TreeMetaV2): Record<string, any> {
    const embedded: TreeMetaEmbedded = {
      original_author: meta.original_author,
      version: meta.version,
      editors: meta.editors.slice(),
      edited_at: meta.edited_at,
      message: meta.message
    };
    return { ...tree, meta: embedded };
  }

  private hashTree(tree: Record<string, any>): string {
    return crypto.createHash('sha256').update(canonicalJson(tree), 'utf8').digest('hex');
  }

  private async writeVersionDir(
    dirName: string,
    tree: Record<string, any>,
    meta: TreeMetaV2
  ): Promise<void> {
    await fsp.mkdir(config.storeDir, { recursive: true });
    const tempDir = path.join(config.storeDir, `.tmp-${crypto.randomBytes(8).toString('hex')}`);
    const finalDir = path.join(config.storeDir, dirName);

    await fsp.mkdir(tempDir, { recursive: true });
    try {
      await fsp.writeFile(path.join(tempDir, 'tree.json'), JSON.stringify(tree, null, 2) + '\n', 'utf8');
      await fsp.writeFile(path.join(tempDir, 'meta.json'), JSON.stringify(meta, null, 2) + '\n', 'utf8');
      await fsp.rename(tempDir, finalDir);
    } catch (error) {
      await fsp.rm(tempDir, { recursive: true, force: true }).catch(() => undefined);
      throw error;
    }
  }

  private async allocateDir(base: string, treeId: string): Promise<string> {
    const candidates = [base, `${base}_${treeId.slice(1, 7)}`];
    for (const name of candidates) {
      if (!(await this.exists(path.join(config.storeDir, name)))) return name;
    }
    for (let index = 2; index < 100; index += 1) {
      const name = `${base}_${treeId.slice(1, 7)}_${index}`;
      if (!(await this.exists(path.join(config.storeDir, name)))) return name;
    }
    throw new HttpException({ error: '无法分配版本目录' }, 500);
  }

  private async exists(target: string): Promise<boolean> {
    try {
      await fsp.access(target);
      return true;
    } catch {
      return false;
    }
  }

  private versionDirName(treeName: string, version: number, uploader: string): string {
    return `${this.safeSegment(treeName, 'tree')}_v${this.formatVersion(version)}_${this.safeSegment(uploader, 'unknown')}`;
  }

  private formatVersion(version: number): string {
    return String(version).padStart(4, '0');
  }

  /** 清洗成安全的文件系统片段，并规避 Windows 保留名 */
  private safeSegment(value: string, fallback: string): string {
    let text = String(value || '')
      .normalize('NFKC')
      .replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_')
      .replace(/\s+/g, '_')
      .replace(/^[.\s]+|[.\s]+$/g, '')
      .slice(0, 48);
    if (!text) text = fallback;
    if (/^(con|prn|aux|nul|com[1-9]|lpt[1-9])$/i.test(text)) text = '_' + text;
    return text;
  }

  private cleanText(value: any, maxLength: number): string {
    if (value === undefined || value === null) return '';
    if (typeof value !== 'string') throw new BadRequestException('文本字段必须是字符串');
    const text = value.replace(/[\u0000-\u001f]/g, '').trim();
    if (text.length > maxLength) throw new BadRequestException(`文本长度不能超过 ${maxLength}`);
    return text;
  }

  private fileNameOf(value: any): string {
    const base = path.basename(String(value || '')).trim();
    if (!base) return 'skill_tree.json';
    const cleaned = base.replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_').slice(0, 80);
    return cleaned.toLowerCase().endsWith('.json') ? cleaned : cleaned + '.json';
  }

  private gameTreeIdOf(value: any, fallback: string): string {
    const text = this.cleanText(value, 128);
    if (!text) return fallback;
    if (!GAME_TREE_ID_PATTERN.test(text)) {
      throw new BadRequestException('game_tree_id 必须形如 namespace:path');
    }
    return text;
  }

  private parseIso(value: any): string {
    if (typeof value === 'string') {
      const time = Date.parse(value);
      if (Number.isFinite(time)) return new Date(time).toISOString();
    }
    return new Date().toISOString();
  }

  private withLock<T>(task: () => Promise<T>): Promise<T> {
    const run = this.writeQueue.then(task, task);
    this.writeQueue = run.then(() => undefined, () => undefined);
    return run;
  }

  private async audit(ip: string, action: string, detail: string): Promise<void> {
    const line = `[${new Date().toISOString()}] ${ip || '-'} ${action} ${detail}\n`;
    try {
      await fsp.appendFile(config.auditLog, line, 'utf8');
    } catch {
      // 审计日志写入失败不影响业务
    }
  }
}

/** 键名排序后的 JSON，用于跨端计算一致的树内容哈希 */
function canonicalJson(value: any): string {
  if (Array.isArray(value)) return '[' + value.map(item => canonicalJson(item)).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value)
      .sort()
      .map(key => JSON.stringify(key) + ':' + canonicalJson(value[key]))
      .join(',') + '}';
  }
  return JSON.stringify(value) ?? 'null';
}
