/*
Uses the browser's built-in Intl.NumberFormat with locale vi-VN and currency VND. 
Returns — for null/undefined. 
This produces correctly formatted Vietnamese Dong strings
*/


export function formatCurrency(value: number | undefined | null): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}
