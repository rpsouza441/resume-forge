'use client';

import { useState } from 'react';
import { useJob } from '@/hooks/queries/useJobs';
import { useGenerateResume } from '@/hooks/queries/useGenerated';
import { useResumes } from '@/hooks/queries/useResumes';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { ArrowLeft, Sparkles, FileOutput } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useGeneratedResumes } from '@/hooks/queries/useGenerated';
import { formatDate } from '@/lib/utils';
import { Spinner } from '@/components/ui/Spinner';
import { Modal } from '@/components/ui/Modal';

const statusLabels: Record<string, string> = {
  saved: 'Salvo',
  applied: 'Aplicado',
  interviewing: 'Entrevista',
  offer: 'Oferta',
  rejected: 'Rejeitado',
  withdrawn: 'Desistiu',
  archived: 'Arquivado',
};

export default function JobDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const { data: job, isLoading } = useJob(params.id);
  const { data: resumesData } = useResumes({ size: 100 });
  const { data: generatedData } = useGeneratedResumes({ jobApplicationId: params.id, size: 50 });
  const generateResume = useGenerateResume();

  const [showResumeModal, setShowResumeModal] = useState(false);
  const [selectedResumeId, setSelectedResumeId] = useState<string | null>(null);

  const handleOpenGenerate = () => {
    if (!resumesData?.data?.length) {
      toast.error('Cadastre um curriculo base primeiro');
      return;
    }
    // Pre-select default resume
    const defaultResume = resumesData.data.find((r) => r.isDefault) ?? resumesData.data[0];
    setSelectedResumeId(defaultResume?.id ?? null);
    setShowResumeModal(true);
  };

  const handleGenerate = async () => {
    if (!job || !selectedResumeId) {
      toast.error('Selecione um curriculo base');
      return;
    }
    setShowResumeModal(false);
    try {
      const result = await generateResume.mutateAsync({
        resumeProfileId: selectedResumeId,
        jobApplicationId: job.id,
      });
      router.push(`/generated/${result.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao gerar curriculo';
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

  if (!job) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-500">Vaga nao encontrada.</p>
        <Link href="/jobs">
          <Button variant="outline" className="mt-4">
            Voltar
          </Button>
        </Link>
      </div>
    );
  }

  const generatedList = generatedData?.data ?? [];
  const resumes = resumesData?.data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/jobs">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-2xl font-semibold text-slate-900">{job.jobTitle}</h1>
          <p className="text-slate-500 text-sm">{job.companyName}</p>
        </div>
        <Button onClick={handleOpenGenerate} isLoading={generateResume.isPending}>
          <Sparkles className="h-4 w-4" />
          Gerar Curriculo
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader>
              <h2 className="text-base font-semibold text-slate-900">Descricao da Vaga</h2>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed">
                {job.jobDescription}
              </p>
            </CardContent>
          </Card>

          {generatedList.length > 0 && (
            <Card>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <FileOutput className="h-4 w-4 text-slate-500" />
                  <h2 className="text-base font-semibold text-slate-900">
                    Curriculos Gerados ({generatedList.length})
                  </h2>
                </div>
              </CardHeader>
              <CardContent className="divide-y divide-slate-100">
                {generatedList.map((gen) => (
                  <div key={gen.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium text-slate-900">
                          v{gen.versionNumber}
                        </p>
                        {gen.adherenceScore != null && (
                          <Badge
                            variant={
                              gen.adherenceScore >= 71
                                ? 'success'
                                : gen.adherenceScore >= 41
                                ? 'warning'
                                : 'danger'
                            }
                          >
                            {gen.adherenceScore}% match
                          </Badge>
                        )}
                        {gen.isCurrent && <Badge variant="info">Atual</Badge>}
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {gen.aiProvider} · {formatDate(gen.createdAt, 'PPp')}
                      </p>
                    </div>
                    <Link href={`/generated/${gen.id}`}>
                      <Button variant="outline" size="sm">
                        Ver
                      </Button>
                    </Link>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <h2 className="text-base font-semibold text-slate-900">Detalhes</h2>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Status</p>
                <Badge className="mt-1">{statusLabels[job.status] ?? job.status}</Badge>
              </div>
              {job.jobLocation && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Localizacao</p>
                  <p className="text-sm text-slate-700 mt-0.5">{job.jobLocation}</p>
                </div>
              )}
              {job.jobType && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Tipo</p>
                  <p className="text-sm text-slate-700 mt-0.5">{job.jobType}</p>
                </div>
              )}
              {job.seniority && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Senioridade</p>
                  <p className="text-sm text-slate-700 mt-0.5">{job.seniority}</p>
                </div>
              )}
              {job.jobUrl && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">URL</p>
                  <a
                    href={job.jobUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-blue-600 hover:underline break-all"
                  >
                    {job.jobUrl}
                  </a>
                </div>
              )}
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Curriculos Gerados</p>
                <p className="text-sm text-slate-700 mt-0.5">{job.generatedCount}</p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Modal para selecionar currículo base */}
      <Modal
        open={showResumeModal}
        onClose={() => setShowResumeModal(false)}
        title="Selecionar Curriculo Base"
      >
        <div className="space-y-3">
          {resumes.length === 0 ? (
            <div className="text-center py-8 text-slate-400">
              <p>Nenhum curriculo cadastrado.</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-3"
                onClick={() => router.push('/resumes/new')}
              >
                Criar curriculo
              </Button>
            </div>
          ) : (
            resumes.map((resume) => (
              <label
                key={resume.id}
                className={`flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                  selectedResumeId === resume.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <input
                  type="radio"
                  name="resume"
                  value={resume.id}
                  checked={selectedResumeId === resume.id}
                  onChange={() => setSelectedResumeId(resume.id)}
                  className="accent-blue-600"
                />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium text-slate-900 truncate">{resume.title}</p>
                    {resume.isDefault && <Badge variant="warning">Padrao</Badge>}
                  </div>
                </div>
              </label>
            ))
          )}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button variant="outline" onClick={() => setShowResumeModal(false)}>
              Cancelar
            </Button>
            <Button onClick={handleGenerate} disabled={!selectedResumeId} isLoading={generateResume.isPending}>
              <Sparkles className="h-4 w-4" />
              Gerar Curriculo
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
