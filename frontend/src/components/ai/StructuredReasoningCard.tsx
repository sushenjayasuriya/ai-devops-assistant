import React from 'react';
import { FOIRResponse } from '../../types/ai';
import { RiskPill } from '../common/RiskPill';
import { ShieldCheck, CheckCircle2, Eye, HelpCircle, Wrench, Terminal } from 'lucide-react';

interface Props {
  data: FOIRResponse;
  onExecuteAction?: (recommendation: any) => void;
}

export const StructuredReasoningCard: React.FC<Props> = ({ data, onExecuteAction }) => {
  return (
    <div className="glass-card rounded-xl p-5 space-y-5 border border-indigo-500/30 glow-indigo">
      {/* Header & Confidence Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-700/60 pb-3">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-indigo-400" />
            <h3 className="font-semibold text-slate-100 text-base">AI Root-Cause Diagnosis</h3>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">FOIR Deterministic Telemetry Validation Engine</p>
        </div>

        <div className="flex items-center gap-2 bg-slate-900/60 px-3 py-1.5 rounded-lg border border-slate-700">
          <span className="text-xs text-slate-400 font-medium">Confidence:</span>
          <span className="text-sm font-mono font-bold text-emerald-400">
            {(data.confidenceScore * 100).toFixed(0)}%
          </span>
        </div>
      </div>

      {/* Summary */}
      <div className="p-3.5 bg-indigo-950/20 border border-indigo-500/20 rounded-lg text-sm text-indigo-200 font-medium">
        {data.summary}
      </div>

      {/* Facts (Verified Telemetry) */}
      {data.facts && data.facts.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-emerald-400">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Facts (Verified Infrastructure Telemetry)</span>
          </div>
          <div className="space-y-1.5">
            {data.facts.map((fact, idx) => (
              <div key={idx} className="flex items-start gap-2 text-xs font-mono bg-emerald-950/10 border border-emerald-500/20 p-2.5 rounded text-emerald-300">
                <span className="text-emerald-500 select-none">▸</span>
                <span>{fact}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Observations */}
      {data.observations && data.observations.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-cyan-400">
            <Eye className="w-3.5 h-3.5" />
            <span>Observations (System Trends & Anomalies)</span>
          </div>
          <div className="space-y-1.5">
            {data.observations.map((obs, idx) => (
              <div key={idx} className="flex items-start gap-2 text-xs bg-cyan-950/10 border border-cyan-500/20 p-2.5 rounded text-cyan-200">
                <span className="text-cyan-400 select-none">•</span>
                <span>{obs}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Inferences */}
      {data.inferences && data.inferences.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-violet-400">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Inferences (Causal Hypothesis)</span>
          </div>
          <div className="space-y-1.5">
            {data.inferences.map((inf, idx) => (
              <div key={idx} className="flex items-start gap-2 text-xs bg-violet-950/10 border border-violet-500/20 p-2.5 rounded text-violet-200">
                <span className="text-violet-400 select-none">✦</span>
                <span>{inf}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recommendations & Action Cards */}
      {data.recommendations && data.recommendations.length > 0 && (
        <div className="space-y-3 pt-2">
          <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-amber-400">
            <Wrench className="w-3.5 h-3.5" />
            <span>Recommended Remediation</span>
          </div>
          <div className="space-y-3">
            {data.recommendations.map((rec, idx) => (
              <div key={idx} className="p-4 bg-slate-900/80 border border-slate-700/80 rounded-xl space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-sm font-semibold text-slate-100">{rec.action}</span>
                    <RiskPill risk={rec.riskLevel} />
                    {rec.requiresApproval && (
                      <span className="text-[10px] bg-rose-500/10 text-rose-400 border border-rose-500/20 px-2 py-0.5 rounded font-medium">
                        Approval Required in Production
                      </span>
                    )}
                  </div>
                </div>

                <p className="text-xs text-slate-300">{rec.rationale}</p>

                {rec.expectedImpact && (
                  <div className="text-[11px] text-amber-300/90 bg-amber-950/20 border border-amber-500/20 p-2 rounded">
                    <strong>Expected Impact:</strong> {rec.expectedImpact}
                  </div>
                )}

                {onExecuteAction && (
                  <div className="pt-1 flex justify-end">
                    <button
                      onClick={() => onExecuteAction(rec)}
                      className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium transition-all shadow-md shadow-indigo-600/20 flex items-center gap-1.5"
                    >
                      <Wrench className="w-3.5 h-3.5" />
                      <span>{rec.requiresApproval ? 'Request Approval & Execute' : 'Execute Action'}</span>
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tool Trail Drawer */}
      {data.toolExecutionTrail && data.toolExecutionTrail.length > 0 && (
        <div className="border-t border-slate-800 pt-3">
          <div className="flex items-center gap-1.5 text-[11px] text-slate-400 font-mono">
            <Terminal className="w-3 h-3 text-slate-500" />
            <span>Executed {data.toolExecutionTrail.length} Telemetry Tools: </span>
            <span className="text-indigo-400">
              {data.toolExecutionTrail.map(t => t.toolName).join(', ')}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
