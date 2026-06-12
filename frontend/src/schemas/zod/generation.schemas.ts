import { z } from 'zod';

export const generationSchema = z.object({
  resumeProfileId: z.string().uuid('Selecione um curriculo base'),
  jobApplicationId: z.string().uuid('Selecione uma vaga'),
  extraInstructions: z.string().optional(),
});

export type GenerationFormData = z.infer<typeof generationSchema>;
