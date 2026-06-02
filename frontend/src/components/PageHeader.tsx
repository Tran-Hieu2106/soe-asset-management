import { Typography, Space } from 'antd';

export default function PageHeader({ title, extra }: { title: string; extra?: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <Typography.Title level={3} style={{ margin: 0 }}>{title}</Typography.Title>
      {extra && <Space>{extra}</Space>}
    </div>
  );
}
