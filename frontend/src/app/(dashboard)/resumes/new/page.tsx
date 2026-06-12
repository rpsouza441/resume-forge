'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useCreateResume } from '@/hooks/queries/useResumes';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { ResumeForm } from '@/components/forms/ResumeForm';
import { Textarea } from '@/components/ui/Input';
import { resumeSchema, type ResumeFormData } from '@/schemas/zod/resume.schemas';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

export default function NewResumePage() {
  const router = useRouter();
  const createResume = useCreateResume();
  const [mode, setMode] = useState<'structured' | 'free'>('free');

  const freeForm = useForm<{ title: string; contentMarkdown: string }>({
    resolver: zodResolver(resumeSchema.pick({ title: true, contentMarkdown: true })),
    defaultValues: { title: '', contentMarkdown: '' },
  });

  const handleFreeSubmit = async (data: { title: string; contentMarkdown: string }) => {
    const result = await createResume.mutateAsync(data);
    router.push(`/resumes/${result.id}/edit`);
  };

  const handleStructuredSubmit = async (data: ResumeFormData) => {
    const result = await createResume.mutateAsync({
      title: data.title,
      contentMarkdown: data.contentMarkdown,
      isDefault: data.isDefault,
    });
    router.push(`/resumes/${result.id}/edit`);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/resumes">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Novo Curriculo Base</h1>
          <p className="text-slate-500 text-sm">Cole ou digite o conteudo do seu curriculo</p>
        </div>
      </div>

      {/* Mode toggle */}
      <div className="flex gap-2">
        <button
          onClick={() => setMode('free')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            mode === 'free'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
          }`}
        >
          Modo Livre
        </button>
        <button
          onClick={() => setMode('structured')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            mode === 'structured'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
          }`}
        >
          Modo Estruturado
        </button>
      </div>

      {mode === 'free' ? (
        <Card>
          <CardContent className="py-6">
            <form
              onSubmit={freeForm.handleSubmit(handleFreeSubmit)}
              className="space-y-5"
            >
              <div className="w-full">
                <label className="text-sm font-medium text-slate-700 mb-1 block">Titulo</label>
                <input
                  {...freeForm.register('title')}
                  placeholder="Ex: Curriculo Full Stack"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                {freeForm.formState.errors.title && (
                  <p className="text-xs text-red-600 mt-1">
                    {freeForm.formState.errors.title.message as string}
                  </p>
                )}
              </div>
              <Textarea
                label="Conteudo (Markdown)"
                placeholder={`# Seu Nome\n\n## Resumo Profissional\n...\n\n## Experiencia\n...\n\n## Formacao\n...\n\n## Habilidades\n...`}
                error={freeForm.formState.errors.contentMarkdown?.message as string}
                hint="Use Markdown para formatar: # Titulo, **negrito**, - listas"
                className="min-h-[400px] font-mono text-sm"
                {...freeForm.register('contentMarkdown')}
              />
              <div className="flex justify-end gap-3">
                <Link href="/resumes">
                  <Button variant="outline" type="button">
                    Cancelar
                  </Button>
                </Link>
                <Button type="submit" isLoading={createResume.isPending}>
                  Criar Curriculo
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      ) : (
        <ResumeForm
          onSubmit={handleStructuredSubmit}
          isSubmitting={createResume.isPending}
          submitLabel="Criar Curriculo"
        />
      )}
    </div>
  );
}
