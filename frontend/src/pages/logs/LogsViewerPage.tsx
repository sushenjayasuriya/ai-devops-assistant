import React, { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { FileText, Search, RefreshCw, Terminal, AlertTriangle } from 'lucide-react';
import { Container } from '../../types/infrastructure';

export const LogsViewerPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const [selectedContainerId, setSelectedContainerId] = useState<string>('');
  const [filter, setFilter] = useState('');
  const [logs, setLogs] = useState<string[]>([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: containers } = useQuery({
    queryKey: ['containers', selectedEnv?.id],
    queryFn: () => infraService.getContainers(selectedEnv?.id),
  });

  useEffect(() => {
    if (containers && containers.length > 0 && !selectedContainerId) {
      setSelectedContainerId(containers[0].id);
    }
  }, [containers, selectedContainerId]);

  useEffect(() => {
    if (selectedContainerId) {
      fetchLogs(selectedContainerId);
    }
  }, [selectedContainerId]);

  const fetchLogs = async (containerId: string) => {
    try {
      setLoadingLogs(true);
      setError(null);
      const rawLogs = await infraService.getContainerLogs(containerId, 150);
      setLogs(rawLogs);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to stream logs from container');
      setLogs([]);
    } finally {
      setLoadingLogs(false);
    }
  };

  const filteredLogs = logs.filter((line) =>
    line.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight flex items-center gap-2">
            <Terminal className="w-5 h-5 text-indigo-400" />
            Structured Logs & Container Console Stream
          </h2>
          <p className="text-xs text-slate-400">
            Real live stdout/stderr streams from infrastructure containers and workloads
          </p>
        </div>

        <div className="flex items-center gap-3">
          {selectedContainerId && (
            <button
              onClick={() => fetchLogs(selectedContainerId)}
              disabled={loadingLogs}
              className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold px-3 py-1.5 rounded-lg border border-slate-700 transition"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingLogs ? 'animate-spin' : ''}`} />
              Refresh Stream
            </button>
          )}
        </div>
      </div>

      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <select
              value={selectedContainerId}
              onChange={(e) => setSelectedContainerId(e.target.value)}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {containers?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.state})
                </option>
              ))}
            </select>
          </div>

          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
            <input
              type="text"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Search or grep log stream..."
              className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-slate-100 focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>

        {error && (
          <div className="p-3 bg-rose-950/30 border border-rose-500/40 text-rose-300 text-xs rounded-lg flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="bg-black/90 p-4 rounded-xl border border-slate-800 font-mono text-xs max-h-[550px] overflow-y-auto space-y-1.5">
          {loadingLogs ? (
            <div className="py-8 text-center text-slate-400 animate-pulse">
              Streaming console logs from container...
            </div>
          ) : filteredLogs.length === 0 ? (
            <div className="py-8 text-center text-slate-500">
              No matching log output recorded for this container.
            </div>
          ) : (
            filteredLogs.map((line, idx) => {
              const isError = line.includes('[ERROR]') || line.includes('Exception') || line.includes('FATAL');
              const isWarn = line.includes('[WARN]') || line.includes('WARNING');
              return (
                <div
                  key={idx}
                  className={`leading-relaxed px-1 py-0.5 rounded hover:bg-slate-900/60 ${
                    isError ? 'text-rose-400 font-semibold' : isWarn ? 'text-amber-300' : 'text-slate-300'
                  }`}
                >
                  {line}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
