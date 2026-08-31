export type RiskLevel = 'READ_ONLY' | 'LOW_RISK' | 'MEDIUM_RISK' | 'HIGH_RISK' | 'CRITICAL';

export interface Recommendation {
  action: string;
  parameters: Record<string, any>;
  riskLevel: RiskLevel;
  requiresApproval: boolean;
  rationale: string;
  expectedImpact: string;
}

export interface ToolCallRecord {
  toolName: string;
  parameters: Record<string, any>;
  result: any;
  success: boolean;
}

export interface FOIRResponse {
  summary: string;
  facts: string[];
  observations: string[];
  inferences: string[];
  recommendations: Recommendation[];
  confidenceScore: number;
  toolExecutionTrail: ToolCallRecord[];
}

export interface ToolMetadata {
  name: string;
  description: string;
  parameterSchema: Record<string, string>;
  riskLevel: RiskLevel;
  requiredRole: string;
  requiresProductionApproval: boolean;
}

export interface ApprovalRequest {
  id: string;
  aiActionId?: string;
  environment: {
    id: string;
    name: string;
    isProduction: boolean;
  };
  requestedByUser: {
    id: string;
    email: string;
    fullName: string;
  };
  resolvedByUser?: {
    id: string;
    email: string;
    fullName: string;
  };
  actionType: string;
  rationale: string;
  expectedImpact: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
  requestedAt: string;
  resolvedAt?: string;
}
