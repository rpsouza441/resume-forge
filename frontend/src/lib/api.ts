import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  CurrentUser,
  PaginatedResponse,
  ResumeProfile,
  ResumeListItem,
  CreateResumeRequest,
  UpdateResumeRequest,
  JobApplication,
  JobListItem,
  CreateJobRequest,
  UpdateJobRequest,
  GeneratedResume,
  GenerationRequest,
  GenerationResponse,
  VersionResponse,
  ManualEditRequest,
} from '@/types';

// Relative URL — Same Origin via nginx reverse proxy. No CORS, no IP fixo no bundle.
export const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('auth_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_user');
        window.location.href = '/login';
      }
    }
    if (error.response?.status === 429) {
      if (typeof window !== 'undefined') {
        import('sonner').then(({ toast }) => {
          toast.error('Muitas solicitacoes. Tente novamente em alguns minutos.');
        });
      }
    }
    return Promise.reject(error);
  }
);

// ─── Auth ───────────────────────────────────────────────────────────────────

export const authApi = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/login', data);
    return res.data;
  },
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register', data);
    return res.data;
  },
  logout: async (): Promise<void> => {
    await api.post('/auth/logout');
  },
  me: async (): Promise<CurrentUser> => {
    const res = await api.get<CurrentUser>('/auth/me');
    return res.data;
  },
};

// ─── Resumes ────────────────────────────────────────────────────────────────

export const resumeApi = {
  list: async (params?: {
    page?: number;
    size?: number;
    sort?: string;
    title?: string;
    isDefault?: boolean;
  }): Promise<PaginatedResponse<ResumeListItem>> => {
    const res = await api.get<PaginatedResponse<ResumeListItem>>('/resumes', { params });
    return res.data;
  },
  get: async (id: string): Promise<ResumeProfile> => {
    const res = await api.get<ResumeProfile>(`/resumes/${id}`);
    return res.data;
  },
  create: async (data: CreateResumeRequest): Promise<ResumeProfile> => {
    const res = await api.post<ResumeProfile>('/resumes', data);
    return res.data;
  },
  update: async (id: string, data: UpdateResumeRequest): Promise<ResumeProfile> => {
    const res = await api.put<ResumeProfile>(`/resumes/${id}`, data);
    return res.data;
  },
  delete: async (id: string): Promise<void> => {
    await api.delete(`/resumes/${id}`);
  },
  setDefault: async (id: string): Promise<void> => {
    await api.put(`/resumes/${id}/default`);
  },
};

// ─── Jobs ───────────────────────────────────────────────────────────────────

export const jobApi = {
  list: async (params?: {
    companyName?: string;
    jobTitle?: string;
    status?: string;
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<JobListItem>> => {
    const res = await api.get<PaginatedResponse<JobListItem>>('/jobs', { params });
    return res.data;
  },
  get: async (id: string): Promise<JobApplication> => {
    const res = await api.get<JobApplication>(`/jobs/${id}`);
    return res.data;
  },
  create: async (data: CreateJobRequest): Promise<JobApplication> => {
    const res = await api.post<JobApplication>('/jobs', data);
    return res.data;
  },
  update: async (id: string, data: UpdateJobRequest): Promise<JobApplication> => {
    const res = await api.put<JobApplication>(`/jobs/${id}`, data);
    return res.data;
  },
  delete: async (id: string): Promise<void> => {
    await api.delete(`/jobs/${id}`);
  },
};

// ─── Generation ─────────────────────────────────────────────────────────────

export const generationApi = {
  generate: async (data: GenerationRequest): Promise<GenerationResponse> => {
    const res = await api.post<GenerationResponse>('/generate', data);
    return res.data;
  },
  list: async (params?: {
    companyName?: string;
    jobTitle?: string;
    resumeProfileId?: string;
    dateFrom?: string;
    dateTo?: string;
    isCurrent?: boolean;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<GeneratedResume>> => {
    const res = await api.get<PaginatedResponse<GeneratedResume>>('/generated', { params });
    return res.data;
  },
  get: async (id: string): Promise<GeneratedResume> => {
    const res = await api.get<GeneratedResume>(`/generated/${id}`);
    return res.data;
  },
  saveEdit: async (id: string, data: ManualEditRequest): Promise<GenerationResponse> => {
    const res = await api.put<GenerationResponse>(`/generated/${id}`, data);
    return res.data;
  },
  regenerate: async (id: string): Promise<GenerationResponse> => {
    const res = await api.post<GenerationResponse>(`/generated/${id}/regenerate`);
    return res.data;
  },
  listVersions: async (id: string): Promise<VersionResponse[]> => {
    const res = await api.get<VersionResponse[]>(`/generated/${id}/versions`);
    return res.data;
  },
  downloadDocx: (id: string): string => {
    return `/api/generated/${id}/docx`;
  },
};
