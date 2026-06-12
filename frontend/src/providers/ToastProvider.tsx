'use client';

import { Toaster } from 'sonner';

/**
 * Toast provider using Sonner.
 * Scaffold only — toast styling to be refined per SPEC-04.
 */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  return (
    <>
      {children}
      <Toaster
        position="top-right"
        richColors
        closeButton
        toastOptions={{
          style: {
            fontFamily: 'Inter, system-ui, sans-serif',
          },
        }}
      />
    </>
  );
}

/**
 * Convenience wrappers around Sonner's toast.
 * Import directly from 'sonner' in components for full API.
 */
export { toast } from 'sonner';
