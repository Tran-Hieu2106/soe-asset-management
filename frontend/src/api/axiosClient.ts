import axios from 'axios';

const axiosClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor
axiosClient.interceptors.request.use(
  (config) => {
    // Grab the token from localStorage
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor
axiosClient.interceptors.response.use(
  (response) => {
    // Our API spec wraps everything in { success, message, data }. 
    // We can extract just the data here if we want, or return the whole response.
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid -> force logout
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('current_username');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default axiosClient;