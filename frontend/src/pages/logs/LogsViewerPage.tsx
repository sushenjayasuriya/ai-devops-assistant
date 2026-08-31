import React, { useState } from 'react';
import { FileText, Search, RefreshCw } from 'lucide-react';

export const LogsViewerPage: React.FC = () => {
  const [filter, setFilter] = useState('');
  const [selectedSource, setSelectedSource] = useState('thingsboard-core-app');

  const rawLogs = [
    { time: '12:08:15', level: 'INFO', msg: 'Starting Thingsboard Server Application v3.6.2...' },
    { time: '12:08:25', level: 'INFO', msg: 'HikariPool-1 - Starting connection initialization...' },
    { time: '12:09:40', level: 'WARN', msg: 'Telemetry queue buffer saturation at 98% capacity' },
    { time: '12:10:15', level: 'ERROR', msg: 'HikariPool-1 - Connection is not available, request timed out after 30005ms.' },
    { time: '12:10:30', level: 'ERROR', msg: 'org.springframework.dao.CannotAcquireLockException: could not execute statement' },
    { time: '12:10:50', level: 'ERROR', msg: 'Fatal thread starvation in event processor. Initiating emergency shutdown.' },
    { time: '12:10:58', level: 'WARN', msg: 'Exiting with code 137 (OOM / Thread Deadlock)' },
  ];

  const filteredLogs = rawLogs.filter(
    (l) =>
      l.msg.toLowerCase().includes(filter.toLowerCase()) ||
      l.level.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Structured Logs & Anomaly Stream</h2>
        <p className="text-xs text-slate-400">
          Inspect container logs, Loki streams, and crash backtraces
        </p>
      </div>

      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <select
              value={selectedSource}
              onChange={(e) => setSelectedSource(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-200 focus:outline-none"
            >
              <option value="thingsboard-core-app">thingsboard-core-app (PROD)</option>
              <option value="postgres-production-cluster">postgres-production-cluster (PROD)</option>
              <option value="redis-session-cache">redis-session-cache (PROD)</option>
            </select>
          </div>

          <div className="relative w-full sm:w-72">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
            <input
              type="text"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Filter log stream..."
              className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-100 focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>

        <div className="bg-black/90 p-4 rounded-xl border border-slate-800 font-mono text-xs max-h-[500px] overflow-y-auto space-y-1.5">
          {filteredLogs.map((log, idx) => (
            <div key={idx} className="flex items-start gap-3">
              <span className="text-slate-500 shrink-0">{log.time}</span>
              <span
                className={`px-1.5 py-0.5 rounded text-[10px] shrink-0 ${
                  log.level === 'ERROR'
                    ? 'bg-rose-500/20 text-rose-400'
                    : log.level === 'WARN'
                    ? 'bg-amber-500/20 text-amber-400'
                    : 'bg-slate-800 text-slate-400'
                }`}
              >
                {log.level}
              </span>
              <span
                className={
                  log.level === 'ERROR'
                    ? 'text-rose-300'
                    : log.level === 'WARN'
                    ? 'text-amber-200'
                    : 'text-slate-300'
                }
              >
                {log.msg}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
