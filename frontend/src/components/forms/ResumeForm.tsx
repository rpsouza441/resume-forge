'use client';

import { useForm, useFieldArray, type UseFormRegister } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { resumeSchema } from '@/schemas/zod/resume.schemas';
import { Button } from '@/components/ui/Button';
import { Input, Textarea } from '@/components/ui/Input';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Plus, Trash2 } from 'lucide-react';
import { z } from 'zod';

// Extended schema for structured form (includes array fields)
const structuredSchema = resumeSchema.extend({
  personalName: z.string().optional(),
  personalEmail: z.string().optional(),
  personalPhone: z.string().optional(),
  personalLocation: z.string().optional(),
  personalLinkedIn: z.string().optional(),
  personalGithub: z.string().optional(),
  summary: z.string().optional(),
  experiences: z.array(z.object({
    id: z.string(),
    company: z.string(),
    role: z.string(),
    period: z.string(),
    description: z.string(),
    achievements: z.string(),
  })).optional(),
  education: z.array(z.object({
    id: z.string(),
    institution: z.string(),
    degree: z.string(),
    period: z.string(),
  })).optional(),
  skills: z.array(z.object({
    id: z.string(),
    value: z.string(),
  })).optional(),
  certifications: z.array(z.object({
    id: z.string(),
    value: z.string(),
  })).optional(),
});

type StructuredFormData = z.infer<typeof structuredSchema>;

interface ResumeFormProps {
  defaultValues?: Partial<z.infer<typeof resumeSchema>>;
  onSubmit: (data: z.infer<typeof resumeSchema>) => Promise<void>;
  isSubmitting?: boolean;
  submitLabel?: string;
}

