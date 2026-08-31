import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { auditService } from '../../services/auditService';
import { Shield, Search, Terminal, CheckCircle2, XCircle } from 'lucide-react';
import { RiskPill } from '../../components/common/RiskPill';

export const AuditLogsPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const [page, setPage] = useState(0);

  const { data: auditData, isLoading } = useQuery({
    queryKey: ['audit-logs', selectedEnv?.name, page],
    queryFn: () => auditService.getAuditLogs(selectedEnv?.name, page, 20),
    refetchInterval: 8000,
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Security & Infrastructure Audit Trail</h2>
        <p className="text-xs text-slate-400">
          Immutable cryptographic log of AI tool executions, operator approvals, and mutations
        </p>
      </div>

      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="text-slate-400 border-b border-slate-800 font-sans">
              <tr>
                <th className="pb-3">Action</th>
                <th className="pb-3">Target</th>
                <th className="pb-3">Environment</th>
                <th className="pb-3">Risk Level</th>
                <th className="pb-3">Actor / Principal</th>
                <th className="pb-3">Status</th>
                <th className="pb-3">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {auditData?.content?.map((log) => (
                <tr key={log.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 font-semibold text-slate-200">{log.action}</td>
                  <td className="py-3 text-indigo-400 max-w-xs truncate">{log.targetResourceId || '-'}</td>
                  <td className="py-3 text-slate-300">{log.environmentName}</td>
                  <td className="py-3">
                    <RiskPill risk={log.riskLevel || 'READ_ONLY'} />
                  </td>
                  <td className="py-3 text-slate-400 font-sans">
                    {log.user?.fullName || 'SRE AI Orchestrator'}
                  </td>
                  <td className="py-3">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] ${
                        log.status === 'SUCCESS'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}
                    >
                      {log.status}
                    </span>
                  </td>
                  <td className="py-3 text-slate-400 font-sans">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-slate-800 pt-3 text-xs text-slate-400">
          <div>Page {page + 1} of {auditData?.totalPages || 1}</div>
          <div className="flex items-center gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded text-slate-300"
            >
              Previous
            </button>
            <button
              disabled={auditData?.last || false}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded text-slate-300"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
