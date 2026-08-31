import React from 'react';
import { Cpu, CheckCircle2, AlertTriangle, ShieldCheck, Box } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const KubernetesPage: React.FC = () => {
  const pods = [
    { name: 'thingsboard-app-69c7f7d79b-z9k2q', namespace: 'production', status: 'CrashLoopBackOff', restarts: 7, cpu: '940m', memory: '1840Mi', ready: '0/1' },
    { name: 'postgres-ha-postgresql-0', namespace: 'production', status: 'Running', restarts: 0, cpu: '120m', memory: '512Mi', ready: '1/1' },
    { name: 'redis-master-0', namespace: 'production', status: 'Running', restarts: 0, cpu: '35m', memory: '128Mi', ready: '1/1' },
    { name: 'prometheus-k8s-0', namespace: 'monitoring', status: 'Running', restarts: 0, cpu: '210m', memory: '1024Mi', ready: '2/2' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Kubernetes Cluster & Pod Mesh</h2>
        <p className="text-xs text-slate-400">
          K8s workloads, node pools, namespaces, and pod health status
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Cluster Status</span>
          <div className="text-lg font-bold font-mono text-emerald-400 flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
            <span>HEALTHY (v1.29.2)</span>
          </div>
        </div>
        <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Control Plane & Workers</span>
          <div className="text-lg font-bold font-mono text-slate-100">3 Ready / 0 Unready</div>
        </div>
        <div className="glass-card rounded-xl p-4 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">CrashLoop Pods</span>
          <div className="text-lg font-bold font-mono text-rose-400">1 Pod Alerting</div>
        </div>
      </div>

      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <h3 className="text-sm font-semibold text-slate-100">Workload Pods</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="text-slate-400 border-b border-slate-800 font-sans">
              <tr>
                <th className="pb-3">Pod Name</th>
                <th className="pb-3">Namespace</th>
                <th className="pb-3">Ready</th>
                <th className="pb-3">Status</th>
                <th className="pb-3">Restarts</th>
                <th className="pb-3">CPU</th>
                <th className="pb-3">Memory</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {pods.map((p, idx) => (
                <tr key={idx} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 text-slate-200">{p.name}</td>
                  <td className="py-3 text-indigo-400">{p.namespace}</td>
                  <td className="py-3 text-slate-400">{p.ready}</td>
                  <td className="py-3">
                    <Badge variant={p.status === 'Running' ? 'emerald' : 'rose'}>
                      {p.status}
                    </Badge>
                  </td>
                  <td className={`py-3 ${p.restarts > 0 ? 'text-rose-400 font-bold' : 'text-slate-400'}`}>{p.restarts}</td>
                  <td className="py-3 text-slate-300">{p.cpu}</td>
                  <td className="py-3 text-slate-300">{p.memory}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
