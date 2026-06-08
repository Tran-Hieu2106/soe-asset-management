/*
formatDate(value) converts ISO date strings (YYYY-MM-DD) to the Vietnamese convention (DD/MM/YYYY). 
formatDateTime(value) strips the T from ISO datetime strings and truncates to minute precision.
*/

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
