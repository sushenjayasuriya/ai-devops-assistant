import React from 'react';
import { RiskLevel } from '../../types/ai';

interface RiskPillProps {
  risk: RiskLevel | string;
}

export const RiskPill: React.FC<RiskPillProps> = ({ risk }) => {
  const getStyle = (r: string) => {
    switch (r) {
      case 'READ_ONLY':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'LOW_RISK':
        return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30';
      case 'MEDIUM_RISK':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'HIGH_RISK':
      case 'HIGH':
        return 'bg-rose-500/15 text-rose-400 border-rose-500/30 font-semibold';
      case 'CRITICAL':
        return 'bg-purple-500/20 text-purple-300 border-purple-500/40 font-bold animate-pulse';
      default:
        return 'bg-slate-700/30 text-slate-300 border-slate-600/30';
    }
  };

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-mono border ${getStyle(risk)}`}>
      {risk.replace('_', ' ')}
    </span>
  );
};
