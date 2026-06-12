'use client';

import { cn } from '@/lib/utils';

interface AdherenceScoreProps {
  score: number;
  showLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function AdherenceScore({ score, showLabel = true, size = 'md', className }: AdherenceScoreProps) {
  const color =
    score >= 71 ? 'text-green-600 bg-green-50' :
    score >= 41 ? 'text-amber-600 bg-amber-50' :
    'text-red-600 bg-red-50';

  const barColor =
    score >= 71 ? 'bg-green-500' :
    score >= 41 ? 'bg-amber-500' :
    'bg-red-500';

  const label =
    score >= 71 ? 'Excelente' :
    score >= 41 ? 'Regular' :
    'Baixo';

  const sizeClasses = {
    sm: { bar: 'h-1.5', text: 'text-xs', badge: 'text-xs px-2 py-0.5' },
    md: { bar: 'h-2', text: 'text-sm', badge: 'text-sm px-2.5 py-1' },
    lg: { bar: 'h-3', text: 'text-base', badge: 'text-base px-3 py-1.5' },
  };

  const s = sizeClasses[size];

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <div className="flex items-center gap-2">
        <div className={cn('flex-1 rounded-full bg-slate-100 overflow-hidden', s.bar)}>
          <div
            className={cn('h-full rounded-full transition-all', barColor)}
            style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
          />
        </div>
        {showLabel && (
          <div className="flex items-center gap-1.5">
            <span className={cn('font-semibold', s.text, color.split(' ')[0])}>
              {score}%
            </span>
            <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', color, s.badge.split(' ')[0])}>
              {label}
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
