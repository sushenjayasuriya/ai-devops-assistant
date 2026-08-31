import React from 'react';
import { useEnvironment } from '../../context/EnvironmentContext';
import { useAuth } from '../../context/AuthContext';
import { Shield, Bell, LogOut, Globe, AlertTriangle } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Header: React.FC = () => {
  const { environments, selectedEnv, setSelectedEnv } = useEnvironment();
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-surface/80 backdrop-blur-md border-b border-surface-border sticky top-0 z-30 flex items-center justify-between px-6">
      {/* Environment Selector */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 bg-slate-900/90 border border-slate-700/80 rounded-lg px-3 py-1.5">
          <Globe className="w-3.5 h-3.5 text-indigo-400" />
          <span className="text-xs text-slate-400 font-medium">Environment:</span>
          <select
            value={selectedEnv?.id || ''}
            onChange={(e) => {
              const env = environments.find((x) => x.id === e.target.value) || null;
              setSelectedEnv(env);
            }}
            className="bg-transparent text-xs font-mono font-semibold text-slate-200 focus:outline-none cursor-pointer"
          >
            {environments.map((env) => (
              <option key={env.id} value={env.id} className="bg-slate-900 text-slate-200">
                {env.name} {env.isProduction ? '⚡ [PROD]' : ''}
              </option>
            ))}
          </select>
        </div>

        {selectedEnv?.isProduction && (
          <span className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-mono font-semibold">
            <AlertTriangle className="w-3 h-3" />
            STRICT APPROVAL GATED
          </span>
        )}
      </div>

      {/* Right User Actions */}
      <div className="flex items-center gap-4">
        <Link
          to="/incidents"
          className="flex items-center gap-1.5 px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 rounded-lg text-xs font-medium transition-all"
        >
          <span className="w-2 h-2 rounded-full bg-rose-400 animate-ping"></span>
          <span>1 Active Incident</span>
        </Link>

        <div className="h-4 w-px bg-slate-700"></div>

        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <div className="text-xs font-semibold text-slate-200">{user?.fullName || 'User'}</div>
            <div className="text-[10px] text-indigo-400 font-mono">
              {user?.roles?.join(', ') || 'VIEWER'}
            </div>
          </div>

          <button
            onClick={logout}
            title="Sign out"
            className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-slate-200 transition-all border border-slate-700"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
