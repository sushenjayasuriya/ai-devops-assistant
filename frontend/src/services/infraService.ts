import { api } from './api';
import { Environment, Server, Container, Deployment, Integration, OverviewStats } from '../types/infrastructure';

export const infraService = {
  async getEnvironments(): Promise<Environment[]> {
    const res = await api.get('/environments');
    return res.data.data;
  },

  async getOverview(envId?: string): Promise<OverviewStats> {
    const res = await api.get('/overview', { params: { envId } });
    return res.data.data;
  },

  async getServers(envId?: string): Promise<Server[]> {
    const res = await api.get('/servers', { params: { envId } });
    return res.data.data;
  },

  async getServerMetrics(serverId: string): Promise<Record<string, any>> {
    const res = await api.get(`/servers/${serverId}/metrics`);
    return res.data.data;
  },

  async getContainers(envId?: string): Promise<Container[]> {
    const res = await api.get('/containers', { params: { envId } });
    return res.data.data;
  },

  async getContainerLogs(containerId: string, tail: number = 100): Promise<string[]> {
    const res = await api.get(`/containers/${containerId}/logs`, { params: { tail } });
    return res.data.data;
  },

  async executeContainerAction(containerId: string, action: string, approved: boolean = false): Promise<any> {
    const res = await api.post(`/containers/${containerId}/actions`, null, {
      params: { action, approved }
    });
    return res.data.data;
  },

  async getDeployments(envId?: string): Promise<Deployment[]> {
    const res = await api.get('/deployments', { params: { envId } });
    return res.data.data;
  },

  async getIntegrations(envId?: string): Promise<Integration[]> {
    const res = await api.get('/integrations', { params: { envId } });
    return res.data.data;
  },

  async testIntegration(id: string): Promise<any> {
    const res = await api.post(`/integrations/${id}/test-connection`);
    return res.data.data;
  }
};
