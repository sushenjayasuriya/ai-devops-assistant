import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Activity, Play, Terminal, CheckCircle2 } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

export const MetricsExplorerPage: React.FC = () => {
  const [promQl, setPromQl] = useState('container_cpu_usage_percent{container="thingsboard"}');
  const [queryResult, setQueryResult] = useState<any | null>(null);
  const [running, setRunning] = useState(false);

  const { data: targets } = useQuery({
    queryKey: ['prometheus-targets'],
    queryFn: async () => {
      const res = await api.get('/metrics/prometheus/targets');
      return res.data.data;
    },
  });

  const handleExecuteQuery = async (queryToRun: string) => {
    setRunning(true);
    try {
      const res = await api.get('/metrics/prometheus/query', { params: { query: queryToRun } });
      setQueryResult(res.data.data);
    } catch (err: any) {
      console.error(err);
    } finally {
      setRunning(false);
    }
  };

  const PRESETS = [
    'container_cpu_usage_percent{container="thingsboard"}',
    'container_memory_usage_bytes{container="thingsboard"}',
    'http_requests_total{status="500"}',
    'node_load1'
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Prometheus Metrics Explorer</h2>
        <p className="text-xs text-slate-400">
          Execute controlled PromQL telemetry expressions and inspect scrape target health
        </p>
      </div>

      {/* PromQL Query Bar */}
      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
          <Terminal className="w-4 h-4 text-indigo-400" />
          <span>PromQL Expression Engine</span>
        </div>

        <div className="flex items-center gap-3">
          <input
            type="text"
            value={promQl}
            onChange={(e) => setPromQl(e.target.value)}
            className="flex-1 px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-xs font-mono text-slate-100 focus:outline-none focus:border-indigo-500"
            placeholder="e.g. rate(http_requests_total[5m])"
          />
          <button
            onClick={() => handleExecuteQuery(promQl)}
            disabled={running}
            className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-medium transition-all shadow-md shadow-indigo-600/20 flex items-center gap-1.5"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Execute</span>
          </button>
        </div>

        <div className="flex items-center gap-2 overflow-x-auto text-[11px]">
          <span className="text-slate-500 font-medium shrink-0">Presets:</span>
          {PRESETS.map((p, idx) => (
            <button
              key={idx}
              onClick={() => {
                setPromQl(p);
                handleExecuteQuery(p);
              }}
              className="px-2.5 py-1 bg-slate-800/80 hover:bg-slate-700 text-slate-300 rounded font-mono shrink-0 text-[10px]"
            >
              {p}
            </button>
          ))}
        </div>

        {queryResult && (
          <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-2">
            <span className="text-xs font-medium text-slate-400 font-mono">Query Result Vector:</span>
            <pre className="text-xs font-mono text-emerald-400 overflow-x-auto">
              {JSON.stringify(queryResult, null, 2)}
            </pre>
          </div>
        )}
      </div>

      {/* Scrape Targets */}
      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <h3 className="text-sm font-semibold text-slate-100">Prometheus Scrape Target Health</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="text-slate-400 border-b border-slate-800 font-sans">
              <tr>
                <th className="pb-3">Scrape Job</th>
                <th className="pb-3">Health</th>
                <th className="pb-3">Scrape URL</th>
                <th className="pb-3">Latency (ms)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {targets?.map((t: any, idx: number) => (
                <tr key={idx} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 text-slate-200">{t.job}</td>
                  <td className="py-3">
                    <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px]">
                      {t.health}
                    </span>
                  </td>
                  <td className="py-3 text-slate-400">{t.scrapeUrl}</td>
                  <td className="py-3 text-indigo-400">{t.lastScrapeDurationMs}ms</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
