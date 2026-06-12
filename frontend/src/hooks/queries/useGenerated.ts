'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { generationApi } from '@/lib/api';
import type { GenerationRequest, ManualEditRequest } from '@/types';
import { toast } from 'sonner';

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
    queryFn: () => generationApi.get(id),
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
