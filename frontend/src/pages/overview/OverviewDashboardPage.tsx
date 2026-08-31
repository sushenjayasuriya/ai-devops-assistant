import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { incidentService } from '../../services/incidentService';
import { Link, useNavigate } from 'react-router-dom';
import {
  Activity,
  Server,
  Box,
  AlertOctagon,
  GitBranch,
  Sparkles,
  ArrowUpRight,
  TrendingUp,
  Cpu,
  HardDrive
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid
} from 'recharts';

const MOCK_TIME_SERIES = [
  { time: '11:40', cpu: 22, memory: 45, latency: 12 },
  { time: '11:45', cpu: 24, memory: 46, latency: 14 },
  { time: '11:50', cpu: 25, memory: 48, latency: 15 },
  { time: '11:55', cpu: 65, memory: 68, latency: 85 }, // Deployment event
  { time: '12:00', cpu: 88, memory: 84, latency: 240 },
  { time: '12:05', cpu: 94, memory: 91, latency: 450 },
  { time: '12:10', cpu: 94, memory: 90, latency: 420 },
];

export const OverviewDashboardPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const navigate = useNavigate();

  const { data: stats } = useQuery({
    queryKey: ['overview', selectedEnv?.id],
    queryFn: () => infraService.getOverview(selectedEnv?.id),
    refetchInterval: 5000,
  });

  const { data: incidents } = useQuery({
    queryKey: ['incidents', selectedEnv?.id],
    queryFn: () => incidentService.getIncidents(selectedEnv?.id, 'OPEN'),
  });

  const { data: deployments } = useQuery({
    queryKey: ['deployments', selectedEnv?.id],
    queryFn: () => infraService.getDeployments(selectedEnv?.id),
  });

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight">Infrastructure Overview</h2>
          <p className="text-xs text-slate-400">
            Real-time observability & telemetry for <span className="text-indigo-400 font-semibold">{selectedEnv?.name || 'All Environments'}</span>
          </p>
        </div>

        <Link
          to="/ai"
          className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-indigo-600 to-cyan-600 hover:from-indigo-500 hover:to-cyan-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-500/20 transition-all group"
        >
          <Sparkles className="w-4 h-4" />
          <span>Launch AI SRE Copilot</span>
          <ArrowUpRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
        </Link>
      </div>

      {/* KPI Telemetry Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Health Summary Card */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Platform Health</span>
            <Activity className="w-4 h-4 text-slate-500" />
          </div>
          <div className="flex items-center gap-2.5">
            <span
              className={`w-3 h-3 rounded-full ${
                stats?.healthSummary === 'HEALTHY' ? 'bg-emerald-400' : 'bg-rose-500 animate-ping'
              }`}
            ></span>
            <span className="text-xl font-bold font-mono text-slate-100">
              {stats?.healthSummary || 'HEALTHY'}
            </span>
          </div>
          <p className="text-[11px] text-slate-400">
            {stats?.openIncidentsCount || 0} active incident(s) detected
          </p>
        </div>

        {/* Servers Card */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Linux Hosts</span>
            <Server className="w-4 h-4 text-slate-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-slate-100">
            {stats?.serverCount || 3}
          </div>
          <p className="text-[11px] text-emerald-400 font-mono">100% Agent Scrape OK</p>
        </div>

        {/* Containers Card */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Containers</span>
            <Box className="w-4 h-4 text-slate-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-slate-100">
            {stats?.containerCount || 3}
          </div>
          <p className="text-[11px] text-amber-400 font-mono">1 Container CrashLoop</p>
        </div>

        {/* Deployments Card */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Deployments (24h)</span>
            <GitBranch className="w-4 h-4 text-slate-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-slate-100">
            {stats?.recentDeploymentsCount || 1}
          </div>
          <p className="text-[11px] text-indigo-400 font-mono">Latest: v3.6.2-patch184</p>
        </div>
      </div>

      {/* Real-time Telemetry Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 glass-card rounded-xl p-5 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-slate-100">Cluster CPU & Memory Utilization</h3>
              <p className="text-xs text-slate-400">Prometheus telemetry spike following Jenkins release #184</p>
            </div>
            <div className="flex items-center gap-3 text-xs font-mono">
              <span className="flex items-center gap-1 text-rose-400">
                <span className="w-2 h-2 rounded bg-rose-400"></span> CPU %
              </span>
              <span className="flex items-center gap-1 text-cyan-400">
                <span className="w-2 h-2 rounded bg-cyan-400"></span> Memory %
              </span>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={MOCK_TIME_SERIES}>
                <defs>
                  <linearGradient id="cpuGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#f43f5e" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="memGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#06b6d4" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} domain={[0, 100]} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="cpu" stroke="#f43f5e" fillOpacity={1} fill="url(#cpuGrad)" />
                <Area type="monotone" dataKey="memory" stroke="#06b6d4" fillOpacity={1} fill="url(#memGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Active Incident Quick Spotlight */}
        <div className="glass-card rounded-xl p-5 border border-rose-500/30 glow-rose flex flex-col justify-between space-y-4">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-rose-400 uppercase tracking-wider flex items-center gap-1.5">
                <AlertOctagon className="w-4 h-4" />
                Active Anomaly Spotlight
              </span>
              <span className="px-2 py-0.5 bg-rose-500/20 text-rose-300 text-[10px] rounded font-mono font-bold">
                HIGH SEVERITY
              </span>
            </div>

            {incidents && incidents.length > 0 ? (
              <div className="space-y-2">
                <h4 className="font-semibold text-slate-100 text-sm">{incidents[0].title}</h4>
                <p className="text-xs text-slate-300 line-clamp-3">{incidents[0].description}</p>
                <div className="p-2.5 bg-slate-900/90 rounded-lg text-xs font-mono space-y-1 text-slate-400 border border-slate-800">
                  <div>Resource: <span className="text-indigo-300">{incidents[0].affectedResourceId}</span></div>
                  <div>Root Cause Confidence: <span className="text-emerald-400 font-bold">91%</span></div>
                </div>
              </div>
            ) : (
              <p className="text-xs text-slate-400">No active incidents detected.</p>
            )}
          </div>

          <button
            onClick={() => navigate('/ai?prompt=' + encodeURIComponent('Investigate the ThingsBoard server high CPU and latency incident'))}
            className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-medium transition-all shadow-lg shadow-indigo-600/25 flex items-center justify-center gap-2"
          >
            <Sparkles className="w-4 h-4" />
            <span>Investigate with AI</span>
          </button>
        </div>
      </div>

      {/* Recent Deployments Table */}
      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-slate-100">Recent Production Releases</h3>
            <p className="text-xs text-slate-400">CI/CD Jenkins and GitHub pipeline trigger history</p>
          </div>
          <Link to="/deployments" className="text-xs text-indigo-400 hover:text-indigo-300">
            View all →
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-slate-400 border-b border-slate-800">
              <tr>
                <th className="pb-3 font-medium">Service</th>
                <th className="pb-3 font-medium">Version Tag</th>
                <th className="pb-3 font-medium">Commit SHA</th>
                <th className="pb-3 font-medium">Triggered By</th>
                <th className="pb-3 font-medium">Status</th>
                <th className="pb-3 font-medium">Deployed At</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {deployments?.map((dep) => (
                <tr key={dep.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 font-sans font-medium text-slate-200">{dep.serviceName}</td>
                  <td className="py-3 text-indigo-400">{dep.versionTag}</td>
                  <td className="py-3 text-slate-400">{dep.commitSha}</td>
                  <td className="py-3 text-slate-300 font-sans">{dep.deployedBy}</td>
                  <td className="py-3">
                    <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      {dep.status}
                    </span>
                  </td>
                  <td className="py-3 text-slate-400 font-sans">
                    {new Date(dep.startedAt).toLocaleTimeString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
