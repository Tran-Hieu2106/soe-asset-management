import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/*
The central auth state, built with Zustand's create + persist middleware. 
It stores token (JWT string) and user (a CurrentUser object with id, username, fullName, roles[], managingUnitCodes[]). 
The persist middleware serialises token and user to localStorage under the key soe-auth so the user stays logged in after a page refresh. 
Three helper functions — isAuthenticated(), hasRole(role), hasAnyRole(roles[]) — let components check permissions without repeating logic. 
It also exports ROLES (an object mapping role keys to their backend codes) and ROLE_LABELS (the Vietnamese display names for those roles).
*/
// ── Types ─────────────────────────────────────────────────────

export interface CurrentUser {
  id: string;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  isActive: boolean;
  roles: string[];
  managingUnitCodes: string[];
}

interface AuthState {
  token: string | null;
  user: CurrentUser | null;

  // Actions
  setAuth: (token: string, user: CurrentUser) => void;
  logout: () => void;

  // Helpers
  isAuthenticated: () => boolean;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
}

// ── Store ─────────────────────────────────────────────────────

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,

      setAuth: (token, user) => set({ token, user }),

      logout: () => set({ token: null, user: null }),

      isAuthenticated: () => {
        const { token } = get();
        return token !== null;
      },

      hasRole: (role) => {
        const { user } = get();
        return user?.roles.includes(role) ?? false;
      },

      hasAnyRole: (roles) => {
        const { user } = get();
        return roles.some(r => user?.roles.includes(r)) ?? false;
      },
    }),
    {
      name: 'soe-auth',         // localStorage key
      partialize: (state) => ({ // only persist token + user, not functions
        token: state.token,
        user: state.user,
      }),
    }
  )
);

// ── Role constants (matches backend role codes exactly) ───────

export const ROLES = {
  SYSTEM_ADMIN:   'SYSTEM_ADMIN',
  ASSET_MANAGER:  'ASSET_MANAGER',
  WAREHOUSE:      'WAREHOUSE',
  APPROVING_AUTH: 'APPROVING_AUTH',
  FINANCE_AUDIT:  'FINANCE_AUDIT',
} as const;

export const ROLE_LABELS: Record<string, string> = {
  SYSTEM_ADMIN:   'Quản trị viên',
  ASSET_MANAGER:  'Quản lý tài sản',
  WAREHOUSE:      'Thủ kho',
  APPROVING_AUTH: 'Người phê duyệt',
  FINANCE_AUDIT:  'Kế toán / Kiểm toán',
};
