'use client';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { useResume, useUpdateResume } from '@/hooks/queries/useResumes';
import { resumeSchema, type ResumeFormData } from '@/schemas/zod/resume.schemas';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { Spinner } from '@/components/ui/Spinner';

export default function EditResumePage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const { data: resume, isLoading } = useResume(params.id);
  const updateResume = useUpdateResume();

  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<ResumeFormData>({
    resolver: zodResolver(resumeSchema.pick({ title: true, contentMarkdown: true, isDefault: true })),
  });

  // Set default values after data loads
  useEffect(() => {
    if (resume) {
      reset({
        title: resume.title,
        contentMarkdown: resume.contentMarkdown,
        isDefault: resume.isDefault,
      });
    }
  }, [resume, reset]);

  const onSubmit = async (data: ResumeFormData) => {
    await updateResume.mutateAsync({
      id: params.id,
      data: {
        title: data.title,
        contentMarkdown: data.contentMarkdown,
        isDefault: data.isDefault,
      },
    });
    router.push('/resumes');
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/resumes">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Editar Curriculo</h1>
          <p className="text-slate-500 text-sm">{resume?.title}</p>
        </div>
      </div>

      <Card>
        <CardContent className="py-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label="Titulo"
              error={errors.title?.message as string}
              {...register('title')}
            />
            <Textarea
              label="Conteudo (Markdown)"
              error={errors.contentMarkdown?.message as string}
              hint="Use Markdown para formatar"
              className="min-h-[400px] font-mono text-sm"
              {...register('contentMarkdown')}
            />
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                {...register('isDefault')}
                className="rounded border-slate-300"
                id="isDefault"
              />
              <label htmlFor="isDefault" className="text-sm text-slate-700 cursor-pointer">
                Definir como curriculo padrao
              </label>
            </div>
            <div className="flex justify-end gap-3">
              <Link href="/resumes">
                <Button variant="outline" type="button">
                  Cancelar
                </Button>
              </Link>
              <Button type="submit" isLoading={isSubmitting}>
                Salvar Alteracoes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
