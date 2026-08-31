export type UserRole = 'ADMIN' | 'DEVOPS_ENGINEER' | 'VIEWER';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  organizationId: string;
  organizationName: string;
  roles: UserRole[];
  lastLoginAt: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  fullName: string;
  organizationId: string;
  roles: UserRole[];
}
