export type UserRole =
  | 'ADMIN'
  | 'RESPONSABLE_TECHNIQUE'
  | 'TECHNICIEN'
  | 'SERVICE_ACHATS'
  | 'DIRECTION'
  | 'SOUS_TRAITANT';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  active: boolean;
  contractorId: string | null;
  version: number;
}

export interface AuthSessionResponse {
  accessToken: string;
  user: UserProfile;
}
