import { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, message, Tag } from 'antd';
import { userApi, type CreateUserPayload } from '../../api/userApi';
import type { CurrentUser } from '../../store/authStore';
import { ROLES, ROLE_LABELS } from '../../store/authStore';
import PageHeader from '../../components/PageHeader';

export default function UserListPage() {
  const [data, setData] = useState<CurrentUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<CreateUserPayload>();

  const load = () => {
    setLoading(true);
    userApi.list().then(setData).catch(() => message.error('Không tải được người dùng.')).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <>
      <PageHeader title="Quản lý người dùng" extra={<Button type="primary" onClick={() => setOpen(true)}>Thêm người dùng</Button>} />
      <Table rowKey="id" loading={loading} dataSource={data} columns={[
        { title: 'Tên đăng nhập', dataIndex: 'username' },
        { title: 'Họ tên', dataIndex: 'fullName' },
        { title: 'Email', dataIndex: 'email' },
        { title: 'Vai trò', render: (_, r) => r.roles.map(role => <Tag key={role}>{ROLE_LABELS[role] || role}</Tag>) },
        { title: 'Trạng thái', render: (_, r) => r.isActive ? <Tag color="green">Hoạt động</Tag> : <Tag color="red">Vô hiệu</Tag> },
        { title: '', render: (_, r) => r.isActive && (
          <Button size="small" danger onClick={() => Modal.confirm({
            title: 'Vô hiệu hóa tài khoản?',
            onOk: () => userApi.deactivate(r.id).then(() => { message.success('Đã vô hiệu hóa.'); load(); }),
          })}>Vô hiệu hóa</Button>
        )},
      ]} />
      <Modal title="Thêm người dùng" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={async (v) => {
          try {
            await userApi.create(v);
            message.success('Tạo người dùng thành công.');
            setOpen(false);
            form.resetFields();
            load();
          } catch {
            message.error('Tạo người dùng thất bại.');
          }
        }}>
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true }, { min: 8, message: 'Tối thiểu 8 ký tự' }]}><Input.Password /></Form.Item>
          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="email" label="Email"><Input type="email" /></Form.Item>
          <Form.Item name="phone" label="Điện thoại"><Input /></Form.Item>
          <Form.Item name="roleCode" label="Vai trò" rules={[{ required: true }]}>
            <Select options={Object.values(ROLES).map(r => ({ value: r, label: ROLE_LABELS[r] || r }))} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
