import React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { OverviewDashboardPage } from './pages/overview/OverviewDashboardPage';
import { ServersPage } from './pages/infrastructure/ServersPage';
import { DockerPage } from './pages/infrastructure/DockerPage';
import { KubernetesPage } from './pages/infrastructure/KubernetesPage';
import { MetricsExplorerPage } from './pages/metrics/MetricsExplorerPage';
import { LogsViewerPage } from './pages/logs/LogsViewerPage';
import { IncidentListPage } from './pages/incidents/IncidentListPage';
import { DeploymentsPage } from './pages/deployments/DeploymentsPage';
import { AIAssistantPage } from './pages/ai/AIAssistantPage';
import { IntegrationsPage } from './pages/integrations/IntegrationsPage';
import { AuditLogsPage } from './pages/audit/AuditLogsPage';
import { SettingsPage } from './pages/settings/SettingsPage';
import { useAuth } from './context/AuthContext';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-[#090d16] flex items-center justify-center text-xs text-slate-400 font-mono">
        Initializing secure session...
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <OverviewDashboardPage /> },
      { path: 'ai', element: <AIAssistantPage /> },
      { path: 'incidents', element: <IncidentListPage /> },
      { path: 'servers', element: <ServersPage /> },
      { path: 'docker', element: <DockerPage /> },
      { path: 'kubernetes', element: <KubernetesPage /> },
      { path: 'metrics', element: <MetricsExplorerPage /> },
      { path: 'logs', element: <LogsViewerPage /> },
      { path: 'deployments', element: <DeploymentsPage /> },
      { path: 'integrations', element: <IntegrationsPage /> },
      { path: 'audit', element: <AuditLogsPage /> },
      { path: 'settings', element: <SettingsPage /> },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
]);
