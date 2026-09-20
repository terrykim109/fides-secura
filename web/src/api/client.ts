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

export type UserResponse = {
  id: number;
  email: string;
  fullName: string;
  role: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  user: UserResponse;
};

const TOKEN_KEY = "fides_access_token";

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

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

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { error?: string } | null;
    throw new Error(body?.error ?? `Login failed (${res.status})`);
  }
  return res.json() as Promise<AuthResponse>;
}

export async function register(
  email: string,
  password: string,
  fullName: string,
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/api/v1/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, fullName, role: "CUSTOMER" }),
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { error?: string } | null;
    throw new Error(body?.error ?? `Register failed (${res.status})`);
  }
  return res.json() as Promise<AuthResponse>;
}

export async function fetchMe(): Promise<UserResponse> {
  const token = getAccessToken();
  if (!token) {
    throw new Error("Not signed in");
  }
  const res = await fetch(`${API_BASE}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    throw new Error(`Me failed (${res.status})`);
  }
  return res.json() as Promise<UserResponse>;
}
