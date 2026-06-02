import { useEffect, useState } from 'react';
import { Table, Select, Space, Button, message } from 'antd';
import { reportApi, type AssetReportRow } from '../../api/reportApi';
import { lookupApi } from '../../api/lookupApi';
import type { LookupItem } from '../../types/common.types';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import ExportButton, { downloadBlob } from '../../components/ExportButton';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function AssetReportPage() {
  const [data, setData] = useState<AssetReportRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<string | undefined>();
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [categories, setCategories] = useState<LookupItem[]>([]);
  const [exporting, setExporting] = useState(false);

  const params = { status, categoryId };

  const load = (p = 0) => {
    setLoading(true);
    reportApi.assetReport({ page: p, size: 20, ...params })
      .then(r => { setData(r.content); setTotal(r.totalElements); setPage(p); })
      .catch(() => message.error('Không tải được báo cáo.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    lookupApi.assetCategories().then(setCategories);
    load(0);
  }, []);

  const exportFile = async (format: 'EXCEL' | 'PDF' | 'CSV') => {
    setExporting(true);
    try {
      const blob = await reportApi.exportAssets(format, params);
      const ext = format === 'EXCEL' ? 'xlsx' : format === 'PDF' ? 'pdf' : 'csv';
      downloadBlob(blob, `bao-cao-tai-san.${ext}`);
    } catch {
      message.error('Xuất báo cáo thất bại.');
    } finally {
      setExporting(false);
    }
  };

  return (
    <>
      <PageHeader title="Báo cáo tài sản cố định" extra={
        <Space>
          <ExportButton label="Excel" loading={exporting} onExport={() => exportFile('EXCEL')} />
          <ExportButton label="PDF" loading={exporting} onExport={() => exportFile('PDF')} />
          <ExportButton label="CSV" loading={exporting} onExport={() => exportFile('CSV')} />
        </Space>
      } />
      <Space style={{ marginBottom: 16 }} wrap>
        <Select allowClear placeholder="Trạng thái" style={{ width: 160 }} onChange={setStatus}
          options={['IN_USE', 'MAINTENANCE', 'IDLE', 'TRANSFERRED', 'LIQUIDATED'].map(v => ({ value: v, label: v }))} />
        <Select allowClear placeholder="Danh mục" style={{ width: 200 }} onChange={setCategoryId}
          options={categories.map(c => ({ value: Number(c.id), label: c.name }))} />
        <Button onClick={() => load(0)}>Lọc</Button>
      </Space>
      <Table rowKey="assetId" loading={loading} dataSource={data}
        pagination={{ current: page + 1, total, pageSize: 20, onChange: (p) => load(p - 1) }}
        columns={[
          { title: 'Mã TS', dataIndex: 'assetCode' },
          { title: 'Tên', dataIndex: 'assetName' },
          { title: 'Danh mục', dataIndex: 'categoryName' },
          { title: 'Đơn vị', dataIndex: 'managingUnitName' },
          { title: 'Nguyên giá', render: (_, r) => formatCurrency(r.originalCost) },
          { title: 'GTCL', render: (_, r) => formatCurrency(r.netBookValue) },
          { title: 'Ngày GT', render: (_, r) => formatDate(r.acquisitionDate) },
          { title: 'Trạng thái', render: (_, r) => <StatusBadge status={r.status} /> },
        ]}
      />
    </>
  );
}
