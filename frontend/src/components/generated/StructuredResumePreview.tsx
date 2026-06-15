'use client';

import { Mail, Phone, Linkedin, Github, MapPin } from 'lucide-react';

// ============================================
// Type Definitions
// ============================================

export interface OptimizedResume {
  // For nested structure: optimized_resume.sections.experience, etc.
  sections?: {
    professional_title?: string;
    professional_summary?: string;
    skills?: Array<{ category: string; items: string[] }>;
    experience?: Array<{
      company: string;
      official_role: string;
      location?: string;
      start_date?: string;
      end_date?: string;
      highlights?: string[];
    }>;
    previous_experience_summary?: string[];
    projects?: Array<{ name: string; description?: string; technologies?: string[] }>;
    education?: Array<{ institution: string; degree: string; period?: string }>;
    certifications?: Array<{ name: string; issuer?: string; date?: string }>;
    trainings?: Array<{ name: string; issuer?: string; date?: string }>;
    languages?: Array<{ language: string; level?: string }>;
  };
  // Flat structure (legacy)
  professional_title?: string;
  professional_summary?: string;
  skills?: Array<{ category: string; items: string[] }>;
  experience?: Array<{
    company: string;
    official_role: string;
    location?: string;
    start_date?: string;
    end_date?: string;
    highlights?: string[];
  }>;
  previous_experience_summary?: string[];
  projects?: Array<{ name: string; description?: string; technologies?: string[] }>;
  education?: Array<{ institution: string; degree: string; period?: string }>;
  certifications?: Array<{ name: string; issuer?: string; date?: string }>;
  trainings?: Array<{ name: string; issuer?: string; date?: string }>;
  languages?: Array<{ language: string; level?: string }>;
}

export interface ResumeHeader {
  name: string;
  title?: string;
  location?: string;
  contacts?: {
    email?: string;
    phone?: string;
    linkedin?: string;
    github?: string;
  };
}

// ============================================
// Section Components
// ============================================

interface SectionTitleProps {
  children: React.ReactNode;
}

function SectionTitle({ children }: SectionTitleProps) {
  return (
    <h2 className="text-base font-semibold text-slate-900 mb-3 mt-6 first:mt-0">
      {children}
    </h2>
  );
}

// --------------------------------------------
// Resume Header
// --------------------------------------------
interface ResumeHeaderSectionProps {
  header: ResumeHeader;
}

function ResumeHeaderSection({ header }: ResumeHeaderSectionProps) {
  const { name, title, location, contacts } = header;

  return (
    <header className="mb-6 pb-4 border-b border-slate-200">
      <h1 className="text-2xl font-bold text-slate-900">{name}</h1>

      {title && (
        <p className="text-lg text-slate-700 mt-1">{title}</p>
      )}

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-3 text-sm text-slate-600">
        {location && (
          <span className="flex items-center gap-1.5">
            <MapPin className="h-3.5 w-3.5" />
            {location}
          </span>
        )}

        {contacts?.email && (
          <span className="flex items-center gap-1.5">
            <Mail className="h-3.5 w-3.5" />
            {contacts.email}
          </span>
        )}

        {contacts?.phone && (
          <span className="flex items-center gap-1.5">
            <Phone className="h-3.5 w-3.5" />
            {contacts.phone}
          </span>
        )}

        {contacts?.linkedin && (
          <a
            href={contacts.linkedin}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 hover:text-blue-600 transition-colors"
          >
            <Linkedin className="h-3.5 w-3.5" />
            LinkedIn
          </a>
        )}

        {contacts?.github && (
          <a
            href={contacts.github}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 hover:text-slate-900 transition-colors"
          >
            <Github className="h-3.5 w-3.5" />
            GitHub
          </a>
        )}
      </div>
    </header>
  );
}

// --------------------------------------------
// Professional Summary
// --------------------------------------------
interface ProfessionalSummarySectionProps {
  summary?: string;
}

