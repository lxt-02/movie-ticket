import Keycloak from 'keycloak-js';
import { config } from '../config';
import type { Role } from '../api/types';

export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.keycloakRealm,
  clientId: config.keycloakClientId,
});

const RANK: Record<Role, number> = { CUSTOMER: 1, STAFF: 2, ADMIN: 3 };

/** Highest realm role in the token: ADMIN > STAFF > CUSTOMER (same rule as the gateway) */
export function roleFromToken(): Role | undefined {
  const roles = (keycloak.tokenParsed as { realm_access?: { roles?: string[] } } | undefined)?.realm_access?.roles ?? [];
  let best: Role | undefined;
  for (const r of roles) {
    if (r in RANK && (!best || RANK[r as Role] > RANK[best])) best = r as Role;
  }
  return best;
}
