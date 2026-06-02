import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Form, Input, InputNumber, Select, Button, message } from 'antd';
import { assetApi } from '../../api/assetApi';
import { lookupApi } from '../../api/lookupApi';
import type { LookupItem } from '../../types/common.types';
import PageHeader from '../../components/PageHeader';

export default function AssetFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [units, setUnits] = useState<LookupItem[]>([]);
  const [categories, setCategories] = useState<LookupItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([lookupApi.managingUnits(), lookupApi.assetCategories()]).then(([u, c]) => {
      setUnits(u);
      setCategories(c);
    });
    if (id) {
      assetApi.getById(id).then((a) => form.setFieldsValue({ ...a }));
    }
  }, [id, form]);

  const onFinish = async (values: Record<string, unknown>) => {
    setLoading(true);
    const payload = { ...values };
    try {
      if (isEdit && id) {
        await assetApi.update(id, payload);
        message.success('Cập nhật tài sản thành công.');
        navigate(`/assets/${id}`);
      } else {
        const created = await assetApi.create(payload);
        message.success('Tạo tài sản thành công.');
        navigate(`/assets/${created.id}`);
      }
    } catch {
      message.error('Lưu tài sản thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <PageHeader title={isEdit ? 'Cập nhật tài sản' : 'Đăng ký tài sản mới'} />
      <Form form={form} layout="vertical" onFinish={onFinish} style={{ maxWidth: 720 }}>
        {!isEdit && <Form.Item name="assetCode" label="Mã tài sản" rules={[{ required: true }]}><Input /></Form.Item>}
        <Form.Item name="name" label="Tên tài sản" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="categoryId" label="Danh mục" rules={[{ required: true }]}>
          <Select options={categories.map(c => ({ value: Number(c.id), label: c.name }))} />
        </Form.Item>
        <Form.Item name="managingUnitId" label="Đơn vị quản lý" rules={[{ required: true }]}>
          <Select options={units.map(u => ({ value: u.id, label: u.name }))} />
        </Form.Item>
        <Form.Item name="originalCost" label="Nguyên giá (VND)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
        <Form.Item name="acquisitionDate" label="Ngày ghi tăng" rules={[{ required: true }]}>
          <Input type="date" />
        </Form.Item>
        <Form.Item name="usefulLifeYears" label="Thời gian SD (năm)" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={1} /></Form.Item>
        <Form.Item name="location" label="Vị trí"><Input /></Form.Item>
        <Form.Item name="notes" label="Ghi chú"><Input.TextArea rows={3} /></Form.Item>
        <Button type="primary" htmlType="submit" loading={loading}>Lưu</Button>
      </Form>
    </>
  );
}
