/** 版本目录里的 meta.json（服务端权威元数据，schema 2） */
export interface TreeMetaV2 {
  schema: 2;
  tree_id: string;
  tree_name: string;
  original_author: string;
  version: number;
  editors: string[];
  uploader: string;
  message: string;
  edited_at: string;
  uploaded_at: string;
  file_name: string;
  game_tree_id: string;
  sha256: string;
}

/** 嵌入 tree.json 根级的 meta（游戏解析器会忽略未知字段） */
export interface TreeMetaEmbedded {
  original_author: string;
  version: number;
  editors: string[];
  edited_at: string;
  message: string;
}

export interface StoredVersion {
  dir: string;
  meta: TreeMetaV2;
}

export interface TreeSummary {
  tree_id: string;
  tree_name: string;
  original_author: string;
  latest_version: number;
  upload_count: number;
  last_uploaded_at: string;
  editors: string[];
}

export interface VersionSummary {
  version: number;
  uploader: string;
  message: string;
  edited_at: string;
  uploaded_at: string;
  editors: string[];
  file_name: string;
  game_tree_id: string;
}

export interface VersionDetail {
  tree_id: string;
  version: number;
  meta: TreeMetaV2;
  tree: Record<string, unknown>;
}
