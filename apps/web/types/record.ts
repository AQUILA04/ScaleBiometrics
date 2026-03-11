export interface BiometricRecord {
  rid: string;
  name: string;
  email?: string;
  phone?: string;
  metadata?: Record<string, string>;
  createdAt: string;
  updatedAt: string;
  fingerprintCount: number;
  lastMatchAt?: string;
}

export interface Fingerprint {
  id: string;
  position: string;
  imageUrl: string;
  templateUrl?: string;
  quality: number;
  createdAt: string;
}

export interface RecordDetail extends BiometricRecord {
  fingerprints: Fingerprint[];
}

export interface RecordHistoryItem {
  resultId: string;
  jobId: string;
  role: "PROBE" | "CANDIDATE";
  matchFound: boolean;
  score?: number;
  matchedWith?: string;
  processedAt: string;
}

export interface RecordHistory {
  rid: string;
  matchingHistory: RecordHistoryItem[];
}

export interface PaginatedRecords {
  content: BiometricRecord[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
