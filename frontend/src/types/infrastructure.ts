export interface Environment {
  id: string;
  name: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';
  description?: string;
  isProduction: boolean;
  createdAt: string;
}

export interface Server {
  id: string;
  environment: Environment;
  hostname: string;
  ipAddress: string;
  osInfo: string;
  status: 'ONLINE' | 'UNREACHABLE' | 'DEGRADED';
  metadata: string;
  lastSeenAt: string;
}

export interface Container {
  id: string;
  environment: Environment;
  server?: Server;
  containerId: string;
  name: string;
  image: string;
  state: 'RUNNING' | 'EXITED' | 'RESTARTING' | 'DEAD';
  restartCount: number;
  portMappings?: string;
  startedAt: string;
}

export interface Deployment {
  id: string;
  environment: Environment;
  serviceName: string;
  versionTag: string;
  commitSha: string;
  deployedBy: string;
  status: 'SUCCESS' | 'FAILED' | 'IN_PROGRESS' | 'ROLLED_BACK';
  changelog: string;
  startedAt: string;
  completedAt?: string;
}

export interface Integration {
  id: string;
  environment: Environment;
  type: string;
  name: string;
  endpointUrl: string;
  healthStatus: 'HEALTHY' | 'UNHEALTHY' | 'UNCONFIGURED';
  lastSyncedAt: string;
}

export interface OverviewStats {
  healthSummary: 'HEALTHY' | 'DEGRADED' | 'CRITICAL';
  serverCount: number;
  containerCount: number;
  openIncidentsCount: number;
  recentDeploymentsCount: number;
  averageCpuPercent: number;
  averageMemoryPercent: number;
  averageDiskPercent: number;
  activeEnvironments: number;
}
