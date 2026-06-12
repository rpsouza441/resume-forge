'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';

const jobFormSchema = {
  companyName: { required: 'Empresa e obrigatoria' },
  jobTitle: { required: 'Titulo da vaga e obrigatorio' },
  jobDescription: { required: 'Descricao e obrigatoria', minLength: { value: 100, message: 'Minimo 100 caracteres' } },
};

interface JobFormProps {
  defaultValues?: {
    companyName?: string;
    jobTitle?: string;
    jobDescription?: string;
    jobUrl?: string;
    jobLocation?: string;
    jobType?: string;
    seniority?: string;
  };
  onSubmit: (data: {
    companyName: string;
    jobTitle: string;
    jobDescription: string;
    jobUrl?: string;
    jobLocation?: string;
    jobType?: string;
    seniority?: string;
  }) => Promise<void>;
  isSubmitting?: boolean;
  submitLabel?: string;
}

export function JobForm({ defaultValues, onSubmit, isSubmitting, submitLabel = 'Salvar' }: JobFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      companyName: defaultValues?.companyName ?? '',
      jobTitle: defaultValues?.jobTitle ?? '',
      jobDescription: defaultValues?.jobDescription ?? '',
      jobUrl: defaultValues?.jobUrl ?? '',
      jobLocation: defaultValues?.jobLocation ?? '',
      jobType: defaultValues?.jobType ?? '',
      seniority: defaultValues?.seniority ?? '',
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Input
          label="Titulo da Vaga"
          placeholder="Ex: Desenvolvedor Java Senior"
          error={errors.jobTitle?.message as string}
          {...register('jobTitle', { required: jobFormSchema.jobTitle.required })}
        />
        <Input
          label="Empresa"
          placeholder="Ex: Acme Corp"
          error={errors.companyName?.message as string}
          {...register('companyName', { required: jobFormSchema.companyName.required })}
        />
      </div>

      <Textarea
        label="Descricao da Vaga"
        placeholder="Cole aqui a descricao completa da vaga (requisitos, responsabilidades, beneficios...)"
        error={errors.jobDescription?.message as string}
        hint="Minimo 100 caracteres. Quanto mais detalhes, melhor a otimizacao."
        className="min-h-[300px] text-sm"
        {...register('jobDescription', {
          required: jobFormSchema.jobDescription.required,
          minLength: {
            value: 100,
            message: jobFormSchema.jobDescription.minLength.message,
          },
        })}
      />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Input
          label="URL da Vaga"
          placeholder="https://..."
          {...register('jobUrl')}
        />
        <Input
          label="Localizacao"
          placeholder="Ex: Remoto, Sao Paulo"
          {...register('jobLocation')}
        />
        <Input
          label="Tipo"
          placeholder="Ex: fulltime, remote"
          {...register('jobType')}
        />
      </div>

      <Input
        label="Senioridade"
        placeholder="Ex: Junior, Plenior, Senior"
        {...register('seniority')}
      />

      <div className="flex justify-end gap-3">
        <Button type="submit" isLoading={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
