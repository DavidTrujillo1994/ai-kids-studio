export interface Series { id: string; name: string; description: string; }

export interface StoryCharacter {
  id: string;
  assetKey: string;
  role: 'PROTAGONISTA' | 'SECUNDARIO';
  name: string;
  personality: string;
  designVersion: number;
  designStatus: 'BORRADOR' | 'APROBADO';
}

export type EpisodeStatus = 'DRAFT' | 'SCRIPT_READY' | 'IN_PRODUCTION' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'FAILED';
export type StepType = 'WALK' | 'SAY' | 'PLAY' | 'PAUSE' | 'APPEAR' | 'SCENE';

export interface PathPoint { second: number; x: number; y: number; }

export interface Step {
  type: StepType;
  character?: string;
  text?: string;
  animation?: string;
  expression?: string;
  to?: number;
  gait?: string;
  seconds?: number;
  prop?: string;
  path?: PathPoint[];
  environment?: string;
  ambience?: string;
  positions?: CastMember[];
}

export interface CastMember { character: string; startX: number; }
export interface Script { cast: CastMember[]; ambience: string; steps: Step[]; }
export interface Problem { step: number; message: string; }
export interface Review { decision: 'APPROVE' | 'REJECT'; comments: string; at: string; }

export interface EpisodeSummary {
  id: string;
  title: string;
  idea: string;
  status: EpisodeStatus;
  hasVideo: boolean;
  createdAt: string;
  updatedAt: string;
}

export type JobStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
export type Stage = 'WAITING' | 'VOICES' | 'ANIMATION' | 'ASSEMBLY' | 'VALIDATION';

export interface Job {
  id: string;
  episodeId: string;
  status: JobStatus;
  stage: Stage;
  progress: number;
  message: string;
  errorDetail: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface Episode extends EpisodeSummary {
  seriesId: string;
  environment: string;
  editable: boolean;
  script: Script;
  problems: Problem[];
  reviews: Review[];
  job: Job | null;
}

export interface EpisodeEdit { title: string; idea: string; environment: string; script: Script; }
export interface CharacterAsset { key: string; name: string; expressions: string[]; }
export interface Catalog { characters: CharacterAsset[]; environments: string[]; ambiences: string[]; props: string[]; animations: string[]; gaits: string[]; }
export interface StoryTemplate { id: string; name: string; icon: string; description: string; roles: number; environment: string; }
export interface NewEpisodeRequest { title: string; idea: string; environment: string; template: string; cast: string[]; }
export interface Tool { name: string; purpose: string; ready: boolean; detail: string; }

export const isActive = (job: Job | null): boolean => job?.status === 'QUEUED' || job?.status === 'RUNNING';
