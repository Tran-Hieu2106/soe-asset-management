import { Card, Col, Row, Statistic } from 'antd';
import { useAuthStore, ROLE_LABELS } from '../store/authStore';
import PageHeader from '../components/PageHeader';

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <>
      <PageHeader title="Tổng quan" />
      <p>Xin chào <strong>{user?.fullName || user?.username}</strong>.</p>
      <p>Quyền: {user?.roles.map((r) => ROLE_LABELS[r] || r).join(', ')}</p>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}><Card><Statistic title="Đơn vị được gán" value={user?.managingUnitCodes?.length || 0} /></Card></Col>
      </Row>
    </>
  );
}
