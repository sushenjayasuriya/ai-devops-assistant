import React, { useEffect, useState } from 'react';
import { Box, Layers, RefreshCw, AlertTriangle, CheckCircle2, Terminal, Server } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { infraService } from '../../services/infraService';
import { Integration, K8sPod } from '../../types/infrastructure';

export const KubernetesPage: React.FC = () => {
  const [integrations, setIntegrations] = useState<Integration[]>([]);
  const [selectedIntegrationId, setSelectedIntegrationId] = useState<string>('');
  const [selectedNamespace, setSelectedNamespace] = useState<string>('all');
  const [pods, setPods] = useState<K8sPod[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [activeLogPod, setActiveLogPod] = useState<string | null>(null);
  const [activeLogNamespace, setActiveLogNamespace] = useState<string>('default');
  const [podLogs, setPodLogs] = useState<string[]>([]);
  const [logsLoading, setLogsLoading] = useState<boolean>(false);

  useEffect(() => {
    loadIntegrations();
  }, []);

  useEffect(() => {
    if (selectedIntegrationId) {
      loadPods(selectedIntegrationId, selectedNamespace);
    }
  }, [selectedIntegrationId, selectedNamespace]);

  const loadIntegrations = async () => {
    try {
      setLoading(true);
      const allIntegrations = await infraService.getIntegrations();
      const k8sList = allIntegrations.filter((i) => i.type === 'KUBERNETES');
      setIntegrations(k8sList);
      if (k8sList.length > 0) {
        setSelectedIntegrationId(k8sList[0].id);
      } else {
        setLoading(false);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load Kubernetes integrations');
      setLoading(false);
    }
  };

  const loadPods = async (integrationId: string, namespace: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await infraService.getKubernetesPods(
        integrationId,
        namespace === 'all' ? undefined : namespace
      );
      setPods(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to connect to Kubernetes cluster');
      setPods([]);
    } finally {
      setLoading(false);
    }
  };

  const handleViewLogs = async (podName: string, namespace: string) => {
    setActiveLogPod(podName);
    setActiveLogNamespace(namespace);
    setLogsLoading(true);
    try {
      const logs = await infraService.getKubernetesLogs(selectedIntegrationId, namespace, podName, undefined, 100);
      setPodLogs(logs);
    } catch (err: any) {
      setPodLogs([`[ERROR] Failed to fetch pod logs: ${err?.response?.data?.message || err.message}`]);
    } finally {
      setLogsLoading(false);
    }
  };

  const runningCount = pods.filter((p) => p.status === 'Running').length;
  const alertingCount = pods.filter((p) => p.status !== 'Running' && p.status !== 'Completed').length;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight flex items-center gap-2">
            <Box className="w-5 h-5 text-indigo-400" />
            Kubernetes Cluster & Pod Mesh
          </h2>
          <p className="text-xs text-slate-400">
            Real Fabric8 cluster workloads, pod health status, and live console logs
          </p>
        </div>

        <div className="flex items-center gap-3">
          {integrations.length > 0 && (
            <>
              <select
                value={selectedIntegrationId}
                onChange={(e) => setSelectedIntegrationId(e.target.value)}
                className="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded-lg px-3 py-1.5 focus:ring-1 focus:ring-indigo-500"
              >
                {integrations.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.name} ({i.healthStatus})
                  </option>
                ))}
              </select>

              <button
                onClick={() => loadPods(selectedIntegrationId, selectedNamespace)}
                disabled={loading}
                className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold px-3 py-1.5 rounded-lg border border-slate-700 transition"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                Refresh
              </button>
            </>
          )}
        </div>
      </div>

      {integrations.length === 0 && !loading && (
        <div className="glass-card rounded-xl p-8 border border-slate-800 text-center space-y-3">
          <Layers className="w-10 h-10 text-slate-500 mx-auto" />
          <h3 className="text-sm font-semibold text-slate-200">No Kubernetes Integrations Configured</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            Connect a Kubernetes cluster in the Integrations settings to inspect live workloads, namespaces, and pod logs.
          </p>
        </div>
      )}

      {error && (
        <div className="glass-card rounded-xl p-4 border border-rose-900/60 bg-rose-950/20 text-rose-300 text-xs flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
          <div>
            <span className="font-semibold">Kubernetes Connection Error: </span>
            {error}
          </div>
        </div>
      )}

      {integrations.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
              <span className="text-xs text-slate-400">Total Workloads</span>
              <div className="text-lg font-bold font-mono text-slate-100">{pods.length} Pods</div>
            </div>
            <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
              <span className="text-xs text-slate-400">Healthy / Running</span>
              <div className="text-lg font-bold font-mono text-emerald-400 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>{runningCount} Active</span>
              </div>
            </div>
            <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
              <span className="text-xs text-slate-400">Alerting / CrashLoop</span>
              <div className="text-lg font-bold font-mono text-rose-400 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-rose-400" />
                <span>{alertingCount} Pods</span>
              </div>
            </div>
          </div>

          <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-100">Active Workload Pods</h3>
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-400">Namespace:</span>
                <select
                  value={selectedNamespace}
                  onChange={(e) => setSelectedNamespace(e.target.value)}
                  className="bg-slate-900 border border-slate-800 text-slate-300 text-xs rounded px-2 py-1"
                >
                  <option value="all">All Namespaces</option>
                  <option value="default">default</option>
                  <option value="kube-system">kube-system</option>
                  <option value="monitoring">monitoring</option>
                  <option value="production">production</option>
                </select>
              </div>
            </div>

            {loading ? (
              <div className="py-8 text-center text-xs text-slate-400 font-mono">
                Querying Kubernetes API Server...
              </div>
            ) : pods.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500">
                No active pods found in this namespace.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs font-mono">
                  <thead className="text-slate-400 border-b border-slate-800 font-sans">
                    <tr>
                      <th className="pb-3">Pod Name</th>
                      <th className="pb-3">Namespace</th>
                      <th className="pb-3">Node</th>
                      <th className="pb-3">Status</th>
                      <th className="pb-3">Restarts</th>
                      <th className="pb-3">IP</th>
                      <th className="pb-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {pods.map((p, idx) => (
                      <tr key={idx} className="hover:bg-slate-800/30 transition-colors">
                        <td className="py-3 text-slate-200 font-medium">{p.name}</td>
                        <td className="py-3 text-indigo-400">{p.namespace}</td>
                        <td className="py-3 text-slate-400">{p.nodeName}</td>
                        <td className="py-3">
                          <Badge variant={p.status === 'Running' ? 'emerald' : 'rose'}>
                            {p.status}
                          </Badge>
                        </td>
                        <td className={`py-3 ${p.restartCount > 0 ? 'text-rose-400 font-bold' : 'text-slate-400'}`}>
                          {p.restartCount}
                        </td>
                        <td className="py-3 text-slate-400">{p.podIp}</td>
                        <td className="py-3 text-right">
                          <button
                            onClick={() => handleViewLogs(p.name, p.namespace)}
                            className="inline-flex items-center gap-1 text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 px-2 py-1 rounded text-2xs transition"
                          >
                            <Terminal className="w-3 h-3 text-indigo-400" />
                            Logs
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* Logs Drawer Modal */}
      {activeLogPod && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl w-full max-w-4xl max-h-[85vh] flex flex-col shadow-2xl">
            <div className="px-5 py-3.5 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Terminal className="w-4 h-4 text-indigo-400" />
                <span className="text-xs font-mono font-bold text-slate-200">
                  {activeLogNamespace}/{activeLogPod} - Pod Logs
                </span>
              </div>
              <button
                onClick={() => setActiveLogPod(null)}
                className="text-slate-400 hover:text-white text-xs px-2 py-1 rounded hover:bg-slate-800"
              >
                Close
              </button>
            </div>
            <div className="p-4 overflow-y-auto font-mono text-2xs bg-slate-950/80 flex-1 space-y-1 text-slate-300">
              {logsLoading ? (
                <div className="py-8 text-center text-slate-400 animate-pulse">Streaming pod logs...</div>
              ) : podLogs.length === 0 ? (
                <div className="text-slate-500">No logs output recorded for this container.</div>
              ) : (
                podLogs.map((l, i) => (
                  <div key={i} className="leading-relaxed hover:bg-slate-900/50 px-1 rounded">
                    {l}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
