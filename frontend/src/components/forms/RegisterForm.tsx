'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';
import { registerSchema, type RegisterFormData } from '@/schemas/zod/auth.schemas';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import Link from 'next/link';
import { toast } from 'sonner';
import axios from 'axios';

export function RegisterForm() {
  const { register: doRegister } = useAuth();
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '' },
  });

  const onSubmit = async (data: RegisterFormData) => {
    try {
      await doRegister(data.name, data.email, data.password);
      toast.success('Conta criada! Bem-vindo ao Resume Forge.');
      router.push('/dashboard');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const msg =
          err.response?.data?.message ??
          err.response?.data?.error ??
          'Cadastro falhou. Tente novamente.';
        toast.error(msg);
      } else {
        toast.error('Cadastro falhou. Tente novamente.');
      }
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <h1 className="text-xl font-semibold text-slate-900">Criar conta</h1>
        <p className="text-sm text-slate-500 mt-1">
          Ja tem conta?{' '}
          <Link href="/login" className="text-blue-600 hover:underline">
            Faca login
          </Link>
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="Nome completo"
            autoComplete="name"
            error={errors.name?.message}
            {...register('name')}
          />
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
            autoComplete="new-password"
            hint="Minimo 8 caracteres"
            error={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            Cadastrar
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
