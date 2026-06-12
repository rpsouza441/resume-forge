export interface GeneratedResume {
  id: string;
  resumeProfileId: string;
  jobApplicationId: string | null;
  companyName: string | null;
  jobTitle: string | null;
  versionNumber: number;
  isCurrent: boolean;
  status: string;
  contentMarkdown: string;
  contentText: string;
  contentJsonb: string;
  adherenceScore: number | null;
  aiProvider: string;
  aiModel: string;
  generationReason: string | null;
  wordCount: number | null;
  charCount: number | null;
  analysis: AnalysisReportDto | null;
  createdAt: string;
  docxGeneratedAt: string | null;
}

export interface AnalysisReportDto {
  id: string;
  overallScore: number | null;
  findings: string;
  recommendations: string;
}

export interface GenerationRequest {
  resumeProfileId: string;
  jobApplicationId: string;
  extraInstructions?: string;
}

export interface GenerationResponse {
  id: string;
  resumeProfileId: string;
  jobApplicationId: string | null;
  versionNumber: number;
  isCurrent: boolean;
  status: string;
  contentMarkdown: string;
  contentText: string;
  contentJsonb: string;
  analysis: {
    id: string;
    adherenceScore: number;
    summary: string;
    keywordMap: {
      matched: string[];
      missing: string[];
    };
    gaps: unknown[];
  };
  aiRun: {
    id: string;
    provider: string;
    model: string;
    status: string;
  };
  createdAt: string;
}

export interface VersionResponse {
  id: string;
  versionNumber: number;
  isCurrent: boolean;
  generationReason: string | null;
  aiProvider: string;
  aiModel: string;
  createdAt: string;
}

export interface ManualEditRequest {
  contentMarkdown: string;
  contentJsonb?: string;
}
