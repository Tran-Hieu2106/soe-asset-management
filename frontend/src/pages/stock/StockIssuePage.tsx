import { useEffect, useState } from 'react';
import { Form, Input, InputNumber, Select, Button, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { stockApi } from '../../api/stockApi';
import { lookupApi } from '../../api/lookupApi';
import type { LookupItem } from '../../types/common.types';
import type { Material } from '../../api/stockApi';
import PageHeader from '../../components/PageHeader';

/*
Records materials going out of the warehouse to a requesting department. 
Captures which organizational unit is taking the materials and who requested it. 
On submit it calls stockApi.issue() and also redirects to the balance page.
*/
export default function StockIssuePage() {
  const navigate = useNavigate();
  const [materials, setMaterials] = useState<Material[]>([]);
  const [locations, setLocations] = useState<LookupItem[]>([]);
  const [units, setUnits] = useState<LookupItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    stockApi.materials({ page: 0, size: 200 }).then(r => setMaterials(r.content));
    lookupApi.storageLocations().then(setLocations);
    lookupApi.managingUnits().then(setUnits);
  }, []);

  return (
    <>
      <PageHeader title="Xuất kho vật tư" />
      <Form layout="vertical" style={{ maxWidth: 640 }} onFinish={async (v) => {
        setLoading(true);
        try {
          await stockApi.issue(v);
          message.success('Xuất kho thành công.');
          navigate('/stock/balance');
        } catch {
          message.error('Xuất kho thất bại.');
        } finally {
          setLoading(false);
        }
      }}>
        <Form.Item name="materialId" label="Vật tư" rules={[{ required: true }]}>
          <Select showSearch optionFilterProp="label" options={materials.map(m => ({ value: m.id, label: `${m.materialCode} - ${m.name}` }))} />
        </Form.Item>
        <Form.Item name="storageLocationId" label="Kho xuất" rules={[{ required: true }]}>
          <Select options={locations.map(l => ({ value: l.id, label: l.name }))} />
        </Form.Item>
        <Form.Item name="requestingDepartmentId" label="Đơn vị yêu cầu" rules={[{ required: true }]}>
          <Select options={units.map(u => ({ value: u.id, label: u.name }))} />
        </Form.Item>
        <Form.Item name="quantity" label="Số lượng" rules={[{ required: true }]}>
          <InputNumber min={0.01} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="documentRef" label="Số chứng từ" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="documentDate" label="Ngày chứng từ" rules={[{ required: true }]}><Input type="date" /></Form.Item>
        <Form.Item name="requestedBy" label="Người yêu cầu"><Input /></Form.Item>
        <Form.Item name="notes" label="Ghi chú"><Input.TextArea rows={2} /></Form.Item>
        <Button type="primary" htmlType="submit" loading={loading}>Ghi nhận xuất kho</Button>
      </Form>
    </>
  );
}
