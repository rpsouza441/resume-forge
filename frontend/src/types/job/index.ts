export interface JobApplication {
  id: string;
  resumeProfileId: string | null;
  jobTitle: string;
  companyName: string;
  jobUrl: string | null;
  jobDescription: string;
  jobSource: string | null;
  jobLocation: string | null;
  jobType: string | null;
  seniority: string | null;
  status: JobStatus;
  statusChangedAt: string;
  appliedAt: string | null;
  notes: string | null;
  contactName: string | null;
  contactEmail: string | null;
  generatedCount: number;
  createdAt: string;
  updatedAt: string;
}

export type JobStatus =
  | 'saved'
  | 'applied'
  | 'interviewing'
  | 'offer'
  | 'rejected'
  | 'withdrawn'
  | 'archived';

export interface JobListItem {
  id: string;
  jobTitle: string;
  companyName: string;
  status: JobStatus;
  generatedCount: number;
  createdAt: string;
}

export interface CreateJobRequest {
  resumeProfileId?: string | null;
  companyName: string;
  jobTitle: string;
  jobDescription: string;
  jobUrl?: string;
  jobLocation?: string;
  jobType?: string;
  seniority?: string;
}

export interface UpdateJobRequest {
  resumeProfileId?: string | null;
  companyName?: string;
  jobTitle?: string;
  jobDescription?: string;
  jobUrl?: string;
  jobLocation?: string;
  jobType?: string;
  seniority?: string;
  status?: JobStatus;
  notes?: string;
}
