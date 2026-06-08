import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Select, Button, message } from 'antd';
import { handoverApi } from '../../api/handoverApi';
import { lookupApi } from '../../api/lookupApi';
import { assetApi } from '../../api/assetApi';
import type { LookupItem } from '../../types/common.types';
import type { FixedAsset } from '../../types/asset.types';
import PageHeader from '../../components/PageHeader';

/*
A create-only form. 
User picks an asset, a from-unit, a to-unit, 
adds a reason and condition, then saves as DRAFT. 
Redirects to the detail page after creation.
*/
export default function HandoverFormPage() {
  const navigate = useNavigate();
  const [units, setUnits] = useState<LookupItem[]>([]);
  const [assets, setAssets] = useState<FixedAsset[]>([]);

  useEffect(() => {
    lookupApi.managingUnits().then(setUnits);
    assetApi.list({ page: 0, size: 100 }).then(r => setAssets(r.content));
  }, []);

  return (
    <>
      <PageHeader title="Tạo yêu cầu bàn giao" />
      <Form layout="vertical" style={{ maxWidth: 640 }} onFinish={async (v) => {
        try {
          const created = await handoverApi.create(v);
          message.success('Tạo yêu cầu thành công.');
          navigate(`/handovers/${created.id}`);
        } catch { message.error('Tạo yêu cầu thất bại.'); }
      }}>
        <Form.Item name="assetId" label="Tài sản" rules={[{ required: true }]}>
          <Select options={assets.map(a => ({ value: a.id, label: `${a.assetCode} - ${a.name}` }))} showSearch optionFilterProp="label" />
        </Form.Item>
        <Form.Item name="fromUnitId" label="Đơn vị bàn giao" rules={[{ required: true }]}>
          <Select options={units.map(u => ({ value: u.id, label: u.name }))} />
        </Form.Item>
        <Form.Item name="toUnitId" label="Đơn vị tiếp nhận" rules={[{ required: true }]}>
          <Select options={units.map(u => ({ value: u.id, label: u.name }))} />
        </Form.Item>
        <Form.Item name="reason" label="Lý do" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
        <Form.Item name="handoverDate" label="Ngày bàn giao"><Input type="date" /></Form.Item>
        <Form.Item name="assetCondition" label="Tình trạng">
          <Select options={['GOOD', 'FAIR', 'POOR'].map(v => ({ value: v, label: v }))} allowClear />
        </Form.Item>
        <Button type="primary" htmlType="submit">Lưu nháp</Button>
      </Form>
    </>
  );
}
