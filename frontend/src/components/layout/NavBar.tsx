'use client';

import Link from 'next/link';
import { useAuth } from '@/providers/AuthProvider';
import { FileText, LogOut } from 'lucide-react';

export function NavBar() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-40 flex items-center justify-between px-6 py-3 bg-white border-b border-slate-200">
      <Link href="/dashboard" className="flex items-center gap-2">
        <div className="h-8 w-8 rounded-lg bg-blue-600 flex items-center justify-center">
          <FileText className="h-4 w-4 text-white" />
        </div>
        <span className="font-semibold text-slate-900">Resume Forge</span>
      </Link>

      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium text-slate-900 leading-none">{user?.name}</p>
          <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-slate-600 hover:bg-slate-100 transition-colors"
          title="Sair"
        >
          <LogOut className="h-4 w-4" />
        </button>
      </div>
    </header>
  );
}
