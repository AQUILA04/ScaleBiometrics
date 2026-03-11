export type MatchResultType = 'MATCH_FOUND' | 'NO_MATCH';

export interface MatchingResult {
  id: string;
  jobId: string;
  type: '1:N' | '1:1';
  probeRid: string;
  probeFilename: string;
  matchFound: boolean;
  candidateCount: number;
  topScore?: number;
  topConfidence?: number;
  processedAt: string;
}

export interface MatchingResultDetail extends MatchingResult {
  probe: {
    rid: string;
    filename: string;
    imageUrl: string;
    metadata: {
      width: number;
      height: number;
      dpi: number;
      quality: number;
    };
  };
  candidates: MatchCandidate[];
  processing: {
    algorithm: string;
    threshold: number;
    useAnn: boolean;
    processingTime: number;
    candidatesScanned: number;
  };
  createdAt: string;
}

export interface MatchCandidate {
  rid: string;
  name?: string;
  score: number;
  confidence: number;
  imageUrl: string;
  metadata: {
    fingerPosition?: string;
    quality?: number;
  };
}

export interface MatchingFilters {
  result?: MatchResultType;
  fromDate?: string;
  toDate?: string;
  search?: string;
}

export interface PaginatedMatchingResults {
  content: MatchingResult[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
