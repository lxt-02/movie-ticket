import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { keycloak, roleFromToken } from './keycloak';
import { setAuthHooks } from '../api/client';
import { usersApi } from '../api/endpoints';
import type { Role, User } from '../api/types';

interface AuthState {
  ready: boolean;
  authenticated: boolean;
  role?: Role;
  username?: string;
  /** profile from user-service (GET /users/me) */
  profile?: User;
  profileError?: string;
  login: (redirectPath?: string) => void;
  register: () => void;
  logout: () => void;
  refreshProfile: () => Promise<void>;
  setProfile: (u: User) => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

// keycloak.init must run only once (React StrictMode mounts effects twice)
let initPromise: Promise<boolean> | null = null;
// GET /users/me must be called once, never in parallel (FRONTEND_API §1.1)
let profilePromise: Promise<User> | null = null;

function loadProfileOnce(force = false) {
  if (!profilePromise || force) {
    profilePromise = usersApi.me().catch((e) => {
      profilePromise = null;
      throw e;
    });
  }
  return profilePromise;
}

setAuthHooks({
  getToken: async (forceRefresh) => {
    if (!keycloak.authenticated) return undefined;
    try {
      await keycloak.updateToken(forceRefresh ? -1 : 30);
    } catch {
      keycloak.login({ redirectUri: window.location.href });
      return undefined;
    }
    return keycloak.token;
  },
  onUnauthorized: () => {
    if (keycloak.authenticated) keycloak.login({ redirectUri: window.location.href });
  },
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [role, setRole] = useState<Role>();
  const [profile, setProfile] = useState<User>();
  const [profileError, setProfileError] = useState<string>();

  const refreshProfile = useCallback(async () => {
    try {
      setProfile(await loadProfileOnce(true));
      setProfileError(undefined);
    } catch (e) {
      setProfileError((e as Error).message);
    }
  }, []);

  useEffect(() => {
    if (!initPromise) {
      initPromise = Promise.race([
        keycloak.init({
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          checkLoginIframe: false,
          silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        }),
        // Keycloak unreachable: still render the public pages as a guest
        new Promise<boolean>((resolve) => setTimeout(() => resolve(false), 8000)),
      ]);
    }
    initPromise
      .then(async (auth) => {
        setAuthenticated(auth);
        if (auth) {
          setRole(roleFromToken());
          try {
            setProfile(await loadProfileOnce());
          } catch (e) {
            setProfileError((e as Error).message);
          }
        }
      })
      .catch(() => setAuthenticated(false))
      .finally(() => setReady(true));

    keycloak.onAuthRefreshSuccess = () => setRole(roleFromToken());
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => setAuthenticated(false));
    };
  }, []);

  const value: AuthState = {
    ready,
    authenticated,
    role,
    username: (keycloak.tokenParsed as { preferred_username?: string } | undefined)?.preferred_username,
    profile,
    profileError,
    login: (redirectPath) =>
      keycloak.login({ redirectUri: window.location.origin + (redirectPath ?? window.location.pathname + window.location.search) }),
    register: () => keycloak.register({ redirectUri: window.location.origin + '/' }),
    logout: () => {
      profilePromise = null;
      keycloak.logout({ redirectUri: window.location.origin + '/' });
    },
    refreshProfile,
    setProfile,
    hasRole: (...roles) => !!role && roles.includes(role),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
