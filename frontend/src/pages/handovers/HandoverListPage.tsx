import { useEffect, useState } from 'react';
import { Table, Button, message } from 'antd';
import { Link } from 'react-router-dom';
import { handoverApi, type Handover } from '../../api/handoverApi';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { formatDateTime } from '../../utils/formatDate';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

/*
Shows a table of all handover requests. 
Admins/managers get a "Create" button. 
Each row links to the detail page.
*/
export default function HandoverListPage() {
  const canCreate = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const [data, setData] = useState<Handover[]>([]);
  const [loading, setLoading] = useState(false);

  const load = () => {
    setLoading(true);
    handoverApi.list().then(r => setData(r.content)).catch(() => message.error('Lỗi tải danh sách.')).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <>
      <PageHeader title="Yêu cầu bàn giao" extra={canCreate && <Link to="/handovers/new"><Button type="primary">Tạo yêu cầu</Button></Link>} />
      <Table rowKey="id" loading={loading} dataSource={data} columns={[
        { title: 'Mã', dataIndex: 'requestCode' },
        { title: 'Trạng thái', render: (_, r) => <StatusBadge status={r.status} /> },
        { title: 'Người tạo', dataIndex: 'initiatedBy' },
        { title: 'Ngày tạo', render: (_, r) => formatDateTime(r.createdAt) },
        { title: '', render: (_, r) => <Link to={`/handovers/${r.id}`}>Chi tiết</Link> },
      ]} />
    </>
  );
}