export function ResumeForm({
  defaultValues,
  onSubmit,
  isSubmitting,
  submitLabel = 'Salvar',
}: ResumeFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
  } = useForm<StructuredFormData>({
    resolver: zodResolver(structuredSchema),
    defaultValues: {
      title: defaultValues?.title ?? '',
      contentMarkdown: defaultValues?.contentMarkdown ?? '',
      isDefault: defaultValues?.isDefault ?? false,
      personalName: '',
      personalEmail: '',
      personalPhone: '',
      personalLocation: '',
      personalLinkedIn: '',
      personalGithub: '',
      summary: '',
      experiences: [],
      education: [],
      skills: [],
      certifications: [],
    },
  });

  const {
    fields: expFields,
    append: appendExp,
    remove: removeExp,
  } = useFieldArray({ control, name: 'experiences' });

  const {
    fields: eduFields,
    append: appendEdu,
    remove: removeEdu,
  } = useFieldArray({ control, name: 'education' });

  const {
    fields: skillFields,
    append: appendSkill,
    remove: removeSkill,
  } = useFieldArray({ control, name: 'skills' });

  const {
    fields: certFields,
    append: appendCert,
    remove: removeCert,
  } = useFieldArray({ control, name: 'certifications' });

  const buildMarkdown = (data: StructuredFormData): string => {
    let markdown = `# ${data.personalName || 'Nome'}\n\n`;
    if (data.personalEmail) markdown += `**Email:** ${data.personalEmail}\n`;
    if (data.personalPhone) markdown += `**Telefone:** ${data.personalPhone}\n`;
    if (data.personalLocation) markdown += `**Localizacao:** ${data.personalLocation}\n`;
    if (data.personalLinkedIn) markdown += `**LinkedIn:** ${data.personalLinkedIn}\n`;
    if (data.personalGithub) markdown += `**GitHub:** ${data.personalGithub}\n`;
    markdown += '\n';

    if (data.summary) {
      markdown += `## Resumo Profissional\n${data.summary}\n\n`;
    }

    if (data.experiences && data.experiences.length > 0) {
      markdown += `## Experiencia Profissional\n\n`;
      data.experiences.forEach((exp) => {
        if (!exp.company && !exp.role) return;
        markdown += `### ${exp.company || 'Empresa'} — ${exp.role || 'Cargo'}\n`;
        if (exp.period) markdown += `*${exp.period}*\n`;
        if (exp.description) markdown += `${exp.description}\n`;
        if (exp.achievements) markdown += `${exp.achievements}\n`;
        markdown += '\n';
      });
    }

    if (data.education && data.education.length > 0) {
      markdown += `## Formacao Academica\n\n`;
      data.education.forEach((edu) => {
        if (!edu.institution && !edu.degree) return;
        markdown += `- **${edu.institution || 'Instituicao'}** — ${edu.degree || 'Curso'}`;
        if (edu.period) markdown += ` (${edu.period})`;
        markdown += '\n';
      });
      markdown += '\n';
    }

    if (data.skills && data.skills.length > 0) {
      const skills = data.skills.map((s) => s.value).filter(Boolean);
      if (skills.length > 0) {
        markdown += `## Habilidades\n${skills.map((s) => `- ${s}`).join('\n')}\n\n`;
      }
    }

    if (data.certifications && data.certifications.length > 0) {
      const certs = data.certifications.map((c) => c.value).filter(Boolean);
      if (certs.length > 0) {
        markdown += `## Certificacoes\n${certs.map((c) => `- ${c}`).join('\n')}\n`;
      }
    }

    return markdown;
  };

  const handleFormSubmit = async (data: StructuredFormData) => {
    const markdown = buildMarkdown(data);
    await onSubmit({
      title: data.title,
      contentMarkdown: markdown,
      isDefault: data.isDefault,
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-slate-900">Dados Pessoais</h2>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Nome Completo"
              placeholder="Seu nome"
              {...register('personalName')}
            />
            <Input
              label="Email"
              type="email"
              placeholder="seu@email.com"
              {...register('personalEmail')}
            />
            <Input
              label="Telefone"
              placeholder="(11) 99999-9999"
              {...register('personalPhone')}
            />
            <Input
              label="Localizacao"
              placeholder="Cidade, Estado"
              {...register('personalLocation')}
            />
            <Input
              label="LinkedIn"
              placeholder="linkedin.com/in/seu-perfil"
              {...register('personalLinkedIn')}
            />
            <Input
              label="GitHub"
              placeholder="github.com/seu-usuario"
              {...register('personalGithub')}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-slate-900">Resumo Profissional</h2>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Descreva sua trajetoria, areas de atuacao e objetivos profissionais..."
            className="min-h-[120px]"
            {...register('summary')}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <h2 className="text-base font-semibold text-slate-900">Experiencia Profissional</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => appendExp({ id: '', company: '', role: '', period: '', description: '', achievements: '' })}
          >
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {expFields.map((field, index) => (
            <div key={field.id} className="border border-slate-200 rounded-lg p-4 space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-700">Experiencia {index + 1}</p>
                <button
                  type="button"
                  onClick={() => removeExp(index)}
                  className="text-red-500 hover:bg-red-50 p-1 rounded"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="Empresa"
                  placeholder="Nome da empresa"
                  {...register(`experiences.${index}.company`)}
                />
                <Input
                  label="Cargo"
                  placeholder="Seu cargo"
                  {...register(`experiences.${index}.role`)}
                />
                <Input
                  label="Periodo"
                  placeholder="Jan 2020 - Dez 2023"
                  {...register(`experiences.${index}.period`)}
                />
              </div>
              <Textarea
                label="Descricao"
                placeholder="Principais atividades..."
                className="min-h-[80px]"
                {...register(`experiences.${index}.description`)}
              />
              <Textarea
                label="Conquistas / Resultados"
                placeholder="Ex: Reduzi o tempo de deploy em 40%..."
                className="min-h-[80px]"
                {...register(`experiences.${index}.achievements`)}
              />
            </div>
          ))}
          {expFields.length === 0 && (
            <p className="text-sm text-slate-400 text-center py-4">
              Nenhuma experiencia adicionada. Clique em "Adicionar" acima.
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <h2 className="text-base font-semibold text-slate-900">Formacao Academica</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => appendEdu({ id: '', institution: '', degree: '', period: '' })}
          >
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </CardHeader>
        <CardContent className="space-y-3">
          {eduFields.map((field, index) => (
            <div key={field.id} className="border border-slate-200 rounded-lg p-4 space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-700">Formacao {index + 1}</p>
                <button
                  type="button"
                  onClick={() => removeEdu(index)}
                  className="text-red-500 hover:bg-red-50 p-1 rounded"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <Input
                  label="Instituicao"
                  placeholder="Nome da instituicao"
                  {...register(`education.${index}.institution`)}
                />
                <Input
                  label="Curso / Titulo"
                  placeholder="Bacharel em..."
                  {...register(`education.${index}.degree`)}
                />
                <Input
                  label="Periodo"
                  placeholder="2018 - 2022"
                  {...register(`education.${index}.period`)}
                />
              </div>
            </div>
          ))}
          {eduFields.length === 0 && (
            <p className="text-sm text-slate-400 text-center py-4">
              Nenhuma formacao adicionada.
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <h2 className="text-base font-semibold text-slate-900">Habilidades</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => appendSkill({ id: '', value: '' })}
          >
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            {skillFields.map((field, index) => (
              <div key={field.id} className="flex items-center gap-1">
                <Input
                  placeholder="Ex: React, Python, AWS"
                  className="w-40"
                  {...register(`skills.${index}.value`)}
                />
                <button
                  type="button"
                  onClick={() => removeSkill(index)}
                  className="text-red-500 hover:bg-red-50 p-1 rounded"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <h2 className="text-base font-semibold text-slate-900">Certificacoes</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => appendCert({ id: '', value: '' })}
          >
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            {certFields.map((field, index) => (
              <div key={field.id} className="flex items-center gap-1">
                <Input
                  placeholder="Ex: AWS Solutions Architect"
                  className="w-60"
                  {...register(`certifications.${index}.value`)}
                />
                <button
                  type="button"
                  onClick={() => removeCert(index)}
                  className="text-red-500 hover:bg-red-50 p-1 rounded"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center gap-3">
        <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
          <input type="checkbox" {...register('isDefault')} className="rounded border-slate-300" />
          Definir como curriculo padrao
        </label>
      </div>

      <div className="flex justify-end gap-3">
        <Button type="submit" isLoading={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
