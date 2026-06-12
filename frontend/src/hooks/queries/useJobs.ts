'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobApi } from '@/lib/api';
import type { CreateJobRequest, UpdateJobRequest } from '@/types';
import { toast } from 'sonner';

export function useJobs(params?: {
  companyName?: string;
  jobTitle?: string;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ['jobs', params],
    queryFn: () => jobApi.list(params),
  });
}

export function useJob(id: string) {
  return useQuery({
    queryKey: ['jobs', id],
    queryFn: () => jobApi.get(id),
    enabled: !!id,
  });
}

export function useCreateJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateJobRequest) => jobApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Vaga criada com sucesso');
    },
    onError: () => {
      toast.error('Erro ao criar vaga');
    },
  });
}

export function useUpdateJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateJobRequest }) => jobApi.update(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ['jobs'] });
      qc.invalidateQueries({ queryKey: ['jobs', id] });
      toast.success('Vaga atualizada');
    },
    onError: () => {
      toast.error('Erro ao atualizar vaga');
    },
  });
}

export function useDeleteJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => jobApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Vaga removida');
    },
    onError: () => {
      toast.error('Erro ao remover vaga');
    },
  });
}
