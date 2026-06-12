'use client';

import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { AdherenceScore } from './AdherenceScore';
import { AlertCircle, CheckCircle, Lightbulb } from 'lucide-react';

interface KeywordMap {
  matched?: string[];
  missing?: string[];
  partial?: string[];
}

interface Gap {
  description: string;
  suggestion?: string;
  requirement?: string;
  type?: string;
}

interface AnalysisPanelProps {
  analysis?: {
    adherenceScore?: number | null;
    summary?: string;
    keywordMap?: KeywordMap;
    gaps?: Gap[];
    strengths?: string[];
  } | AnalysisReportDtoShape | null;
  className?: string;
}

type AnalysisReportDtoShape = {
  id: string;
  overallScore?: number | null;
  findings?: unknown;
  recommendations?: unknown;
};

export function AnalysisPanel({ analysis, className }: AnalysisPanelProps) {
  if (!analysis) {
    return (
      <Card className={className}>
        <CardContent className="py-8 text-center text-slate-400 text-sm">
          Analise nao disponivel
        </CardContent>
      </Card>
    );
  }

  // Support both GeneratedResume.analysis (AnalysisReportDto) and GenerationResponse.analysis
  const analysisAny = analysis as {
    adherenceScore?: number | null;
    summary?: string;
    keywordMap?: KeywordMap;
    gaps?: Gap[];
    strengths?: string[];
  };
  const score = analysisAny.adherenceScore
    ?? (analysis as { overallScore?: number | null }).overallScore
    ?? 0;
  const keywordMap = analysisAny.keywordMap;
  const matched = keywordMap?.matched ?? [];
  const missing = keywordMap?.missing ?? [];
  const gaps = analysisAny.gaps ?? [];
  const strengths = analysisAny.strengths ?? [];

  return (
    <div className={className}>
      {score > 0 && (
        <Card>
          <CardHeader>
            <h3 className="text-base font-semibold text-slate-900">Score de Aderencia</h3>
          </CardHeader>
          <CardContent>
            <AdherenceScore score={score} size="lg" />
            {analysisAny.summary && (
              <p className="mt-3 text-sm text-slate-600">{analysisAny.summary}</p>
            )}
          </CardContent>
        </Card>
      )}

      {matched.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-green-500" />
              <h3 className="text-base font-semibold text-slate-900">Palavras-chave encontradas</h3>
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-1.5">
              {matched.map((kw) => (
                <Badge key={kw} variant="success">{kw}</Badge>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {missing.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-red-500" />
              <h3 className="text-base font-semibold text-slate-900">Palavras-chave faltando</h3>
            </div>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-1.5">
              {missing.map((kw) => (
                <Badge key={kw} variant="danger">{kw}</Badge>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {gaps.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-amber-500" />
              <h3 className="text-base font-semibold text-slate-900">Lacunas identificadas</h3>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            {gaps.map((gap, i) => (
              <div key={i} className="text-sm">
                <p className="text-slate-700">{gap.description}</p>
                {gap.suggestion && (
                  <p className="text-xs text-slate-500 mt-0.5">
                    <span className="font-medium">Sugestao:</span> {gap.suggestion}
                  </p>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {strengths.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Lightbulb className="h-4 w-4 text-blue-500" />
              <h3 className="text-base font-semibold text-slate-900">Pontos fortes</h3>
            </div>
          </CardHeader>
          <CardContent>
            <ul className="space-y-1.5">
              {strengths.map((s, i) => (
                <li key={i} className="flex items-start gap-2 text-sm text-slate-700">
                  <CheckCircle className="h-3.5 w-3.5 text-green-500 mt-0.5 flex-shrink-0" />
                  {s}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
