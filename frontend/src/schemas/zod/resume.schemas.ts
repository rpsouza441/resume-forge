import { z } from 'zod';

export const resumeSchema = z.object({
  title: z.string().min(1, 'Titulo e obrigatorio').max(255),
  contentMarkdown: z.string().min(50, 'Conteudo muito curto (minimo 50 caracteres)'),
  contentJsonb: z.record(z.unknown()).optional(),
  isDefault: z.boolean().optional(),
});

export type ResumeFormData = z.infer<typeof resumeSchema>;
