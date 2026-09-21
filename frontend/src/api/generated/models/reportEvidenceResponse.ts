/** Evidência de uma denúncia associada ao caso de moderação. */
export interface ReportEvidenceResponse {
  id: string;
  reporterId: string;
  reasonId: number;
  description?: string;
  resolution: string;
  createdAt: string;
}
