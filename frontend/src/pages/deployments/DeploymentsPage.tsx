import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEnvironment } from '../../context/EnvironmentContext';
import { infraService } from '../../services/infraService';
import { GitBranch, User, Calendar, CheckCircle2 } from 'lucide-react';
import { Badge } from '../../components/common/Badge';

export const DeploymentsPage: React.FC = () => {
  const { selectedEnv } = useEnvironment();

  const { data: deployments, isLoading } = useQuery({
    queryKey: ['deployments', selectedEnv?.id],
    queryFn: () => infraService.getDeployments(selectedEnv?.id),
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Deployments & Release Pipeline</h2>
        <p className="text-xs text-slate-400">
          CI/CD build provenance, changelogs, and incident correlation history
        </p>
      </div>

      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="text-slate-400 border-b border-slate-800 font-sans">
              <tr>
                <th className="pb-3">Service Name</th>
                <th className="pb-3">Version Tag</th>
                <th className="pb-3">Commit SHA</th>
                <th className="pb-3">Deployed By</th>
                <th className="pb-3">Status</th>
                <th className="pb-3">Changelog</th>
                <th className="pb-3">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {deployments?.map((dep) => (
                <tr key={dep.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 font-sans font-medium text-slate-200">{dep.serviceName}</td>
                  <td className="py-3 text-indigo-400">{dep.versionTag}</td>
                  <td className="py-3 text-slate-400">{dep.commitSha}</td>
                  <td className="py-3 text-slate-300 font-sans">{dep.deployedBy}</td>
                  <td className="py-3">
                    <Badge variant={dep.status === 'SUCCESS' ? 'emerald' : 'rose'}>
                      {dep.status}
                    </Badge>
                  </td>
                  <td className="py-3 text-slate-400 font-sans max-w-xs truncate">{dep.changelog}</td>
                  <td className="py-3 text-slate-400 font-sans">
                    {new Date(dep.startedAt).toLocaleString()}
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
