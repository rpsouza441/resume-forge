'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Clock, Bot, User } from 'lucide-react';
import { formatDate } from '@/lib/utils';

interface VersionHistoryProps {
  versions: Array<{
    id: string;
    versionNumber: number;
    isCurrent: boolean;
    generationReason?: string | null;
    aiProvider?: string;
    aiModel?: string;
    createdAt: string;
  }>;
  className?: string;
}

export function VersionHistory({ versions, className }: VersionHistoryProps) {
  if (!versions || versions.length === 0) {
    return (
      <Card className={className}>
        <CardContent className="py-8 text-center text-slate-400 text-sm">
          Nenhuma versao disponivel
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <h3 className="text-base font-semibold text-slate-900">Historico de Versoes</h3>
      </CardHeader>
      <CardContent className="divide-y divide-slate-100">
        {versions.map((v) => (
          <div key={v.id} className="flex items-start justify-between py-3 first:pt-0 last:pb-0">
            <div className="flex items-start gap-3">
              <Clock className="h-4 w-4 text-slate-400 mt-0.5 flex-shrink-0" />
              <div>
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-slate-900">
                    Versao {v.versionNumber}
                  </p>
                  {v.isCurrent && <Badge variant="info">Atual</Badge>}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  {formatDate(v.createdAt, 'PPp')}
                </p>
                {v.generationReason && (
                  <p className="text-xs text-slate-400 mt-0.5">{v.generationReason}</p>
                )}
              </div>
            </div>
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              {v.aiProvider ? (
                <>
                  <Bot className="h-3 w-3" />
                  <span>{v.aiProvider}</span>
                </>
              ) : (
                <>
                  <User className="h-3 w-3" />
                  <span>Manual</span>
                </>
              )}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
