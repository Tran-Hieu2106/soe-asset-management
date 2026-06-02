import { useState } from 'react';
import { Table, Input, Space, Button, message } from 'antd';
import { reportApi } from '../../api/reportApi';
import PageHeader from '../../components/PageHeader';
import ExportButton, { downloadBlob } from '../../components/ExportButton';

export default function StockReportPage() {
  const [data, setData] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(false);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const load = () => {
    setLoading(true);
    reportApi.stockReport(startDate || undefined, endDate || undefined)
      .then(r => setData(r as Record<string, unknown>[]))
      .catch(() => message.error('Không tải được báo cáo.'))
      .finally(() => setLoading(false));
  };

  const columns = data.length > 0
    ? Object.keys(data[0]).map(k => ({ title: k, dataIndex: k, key: k }))
    : [];

  return (
    <>
      <PageHeader title="Báo cáo xuất nhập vật tư" extra={
        <ExportButton label="Xuất Excel" onExport={async () => {
          try {
            const blob = await reportApi.exportStock(startDate || undefined, endDate || undefined);
            downloadBlob(blob, 'bao-cao-vat-tu.xlsx');
          } catch {
            message.error('Xuất báo cáo thất bại.');
          }
        }} />
      } />
      <Space style={{ marginBottom: 16 }} wrap>
        <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        <Button onClick={load}>Tải dữ liệu</Button>
      </Space>
      <Table rowKey={(_, i) => String(i)} loading={loading} dataSource={data} columns={columns} />
    </>
  );
}
