'use client';

import { useAuth } from '@/providers/AuthProvider';
import { useResumes } from '@/hooks/queries/useResumes';
import { useJobs } from '@/hooks/queries/useJobs';
import { useGeneratedResumes } from '@/hooks/queries/useGenerated';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { FileText, Briefcase, FileOutput, TrendingUp, Plus, Sparkles } from 'lucide-react';
import { formatDate } from '@/lib/utils';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';

export default function DashboardPage() {
  const { user } = useAuth();
  const { data: resumesData } = useResumes({ size: 1 });
  const { data: jobsData } = useJobs({ status: 'saved', size: 1 });
  const { data: generatedData } = useGeneratedResumes({ size: 5 });

  // Calculate average score from generated resumes
  const avgScore =
    generatedData?.data && generatedData.data.length > 0
      ? Math.round(
          generatedData.data
            .filter((g) => g.adherenceScore != null)
            .reduce((sum, g) => sum + (g.adherenceScore ?? 0), 0) /
            generatedData.data.filter((g) => g.adherenceScore != null).length || 0
        )
      : null;

  const stats = [
    {
      label: 'Curriculos',
      value: resumesData?.total ?? 0,
      icon: FileText,
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      label: 'Vagas Ativas',
      value: jobsData?.total ?? 0,
      icon: Briefcase,
      color: 'text-green-600',
      bg: 'bg-green-50',
    },
    {
      label: 'Gerados',
      value: generatedData?.total ?? 0,
      icon: FileOutput,
      color: 'text-purple-600',
      bg: 'bg-purple-50',
    },
 ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          Ola, {user?.name?.split(' ')[0]}
        </h1>
        <p className="text-slate-500 mt-1">Resumo da sua atividade no Resume Forge</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {stats.map(({ label, value, icon: Icon, color, bg }) => (
          <Card key={label}>
            <CardContent className="flex items-center gap-4 py-5">
              <div className={`flex-shrink-0 h-10 w-10 rounded-lg ${bg} flex items-center justify-center`}>
                <Icon className={`h-5 w-5 ${color}`} />
              </div>
              <div>
                <p className="text-2xl font-semibold text-slate-900">{value}</p>
                <p className="text-sm text-slate-500">{label}</p>
              </div>
            </CardContent>
          </Card>
        ))}

        {avgScore != null && (
          <Card>
            <CardContent className="flex items-center gap-4 py-5">
              <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-amber-50 flex items-center justify-center">
                <TrendingUp className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <p className="text-2xl font-semibold text-slate-900">{avgScore}%</p>
                <p className="text-sm text-slate-500">Score medio</p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Quick actions */}
      <div className="flex gap-3">
        <Link href="/resumes/new">
          <Button variant="outline" size="sm">
            <Plus className="h-4 w-4" />
            Novo Curriculo
          </Button>
        </Link>
        <Link href="/jobs/new">
          <Button variant="outline" size="sm">
            <Briefcase className="h-4 w-4" />
            Nova Vaga
          </Button>
        </Link>
      </div>

      {generatedData?.data && generatedData.data.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-slate-500" />
              <h2 className="text-base font-semibold text-slate-900">Ultimos gerados</h2>
            </div>
          </CardHeader>
          <CardContent className="divide-y divide-slate-100">
            {generatedData.data.map((item) => (
              <div key={item.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <p className="text-sm font-medium text-slate-900">
                    {item.jobTitle ?? 'Sem titulo'} @ {item.companyName ?? 'Sem empresa'}
                  </p>
                  <p className="text-xs text-slate-500">
                    v{item.versionNumber} · {formatDate(item.createdAt, 'PPp')}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {item.adherenceScore != null && (
                    <Badge variant={item.adherenceScore >= 70 ? 'success' : 'warning'}>
                      {item.adherenceScore}% match
                    </Badge>
                  )}
                  <Badge variant={item.isCurrent ? 'info' : 'muted'}>
                    {item.isCurrent ? 'Atual' : `v${item.versionNumber}`}
                  </Badge>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {(!generatedData?.data || generatedData.data.length === 0) && (
        <Card>
          <CardContent className="py-12 text-center">
            <FileOutput className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500">Nenhum curriculo gerado ainda.</p>
            <p className="text-sm text-slate-400 mt-1">
              Cadastre uma vaga e um curriculo base para comecar.
            </p>
            <Link href="/jobs/new">
              <Button className="mt-4" size="sm">
                <Sparkles className="h-4 w-4" />
                Gerar primeiro curriculo
              </Button>
            </Link>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
