export interface AuditLog {
  id: string;
  correlationId: string;
  action: string;
  targetResourceType?: string;
  targetResourceId?: string;
  environmentName?: string;
  riskLevel?: string;
  sanitizedParameters?: string;
  status: 'SUCCESS' | 'FAILURE' | 'REJECTED';
  errorDetails?: string;
  ipAddress?: string;
  timestamp: string;
  user?: {
    id: string;
    email: string;
    fullName: string;
  };
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
