'use client';

import { useForm } from 'react-hook-form';
import { useRouter } from 'next/navigation';
import { useGeneratedResume, useSaveEdit } from '@/hooks/queries/useGenerated';
import { Button } from '@/components/ui/Button';
import { Textarea } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { ArrowLeft, Save } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { Spinner } from '@/components/ui/Spinner';
import { DocxDownloadButton } from '@/components/shared/DocxDownloadButton';

export default function EditGeneratedPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const { data: resume, isLoading } = useGeneratedResume(params.id);
  const saveEdit = useSaveEdit();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm();

  const onSubmit = async (data: Record<string, string>) => {
    try {
      const result = await saveEdit.mutateAsync({
        id: params.id,
        data: { contentMarkdown: data.contentMarkdown },
      });
      toast.success('Edicao salva como nova versao');
      router.push(`/generated/${result.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar';
      toast.error(msg);
    }
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
        <Link href={`/generated/${params.id}`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">
            Editando Curriculo v{resume?.versionNumber}
          </h1>
          <p className="text-slate-500 text-sm">
            Suas alteracoes serao salvas como uma nova versao
          </p>
        </div>
      </div>

      <Card>
        <CardContent className="py-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Textarea
              label="Conteudo (Markdown)"
              defaultValue={resume?.contentMarkdown}
              error={errors.contentMarkdown?.message as string}
              className="min-h-[500px] font-mono text-sm"
              {...register('contentMarkdown')}
            />
            <div className="flex justify-end gap-3">
              <Link href={`/generated/${params.id}`}>
                <Button variant="outline" type="button">
                  Cancelar
                </Button>
              </Link>
              <Button type="submit" isLoading={isSubmitting}>
                <Save className="h-4 w-4" />
                Salvar como Nova Versao
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
