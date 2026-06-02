export function formatDate(value: string | undefined | null): string {
  if (!value) return '—';
  const d = value.length >= 10 ? value.slice(0, 10) : value;
  const [y, m, day] = d.split('-');
  if (!y || !m || !day) return value;
  return `${day}/${m}/${y}`;
}

export function formatDateTime(value: string | undefined | null): string {
  if (!value) return '—';
  return value.replace('T', ' ').slice(0, 16);
}
