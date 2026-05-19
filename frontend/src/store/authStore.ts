import { create } from 'zustand';
import { persist } from 'zustand/middleware';

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
  SYSTEM_ADMIN:   'ROLE_SYSTEM_ADMIN',
  ASSET_MANAGER:  'ROLE_ASSET_MANAGER',
  WAREHOUSE:      'ROLE_WAREHOUSE',
  APPROVING_AUTH: 'ROLE_APPROVING_AUTH',
  FINANCE_AUDIT:  'ROLE_FINANCE_AUDIT',
} as const;

// ── Role display names (Vietnamese) ──────────────────────────

export const ROLE_LABELS: Record<string, string> = {
  ROLE_SYSTEM_ADMIN:   'Quản trị viên',
  ROLE_ASSET_MANAGER:  'Quản lý tài sản',
  ROLE_WAREHOUSE:      'Thủ kho',
  ROLE_APPROVING_AUTH: 'Người phê duyệt',
  ROLE_FINANCE_AUDIT:  'Kế toán / Kiểm toán',
};
