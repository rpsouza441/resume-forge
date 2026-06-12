'use client';

import { useRouter } from 'next/navigation';
import { GenerationWizard } from '@/components/forms/GenerationWizard';
import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';

export default function NewJobPage() {
  const router = useRouter();

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/jobs">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Nova Vaga</h1>
          <p className="text-slate-500 text-sm">
            Cadastre uma vaga e gere um curriculo otimizado em3 etapas
          </p>
        </div>
      </div>

      <GenerationWizard onCancel={() => router.push('/jobs')} />
    </div>
  );
}
