import { cn } from '@/lib/utils';

interface SkeletonProps {
  variant?: 'text' | 'card' | 'table-row';
  className?: string;
}

export function Skeleton({ variant = 'text', className }: SkeletonProps) {
  if (variant === 'card') {
    return<div className={cn('h-20 rounded-xl bg-slate-100 animate-pulse', className)} />;
  }
  if (variant === 'table-row') {
    return (
      <div className={cn('h-14 rounded-lg bg-slate-100 animate-pulse', className)} />
    );
  }
  return <div className={cn('h-4 rounded bg-slate-100 animate-pulse', className)} />;
}
