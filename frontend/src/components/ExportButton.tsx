import { Button } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';

interface ExportButtonProps {
  label?: string;
  loading?: boolean;
  onExport: () => void;
}

export default function ExportButton({ label = 'Xuất báo cáo', loading, onExport }: ExportButtonProps) {
  return (
    <Button type="primary" icon={<DownloadOutlined />} loading={loading} onClick={onExport}>
      {label}
    </Button>
  );
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
