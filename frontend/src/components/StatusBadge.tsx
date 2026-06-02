import { Tag } from 'antd';

const LABELS: Record<string, string> = {
  IN_USE: 'Đang sử dụng',
  MAINTENANCE: 'Bảo trì',
  IDLE: 'Chờ phân bổ',
  TRANSFERRED: 'Đã bàn giao',
  LIQUIDATED: 'Đã thanh lý',
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ phê duyệt',
  APPROVED: 'Đã phê duyệt',
  CONFIRMED: 'Đã xác nhận',
  COMPLETED: 'Hoàn thành',
  REJECTED: 'Từ chối',
  PENDING_MANAGER: 'Chờ quản lý',
  PENDING_DIRECTOR: 'Chờ giám đốc',
};

const COLORS: Record<string, string> = {
  IN_USE: 'green',
  MAINTENANCE: 'orange',
  IDLE: 'default',
  TRANSFERRED: 'blue',
  LIQUIDATED: 'red',
  DRAFT: 'default',
  PENDING_APPROVAL: 'gold',
  PENDING_MANAGER: 'gold',
  PENDING_DIRECTOR: 'gold',
  APPROVED: 'blue',
  CONFIRMED: 'cyan',
  COMPLETED: 'green',
  REJECTED: 'red',
};

export default function StatusBadge({ status }: { status: string }) {
  return <Tag color={COLORS[status] || 'default'}>{LABELS[status] || status}</Tag>;
}
