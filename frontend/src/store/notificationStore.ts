import { create } from 'zustand';

interface NotificationState {
  message: string | null;
  type: 'success' | 'error' | 'info';
  notify: (message: string, type?: 'success' | 'error' | 'info') => void;
  clear: () => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  message: null,
  type: 'info',
  notify: (message, type = 'info') => set({ message, type }),
  clear: () => set({ message: null }),
}));
