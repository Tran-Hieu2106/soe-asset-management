import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import LoginPage from './pages/auth/LoginPage';
import AppLayout from './components/AppLayout';
import DashboardPage from './pages/DashboardPage';
import AssetListPage from './pages/assets/AssetListPage';
import AssetDetailPage from './pages/assets/AssetDetailPage';
import AssetFormPage from './pages/assets/AssetFormPage';
import MaterialListPage from './pages/stock/MaterialListPage';
import StockBalancePage from './pages/stock/StockBalancePage';
import StockReceiptPage from './pages/stock/StockReceiptPage';
import StockIssuePage from './pages/stock/StockIssuePage';
import HandoverListPage from './pages/handovers/HandoverListPage';
import HandoverDetailPage from './pages/handovers/HandoverDetailPage';
import HandoverFormPage from './pages/handovers/HandoverFormPage';
import LiquidationListPage from './pages/liquidations/LiquidationListPage';
import LiquidationDetailPage from './pages/liquidations/LiquidationDetailPage';
import LiquidationFormPage from './pages/liquidations/LiquidationFormPage';
import AssetReportPage from './pages/reports/AssetReportPage';
import StockReportPage from './pages/reports/StockReportPage';
import AuditLogPage from './pages/audit/AuditLogPage';
import UserListPage from './pages/users/UserListPage';
import { useAuthStore } from './store/authStore';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated());
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

const App: React.FC = () => (
  <ConfigProvider locale={viVN}>
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="assets" element={<AssetListPage />} />
          <Route path="assets/new" element={<AssetFormPage />} />
          <Route path="assets/:id" element={<AssetDetailPage />} />
          <Route path="assets/:id/edit" element={<AssetFormPage />} />
          <Route path="materials" element={<MaterialListPage />} />
          <Route path="stock/balance" element={<StockBalancePage />} />
          <Route path="stock/receipt" element={<StockReceiptPage />} />
          <Route path="stock/issue" element={<StockIssuePage />} />
          <Route path="handovers" element={<HandoverListPage />} />
          <Route path="handovers/new" element={<HandoverFormPage />} />
          <Route path="handovers/:id" element={<HandoverDetailPage />} />
          <Route path="liquidations" element={<LiquidationListPage />} />
          <Route path="liquidations/new" element={<LiquidationFormPage />} />
          <Route path="liquidations/:id" element={<LiquidationDetailPage />} />
          <Route path="reports/assets" element={<AssetReportPage />} />
          <Route path="reports/stock" element={<StockReportPage />} />
          <Route path="audit-logs" element={<AuditLogPage />} />
          <Route path="users" element={<UserListPage />} />
        </Route>
      </Routes>
    </Router>
  </ConfigProvider>
);

export default App;
