import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { Layers, CheckCircle2, XCircle, RefreshCw, Plus } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const IntegrationsPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const queryClient = useQueryClient();
  const [testResult, setTestResult] = useState<string | null>(null);

  const { data: integrations, isLoading } = useQuery({
    queryKey: ['integrations', selectedEnv?.id],
    queryFn: () => infraService.getIntegrations(selectedEnv?.id),
  });

  const testMutation = useMutation({
    mutationFn: (id: string) => infraService.testIntegration(id),
    onSuccess: (data) => {
      setTestResult(`Integration [${data.name}] connection test PASSED (${data.healthStatus}).`);
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight">Infrastructure Integrations</h2>
          <p className="text-xs text-slate-400">
            Telemetry connectors for Prometheus, Docker, Kubernetes, Linux SSH, Loki, and CI/CD
          </p>
        </div>
      </div>

      {testResult && (
        <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/30 rounded-xl text-xs text-emerald-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{testResult}</span>
          </div>
          <button onClick={() => setTestResult(null)} className="text-slate-400 hover:text-white text-xs">✕</button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {integrations?.map((integ) => (
          <div
            key={integ.id}
            className="glass-card rounded-xl p-5 border border-slate-800 space-y-4 hover:border-slate-700 transition-all flex flex-col justify-between"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <Layers className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-100 text-xs">{integ.name}</h3>
                    <span className="text-[10px] text-indigo-400 font-mono font-medium">{integ.type}</span>
                  </div>
                </div>
                <Badge variant={integ.healthStatus === 'HEALTHY' ? 'emerald' : 'rose'}>
                  {integ.healthStatus}
                </Badge>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-lg text-xs space-y-1 text-slate-300 font-mono border border-slate-800/80">
                <div className="truncate">Endpoint: <span className="text-slate-400">{integ.endpointUrl}</span></div>
                <div>Environment: <span className="text-indigo-400">{integ.environment?.name || 'PRODUCTION'}</span></div>
              </div>
            </div>

            <button
              onClick={() => testMutation.mutate(integ.id)}
              disabled={testMutation.isPending}
              className="w-full py-2 bg-slate-800 hover:bg-indigo-600 hover:text-white text-slate-300 rounded-lg text-xs font-medium border border-slate-700 hover:border-indigo-500 transition-all flex items-center justify-center gap-1.5"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${testMutation.isPending ? 'animate-spin' : ''}`} />
              <span>Test Connection</span>
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
