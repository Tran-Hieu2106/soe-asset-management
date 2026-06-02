import { useEffect, useState } from 'react';
import { Form, Input, InputNumber, Select, Button, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { stockApi } from '../../api/stockApi';
import { lookupApi } from '../../api/lookupApi';
import type { LookupItem } from '../../types/common.types';
import type { Material } from '../../api/stockApi';
import PageHeader from '../../components/PageHeader';

export default function StockReceiptPage() {
  const navigate = useNavigate();
  const [materials, setMaterials] = useState<Material[]>([]);
  const [locations, setLocations] = useState<LookupItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    stockApi.materials({ page: 0, size: 200 }).then(r => setMaterials(r.content));
    lookupApi.storageLocations().then(setLocations);
  }, []);

  return (
    <>
      <PageHeader title="Nhập kho vật tư" />
      <Form layout="vertical" style={{ maxWidth: 640 }} onFinish={async (v) => {
        setLoading(true);
        try {
          await stockApi.receipt(v);
          message.success('Nhập kho thành công.');
          navigate('/stock/balance');
        } catch {
          message.error('Nhập kho thất bại.');
        } finally {
          setLoading(false);
        }
      }}>
        <Form.Item name="materialId" label="Vật tư" rules={[{ required: true }]}>
          <Select showSearch optionFilterProp="label" options={materials.map(m => ({ value: m.id, label: `${m.materialCode} - ${m.name}` }))} />
        </Form.Item>
        <Form.Item name="storageLocationId" label="Kho" rules={[{ required: true }]}>
          <Select options={locations.map(l => ({ value: l.id, label: l.name }))} />
        </Form.Item>
        <Form.Item name="quantity" label="Số lượng" rules={[{ required: true }]}>
          <InputNumber min={0.01} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="unitPrice" label="Đơn giá"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
        <Form.Item name="documentRef" label="Số chứng từ" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="documentDate" label="Ngày chứng từ" rules={[{ required: true }]}><Input type="date" /></Form.Item>
        <Form.Item name="notes" label="Ghi chú"><Input.TextArea rows={2} /></Form.Item>
        <Button type="primary" htmlType="submit" loading={loading}>Ghi nhận nhập kho</Button>
      </Form>
    </>
  );
}
