export type JobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type JobPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type JobType = '1:N' | '1:1';

export interface Job {
  id: string;
  type: JobType;
  status: JobStatus;
  priority: JobPriority;
  probeRid: string;
  probeFilename?: string;
  candidateCount?: number;
  threshold?: number;
  createdAt: string;
  updatedAt: string;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
  payload?: {
    algorithm?: string;
    useAnn?: boolean;
    topK?: number;
  };
}

export interface JobDetail extends Job {
  timeline: JobTimelineEvent[];
  result?: {
    matchFound: boolean;
    candidates: JobCandidate[];
  };
}

export interface JobTimelineEvent {
  status: JobStatus;
  timestamp: string;
}

export interface JobCandidate {
  candidateRid: string;
  score: number;
  confidence: number;
}

export interface JobFilters {
  status?: JobStatus;
  type?: JobType;
  priority?: JobPriority;
  search?: string;
}

export interface PaginatedJobs {
  content: Job[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
