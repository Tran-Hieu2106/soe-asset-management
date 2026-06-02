import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, Descriptions, Table, Button, message, Spin } from 'antd';
import { assetApi } from '../../api/assetApi';
import type { FixedAsset, AssetHistory } from '../../types/asset.types';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

export default function AssetDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const canEdit = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const [asset, setAsset] = useState<FixedAsset | null>(null);
  const [history, setHistory] = useState<AssetHistory[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    Promise.all([assetApi.getById(id), assetApi.history(id)])
      .then(([a, h]) => { setAsset(a); setHistory(h); })
      .catch(() => message.error('Không tải được tài sản.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading || !asset) return <Spin />;

  return (
    <>
      <PageHeader
        title={`Tài sản: ${asset.assetCode}`}
        extra={canEdit && <Button onClick={() => navigate(`/assets/${id}/edit`)}>Sửa</Button>}
      />
      <Card title="Thông tin chung" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="Tên">{asset.name}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái"><StatusBadge status={asset.status} /></Descriptions.Item>
          <Descriptions.Item label="Danh mục">{asset.categoryName}</Descriptions.Item>
          <Descriptions.Item label="Đơn vị QL">{asset.managingUnitName}</Descriptions.Item>
          <Descriptions.Item label="Nguyên giá">{formatCurrency(asset.originalCost)}</Descriptions.Item>
          <Descriptions.Item label="KH lũy kế">{formatCurrency(asset.accumulatedDepreciation)}</Descriptions.Item>
          <Descriptions.Item label="Giá trị còn lại">{formatCurrency(asset.netBookValue)}</Descriptions.Item>
          <Descriptions.Item label="KH hàng năm">{formatCurrency(asset.annualDepreciationAmount)}</Descriptions.Item>
          <Descriptions.Item label="Tỷ lệ KH">{asset.annualDepreciationRate != null ? `${asset.annualDepreciationRate}%` : '—'}</Descriptions.Item>
          <Descriptions.Item label="Ngày ghi tăng">{formatDate(asset.acquisitionDate)}</Descriptions.Item>
          <Descriptions.Item label="Kết thúc KH">{formatDate(asset.depreciationEndDate)}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="Lịch sử vòng đời">
        <Table rowKey="id" dataSource={history} pagination={false} columns={[
          { title: 'Sự kiện', dataIndex: 'eventType' },
          { title: 'Mô tả', dataIndex: 'description' },
          { title: 'Người thực hiện', dataIndex: 'performedBy' },
          { title: 'Thời gian', render: (_, r) => formatDateTime(r.performedAt) },
        ]} />
      </Card>
    </>
  );
}