function ProfessionalSummarySection({ summary }: ProfessionalSummarySectionProps) {
  if (!summary) return null;

  return (
    <section>
      <SectionTitle>Resumo Profissional</SectionTitle>
      <p className="text-sm text-slate-700 leading-relaxed">{summary}</p>
    </section>
  );
}

// --------------------------------------------
// Experience
// --------------------------------------------
interface ExperienceSectionProps {
  experience?: OptimizedResume['experience'];
  previousSummary?: string[];
}

function ExperienceSection({ experience, previousSummary }: ExperienceSectionProps) {
  if (!experience?.length && !previousSummary?.length) return null;

  return (
    <section>
      <SectionTitle>Experiencia Profissional</SectionTitle>

      {previousSummary && previousSummary.length > 0 && (
        <ul className="mb-4 space-y-1">
          {previousSummary.map((item, index) => (
            <li key={index} className="text-sm text-slate-700 pl-5 relative before:content-['-'] before:absolute before:left-0 before:text-slate-400">
              {item}
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-5">
        {experience?.map((exp, index) => (
          <div key={index} className="text-sm">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h3 className="font-medium text-slate-900">{exp.official_role}</h3>
              <span className="text-xs text-slate-500 whitespace-nowrap">
                {exp.start_date}
                {(exp.start_date && (exp.end_date || exp.start_date !== exp.end_date)) && ' - '}
                {exp.end_date}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-x-2 text-slate-600 mt-0.5">
              <span>{exp.company}</span>
              {exp.location && (
                <>
                  <span className="text-slate-300">|</span>
                  <span className="flex items-center gap-1">
                    <MapPin className="h-3 w-3" />
                    {exp.location}
                  </span>
                </>
              )}
            </div>
            {exp.highlights && exp.highlights.length > 0 && (
              <ul className="mt-2 space-y-1">
                {exp.highlights.map((highlight, hIndex) => (
                  <li key={hIndex} className="text-slate-700 pl-5 relative">
                    <span className="absolute left-0 text-slate-400">-</span>
                    {highlight}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

// --------------------------------------------
// Skills
// --------------------------------------------
interface SkillsSectionProps {
  skills?: OptimizedResume['skills'];
}

function SkillsSection({ skills }: SkillsSectionProps) {
  if (!skills?.length) return null;

  return (
    <section>
      <SectionTitle>Competencias</SectionTitle>
      <div className="space-y-3">
        {skills.map((group, index) => (
          <div key={index}>
            <h4 className="text-sm font-medium text-slate-800">{group.category}</h4>
            <p className="text-sm text-slate-600 mt-0.5">
              {group.items.join(', ')}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

// --------------------------------------------
// Projects
// --------------------------------------------
interface ProjectsSectionProps {
  projects?: OptimizedResume['projects'];
}

function ProjectsSection({ projects }: ProjectsSectionProps) {
  if (!projects?.length) return null;

  return (
    <section>
      <SectionTitle>Projetos</SectionTitle>
      <div className="space-y-4">
        {projects.map((project, index) => (
          <div key={index} className="text-sm">
            <h3 className="font-medium text-slate-900">{project.name}</h3>
            {project.description && (
              <p className="text-slate-700 mt-1">{project.description}</p>
            )}
            {project.technologies && project.technologies.length > 0 && (
              <p className="text-xs text-slate-500 mt-1">
                <span className="font-medium">Tecnologias:</span> {project.technologies.join(', ')}
              </p>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

// --------------------------------------------
// Education
// --------------------------------------------
interface EducationSectionProps {
  education?: OptimizedResume['education'];
}

function EducationSection({ education }: EducationSectionProps) {
  if (!education?.length) return null;

  return (
    <section>
      <SectionTitle>Formacao Academica</SectionTitle>
      <div className="space-y-3">
        {education.map((edu, index) => (
          <div key={index} className="text-sm">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h3 className="font-medium text-slate-900">{edu.degree}</h3>
              {edu.period && (
                <span className="text-xs text-slate-500">{edu.period}</span>
              )}
            </div>
            <p className="text-slate-600 mt-0.5">{edu.institution}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

// --------------------------------------------
// Certifications
// --------------------------------------------
interface CertificationsSectionProps {
  certifications?: OptimizedResume['certifications'];
}

function CertificationsSection({ certifications }: CertificationsSectionProps) {
  if (!certifications?.length) return null;

  return (
    <section>
      <SectionTitle>Certificacoes</SectionTitle>
      <ul className="space-y-2">
        {certifications.map((cert, index) => (
          <li key={index} className="text-sm">
            <span className="text-slate-900 font-medium">{cert.name}</span>
            {(cert.issuer || cert.date) && (
              <span className="text-slate-500">
                {' '}
                {cert.issuer && <span> - {cert.issuer}</span>}
                {cert.date && <span> ({cert.date})</span>}
              </span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

// --------------------------------------------
// Trainings
// --------------------------------------------
interface TrainingsSectionProps {
  trainings?: OptimizedResume['trainings'];
}

function TrainingsSection({ trainings }: TrainingsSectionProps) {
  if (!trainings?.length) return null;

  return (
    <section>
      <SectionTitle>Cursos e Treinamentos</SectionTitle>
      <ul className="space-y-2">
        {trainings.map((training, index) => (
          <li key={index} className="text-sm">
            <span className="text-slate-900 font-medium">{training.name}</span>
            {(training.issuer || training.date) && (
              <span className="text-slate-500">
                {' '}
                {training.issuer && <span> - {training.issuer}</span>}
                {training.date && <span> ({training.date})</span>}
              </span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

// --------------------------------------------
// Languages
// --------------------------------------------
interface LanguagesSectionProps {
  languages?: OptimizedResume['languages'];
}

function LanguagesSection({ languages }: LanguagesSectionProps) {
  if (!languages?.length) return null;

  return (
    <section>
      <SectionTitle>Idiomas</SectionTitle>
      <ul className="space-y-1">
        {languages.map((lang, index) => (
          <li key={index} className="text-sm text-slate-700">
            <span className="text-slate-900">{lang.language}</span>
            {lang.level && (
              <span className="text-slate-500"> - {lang.level}</span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

// ============================================
// Main Component
// ============================================

interface StructuredResumePreviewProps {
  header?: ResumeHeader;
  resume?: OptimizedResume;
  className?: string;
}

export function StructuredResumePreview({ header, resume, className }: StructuredResumePreviewProps) {
  // Check if we have any data to display
  const hasAnyData =
    resume?.professional_summary ||
    resume?.experience?.length ||
    resume?.previous_experience_summary?.length ||
    resume?.skills?.length ||
    resume?.projects?.length ||
    resume?.education?.length ||
    resume?.certifications?.length ||
    resume?.trainings?.length ||
    resume?.languages?.length;

  if (!hasAnyData) {
    return (
      <div className={`p-8 text-center text-slate-500 text-sm ${className ?? ''}`}>
        Nenhuma informacao de curriculo disponivel. Preencha os dados do candidato para gerar o curriculo.
      </div>
    );
  }

  return (
    <div className={`bg-white p-6 rounded-xl border border-slate-200 shadow-sm ${className ?? ''}`}>
      {header && <ResumeHeaderSection header={header} />}

      <ProfessionalSummarySection summary={resume?.professional_summary} />
      <ExperienceSection
        experience={resume?.experience}
        previousSummary={resume?.previous_experience_summary}
      />
      <SkillsSection skills={resume?.skills} />
      <ProjectsSection projects={resume?.projects} />
      <EducationSection education={resume?.education} />
      <CertificationsSection certifications={resume?.certifications} />
      <TrainingsSection trainings={resume?.trainings} />
      <LanguagesSection languages={resume?.languages} />
    </div>
  );
}

export default StructuredResumePreview;