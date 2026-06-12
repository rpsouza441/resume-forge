'use client';

import { useAuth } from '@/providers/AuthProvider';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * Root page: redirects to /login if unauthenticated, /dashboard if authenticated.
 * Scaffold only — auth logic will be implemented per SPEC-02.
 */
export default function HomePage() {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (isAuthenticated) {
        router.replace('/dashboard');
      } else {
        router.replace('/login');
      }
    }
  }, [isAuthenticated, isLoading, router]);

  return (
    <div className="flex h-screen items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-t-transparent" />
    </div>
  );
}
