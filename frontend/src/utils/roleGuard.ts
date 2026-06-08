import { useAuthStore, ROLES } from '../store/authStore';

/*
Re-exports ROLES from the auth store 
and provides two custom hooks — useHasAnyRole(roles[]) and useHasRole(role) — that read from the auth store reactively. 
Any component that calls these will re-render if the user's roles change (e.g. after login).
*/
export { ROLES };

export function useHasAnyRole(roles: string[]): boolean {
  return useAuthStore((s) => s.hasAnyRole(roles));
}

export function useHasRole(role: string): boolean {
  return useAuthStore((s) => s.hasRole(role));
}
