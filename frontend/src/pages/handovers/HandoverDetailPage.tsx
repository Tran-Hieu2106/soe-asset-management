import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Descriptions, Input, Modal, Space, message } from 'antd';
import { handoverApi, type Handover } from '../../api/handoverApi';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import ExportButton, { downloadBlob } from '../../components/ExportButton';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

export default function HandoverDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [item, setItem] = useState<Handover | null>(null);
  const isManager = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const isApprover = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.APPROVING_AUTH]);

  const reload = async () => {
    if (!id) return;
    setItem(await handoverApi.getById(id));
  };

  useEffect(() => { reload(); }, [id]);

  if (!item || !id) return null;

  const prompt = (title: string, onOk: (value: string) => Promise<void>, required = false) => {
    let value = '';
    Modal.confirm({
      title,
      content: <Input onChange={(e) => { value = e.target.value; }} />,
      onOk: async () => {
        if (required && !value.trim()) { message.warning('Vui lòng nhập nội dung.'); return Promise.reject(); }
        await onOk(value);
        message.success('Thành công.');
        reload();
      },
    });
  };

  return (
    <>
      <PageHeader title={`Bàn giao ${item.requestCode}`} extra={
        item.status === 'COMPLETED' && (
          <ExportButton label="Tải PDF" onExport={async () => {
            const blob = await handoverApi.downloadDocument(id);
            downloadBlob(blob, `${item.requestCode}.pdf`);
          }} />
        )
      } />
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="Trạng thái"><StatusBadge status={item.status} /></Descriptions.Item>
        <Descriptions.Item label="Lý do">{item.reason}</Descriptions.Item>
        <Descriptions.Item label="Người tạo">{item.initiatedBy}</Descriptions.Item>
      </Descriptions>
      <Space style={{ marginTop: 16 }} wrap>
        {item.status === 'DRAFT' && isManager && <Button onClick={() => { handoverApi.submit(id).then(() => reload()); }}>Nộp duyệt</Button>}
        {item.status === 'PENDING_APPROVAL' && isApprover && <Button type="primary" onClick={() => prompt('Ghi chú phê duyệt', async (n) => { await handoverApi.approve(id, n); })}>Phê duyệt</Button>}
        {item.status === 'APPROVED' && <Button onClick={() => prompt('Ghi chú xác nhận', async (n) => { await handoverApi.confirm(id, n); })}>Xác nhận nhận</Button>}
        {item.status === 'CONFIRMED' && isManager && <Button type="primary" onClick={() => { handoverApi.complete(id).then(() => reload()); }}>Hoàn tất</Button>}
        {['PENDING_APPROVAL', 'APPROVED'].includes(item.status) && isApprover && (
          <Button danger onClick={() => prompt('Lý do từ chối', async (r) => { await handoverApi.reject(id, r); }, true)}>Từ chối</Button>
        )}
      </Space>
    </>
  );
}
