import { api } from './api';
import { ApprovalRequest } from '../types/ai';

export const approvalService = {
  async getPendingApprovals(envId?: string): Promise<ApprovalRequest[]> {
    const res = await api.get('/approvals', { params: { envId } });
    return res.data.data;
  },

  async resolveApproval(id: string, decision: 'APPROVED' | 'REJECTED', comment?: string): Promise<ApprovalRequest> {
    const res = await api.post(`/approvals/${id}/resolve`, { decision, comment });
    return res.data.data;
  }
};
