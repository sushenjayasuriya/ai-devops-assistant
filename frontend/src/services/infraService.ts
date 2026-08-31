import { api } from './api';
import {
  Environment,
  Server,
  Container,
  Deployment,
  Integration,
  OverviewStats,
  DockerContainerSummary,
  DockerStats,
  K8sPod,
  K8sDeployment,
  K8sService,
  LinuxTelemetry,
  TestConnectionResult
} from '../types/infrastructure';

export const infraService = {
  // Environments & Overview
  async getEnvironments(): Promise<Environment[]> {
    const res = await api.get('/environments');
    return res.data.data;
  },

  async getOverview(envId?: string): Promise<OverviewStats> {
    const res = await api.get('/overview', { params: { envId } });
    return res.data.data;
  },

  // Servers
  async getServers(envId?: string): Promise<Server[]> {
    const res = await api.get('/servers', { params: { envId } });
    return res.data.data;
  },

  async getServerMetrics(serverId: string): Promise<Record<string, any>> {
    const res = await api.get(`/servers/${serverId}/metrics`);
    return res.data.data;
  },

  // Containers
  async getContainers(envId?: string): Promise<Container[]> {
    const res = await api.get('/containers', { params: { envId } });
    return res.data.data;
  },

  async getContainerLogs(containerId: string, tail: number = 100): Promise<string[]> {
    const res = await api.get(`/containers/${containerId}/logs`, { params: { tail } });
    return res.data.data;
  },

  async executeContainerAction(containerId: string, action: string): Promise<any> {
    const res = await api.post(`/containers/${containerId}/actions`, null, {
      params: { action }
    });
    return res.data.data;
  },

  // Deployments
  async getDeployments(envId?: string): Promise<Deployment[]> {
    const res = await api.get('/deployments', { params: { envId } });
    return res.data.data;
  },

  // Integrations Management
  async getIntegrations(envId?: string): Promise<Integration[]> {
    const res = await api.get('/integrations', { params: { envId } });
    return res.data.data;
  },

  async getIntegrationById(id: string): Promise<Integration> {
    const res = await api.get(`/integrations/${id}`);
    return res.data.data;
  },

  async createIntegration(data: {
    environmentId: string;
    type: string;
    name: string;
    endpointUrl: string;
    authType?: string;
    configRaw?: string;
    timeoutMs?: number;
    enabled?: boolean;
  }): Promise<Integration> {
    const res = await api.post('/integrations', data);
    return res.data.data;
  },

  async testIntegration(id: string): Promise<TestConnectionResult> {
    const res = await api.post(`/integrations/${id}/test-connection`);
    return res.data.data;
  },

  // Prometheus Data Plane
  async queryPrometheusInstant(integrationId: string, query: string): Promise<any> {
    const res = await api.get(`/integrations/${integrationId}/prometheus/query`, { params: { query } });
    return res.data.data;
  },

  async queryPrometheusRange(integrationId: string, query: string, start?: string, end?: string, step?: string): Promise<any> {
    const res = await api.get(`/integrations/${integrationId}/prometheus/query_range`, { params: { query, start, end, step } });
    return res.data.data;
  },

  async getPrometheusTargets(integrationId: string): Promise<any> {
    const res = await api.get(`/integrations/${integrationId}/prometheus/targets`);
    return res.data.data;
  },

  // Docker Data Plane
  async listDockerContainers(integrationId: string, all: boolean = true): Promise<DockerContainerSummary[]> {
    const res = await api.get(`/integrations/${integrationId}/docker/containers`, { params: { all } });
    return res.data.data;
  },

  async getDockerContainerStats(integrationId: string, containerId: string): Promise<DockerStats> {
    const res = await api.get(`/integrations/${integrationId}/docker/containers/${containerId}/stats`);
    return res.data.data;
  },

  async getDockerContainerLogs(integrationId: string, containerId: string, tail: number = 100): Promise<string[]> {
    const res = await api.get(`/integrations/${integrationId}/docker/containers/${containerId}/logs`, { params: { tail } });
    return res.data.data;
  },

  // Kubernetes Data Plane
  async getKubernetesPods(integrationId: string, namespace?: string): Promise<K8sPod[]> {
    const res = await api.get(`/integrations/${integrationId}/kubernetes/pods`, { params: { namespace } });
    return res.data.data;
  },

  async getKubernetesDeployments(integrationId: string, namespace?: string): Promise<K8sDeployment[]> {
    const res = await api.get(`/integrations/${integrationId}/kubernetes/deployments`, { params: { namespace } });
    return res.data.data;
  },

  async getKubernetesServices(integrationId: string, namespace?: string): Promise<K8sService[]> {
    const res = await api.get(`/integrations/${integrationId}/kubernetes/services`, { params: { namespace } });
    return res.data.data;
  },

  async getKubernetesLogs(integrationId: string, namespace: string, podName: string, containerName?: string, tail: number = 100): Promise<string[]> {
    const res = await api.get(`/integrations/${integrationId}/kubernetes/logs`, {
      params: { namespace, podName, containerName, tail }
    });
    return res.data.data;
  },

  // Linux SSH Data Plane
  async getLinuxTelemetry(integrationId: string): Promise<LinuxTelemetry> {
    const res = await api.get(`/integrations/${integrationId}/linux/telemetry`);
    return res.data.data;
  }
};
