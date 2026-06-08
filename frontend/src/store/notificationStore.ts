import { create } from 'zustand';

/*
A lightweight global toast store. 
Components can call notify(message, type) to set a message, and clear() to dismiss it. 
In practice, most error toasts are handled directly by Ant Design's message.error() utility, 
so this store serves as a fallback/alternative channel.
*/
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
