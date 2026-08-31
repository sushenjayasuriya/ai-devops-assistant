import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { Server, Activity, ShieldCheck, Terminal, Cpu, HardDrive } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const ServersPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const [activeServerMetrics, setActiveServerMetrics] = useState<any | null>(null);

  const { data: servers, isLoading } = useQuery({
    queryKey: ['servers', selectedEnv?.id],
    queryFn: () => infraService.getServers(selectedEnv?.id),
  });

  const handleInspectMetrics = async (serverId: string) => {
    const metrics = await infraService.getServerMetrics(serverId);
    setActiveServerMetrics(metrics);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight">Linux Hosts & Node Infrastructure</h2>
          <p className="text-xs text-slate-400">
            Secure, allowlist-bounded host monitoring agent & OS telemetry
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {servers?.map((server) => (
          <div
            key={server.id}
            className="glass-card rounded-xl p-5 border border-slate-800 space-y-4 hover:border-slate-700 transition-all flex flex-col justify-between"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <Server className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-100 text-xs font-mono">{server.hostname}</h3>
                    <p className="text-[11px] text-slate-400 font-mono">{server.ipAddress}</p>
                  </div>
                </div>
                <Badge variant={server.status === 'ONLINE' ? 'emerald' : 'rose'}>
                  {server.status}
                </Badge>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-lg text-xs space-y-1 text-slate-300 font-mono border border-slate-800/80">
                <div>OS: <span className="text-slate-400 font-sans">{server.osInfo || 'Ubuntu 22.04 LTS'}</span></div>
                <div>Environment: <span className="text-indigo-400">{server.environment?.name || 'PRODUCTION'}</span></div>
              </div>
            </div>

            <button
              onClick={() => handleInspectMetrics(server.id)}
              className="w-full py-2 bg-slate-800 hover:bg-indigo-600 hover:text-white text-slate-300 rounded-lg text-xs font-medium border border-slate-700 hover:border-indigo-500 transition-all flex items-center justify-center gap-2"
            >
              <Activity className="w-3.5 h-3.5" />
              <span>Inspect Live Telemetry</span>
            </button>
          </div>
        ))}
      </div>

      {/* Live Server Telemetry Modal */}
      {activeServerMetrics && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="max-w-2xl w-full glass-card rounded-2xl p-6 border border-slate-700 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Terminal className="w-5 h-5 text-indigo-400" />
                <h3 className="text-sm font-semibold text-slate-100 font-mono">
                  {activeServerMetrics.hostname} ({activeServerMetrics.ipAddress})
                </h3>
              </div>
              <button
                onClick={() => setActiveServerMetrics(null)}
                className="text-slate-400 hover:text-white text-xs px-2 py-1 bg-slate-800 rounded"
              >
                ✕ Close
              </button>
            </div>

            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800">
                <span className="text-[10px] text-slate-400">CPU Usage</span>
                <p className="text-lg font-bold font-mono text-rose-400">{activeServerMetrics.cpuUsagePercent}%</p>
              </div>
              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800">
                <span className="text-[10px] text-slate-400">RAM Usage</span>
                <p className="text-lg font-bold font-mono text-cyan-400">{activeServerMetrics.memoryUsagePercent}%</p>
              </div>
              <div className="p-3 bg-slate-900/80 rounded-xl border border-slate-800">
                <span className="text-[10px] text-slate-400">Load (1m / 5m / 15m)</span>
                <p className="text-xs font-bold font-mono text-amber-400 mt-1">
                  {activeServerMetrics.loadAverage1m}, {activeServerMetrics.loadAverage5m}, {activeServerMetrics.loadAverage15m}
                </p>
              </div>
            </div>

            <div className="space-y-2">
              <span className="text-xs font-semibold text-slate-300">Top Running Host Processes</span>
              <div className="bg-slate-900 rounded-lg p-3 text-xs font-mono border border-slate-800 space-y-1.5">
                {activeServerMetrics.topProcesses?.map((p: any) => (
                  <div key={p.pid} className="flex items-center justify-between text-slate-300">
                    <span>[{p.pid}] {p.name}</span>
                    <span className="text-rose-400">CPU: {p.cpu}% | MEM: {p.mem}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
