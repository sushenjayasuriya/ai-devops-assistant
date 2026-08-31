import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Settings, Shield, User, Key, Database, Bell } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-xl font-bold text-slate-100 tracking-tight">Platform Settings & Security Policy</h2>
        <p className="text-xs text-slate-400">
          Organization configuration, role boundaries, and AI agent guardrails
        </p>
      </div>

      <div className="glass-card rounded-xl p-6 border border-slate-800 space-y-6">
        <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
          <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <User className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-semibold text-sm text-slate-100">User Identity & Access</h3>
            <p className="text-xs text-slate-400">Current authenticated account profile</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-mono">
          <div className="p-3.5 bg-slate-900/70 rounded-lg border border-slate-800">
            <span className="text-slate-500">Full Name:</span>
            <p className="text-slate-200 font-sans font-semibold mt-0.5">{user?.fullName}</p>
          </div>
          <div className="p-3.5 bg-slate-900/70 rounded-lg border border-slate-800">
            <span className="text-slate-500">Email:</span>
            <p className="text-slate-200 mt-0.5">{user?.email}</p>
          </div>
          <div className="p-3.5 bg-slate-900/70 rounded-lg border border-slate-800">
            <span className="text-slate-500">Organization:</span>
            <p className="text-slate-200 font-sans mt-0.5">{user?.organizationName}</p>
          </div>
          <div className="p-3.5 bg-slate-900/70 rounded-lg border border-slate-800">
            <span className="text-slate-500">Assigned Roles:</span>
            <p className="text-indigo-400 font-bold mt-0.5">{user?.roles?.join(', ')}</p>
          </div>
        </div>
      </div>

      <div className="glass-card rounded-xl p-6 border border-slate-800 space-y-4">
        <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
          <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-semibold text-sm text-slate-100">AI Safety & Zero-Trust Policies</h3>
            <p className="text-xs text-slate-400">Strict execution constraints for AI agent tools</p>
          </div>
        </div>

        <div className="space-y-3 text-xs text-slate-300">
          <div className="flex items-center justify-between p-3 bg-slate-900/60 rounded-lg border border-slate-800">
            <div>
              <div className="font-semibold text-slate-200">Strict Human Approval in Production</div>
              <div className="text-[11px] text-slate-400">Require interactive DevOps/Admin sign-off for container restart/stop in PROD</div>
            </div>
            <span className="px-2 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded font-mono text-[10px]">
              ENFORCED
            </span>
          </div>

          <div className="flex items-center justify-between p-3 bg-slate-900/60 rounded-lg border border-slate-800">
            <div>
              <div className="font-semibold text-slate-200">Fact-Observation-Inference Guardrail</div>
              <div className="text-[11px] text-slate-400">Prohibit unverifiable assertions without actual telemetry tool outputs</div>
            </div>
            <span className="px-2 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded font-mono text-[10px]">
              ENFORCED
            </span>
          </div>

          <div className="flex items-center justify-between p-3 bg-slate-900/60 rounded-lg border border-slate-800">
            <div>
              <div className="font-semibold text-slate-200">Host Command Allowlisting</div>
              <div className="text-[11px] text-slate-400">Block arbitrary bash execution on Linux nodes; permit only inspected metrics</div>
            </div>
            <span className="px-2 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded font-mono text-[10px]">
              ENFORCED
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
