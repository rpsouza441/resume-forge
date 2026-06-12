'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { resumeApi } from '@/lib/api';
import type { CreateResumeRequest, UpdateResumeRequest } from '@/types';
import { toast } from 'sonner';

export function useResumes(params?: { page?: number; size?: number; title?: string; isDefault?: boolean }) {
  return useQuery({
    queryKey: ['resumes', params],
    queryFn: () => resumeApi.list(params),
  });
}

export function useResume(id: string) {
  return useQuery({
    queryKey: ['resumes', id],
    queryFn: () => resumeApi.get(id),
    enabled: !!id,
  });
}

export function useCreateResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateResumeRequest) => resumeApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['resumes'] });
      toast.success('Curriculo criado com sucesso');
    },
    onError: () => {
      toast.error('Erro ao criar curriculo');
    },
  });
}

export function useUpdateResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateResumeRequest }) => resumeApi.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ['resumes'] });
      qc.invalidateQueries({ queryKey: ['resumes', id] });
      toast.success('Curriculo atualizado');
    },
    onError: () => {
      toast.error('Erro ao atualizar curriculo');
    },
  });
}

export function useDeleteResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => resumeApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['resumes'] });
      toast.success('Curriculo removido');
    },
    onError: () => {
      toast.error('Erro ao remover curriculo');
    },
  });
}

export function useSetDefaultResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => resumeApi.setDefault(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['resumes'] });
      toast.success('Curriculo definido como padrao');
    },
    onError: () => {
      toast.error('Erro ao definir curriculo padrao');
    },
  });
}
