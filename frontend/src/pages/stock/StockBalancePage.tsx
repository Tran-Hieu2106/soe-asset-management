import { useEffect, useState } from 'react';
import { Table, Tag, message } from 'antd';
import { Link } from 'react-router-dom';
import { stockApi, type StockBalance } from '../../api/stockApi';
import PageHeader from '../../components/PageHeader';
import { ROLES, useHasAnyRole } from '../../utils/roleGuard';

export default function StockBalancePage() {
  const canTransact = useHasAnyRole([ROLES.SYSTEM_ADMIN, ROLES.WAREHOUSE]);
  const [data, setData] = useState<StockBalance[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    stockApi.balance()
      .then(setData)
      .catch(() => message.error('Không tải được tồn kho.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <PageHeader
        title="Tồn kho vật tư"
        extra={canTransact && (
          <>
            <Link to="/stock/receipt" style={{ marginRight: 8 }}>Nhập kho</Link>
            <Link to="/stock/issue">Xuất kho</Link>
          </>
        )}
      />
      <Table rowKey={(r) => `${r.materialId}-${r.storageLocationId}`} loading={loading} dataSource={data}
        columns={[
          { title: 'Mã VT', dataIndex: 'materialCode' },
          { title: 'Tên vật tư', dataIndex: 'materialName' },
          { title: 'Kho', dataIndex: 'storageLocationName' },
          { title: 'ĐVT', dataIndex: 'unitOfMeasure' },
          { title: 'Tồn hiện tại', dataIndex: 'currentBalance' },
          { title: 'Tồn tối thiểu', dataIndex: 'minimumStock' },
          { title: 'Cảnh báo', render: (_, r) => r.isBelowMinimum ? <Tag color="red">Dưới mức tối thiểu</Tag> : <Tag color="green">OK</Tag> },
        ]}
      />
    </>
  );
}
