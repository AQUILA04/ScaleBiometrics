export interface TenantGeneralSettings {
  name: string;
  logo?: string;
  contactEmail: string;
  contactPhone?: string;
}

export interface TenantMatchingSettings {
  confidenceThreshold: number;
  topK: number;
  annEnabled: boolean;
  algorithm: string;
}

export interface TenantStorageSettings {
  retentionDays: number;
  compressionEnabled: boolean;
  autoArchiveEnabled: boolean;
}

export interface TenantSettings {
  general: TenantGeneralSettings;
  matching: TenantMatchingSettings;
  storage: TenantStorageSettings;
}

export interface UpdateSettingsRequest {
  general?: Partial<TenantGeneralSettings>;
  matching?: Partial<TenantMatchingSettings>;
  storage?: Partial<TenantStorageSettings>;
}
