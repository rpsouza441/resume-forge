'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';
import { loginSchema, type LoginFormData } from '@/schemas/zod/auth.schemas';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import Link from 'next/link';
import { toast } from 'sonner';
import axios from 'axios';

export function LoginForm() {
  const { login } = useAuth();
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data.email, data.password);
      router.push('/dashboard');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const msg =
          err.response?.data?.message ??
          err.response?.data?.error ??
          'Login falhou. Verifique suas credenciais.';
        toast.error(msg);
      } else {
        toast.error('Login falhou. Verifique suas credenciais.');
      }
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <h1 className="text-xl font-semibold text-slate-900">Entrar</h1>
        <p className="text-sm text-slate-500 mt-1">
          Nao tem conta?{' '}
          <Link href="/register" className="text-blue-600 hover:underline">
            Cadastre-se
          </Link>
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <Input
            label="Senha"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            Entrar
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
