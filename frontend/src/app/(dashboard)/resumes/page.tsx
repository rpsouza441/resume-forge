'use client';

import Link from 'next/link';
import { useResumes, useDeleteResume, useSetDefaultResume } from '@/hooks/queries/useResumes';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Plus, Star, Trash2, FileText } from 'lucide-react';
import { useState } from 'react';
import { timeAgo } from '@/lib/utils';
import { toast } from 'sonner';

export default function ResumesPage() {
  const { data, isLoading } = useResumes();
  const deleteResume = useDeleteResume();
  const setDefault = useSetDefaultResume();
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      await deleteResume.mutateAsync(deleteId);
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
          <h1 className="text-2xl font-semibold text-slate-900">Curriculos Base</h1>
          <p className="text-slate-500 text-sm mt-1">
            {data?.total ?? 0} curriculo(s) cadastrado(s)
          </p>
        </div>
        <Link href="/resumes/new">
          <Button>
            <Plus className="h-4 w-4" />
            Novo Curriculo
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
            <FileText className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500">Nenhum curriculo cadastrado.</p>
            <Link href="/resumes/new">
              <Button className="mt-4" size="sm">
                Criar primeiro curriculo
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {data?.data.map((resume) => (
            <Card key={resume.id}>
              <CardContent className="flex items-center justify-between py-4">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="flex-shrink-0">
                    {resume.isDefault ? (
                      <Star className="h-5 w-5 text-amber-500 fill-amber-500" />
                    ) : (
                      <FileText className="h-5 w-5 text-slate-400" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-medium text-slate-900 truncate">{resume.title}</p>
                      {resume.isDefault && <Badge variant="warning">Padrao</Badge>}
                    </div>
                    <p className="text-xs text-slate-500">
                      Atualizado {timeAgo(resume.updatedAt)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {!resume.isDefault && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDefault.mutate(resume.id)}
                      disabled={setDefault.isPending}
                    >
                      <Star className="h-4 w-4" />
                      Padrao
                    </Button>
                  )}
                  <Link href={`/resumes/${resume.id}/edit`}>
                    <Button variant="outline" size="sm">
                      Editar
                    </Button>
                  </Link>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDeleteId(resume.id)}
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
        title="Excluir Curriculo"
      >
        <p className="text-slate-600 mb-6">
          Tem certeza que deseja excluir este curriculo? Esta acao nao pode ser desfeita.
        </p>
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={() => setDeleteId(null)}>
            Cancelar
          </Button>
          <Button
            variant="danger"
            isLoading={deleteResume.isPending}
            onClick={handleDelete}
          >
            Excluir
          </Button>
        </div>
      </Modal>
    </div>
  );
}
