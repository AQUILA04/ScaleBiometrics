export type ApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export type ApiKeyScope = 'read' | 'write' | 'delete' | 'matching' | 'admin';

export interface ApiKey {
  id: string;
  name: string;
  prefix: string;
  scopes: ApiKeyScope[];
  status: ApiKeyStatus;
  expiresAt: string;
  createdAt: string;
  lastUsedAt?: string;
}

export interface CreateApiKeyRequest {
  name: string;
  scopes: ApiKeyScope[];
  expiresIn: string;
}

export interface CreatedApiKey extends ApiKey {
  key: string;
}

export interface ApiKeyResponse {
  keys: ApiKey[];
}
