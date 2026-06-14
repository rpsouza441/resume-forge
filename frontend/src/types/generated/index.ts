export interface OptimizedResume {
  professional_title?: string;
  professional_summary?: string;
  skills?: Array<{ category: string; items: string[] }>;
  experience?: Array<{
    company: string;
    official_role: string;
    location?: string;
    start_date?: string;
    end_date?: string;
    highlights?: string[];
  }>;
  previous_experience_summary?: string[];
  projects?: Array<{ name: string; description?: string; technologies?: string[] }>;
  education?: Array<{ institution: string; degree: string; period?: string }>;
  certifications?: Array<{ name: string; issuer?: string; date?: string }>;
  trainings?: Array<{ name: string; issuer?: string; date?: string }>;
  languages?: Array<{ language: string; level?: string }>;
}

export interface ResumeHeader {
  name: string;
  title?: string;
  location?: string;
  contacts?: {
    email?: string;
    phone?: string;
    linkedin?: string;
    github?: string;
  };
}

export interface GeneratedResumeContentJsonb {
  optimized_resume?: OptimizedResume;
  [key: string]: unknown;
}

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
  contentJsonb: GeneratedResumeContentJsonb | null;
  header?: ResumeHeader;
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
