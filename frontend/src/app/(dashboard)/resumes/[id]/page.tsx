'use client';

import { useResume } from '@/hooks/queries/useResumes';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { ArrowLeft, Edit } from 'lucide-react';
import Link from 'next/link';
import { formatDate } from '@/lib/utils';

export default function ResumeDetailPage({ params }: { params: { id: string } }) {
  const { data: resume, isLoading } = useResume(params.id);

  if (isLoading) {
    return <div className="h-64 animate-pulse bg-slate-100 rounded-xl" />;
  }

  if (!resume) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-500">Curriculo nao encontrado.</p>
        <Link href="/resumes">
          <Button variant="outline" className="mt-4">
            Voltar
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/resumes">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-semibold text-slate-900">{resume.title}</h1>
              {resume.isDefault && <Badge variant="warning">Padrao</Badge>}
            </div>
            <p className="text-slate-500 text-sm">
              Atualizado em {formatDate(resume.updatedAt, 'PPp')}
            </p>
          </div>
        </div>
        <Link href={`/resumes/${params.id}/edit`}>
          <Button>
            <Edit className="h-4 w-4" />
            Editar
          </Button>
        </Link>
      </div>

      <Card>
        <CardContent className="py-6">
          <pre className="whitespace-pre-wrap text-sm text-slate-700 font-sans">
            {resume.contentMarkdown}
          </pre>
        </CardContent>
      </Card>
    </div>
  );
}
