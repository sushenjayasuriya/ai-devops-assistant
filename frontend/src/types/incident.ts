import { Environment, Deployment } from './infrastructure';

export type IncidentSeverity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'MITIGATED' | 'RESOLVED' | 'CLOSED';

export interface IncidentEvent {
  id: string;
  eventType: string;
  message: string;
  payload?: string;
  timestamp: string;
}

export interface Incident {
  id: string;
  environment: Environment;
  title: string;
  description: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  affectedResourceType: string;
  affectedResourceId: string;
  rootCauseDeployment?: Deployment;
  rootCauseSummary?: string;
  confidenceScore?: number;
  startedAt: string;
  resolvedAt?: string;
  events?: IncidentEvent[];
}
