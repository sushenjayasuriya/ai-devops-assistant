import { api } from './api';
import { Incident, IncidentEvent } from '../types/incident';

export const incidentService = {
  async getIncidents(envId?: string, status?: string): Promise<Incident[]> {
    const res = await api.get('/incidents', { params: { envId, status } });
    return res.data.data;
  },

  async getIncidentById(id: string): Promise<Incident> {
    const res = await api.get(`/incidents/${id}`);
    return res.data.data;
  },

  async getIncidentEvents(id: string): Promise<IncidentEvent[]> {
    const res = await api.get(`/incidents/${id}/events`);
    return res.data.data;
  },

  async updateIncidentStatus(id: string, status: string): Promise<Incident> {
    const res = await api.patch(`/incidents/${id}/status`, null, { params: { status } });
    return res.data.data;
  },

  async getIncidentInvestigation(id: string): Promise<Record<string, any>> {
    const res = await api.get(`/incidents/${id}/investigation`);
    return res.data.data;
  }
};
