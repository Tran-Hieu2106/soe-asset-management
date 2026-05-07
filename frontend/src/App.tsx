import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/auth/Login';

// A simple wrapper to protect routes from unauthenticated users
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const token = localStorage.getItem('jwt_token');
  if (!token) {
    // If no token is found, kick them back to the login screen
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Temporary placeholder for your dashboard
const MainLayout = () => {
  const username = localStorage.getItem('current_username');

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('current_username');
    window.location.href = '/login';
  };

  return (
    <div style={{ padding: '50px', textAlign: 'center', fontFamily: 'sans-serif' }}>
      <h1>SOE Asset Management System</h1>
      <p>Welcome, <strong>{username}</strong>! You are successfully logged in.</p>
      
      <button 
        onClick={handleLogout}
        style={{ 
          padding: '10px 20px', 
          backgroundColor: '#ff4d4f', 
          color: 'white', 
          border: 'none', 
          borderRadius: '4px',
          cursor: 'pointer',
          marginTop: '20px'
        }}
      >
        Logout
      </button>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Public Route */}
        <Route path="/login" element={<Login />} />

        {/* Protected Routes - Anything matching "/*" will hit MainLayout */}
        <Route 
          path="/*" 
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          } 
        />
      </Routes>
    </Router>
  );
};

export default App;