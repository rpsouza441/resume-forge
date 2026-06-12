'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/Button';
import { Spinner } from '@/components/ui/Spinner';
import { Download } from 'lucide-react';
import { toast } from 'sonner';

type DownloadState = 'idle' | 'loading' | 'error';

interface DocxDownloadButtonProps {
  generatedResumeId: string;
  filename?: string;
  className?: string;
}

export function DocxDownloadButton({
  generatedResumeId,
  filename,
  className,
}: DocxDownloadButtonProps) {
  const [state, setState] = useState<DownloadState>('idle');

  const handleDownload = async () => {
    setState('loading');
    try {
      const res = await api.get(`/generated/${generatedResumeId}/docx`, {
        responseType: 'blob',
      });

      const blob = new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename ?? `curriculo-${generatedResumeId}.docx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setState('idle');
    } catch {
      setState('error');
      toast.error('Falha ao gerar DOCX. Tente novamente.');
 setState('idle');
    }
  };

  return (
    <Button
      onClick={handleDownload}
      disabled={state === 'loading'}
      isLoading={state === 'loading'}
      className={className}
    >
      {state === 'loading' ? <Spinner size="xs" /> : <Download className="h-4 w-4" />}
      Baixar DOCX
</Button>
  );
}
