import axios from 'axios';
import { message } from 'antd';
import { useAuthStore } from '../store/authStore';

/*
Creates a single shared Axios instance with baseURL from VITE_API_BASE_URL (fallback localhost:8080/api), 
Content-Type: application/json, and a 15-second timeout. Two interceptors are attached:

Request interceptor: reads token from useAuthStore.getState() 
(direct Zustand store access, not a hook, so it works outside React) and attaches it as Authorization: Bearer <token>.

Response interceptor: on 401, calls logout() and redirects to /login. 
On any other error with a message field in the response body, it shows that message via Ant Design's message.error(). 
This means API error toasts are handled centrally — individual pages don't need error-message logic.
*/
// ── Base instance ─────────────────────────────────────────────

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// ── Request interceptor — attach JWT token ────────────────────

axiosInstance.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor — handle 401 globally ───────────────

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    } else if (error.response?.data?.message) {
      message.error(error.response.data.message);
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
