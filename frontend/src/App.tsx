import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/auth/LoginPage';
import { useAuthStore } from './store/authStore'; // Import store mới

// Wrapper bảo vệ Route dùng Zustand
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  // Lấy hàm check đăng nhập từ store
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated());

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

// Placeholder tạm thời cho Dashboard
const DashboardPlaceholder = () => {
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

  const handleLogout = () => {
    logout(); // Xóa state trong store
    window.location.href = '/login';
  };

  return (
    <div style={{ padding: '50px', textAlign: 'center' }}>
      <h1>Dashboard</h1>
      <p>Chào mừng <strong>{user?.fullName || user?.username}</strong>!</p>
      <p>Quyền của bạn: {user?.roles.join(', ')}</p>
      <button onClick={handleLogout}>Đăng xuất</button>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected Routes */}
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <DashboardPlaceholder />
            </ProtectedRoute>
          } 
        />
        
        {/* Tự động chuyển hướng / về /dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
};

export default App;