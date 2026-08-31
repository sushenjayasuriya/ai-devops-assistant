import { api } from './api';
import { AuditLog, PageResponse } from '../types/audit';

export const auditService = {
  async getAuditLogs(env?: string, page: number = 0, size: number = 20): Promise<PageResponse<AuditLog>> {
    const res = await api.get('/audit', { params: { env, page, size } });
    return res.data.data;
  }
};
