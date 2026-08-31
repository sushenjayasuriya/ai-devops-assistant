import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { incidentService } from '../../services/incidentService';
import { useNavigate } from 'react-router-dom';
import { AlertOctagon, Sparkles, CheckCircle2, Clock, ShieldAlert } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const IncidentListPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: incidents, isLoading } = useQuery({
    queryKey: ['incidents', selectedEnv?.id],
    queryFn: () => incidentService.getIncidents(selectedEnv?.id),
    refetchInterval: 5000,
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      incidentService.updateIncidentStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
      queryClient.invalidateQueries({ queryKey: ['overview'] });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 tracking-tight">Active & Historical Incidents</h2>
          <p className="text-xs text-slate-400">
            Real-time anomaly detection, correlation timelines, and AI root-cause diagnostics
          </p>
        </div>
      </div>

      <div className="space-y-4">
        {isLoading ? (
          <div className="p-8 text-center text-xs text-slate-400">Loading incident stream...</div>
        ) : !incidents || incidents.length === 0 ? (
          <div className="p-12 glass-card rounded-xl border border-slate-800 text-center space-y-2">
            <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
            <h3 className="text-sm font-semibold text-slate-200">No Open Incidents</h3>
            <p className="text-xs text-slate-400">All infrastructure systems operating within normal parameters.</p>
          </div>
        ) : (
          incidents.map((incident) => (
            <div
              key={incident.id}
              className="glass-card rounded-xl p-6 border border-slate-800 hover:border-slate-700 transition-all space-y-4 shadow-lg"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className={`p-2.5 rounded-xl ${
                    incident.severity === 'HIGH' || incident.severity === 'CRITICAL'
                      ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                      : 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
                  }`}>
                    <AlertOctagon className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-slate-100 text-sm">{incident.title}</h3>
                      <Badge variant={incident.severity === 'HIGH' ? 'rose' : 'amber'}>
                        {incident.severity}
                      </Badge>
                      <Badge variant={incident.status === 'OPEN' ? 'rose' : 'emerald'}>
                        {incident.status}
                      </Badge>
                    </div>
                    <p className="text-xs text-slate-400 font-mono mt-0.5">
                      Resource: <span className="text-indigo-400">{incident.affectedResourceId}</span> • Environment: {incident.environment?.name || 'PRODUCTION'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => navigate(`/ai?prompt=${encodeURIComponent(`Investigate incident: ${incident.title} on ${incident.affectedResourceId}`)}`)}
                    className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold shadow-md shadow-indigo-600/20 transition-all flex items-center gap-1.5"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>AI Deep Investigation</span>
                  </button>

                  {incident.status === 'OPEN' && (
                    <button
                      onClick={() => resolveMutation.mutate({ id: incident.id, status: 'RESOLVED' })}
                      className="px-3.5 py-2 bg-slate-800 hover:bg-emerald-950/50 hover:text-emerald-400 hover:border-emerald-500/30 text-slate-300 border border-slate-700 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Mark Resolved</span>
                    </button>
                  )}
                </div>
              </div>

              <p className="text-xs text-slate-300 bg-slate-900/50 p-3.5 rounded-lg border border-slate-800/80 leading-relaxed">
                {incident.description}
              </p>

              {incident.rootCauseSummary && (
                <div className="p-3.5 bg-indigo-950/20 border border-indigo-500/30 rounded-lg text-xs space-y-1">
                  <div className="text-indigo-300 font-semibold flex items-center gap-1.5">
                    <ShieldAlert className="w-3.5 h-3.5 text-indigo-400" />
                    <span>Correlated Root Cause Diagnosis:</span>
                  </div>
                  <p className="text-indigo-200/90">{incident.rootCauseSummary}</p>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};
