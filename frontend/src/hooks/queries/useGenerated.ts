'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { generationApi, resumeApi } from '@/lib/api';
import type { GenerationRequest, ManualEditRequest, ResumeHeader } from '@/types';
import { toast } from 'sonner';

/**
 * Extracts header from resume profile contentJsonb
 */
function extractHeaderFromProfile(profileJsonb: Record<string, unknown> | null): ResumeHeader | null {
  if (!profileJsonb) return null;

  // Try new schema: profile.contacts
  const profile = profileJsonb.profile as Record<string, unknown> | undefined;
  if (profile && typeof profile === 'object') {
    const name = (profile.name as string) || '';
    const location = (profile.location as string) || '';
    const contacts = profile.contacts as Record<string, string> | undefined;

    if (name || contacts) {
      return {
        name: name || 'Nome nao disponivel',
        title: (profile.title as string) || undefined,
        location: location || undefined,
        contacts: contacts ? {
          email: contacts.email,
          phone: contacts.phone,
          linkedin: contacts.linkedin,
          github: contacts.github,
        } : undefined,
      };
    }
  }

  // Fallback to legacy schema: personalInfo
  const personalInfo = profileJsonb.personalInfo as Record<string, string> | undefined;
  if (personalInfo) {
    return {
      name: personalInfo.fullName || 'Nome nao disponivel',
      location: personalInfo.location || undefined,
      contacts: {
        email: personalInfo.email,
        phone: personalInfo.phone,
        linkedin: personalInfo.linkedin,
        github: personalInfo.github,
      },
    };
  }

  return null;
}

export function useGeneratedResumes(params?: {
  companyName?: string;
  jobTitle?: string;
  resumeProfileId?: string;
  jobApplicationId?: string;
  dateFrom?: string;
  dateTo?: string;
  isCurrent?: boolean;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ['generated', params],
    queryFn: async () => {
      const response = await generationApi.list(params);
      return response; // The response already has { data: [], page, size, total, totalPages }
    },
  });
}

export function useGeneratedResume(id: string) {
  return useQuery({
    queryKey: ['generated', id],
    queryFn: async (): Promise<import('@/types').GeneratedResume & { header?: ResumeHeader }> => {
      const resume = await generationApi.get(id);

      // Extract header from resume profile if not present in contentJsonb
      let header: ResumeHeader | undefined;
      if (resume.resumeProfileId) {
        try {
          const profile = await resumeApi.get(resume.resumeProfileId);
          header = extractHeaderFromProfile(profile.contentJsonb as Record<string, unknown> | null) || undefined;
        } catch (e) {
          console.warn('Failed to fetch resume profile for header extraction:', e);
        }
      }

      return { ...resume, header };
    },
    enabled: !!id,
  });
}

export function useGenerateResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: GenerationRequest) => generationApi.generate(data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['generated'] });
      qc.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Curriculo gerado com sucesso');
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Erro ao gerar curriculo';
      toast.error(msg);
    },
  });
}

export function useSaveEdit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ManualEditRequest }) =>
      generationApi.saveEdit(id, data),
    onSuccess: (data, { id }) => {
      qc.invalidateQueries({ queryKey: ['generated'] });
      qc.invalidateQueries({ queryKey: ['generated', id] });
      toast.success('Edicao salva como nova versao');
    },
    onError: () => {
      toast.error('Erro ao salvar edicao');
    },
  });
}

export function useRegenerate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => generationApi.regenerate(id),
    onSuccess: (_data, id) => {
      qc.invalidateQueries({ queryKey: ['generated'] });
      qc.invalidateQueries({ queryKey: ['generated', id] });
      toast.success('Curriculo regenerado');
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Erro ao regenerar';
      toast.error(msg);
    },
  });
}

export function useVersions(id: string) {
  return useQuery({
    queryKey: ['generated', id, 'versions'],
    queryFn: () => generationApi.listVersions(id),
    enabled: !!id,
  });
}
