'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useResumes } from '@/hooks/queries/useResumes';
import { useCreateJob } from '@/hooks/queries/useJobs';
import { generationApi } from '@/lib/api';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ArrowLeft, ArrowRight, Check, Sparkles } from 'lucide-react';
import { toast } from 'sonner';

interface GenerationWizardProps {
  onCancel?: () => void;
}

export function GenerationWizard({ onCancel }: GenerationWizardProps) {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [jobId, setJobId] = useState<string | null>(null);
  const [selectedResumeId, setSelectedResumeId] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const [jobForm, setJobForm] = useState({
    companyName: '',
    jobTitle: '',
    jobDescription: '',
    jobUrl: '',
    jobLocation: '',
    jobType: '',
    seniority: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: resumesData, isLoading: resumesLoading } = useResumes({ size: 100 });
  const createJob = useCreateJob();

  const validateStep1 = () => {
    const newErrors: Record<string, string> = {};
    if (!jobForm.companyName.trim()) newErrors.companyName = 'Empresa obrigatoria';
    if (!jobForm.jobTitle.trim()) newErrors.jobTitle = 'Titulo obrigatorio';
    if (jobForm.jobDescription.length < 100) newErrors.jobDescription = 'Minimo 100 caracteres';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleStep1Next = () => {
    if (validateStep1()) setStep(2);
  };

  const handleCreateJobAndNext = async () => {
    if (!validateStep1()) return;
    try {
      const job = await createJob.mutateAsync({
        companyName: jobForm.companyName,
        jobTitle: jobForm.jobTitle,
        jobDescription: jobForm.jobDescription,
        jobUrl: jobForm.jobUrl || undefined,
        jobLocation: jobForm.jobLocation || undefined,
        jobType: jobForm.jobType || undefined,
        seniority: jobForm.seniority || undefined,
      });
      setJobId(job.id);
      setStep(2);
    } catch {
      toast.error('Erro ao criar vaga');
    }
  };

  const handleGenerate = async () => {
    if (!selectedResumeId || !jobId) {
      toast.error('Selecione um curriculo base');
      return;
    }
    setIsGenerating(true);
    try {
      const result = await generationApi.generate({
        resumeProfileId: selectedResumeId,
        jobApplicationId: jobId,
      });
      router.push(`/generated/${result.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao gerar curriculo';
      toast.error(msg);
    } finally {
      setIsGenerating(false);
    }
  };

  const resumes = resumesData?.data ?? [];

  return (
<div className="space-y-6">
      {/* Step indicator */}
      <div className="flex items-center gap-0">
        {[1, 2, 3].map((s) => (
          <div key={s} className="flex items-center">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-colors ${
                s< step
                  ? 'bg-blue-600 text-white'
                  : s === step
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-100 text-slate-400'
              }`}
            >
              {s < step ? <Check className="h-4 w-4" /> : s}
            </div>
            {s < 3 && (
              <div
                className={`h-0.5 w-12 sm:w-24 ${
                  s < step ? 'bg-blue-600' : 'bg-slate-100'
                }`}
              />
            )}
          </div>
        ))}
      </div>

      {/* Step 1: Job details */}
      {step === 1 && (
        <Card>
          <CardHeader>
            <h2 className="text-lg font-semibold text-slate-900">Dados da Vaga</h2>
            <p className="text-sm text-slate-500">Informe os detalhes da vaga de emprego</p>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Titulo da Vaga"
                placeholder="Ex: Desenvolvedor Java Senior"
                value={jobForm.jobTitle}
                onChange={(e) => setJobForm((p) => ({ ...p, jobTitle: e.target.value }))}
                error={errors.jobTitle}
              />
              <Input
                label="Empresa"
                placeholder="Ex: Acme Corp"
                value={jobForm.companyName}
                onChange={(e) => setJobForm((p) => ({ ...p, companyName: e.target.value }))}
                error={errors.companyName}
              />
            </div>
            <Textarea
              label="Descricao da Vaga"
              placeholder="Cole aqui a descricao completa da vaga (requisitos, responsabilidades, beneficios...)"
              value={jobForm.jobDescription}
              onChange={(e) => setJobForm((p) => ({ ...p, jobDescription: e.target.value }))}
              error={errors.jobDescription}
              hint="Minimo 100 caracteres. Quanto mais detalhes, melhor a otimizacao."
              className="min-h-[300px] text-sm"
            />
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Input
                label="URL da Vaga"
                placeholder="https://..."
                value={jobForm.jobUrl}
                onChange={(e) => setJobForm((p) => ({ ...p, jobUrl: e.target.value }))}
              />
              <Input
                label="Localizacao"
                placeholder="Ex: Remoto, Sao Paulo"
                value={jobForm.jobLocation}
                onChange={(e) => setJobForm((p) => ({ ...p, jobLocation: e.target.value }))}
              />
              <Input
                label="Tipo"
                placeholder="Ex: fulltime, remote"
                value={jobForm.jobType}
                onChange={(e) => setJobForm((p) => ({ ...p, jobType: e.target.value }))}
              />
            </div>
            <div className="flex justify-end gap-3">
              {onCancel && (
                <Button variant="outline" onClick={onCancel}>
                  Cancelar
                </Button>
              )}
              <Button onClick={handleCreateJobAndNext} isLoading={createJob.isPending}>
                Salvar e Continuar
                <ArrowRight className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Step 2: Select resume */}
      {step === 2 && (
        <Card>
          <CardHeader>
            <h2 className="text-lg font-semibold text-slate-900">Selecionar Curriculo Base</h2>
            <p className="text-sm text-slate-500">Escolha o curriculo que sera adaptado para esta vaga</p>
          </CardHeader>
          <CardContent className="space-y-3">
            {resumesLoading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-16 rounded-lg bg-slate-100 animate-pulse" />
                ))}
              </div>
            ) : resumes.length === 0 ? (
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
            <div className="flex justify-between pt-2">
              <Button variant="ghost" onClick={() => setStep(1)}>
                <ArrowLeft className="h-4 w-4" />
                Voltar
              </Button>
              <Button onClick={() => setStep(3)} disabled={!selectedResumeId}>
                Continuar
                <ArrowRight className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Step 3: Review and generate */}
      {step === 3 && (
        <Card>
          <CardHeader>
            <h2 className="text-lg font-semibold text-slate-900">Revisao e Geracao</h2>
            <p className="text-sm text-slate-500">Revise as informacoes antes de gerar o curriculo</p>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Vaga</p>
                <p className="font-medium text-slate-900">{jobForm.jobTitle}</p>
                <p className="text-slate-600">{jobForm.companyName}</p>
              </div>
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Curriculo Base</p>
                <p className="font-medium text-slate-900">
                  {resumes.find((r) => r.id === selectedResumeId)?.title ?? 'N/A'}
                </p>
              </div>
            </div>
            <div className="flex justify-between">
              <Button variant="ghost" onClick={() => setStep(2)}>
                <ArrowLeft className="h-4 w-4" />
                Voltar
              </Button>
              <Button onClick={handleGenerate} isLoading={isGenerating}>
                {isGenerating ? (
                  <Spinner size="xs" />
                ) : (
                  <Sparkles className="h-4 w-4" />
                )}
                Gerar Curriculo
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
