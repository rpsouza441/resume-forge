'use client';

import { type ReactNode } from 'react';
import { Sidebar } from '@/components/layout/Sidebar';
import { NavBar } from '@/components/layout/NavBar';

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <NavBar />
        <main className="flex-1 overflow-auto">
          <div className="max-w-5xl mx-auto px-8 py-8">{children}</div>
        </main>
      </div>
    </div>
  );
}
