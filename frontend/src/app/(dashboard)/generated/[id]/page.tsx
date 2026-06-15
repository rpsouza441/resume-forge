'use client';

import { useState } from 'react';
import { useGeneratedResume, useRegenerate, useVersions } from '@/hooks/queries/useGenerated';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { ArrowLeft, RefreshCw, Edit } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { formatDate } from '@/lib/utils';
import { toast } from 'sonner';
import { DocxDownloadButton } from '@/components/shared/DocxDownloadButton';
import { AnalysisPanel } from '@/components/shared/AnalysisPanel';
import { VersionHistory } from '@/components/shared/VersionHistory';
import { Spinner } from '@/components/ui/Spinner';
import { StructuredResumePreview } from '@/components/generated/StructuredResumePreview';

type Tab = 'curriculo' | 'analise' | 'historico';

export default function GeneratedDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<Tab>('curriculo');
  const { data: resume, isLoading } = useGeneratedResume(params.id);
  const { data: versions } = useVersions(params.id);
  const regenerate = useRegenerate();

  const handleRegenerate = async () => {
    try {
      const result = await regenerate.mutateAsync(params.id);
      router.push(`/generated/${result.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao regenerar';
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

  if (!resume) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-500">Curriculo nao encontrado.</p>
        <Link href="/generated">
          <Button variant="outline" className="mt-4">
            Voltar
          </Button>
        </Link>
      </div>
    );
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: 'curriculo', label: 'Curriculo' },
    { key: 'analise', label: 'Analise' },
    { key: 'historico', label: 'Historico' },
  ];

  return (
<div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
<div className="flex items-center gap-3">
          <Link href="/generated">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-semibold text-slate-900">
                {resume.jobTitle ?? 'Curriculo Gerado'}
              </h1>
              {resume.adherenceScore != null && (
                <Badge
                  variant={
                    resume.adherenceScore >= 71
                      ? 'success'
                      : resume.adherenceScore >= 41
                      ? 'warning'
                      : 'danger'
                  }
                >
                  {resume.adherenceScore}% match
                </Badge>
              )}
            </div>
            <p className="text-slate-500 text-sm">
              {resume.companyName ?? ''} · v{resume.versionNumber} ·{' '}
              {formatDate(resume.createdAt, 'PPp')}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={handleRegenerate}
            isLoading={regenerate.isPending}
          >
            <RefreshCw className="h-4 w-4" />
            Regenerar
          </Button>
          <Link href={`/generated/${params.id}/edit`}>
            <Button variant="outline">
              <Edit className="h-4 w-4" />
              Editar
            </Button>
          </Link>
          <DocxDownloadButton generatedResumeId={params.id} />
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200">
        <nav className="flex gap-1">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                activeTab === tab.key
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab content */}
      {activeTab === 'curriculo' && (
        <Card>
          <CardContent className="py-6">
            <StructuredResumePreview
              // Handle both flat structure and nested "sections" structure
              // AI prompt returns: optimized_resume.sections.experience, etc.
              // But frontend expects: optimized_resume.experience at root level
              resume={resume.contentJsonb?.optimized_resume?.sections ?? resume.contentJsonb?.optimized_resume}
              header={resume.header}
            />
          </CardContent>
        </Card>
      )}

      {activeTab === 'analise' && (
        <div className="space-y-4">
          <AnalysisPanel analysis={resume.analysis} />
          <Card>
            <CardHeader>
              <h3 className="text-base font-semibold text-slate-900">Metadados</h3>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Provedor IA</p>
                <p className="text-sm text-slate-700">{resume.aiProvider}</p>
              </div>
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Modelo</p>
                <p className="text-sm text-slate-700">{resume.aiModel}</p>
              </div>
              {resume.wordCount != null && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Palavras</p>
                  <p className="text-sm text-slate-700">{resume.wordCount}</p>
                </div>
              )}
              {resume.charCount != null && (
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wide">Caracteres</p>
                  <p className="text-sm text-slate-700">{resume.charCount}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'historico' && (
        <VersionHistory versions={versions ?? []} />
      )}
    </div>
  );
}
