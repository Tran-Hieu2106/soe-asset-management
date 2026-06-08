import { Button } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';

/*
A reusable Ant Design <Button> with a download icon, 
loading state, and an onExport callback. 
Also exports the standalone downloadBlob(blob, filename) utility which creates a temporary <a> element, 
triggers a click to start the browser's file download, 
then cleans up the object URL
*/
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
