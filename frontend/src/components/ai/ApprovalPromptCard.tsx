import React, { useState } from 'react';
import { ApprovalRequest } from '../../types/ai';
import { RiskPill } from '../common/RiskPill';
import { AlertTriangle, CheckCircle, XCircle, Clock, ShieldAlert } from 'lucide-react';

interface Props {
  approval: ApprovalRequest;
  onResolve: (id: string, decision: 'APPROVED' | 'REJECTED', comment?: string) => Promise<void>;
}

export const ApprovalPromptCard: React.FC<Props> = ({ approval, onResolve }) => {
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);

  const handleDecision = async (decision: 'APPROVED' | 'REJECTED') => {
    setLoading(true);
    try {
      await onResolve(approval.id, decision, comment);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="glass-card rounded-xl p-5 space-y-4 border border-amber-500/30 glow-amber">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <h4 className="font-semibold text-slate-100 text-sm">Human Approval Required</h4>
            <p className="text-xs text-slate-400">Production Infrastructure Mutation Action</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-mono px-2 py-0.5 bg-rose-500/10 text-rose-400 border border-rose-500/20 rounded">
            {approval.environment?.name || 'PRODUCTION'}
          </span>
          <RiskPill risk="HIGH_RISK" />
        </div>
      </div>

      <div className="bg-slate-900/90 rounded-lg p-3.5 space-y-2 border border-slate-800 font-mono text-xs">
        <div className="text-slate-300">
          <span className="text-slate-500">Action: </span>
          <span className="text-indigo-400 font-semibold">{approval.actionType}</span>
        </div>
        <div className="text-slate-300">
          <span className="text-slate-500">Requested By: </span>
          <span className="text-slate-200">{approval.requestedByUser?.fullName || 'SRE AI Orchestrator'}</span>
        </div>
        <div className="text-slate-300">
          <span className="text-slate-500">Rationale: </span>
          <span className="text-slate-200">{approval.rationale}</span>
        </div>
        <div className="text-amber-300/90 bg-amber-950/20 border border-amber-500/20 p-2 rounded">
          <span className="text-amber-400 font-bold">Expected Impact: </span>
          {approval.expectedImpact}
        </div>
      </div>

      {approval.status === 'PENDING' ? (
        <div className="space-y-3 pt-1">
          <input
            type="text"
            placeholder="Optional approval note or reason for rejection..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-indigo-500 placeholder:text-slate-500"
          />

          <div className="flex items-center justify-end gap-2.5">
            <button
              disabled={loading}
              onClick={() => handleDecision('REJECTED')}
              className="px-4 py-2 bg-slate-800 hover:bg-rose-950/50 hover:text-rose-400 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 hover:border-rose-500/30 transition-all flex items-center gap-1.5"
            >
              <XCircle className="w-4 h-4" />
              <span>Reject Action</span>
            </button>

            <button
              disabled={loading}
              onClick={() => handleDecision('APPROVED')}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium shadow-md shadow-emerald-600/20 transition-all flex items-center gap-1.5"
            >
              <CheckCircle className="w-4 h-4" />
              <span>Approve & Execute</span>
            </button>
          </div>
        </div>
      ) : (
        <div className="flex items-center gap-2 text-xs font-medium pt-1">
          <span className="text-slate-400">Resolution Status:</span>
          <span className={approval.status === 'APPROVED' ? 'text-emerald-400 font-bold' : 'text-rose-400 font-bold'}>
            {approval.status}
          </span>
          {approval.resolvedByUser && (
            <span className="text-slate-500">by {approval.resolvedByUser.fullName}</span>
          )}
        </div>
      )}
    </div>
  );
};
