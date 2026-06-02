import { Layout, Menu, Typography, Button, Space } from 'antd';
import type { ReactNode } from 'react';
import {
  DashboardOutlined,
  DatabaseOutlined,
  InboxOutlined,
  SwapOutlined,
  DeleteOutlined,
  BarChartOutlined,
  AuditOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore, ROLE_LABELS } from '../store/authStore';
import { ROLES } from '../utils/roleGuard';

const { Header, Sider, Content } = Layout;

type MenuItem = { key: string; icon: ReactNode; label: ReactNode; roles: string[] };

const MENU: MenuItem[] = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: <Link to="/dashboard">Tổng quan</Link>, roles: [] },
  { key: '/assets', icon: <DatabaseOutlined />, label: <Link to="/assets">Tài sản cố định</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER, ROLES.FINANCE_AUDIT, ROLES.APPROVING_AUTH] },
  { key: '/materials', icon: <InboxOutlined />, label: <Link to="/materials">Vật tư</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.WAREHOUSE, ROLES.FINANCE_AUDIT, ROLES.APPROVING_AUTH] },
  { key: '/stock/balance', icon: <InboxOutlined />, label: <Link to="/stock/balance">Tồn kho</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.WAREHOUSE, ROLES.FINANCE_AUDIT, ROLES.APPROVING_AUTH] },
  { key: '/handovers', icon: <SwapOutlined />, label: <Link to="/handovers">Bàn giao</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER, ROLES.APPROVING_AUTH] },
  { key: '/liquidations', icon: <DeleteOutlined />, label: <Link to="/liquidations">Thanh lý</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER, ROLES.APPROVING_AUTH] },
  { key: '/reports/assets', icon: <BarChartOutlined />, label: <Link to="/reports/assets">Báo cáo tài sản</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.FINANCE_AUDIT, ROLES.APPROVING_AUTH] },
  { key: '/reports/stock', icon: <BarChartOutlined />, label: <Link to="/reports/stock">Báo cáo vật tư</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.FINANCE_AUDIT, ROLES.APPROVING_AUTH] },
  { key: '/audit-logs', icon: <AuditOutlined />, label: <Link to="/audit-logs">Nhật ký hệ thống</Link>, roles: [ROLES.SYSTEM_ADMIN, ROLES.FINANCE_AUDIT] },
  { key: '/users', icon: <UserOutlined />, label: <Link to="/users">Người dùng</Link>, roles: [ROLES.SYSTEM_ADMIN] },
];

export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const hasAnyRole = useAuthStore((s) => s.hasAnyRole);

  const items = MENU.filter((m) => m.roles.length === 0 || hasAnyRole(m.roles)).map(({ key, icon, label }) => ({ key, icon, label }));

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={0} theme="dark">
        <div style={{ padding: 16, color: '#fff', fontWeight: 700, textAlign: 'center' }}>SOE AMS</div>
        <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} items={items} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography.Text strong>Hệ thống quản lý tài sản doanh nghiệp nhà nước</Typography.Text>
          <Space>
            <Typography.Text>{user?.fullName || user?.username}</Typography.Text>
            <Typography.Text type="secondary">
              {user?.roles.map((r) => ROLE_LABELS[r] || r).join(', ')}
            </Typography.Text>
            <Button icon={<LogoutOutlined />} onClick={handleLogout}>Đăng xuất</Button>
          </Space>
        </Header>
        <Content style={{ margin: 24, background: '#fff', padding: 24, minHeight: 360 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
