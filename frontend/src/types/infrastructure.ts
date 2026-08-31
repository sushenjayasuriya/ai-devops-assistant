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
  osInfo?: string;
  status: 'ONLINE' | 'UNREACHABLE' | 'DEGRADED';
  metadata?: string;
  sshPort?: number;
  sshUser?: string;
  lastSeenAt?: string;
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
  startedAt?: string;
}

export interface DockerContainerSummary {
  id: string;
  names: string[];
  image: string;
  imageId?: string;
  command?: string;
  created?: number;
  state: string;
  status: string;
}

export interface DockerStats {
  containerId: string;
  name: string;
  cpuPercent: number;
  memoryUsageBytes: number;
  memoryLimitBytes: number;
  memoryPercent: number;
  networkRxBytes: number;
  networkTxBytes: number;
  pids: number;
}

export interface K8sPod {
  name: string;
  namespace: string;
  status: string;
  nodeName: string;
  podIp: string;
  restartCount: number;
  createdAt: string;
  containers: string[];
  labels?: Record<string, string>;
}

export interface K8sDeployment {
  name: string;
  namespace: string;
  replicas: number;
  readyReplicas: number;
  availableReplicas: number;
  updatedReplicas?: number;
  image: string;
  createdAt: string;
  labels?: Record<string, string>;
}

export interface K8sService {
  name: string;
  namespace: string;
  type: string;
  clusterIp: string;
  ports: string[];
  selector?: Record<string, string>;
  createdAt: string;
}

export interface LinuxTelemetry {
  hostname: string;
  ipAddress: string;
  status: string;
  uptimeString?: string;
  uptimeSeconds?: number;
  loadAverage1m?: number;
  loadAverage5m?: number;
  loadAverage15m?: number;
  memory?: {
    totalMb: number;
    usedMb: number;
    freeMb: number;
    availableMb: number;
    usedPercent: number;
  };
  disks?: Array<{
    filesystem: string;
    size: string;
    used: string;
    available: string;
    usePercent: string;
    mountedOn: string;
  }>;
  topProcesses?: Array<{
    user: string;
    pid: number;
    cpuPercent: number;
    memPercent: number;
    command: string;
  }>;
}

export interface Deployment {
  id: string;
  environment: Environment;
  serviceName: string;
  versionTag: string;
  commitSha: string;
  deployedBy: string;
  status: 'SUCCESS' | 'FAILED' | 'IN_PROGRESS' | 'ROLLED_BACK';
  changelog?: string;
  startedAt: string;
  completedAt?: string;
}

export interface Integration {
  id: string;
  environmentId?: string;
  environmentName?: string;
  isProduction?: boolean;
  type: 'PROMETHEUS' | 'DOCKER' | 'KUBERNETES' | 'LINUX_SSH' | 'AWS_CLOUDWATCH' | 'GITHUB_ACTIONS';
  name: string;
  endpointUrl: string;
  authType: string;
  timeoutMs: number;
  enabled: boolean;
  healthStatus: 'HEALTHY' | 'UNHEALTHY' | 'DEGRADED' | 'UNCONFIGURED';
  lastSyncedAt?: string;
  createdAt?: string;
}

export interface TestConnectionResult {
  integrationId: string;
  name: string;
  type: string;
  connected: boolean;
  status: string;
  latencyMs: number;
  checkedAt: string;
  errorCode?: string;
  errorMessage?: string;
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
