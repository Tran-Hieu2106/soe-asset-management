import { useEffect, useState } from 'react';
import { Table, Button, message } from 'antd';
import { Link } from 'react-router-dom';
import { liquidationApi, type Liquidation } from '../../api/liquidationApi';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

/*
Lists all liquidation requests with their disposal method visible at a glance. 
Only admins and asset managers can create new requests.
*/
export default function LiquidationListPage() {
  const canCreate = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const [data, setData] = useState<Liquidation[]>([]);
  const [loading, setLoading] = useState(false);

  const load = () => {
    setLoading(true);
    liquidationApi.list().then(r => setData(r.content)).catch(() => message.error('Lỗi tải danh sách.')).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <>
      <PageHeader title="Yêu cầu thanh lý" extra={canCreate && <Link to="/liquidations/new"><Button type="primary">Tạo yêu cầu</Button></Link>} />
      <Table rowKey="id" loading={loading} dataSource={data} columns={[
        { title: 'Mã', dataIndex: 'requestCode' },
        { title: 'Trạng thái', render: (_, r) => <StatusBadge status={r.status} /> },
        { title: 'Người tạo', dataIndex: 'initiatedBy' },
        { title: 'Phương thức', dataIndex: 'disposalMethod' },
        { title: '', render: (_, r) => <Link to={`/liquidations/${r.id}`}>Chi tiết</Link> },
      ]} />
    </>
  );
}
