export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  keycloakRealm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'mss301',
  keycloakClientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'mss301-frontend',
  /** Backend does not limit seats per booking yet (FRONTEND_API §5) */
  maxSeatsPerBooking: 8,
  /** Hide the booking button when the showtime starts in less than this (FRONTEND_API §5) */
  bookingCutoffMinutes: 15,
};
