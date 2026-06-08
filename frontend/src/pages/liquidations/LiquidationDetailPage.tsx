import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Descriptions, Input, InputNumber, Modal, Space, message } from 'antd';
import { liquidationApi, type Liquidation } from '../../api/liquidationApi';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

/*
Workflow control panel with two sequential approve buttons (manager → director). 
On final approval, opens a number input to record the actual disposal value before closing the request.

*/
export default function LiquidationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [item, setItem] = useState<Liquidation | null>(null);
  const isManager = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const isApprover = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.APPROVING_AUTH]);

  const reload = async () => {
    if (!id) return;
    setItem(await liquidationApi.getById(id));
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

  const promptComplete = () => {
    let value = 0;
    Modal.confirm({
      title: 'Giá trị thanh lý cuối cùng',
      content: <InputNumber style={{ width: '100%' }} min={0} onChange={(v) => { value = Number(v) || 0; }} />,
      onOk: async () => {
        await liquidationApi.complete(id, value);
        message.success('Hoàn tất thanh lý.');
        reload();
      },
    });
  };

  return (
    <>
      <PageHeader title={`Thanh lý ${item.requestCode}`} />
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="Trạng thái"><StatusBadge status={item.status} /></Descriptions.Item>
        <Descriptions.Item label="Lý do">{item.justification}</Descriptions.Item>
        <Descriptions.Item label="Tình trạng TS">{item.assetCondition}</Descriptions.Item>
        <Descriptions.Item label="Giá trị thị trường">{item.currentMarketValue != null ? formatCurrency(item.currentMarketValue) : '—'}</Descriptions.Item>
        <Descriptions.Item label="Phương thức">{item.disposalMethod}</Descriptions.Item>
        <Descriptions.Item label="Giá trị cuối">{item.finalDisposalValue != null ? formatCurrency(item.finalDisposalValue) : '—'}</Descriptions.Item>
      </Descriptions>
      <Space style={{ marginTop: 16 }} wrap>
        {item.status === 'DRAFT' && isManager && <Button onClick={() => { liquidationApi.submit(id).then(() => reload()); }}>Nộp duyệt</Button>}
        {item.status === 'PENDING_MANAGER' && isManager && <Button type="primary" onClick={() => prompt('Ghi chú quản lý', async (n) => { await liquidationApi.approveManager(id, n); })}>Duyệt (QL)</Button>}
        {item.status === 'PENDING_DIRECTOR' && isApprover && <Button type="primary" onClick={() => prompt('Ghi chú giám đốc', async (n) => { await liquidationApi.approveDirector(id, n); })}>Duyệt (GĐ)</Button>}
        {item.status === 'APPROVED' && isManager && <Button type="primary" onClick={promptComplete}>Hoàn tất</Button>}
        {['PENDING_MANAGER', 'PENDING_DIRECTOR'].includes(item.status) && (isManager || isApprover) && (
          <Button danger onClick={() => prompt('Lý do từ chối', async (r) => { await liquidationApi.reject(id, r); }, true)}>Từ chối</Button>
        )}
      </Space>
    </>
  );
}
