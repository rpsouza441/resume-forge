'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useGeneratedResumes } from '@/hooks/queries/useGenerated';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Input } from '@/components/ui/Input';
import { DocxDownloadButton } from '@/components/shared/DocxDownloadButton';
import { FileOutput, RefreshCw, Edit, Search, X } from 'lucide-react';
import { formatDate } from '@/lib/utils';
import { Skeleton } from '@/components/ui/Skeleton';

export default function GeneratedPage() {
  const [filters, setFilters] = useState({
    companyName: '',
    jobTitle: '',
    dateFrom: '',
    dateTo: '',
  });
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading } = useGeneratedResumes({
    ...filters,
    page,
    size: 20,
  });

  const clearFilters = () => {
    setFilters({ companyName: '', jobTitle: '', dateFrom: '', dateTo: '' });
    setPage(0);
  };

  const hasFilters = Object.values(filters).some(Boolean);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Curriculos Gerados</h1>
          <p className="text-slate-500 text-sm mt-1">
            {data?.total ?? 0} geracao(oes) registrada(s)
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowFilters((v) => !v)}
          >
            <Search className="h-4 w-4" />
            Filtros
 {hasFilters && (
              <span className="ml-1 h-2 w-2 rounded-full bg-blue-600" />
            )}
          </Button>
        </div>
      </div>

      {showFilters && (
        <Card>
          <CardContent className="py-4">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
              <Input
                label="Empresa"
                placeholder="Filtrar por empresa"
                value={filters.companyName}
                onChange={(e) => {
                  setFilters((f) => ({ ...f, companyName: e.target.value }));
                  setPage(0);
                }}
              />
              <Input
                label="Titulo da Vaga"
                placeholder="Filtrar por titulo"
                value={filters.jobTitle}
                onChange={(e) => {
                  setFilters((f) => ({ ...f, jobTitle: e.target.value }));
                  setPage(0);
                }}
              />
              <Input
                label="Data de"
                type="date"
                value={filters.dateFrom}
                onChange={(e) => {
                  setFilters((f) => ({ ...f, dateFrom: e.target.value }));
                  setPage(0);
                }}
              />
              <Input
                label="Data ate"
                type="date"
                value={filters.dateTo}
                onChange={(e) => {
                  setFilters((f) => ({ ...f, dateTo: e.target.value }));
                  setPage(0);
                }}
              />
            </div>
            {hasFilters && (
              <button
                onClick={clearFilters}
                className="mt-3 flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700"
              >
                <X className="h-3 w-3" />
                Limpar filtros
              </button>
            )}
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => <Skeleton key={i} variant="card" />)}
        </div>
      ) : data?.data.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileOutput className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-500">Nenhum curriculo gerado ainda.</p>
            <p className="text-sm text-slate-400 mt-1">
              Cadastre uma vaga e um curriculo base para comecar.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {data?.data.map((item) => (
            <Card key={item.id}>
              <CardContent className="py-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-medium text-slate-900 truncate">
                        {item.jobTitle ?? 'Sem titulo'}
                      </p>
                      <Badge variant={item.isCurrent ? 'info' : 'muted'}>
                        v{item.versionNumber}
                      </Badge>
                      {item.isCurrent && <Badge variant="success">Atual</Badge>}
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {item.companyName ?? 'Sem empresa'} · {item.aiProvider} ·{' '}
                      {formatDate(item.createdAt, 'PPp')}
                    </p>
                    {item.adherenceScore != null && (
                      <div className="flex items-center gap-1 mt-2">
                        <div className="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden max-w-[200px]">
                          <div
                            className={`h-full rounded-full ${
                              item.adherenceScore >= 70
                                ? 'bg-green-500'
                                : item.adherenceScore >= 41
                                ? 'bg-amber-500'
                                : 'bg-red-500'
                            }`}
                            style={{ width: `${item.adherenceScore}%` }}
                          />
                        </div>
                        <span className="text-xs text-slate-500 font-medium">
                          {item.adherenceScore}%
                        </span>
                      </div>
                    )}
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <DocxDownloadButton
                      generatedResumeId={item.id}
                      className="hidden sm:flex"
                    />
                    <Link href={`/generated/${item.id}`}>
                      <Button variant="outline" size="sm">
                        <Edit className="h-4 w-4" />
                        Ver
                      </Button>
                    </Link>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-slate-500">
            Pagina {data.page + 1} de {data.totalPages} ({data.total} total)
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Proxima
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
