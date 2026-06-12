'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';
import {
  LayoutDashboard,
  FileText,
  Briefcase,
  FileOutput,
  LogOut,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/resumes', label: 'Curriculos', icon: FileText },
  { href: '/jobs', label: 'Vagas', icon: Briefcase },
  { href: '/generated', label: 'Gerados', icon: FileOutput },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="flex flex-col w-64 min-h-screen bg-white border-r border-slate-200">
      <div className="px-6 py-5 border-b border-slate-100">
        <Link href="/dashboard" className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-blue-600 flex items-center justify-center">
            <FileText className="h-4 w-4 text-white" />
          </div>
          <span className="font-semibold text-slate-900">Resume Forge</span>
        </Link>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ href, label, icon: Icon }) => {
          const isActive = pathname === href || pathname.startsWith(href + '/');
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              )}
            >
              <Icon className="h-4 w-4" />
              {label}
              {isActive && <ChevronRight className="ml-auto h-3 w-3" />}
            </Link>
          );
        })}
      </nav>

      <div className="px-3 py-4 border-t border-slate-100">
        <div className="px-3 py-2 mb-2">
          <p className="text-sm font-medium text-slate-900 truncate">{user?.name}</p>
          <p className="text-xs text-slate-500 truncate">{user?.email}</p>
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors"
        >
          <LogOut className="h-4 w-4" />
          Sair
        </button>
      </div>
    </aside>
  );
}
