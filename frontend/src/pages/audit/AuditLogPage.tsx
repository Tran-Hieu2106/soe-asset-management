import { useEffect, useState } from 'react';
import { Table, Input, Button, message } from 'antd';
import { reportApi, type AuditLog } from '../../api/reportApi';
import PageHeader from '../../components/PageHeader';
import { formatDateTime } from '../../utils/formatDate';

export default function AuditLogPage() {
  const [data, setData] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [module, setModule] = useState('');
  const [performedBy, setPerformedBy] = useState('');

  const load = (p = 0) => {
    setLoading(true);
    reportApi.auditLogs({ page: p, size: 20, module: module || undefined, performedBy: performedBy || undefined })
      .then(r => { setData(r.content); setTotal(r.totalElements); setPage(p); })
      .catch(() => message.error('Không tải được nhật ký.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(0); }, []);

  return (
    <>
      <PageHeader title="Nhật ký hệ thống" />
      <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Input placeholder="Module" value={module} onChange={(e) => setModule(e.target.value)} style={{ width: 160 }} />
        <Input placeholder="Người thực hiện" value={performedBy} onChange={(e) => setPerformedBy(e.target.value)} style={{ width: 200 }} />
        <Button onClick={() => load(0)}>Lọc</Button>
      </div>
      <Table rowKey="id" loading={loading} dataSource={data}
        pagination={{ current: page + 1, total, pageSize: 20, onChange: (p) => load(p - 1) }}
        columns={[
          { title: 'Thời gian', render: (_, r) => formatDateTime(r.performedAt) },
          { title: 'Module', dataIndex: 'module' },
          { title: 'Hành động', dataIndex: 'action' },
          { title: 'Mã bản ghi', dataIndex: 'recordCode' },
          { title: 'Mô tả', dataIndex: 'description' },
          { title: 'Người TH', dataIndex: 'performedBy' },
        ]}
      />
    </>
  );
}
