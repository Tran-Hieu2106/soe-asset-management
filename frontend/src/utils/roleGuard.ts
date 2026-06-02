import { useAuthStore, ROLES } from '../store/authStore';

export { ROLES };

export function useHasAnyRole(roles: string[]): boolean {
  return useAuthStore((s) => s.hasAnyRole(roles));
}

export function useHasRole(role: string): boolean {
  return useAuthStore((s) => s.hasRole(role));
}
