import { useEffect, useState } from 'react';
import { Button, Table, Input, Select, Space, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { assetApi } from '../../api/assetApi';
import { lookupApi } from '../../api/lookupApi';
import type { FixedAsset, AssetStatus } from '../../types/asset.types';
import type { LookupItem } from '../../types/common.types';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

/*
Fetches a paginated asset list with filter controls (keyword search, status dropdown, category dropdown). 
Renders a table with formatted currency and date columns. 
The "Add asset" button is only shown if the user has SYSTEM_ADMIN or ASSET_MANAGER roles (checked via useHasAnyRole).

*/
export default function AssetListPage() {
  const navigate = useNavigate();
  const canEdit = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.ASSET_MANAGER]);
  const [data, setData] = useState<FixedAsset[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<AssetStatus | undefined>();
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [keyword, setKeyword] = useState('');
  const [categories, setCategories] = useState<LookupItem[]>([]);

  const load = async (p = page) => {
    setLoading(true);
    try {
      const res = await assetApi.list({ page: p, size: 20, status, categoryId, keyword: keyword || undefined });
      setData(res.content);
      setTotal(res.totalElements);
      setPage(p);
    } catch {
      message.error('Không tải được danh sách tài sản.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    lookupApi.assetCategories().then(setCategories).catch(() => undefined);
    load(0);
  }, []);

  return (
    <>
      <PageHeader
        title="Danh sách tài sản"
        extra={canEdit && <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/assets/new')}>Thêm tài sản</Button>}
      />
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search placeholder="Tìm mã hoặc tên" allowClear onSearch={() => load(0)} onChange={(e) => setKeyword(e.target.value)} style={{ width: 220 }} />
        <Select allowClear placeholder="Trạng thái" style={{ width: 160 }} onChange={(v) => setStatus(v)} options={[
          'IN_USE', 'MAINTENANCE', 'IDLE', 'TRANSFERRED', 'LIQUIDATED'
        ].map(v => ({ value: v, label: v }))} />
        <Select allowClear placeholder="Danh mục" style={{ width: 200 }} onChange={(v) => setCategoryId(v)} options={categories.map(c => ({ value: Number(c.id), label: c.name }))} />
        <Button onClick={() => load(0)}>Lọc</Button>
      </Space>
      <Table rowKey="id" loading={loading} dataSource={data} pagination={{ current: page + 1, total, pageSize: 20, onChange: (p) => load(p - 1) }}
        columns={[
          { title: 'Mã TS', dataIndex: 'assetCode' },
          { title: 'Tên', dataIndex: 'name' },
          { title: 'Danh mục', dataIndex: 'categoryName' },
          { title: 'Đơn vị', dataIndex: 'managingUnitName' },
          { title: 'Nguyên giá', render: (_, r) => formatCurrency(r.originalCost) },
          { title: 'GTCL', render: (_, r) => formatCurrency(r.netBookValue) },
          { title: 'Ngày GT', render: (_, r) => formatDate(r.acquisitionDate) },
          { title: 'Trạng thái', render: (_, r) => <StatusBadge status={r.status} /> },
          { title: '', render: (_, r) => <Link to={`/assets/${r.id}`}>Chi tiết</Link> },
        ]}
      />
    </>
  );
}
