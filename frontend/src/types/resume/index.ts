export interface ResumeProfile {
  id: string;
  title: string;
  contentText: string;
  contentMarkdown: string;
  contentJsonb: Record<string, unknown>;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeListItem {
  id: string;
  title: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateResumeRequest {
  title: string;
  contentMarkdown: string;
  contentJsonb?: Record<string, unknown>;
  isDefault?: boolean;
}

export interface UpdateResumeRequest {
  title?: string;
  contentMarkdown?: string;
  contentJsonb?: Record<string, unknown>;
  isDefault?: boolean;
}
