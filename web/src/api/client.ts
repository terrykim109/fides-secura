const API_BASE = "";

export type HealthResponse = {
  status: string;
  service: string;
  focus?: string;
  timestamp: string;
};

export type FraudSample = {
  riskScore: number;
  reasons: string[];
  flagged: boolean;
};

export type CorrelationSample = {
  ruleName: string;
  title: string;
  severity: string;
  summary: string;
};

export type CapabilitiesResponse = {
  product: string;
  status?: string;
  note?: string;
  focus?: string;
  fraudSample: FraudSample;
  correlationSample: CorrelationSample;
  roles?: string[];
  rolesPlanned?: string[];
};

export async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch(`${API_BASE}/api/v1/health`);
  if (!res.ok) {
    throw new Error(`Health check failed (${res.status})`);
  }
  return res.json() as Promise<HealthResponse>;
}

export async function fetchCapabilities(): Promise<CapabilitiesResponse> {
  const res = await fetch(`${API_BASE}/api/v1/security/capabilities`);
  if (!res.ok) {
    throw new Error(`Capabilities failed (${res.status})`);
  }
  return res.json() as Promise<CapabilitiesResponse>;
}
