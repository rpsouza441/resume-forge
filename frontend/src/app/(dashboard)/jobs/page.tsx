'use client';

import Link from 'next/link';
import { useJobs, useDeleteJob } from '@/hooks/queries/useJobs';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Plus, Briefcase, Trash2, ExternalLink } from 'lucide-react';
import { useState } from 'react';
import { timeAgo } from '@/lib/utils';
import { toast } from 'sonner';

const statusLabels: Record<string, string> = {
  saved: 'Salvo',
  applied: 'Aplicado',
  interviewing: 'Entrevista',
  offer: 'Oferta',
  rejected: 'Rejeitado',
  withdrawn: 'Desistiu',
  archived: 'Arquivado',
};

const statusVariants: Record<string, 'default' | 'success' | 'warning' | 'danger' | 'info' | 'muted'> = {
  saved: 'default',
  applied: 'info',
  interviewing: 'warning',
  offer: 'success',
  rejected: 'danger',
  withdrawn: 'muted',
  archived: 'muted',
};

export default function JobsPage() {
  const { data, isLoading } = useJobs();
  const deleteJob = useDeleteJob();
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await deleteJob.mutateAsync(deleteId);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Nao e possivel excluir';
      toast.error(msg);
    } finally {
      setDeleteId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Vagas</h1>
          <p className="text-slate-500 text-sm">{data?.total ?? 0} vaga(s) cadastrada(s)</p>
        </div>
        <Link href="/jobs/new">
          <Button>
            <Plus className="h-4 w-4" />
            Nova Vaga
          </Button>
        </Link>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-20 rounded-xl bg-slate-100 animate-pulse" />
          ))}
        </div>
      ) : data?.data.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Briefcase className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500">Nenhuma vaga cadastrada.</p>
            <Link href="/jobs/new">
              <Button className="mt-4" size="sm">
                Cadastrar primeira vaga
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {data?.data.map((job) => (
            <Card key={job.id}>
              <CardContent className="flex items-center justify-between py-4">
                <div className="flex items-center gap-3 min-w-0">
                  <Briefcase className="h-5 w-5 text-slate-400 flex-shrink-0" />
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">
                      {job.jobTitle}
</p>
                    <p className="text-xs text-slate-500">{job.companyName}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <Badge variant={statusVariants[job.status] ?? 'default'}>
                    {statusLabels[job.status] ?? job.status}
                  </Badge>
                  {job.generatedCount > 0 && (
                    <Badge variant="info">{job.generatedCount} gerado(s)</Badge>
                  )}
                  <Link href={`/jobs/${job.id}`}>
                    <Button variant="outline" size="sm">
                      Ver
                    </Button>
                  </Link>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDeleteId(job.id)}
                    className="text-red-600 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Excluir Vaga"
      >
        <p className="text-slate-600 mb-6">
          Tem certeza que deseja excluir esta vaga? Curriculos gerados para ela serao mantidos.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={() => setDeleteId(null)}>
            Cancelar
          </Button>
          <Button
            variant="danger"
            isLoading={deleteJob.isPending}
            onClick={handleDelete}
          >
            Excluir
          </Button>
        </div>
      </Modal>
    </div>
  );
}
