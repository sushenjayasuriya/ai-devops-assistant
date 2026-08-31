import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { Box, RotateCw, Square, Play, FileText, AlertTriangle, CheckCircle2, Activity } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const DockerPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const queryClient = useQueryClient();

  const [activeLogContainer, setActiveLogContainer] = useState<{ id: string; name: string } | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'approval' | 'error'; message: string } | null>(null);

  const { data: containers, isLoading } = useQuery({
    queryKey: ['containers', selectedEnv?.id],
    queryFn: () => infraService.getContainers(selectedEnv?.id),
    refetchInterval: 4000,
  });

  const actionMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: string }) =>
      infraService.executeContainerAction(id, action),
    onSuccess: (data) => {
      setFeedback({
        type: 'success',
        message: `Action '${data.action}' executed successfully on container '${data.name}'. State is now: ${data.newState}.`
      });
      queryClient.invalidateQueries({ queryKey: ['containers'] });
    },
    onError: (err: any) => {
      if (err.response?.data?.code === 'APPROVAL_REQUIRED') {
        setFeedback({
          type: 'approval',
          message: err.response.data.message
        });
      } else {
        setFeedback({
          type: 'error',
          message: err.response?.data?.message || err.message
        });
      }
    }
  });

  const handleOpenLogs = async (container: { id: string; name: string }) => {
    setActiveLogContainer(container);
    try {
      const containerLogs = await infraService.getContainerLogs(container.id, 50);
      setLogs(containerLogs);
    } catch (err: any) {
      setLogs([`Failed to fetch logs: ${err?.response?.data?.message || err.message}`]);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight">Docker Runtime & Containers</h2>
          <p className="text-xs text-slate-400">
            Container lifecycle operations with strict production approval gating
          </p>
        </div>
      </div>

      {feedback && (
        <div className={`p-4 rounded-xl text-xs flex items-center justify-between gap-3 border ${
          feedback.type === 'success'
            ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-300'
            : feedback.type === 'approval'
            ? 'bg-amber-950/30 border-amber-500/40 text-amber-300 glow-amber'
            : 'bg-rose-950/20 border-rose-500/30 text-rose-300'
        }`}>
          <div className="flex items-center gap-2">
            {feedback.type === 'approval' ? (
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-slate-400 hover:text-white text-xs px-2 py-0.5 bg-slate-900 rounded"
          >
            ✕
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {containers?.map((container) => (
          <div
            key={container.id}
            className="glass-card rounded-xl p-5 border border-slate-800 space-y-4 hover:border-slate-700 transition-all flex flex-col justify-between"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <Box className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-100 text-xs font-mono">{container.name}</h3>
                    <p className="text-[11px] text-slate-400 font-mono">{container.containerId}</p>
                  </div>
                </div>

                <Badge
                  variant={
                    container.state === 'RUNNING'
                      ? 'emerald'
                      : container.state === 'RESTARTING'
                      ? 'rose'
                      : 'slate'
                  }
                >
                  {container.state}
                </Badge>
              </div>

              <div className="p-3 bg-slate-900/60 rounded-lg text-xs space-y-1.5 text-slate-300 font-mono border border-slate-800/80">
                <div>Image: <span className="text-slate-400 font-sans">{container.image}</span></div>
                <div>
                  Restarts: <span className={container.restartCount > 3 ? 'text-rose-400 font-bold' : 'text-slate-300'}>{container.restartCount}</span>
                </div>
                <div>Environment: <span className="text-indigo-400">{container.environment?.name || 'PRODUCTION'}</span></div>
              </div>
            </div>

            <div className="space-y-2 pt-2">
              <div className="grid grid-cols-3 gap-1.5 text-[11px]">
                <button
                  onClick={() => actionMutation.mutate({ id: container.id, action: 'restart' })}
                  className="py-1.5 bg-slate-800 hover:bg-indigo-600 hover:text-white text-slate-300 rounded-lg border border-slate-700 transition-all flex items-center justify-center gap-1 font-medium"
                >
                  <RotateCw className="w-3 h-3" />
                  <span>Restart</span>
                </button>
                <button
                  onClick={() => actionMutation.mutate({ id: container.id, action: 'stop' })}
                  className="py-1.5 bg-slate-800 hover:bg-rose-600 hover:text-white text-slate-300 rounded-lg border border-slate-700 transition-all flex items-center justify-center gap-1 font-medium"
                >
                  <Square className="w-3 h-3" />
                  <span>Stop</span>
                </button>
                <button
                  onClick={() => actionMutation.mutate({ id: container.id, action: 'start' })}
                  className="py-1.5 bg-slate-800 hover:bg-emerald-600 hover:text-white text-slate-300 rounded-lg border border-slate-700 transition-all flex items-center justify-center gap-1 font-medium"
                >
                  <Play className="w-3 h-3" />
                  <span>Start</span>
                </button>
              </div>

              <button
                onClick={() => handleOpenLogs(container)}
                className="w-full py-1.5 bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-slate-200 rounded-lg text-xs border border-slate-800 transition-all flex items-center justify-center gap-1.5"
              >
                <FileText className="w-3 h-3" />
                <span>View Container Logs</span>
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Container Logs Viewer Modal */}
      {activeLogContainer && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="max-w-3xl w-full glass-card rounded-2xl p-6 border border-slate-700 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <FileText className="w-5 h-5 text-indigo-400" />
                <h3 className="text-sm font-semibold text-slate-100 font-mono">
                  Logs for: {activeLogContainer.name}
                </h3>
              </div>
              <button
                onClick={() => setActiveLogContainer(null)}
                className="text-slate-400 hover:text-white text-xs px-2.5 py-1 bg-slate-800 rounded"
              >
                ✕ Close
              </button>
            </div>

            <div className="bg-black/90 p-4 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 max-h-96 overflow-y-auto space-y-1">
              {logs.map((line, idx) => (
                <div
                  key={idx}
                  className={
                    line.includes('[ERROR]') || line.includes('Exception')
                      ? 'text-rose-400'
                      : line.includes('[WARN]')
                      ? 'text-amber-400'
                      : 'text-slate-400'
                  }
                >
                  {line}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
