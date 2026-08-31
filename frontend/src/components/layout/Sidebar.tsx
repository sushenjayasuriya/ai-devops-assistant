import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Server,
  Box,
  Cpu,
  Activity,
  FileText,
  AlertOctagon,
  GitBranch,
  Bot,
  Layers,
  Shield,
  Settings,
  Sparkles
} from 'lucide-react';

const NAV_ITEMS = [
  { path: '/', label: 'Overview', icon: LayoutDashboard },
  { path: '/ai', label: 'AI SRE Assistant', icon: Bot, highlight: true },
  { path: '/incidents', label: 'Incidents', icon: AlertOctagon },
  { path: '/servers', label: 'Servers', icon: Server },
  { path: '/docker', label: 'Docker', icon: Box },
  { path: '/kubernetes', label: 'Kubernetes', icon: Cpu },
  { path: '/metrics', label: 'Metrics Explorer', icon: Activity },
  { path: '/logs', label: 'Logs Viewer', icon: FileText },
  { path: '/deployments', label: 'Deployments', icon: GitBranch },
  { path: '/integrations', label: 'Integrations', icon: Layers },
  { path: '/audit', label: 'Audit Logs', icon: Shield },
  { path: '/settings', label: 'Settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="w-64 bg-surface border-r border-surface-border flex flex-col shrink-0 h-screen sticky top-0">
      {/* Brand Logo */}
      <div className="h-16 flex items-center px-6 border-b border-surface-border gap-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-600 to-cyan-400 flex items-center justify-center shadow-md shadow-indigo-500/20">
          <Sparkles className="w-4 h-4 text-white" />
        </div>
        <div>
          <h1 className="font-bold text-sm text-slate-100 tracking-tight">AI DevOps</h1>
          <p className="text-[10px] text-indigo-400 font-mono font-medium">AUTONOMOUS SRE</p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 overflow-y-auto p-4 space-y-1">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-xs font-medium transition-all ${
                  isActive
                    ? 'bg-indigo-600/15 text-indigo-400 border border-indigo-500/30 font-semibold shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                } ${item.highlight && !location.pathname.includes('/ai') ? 'text-indigo-300' : ''}`
              }
            >
              <Icon className="w-4 h-4" />
              <span>{item.label}</span>
              {item.highlight && (
                <span className="ml-auto px-1.5 py-0.5 text-[9px] bg-indigo-500/20 text-indigo-300 rounded font-mono">
                  LIVE
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* System Status Footer */}
      <div className="p-4 border-t border-surface-border bg-surface-elevated/40">
        <div className="flex items-center justify-between text-[11px]">
          <span className="text-slate-400">Agent Guardrail</span>
          <span className="text-emerald-400 font-mono font-medium flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            ACTIVE
          </span>
        </div>
      </div>
    </aside>
  );
};
