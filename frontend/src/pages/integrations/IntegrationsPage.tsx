import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { Layers, CheckCircle2, XCircle, RefreshCw, Plus, Server, Activity, ShieldCheck, Box, Terminal } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { Integration, TestConnectionResult } from '../../types/infrastructure';

export const IntegrationsPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const queryClient = useQueryClient();
  const [testResults, setTestResults] = useState<Record<string, TestConnectionResult>>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    type: 'PROMETHEUS',
    endpointUrl: '',
    authType: 'NONE',
    configRaw: '',
    timeoutMs: 5000,
    enabled: true
  });
  const [formError, setFormError] = useState<string | null>(null);

  const { data: integrations, isLoading } = useQuery({
    queryKey: ['integrations', selectedEnv?.id],
    queryFn: () => infraService.getIntegrations(selectedEnv?.id),
  });

  const testMutation = useMutation({
    mutationFn: (id: string) => infraService.testIntegration(id),
    onSuccess: (data) => {
      setTestResults(prev => ({ ...prev, [data.integrationId]: data }));
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
    },
  });

  const createMutation = useMutation({
    mutationFn: (payload: any) => infraService.createIntegration(payload),
    onSuccess: () => {
      setIsModalOpen(false);
      setFormData({
        name: '',
        type: 'PROMETHEUS',
        endpointUrl: '',
        authType: 'NONE',
        configRaw: '',
        timeoutMs: 5000,
        enabled: true
      });
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
    },
    onError: (err: any) => {
      setFormError(err?.response?.data?.message || err.message || 'Failed to create integration');
    }
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEnv?.id) {
      setFormError('Please select an active environment first');
      return;
    }
    createMutation.mutate({
      ...formData,
      environmentId: selectedEnv.id,
      timeoutMs: Number(formData.timeoutMs)
    });
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'PROMETHEUS': return <Activity className="w-4 h-4 text-amber-400" />;
      case 'DOCKER': return <Box className="w-4 h-4 text-cyan-400" />;
      case 'KUBERNETES': return <Layers className="w-4 h-4 text-indigo-400" />;
      case 'LINUX_SSH': return <Terminal className="w-4 h-4 text-emerald-400" />;
      default: return <Server className="w-4 h-4 text-slate-400" />;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-400" />
            Infrastructure Integrations & Data Plane
          </h2>
          <p className="text-xs text-slate-400">
            Real Prometheus, Docker Engine, Kubernetes Fabric8, and Linux SSH data plane connectors
          </p>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          className="inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-3 py-2 rounded-lg transition shadow-md shadow-indigo-600/20"
        >
          <Plus className="w-4 h-4" />
          Add Integration
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {integrations?.map((integ) => {
          const testResult = testResults[integ.id];
          return (
            <div
              key={integ.id}
              className="glass-card rounded-xl p-5 border border-slate-800 space-y-4 hover:border-slate-700 transition-all flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 rounded-lg bg-slate-800 border border-slate-700">
                      {getTypeIcon(integ.type)}
                    </div>
                    <div>
                      <h3 className="font-semibold text-slate-100 text-xs">{integ.name}</h3>
                      <span className="text-[10px] text-indigo-400 font-mono font-medium">{integ.type}</span>
                    </div>
                  </div>
                  <Badge variant={integ.healthStatus === 'HEALTHY' ? 'emerald' : integ.healthStatus === 'UNHEALTHY' ? 'rose' : 'slate'}>
                    {integ.healthStatus}
                  </Badge>
                </div>

                <div className="p-3 bg-slate-900/60 rounded-lg text-xs space-y-1.5 text-slate-300 font-mono border border-slate-800/80">
                  <div className="truncate">Endpoint: <span className="text-slate-400">{integ.endpointUrl}</span></div>
                  <div>Environment: <span className="text-indigo-400">{integ.environmentName || selectedEnv?.name || 'PRODUCTION'}</span></div>
                  <div>Timeout: <span className="text-slate-400">{integ.timeoutMs || 5000} ms</span></div>
                </div>

                {testResult && (
                  <div className={`p-2.5 rounded-lg text-2xs font-mono border ${
                    testResult.connected
                      ? 'bg-emerald-950/30 border-emerald-500/30 text-emerald-300'
                      : 'bg-rose-950/30 border-rose-500/30 text-rose-300'
                  }`}>
                    <div className="flex items-center justify-between">
                      <span>Status: {testResult.status}</span>
                      <span>{testResult.latencyMs}ms</span>
                    </div>
                    {testResult.errorMessage && (
                      <div className="mt-1 text-rose-400 truncate">{testResult.errorMessage}</div>
                    )}
                  </div>
                )}
              </div>

              <button
                onClick={() => testMutation.mutate(integ.id)}
                disabled={testMutation.isPending}
                className="w-full py-2 bg-slate-800 hover:bg-indigo-600 hover:text-white text-slate-300 rounded-lg text-xs font-medium border border-slate-700 hover:border-indigo-500 transition-all flex items-center justify-center gap-1.5"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${testMutation.isPending ? 'animate-spin' : ''}`} />
                <span>Test Live Connection</span>
              </button>
            </div>
          );
        })}
      </div>

      {/* Add Integration Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="glass-card rounded-2xl p-6 border border-slate-700 shadow-2xl max-w-lg w-full space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-sm font-semibold text-slate-100">Register Infrastructure Integration</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white text-xs">✕</button>
            </div>

            {formError && (
              <div className="p-2.5 bg-rose-950/30 border border-rose-500/40 text-rose-300 text-xs rounded-lg">
                {formError}
              </div>
            )}

            <form onSubmit={handleCreate} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-slate-300 mb-1">Integration Name</label>
                <input
                  type="text"
                  required
                  placeholder="Production Prometheus"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Type</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="PROMETHEUS">Prometheus</option>
                    <option value="DOCKER">Docker Engine</option>
                    <option value="KUBERNETES">Kubernetes (Fabric8)</option>
                    <option value="LINUX_SSH">Linux SSH Host</option>
                  </select>
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Auth Type</label>
                  <select
                    value={formData.authType}
                    onChange={(e) => setFormData({ ...formData, authType: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    <option value="NONE">None / Anonymous</option>
                    <option value="BEARER_TOKEN">Bearer Token</option>
                    <option value="BASIC_AUTH">Basic Auth</option>
                    <option value="SSH_KEY">SSH Key / Password</option>
                    <option value="KUBECONFIG">Kubeconfig YAML</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-slate-300 mb-1">Endpoint URL / Host</label>
                <input
                  type="text"
                  required
                  placeholder="http://prometheus:9090 or tcp://docker:2375 or 10.0.1.5"
                  value={formData.endpointUrl}
                  onChange={(e) => setFormData({ ...formData, endpointUrl: e.target.value })}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 font-mono text-xs focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              {formData.authType !== 'NONE' && (
                <div>
                  <label className="block text-slate-300 mb-1">
                    Credentials / Token / Private Key / Kubeconfig (Encrypted with AES-256-GCM)
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Enter token, password, JSON, or kubeconfig YAML..."
                    value={formData.configRaw}
                    onChange={(e) => setFormData({ ...formData, configRaw: e.target.value })}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 font-mono text-2xs focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-lg text-xs"
                >
                  {createMutation.isPending ? 'Saving...' : 'Save Integration'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
