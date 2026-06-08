/*
Defines three reusable generics that mirror the backend's standard response envelope:
ApiResponse<T> — wraps any response: { success, message, data: T }
PageResponse<T> — paginated list: { content: T[], page, size, totalElements, totalPages }
LookupItem — a generic dropdown option: { id, code, name }
*/

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LookupItem {
  id: string;
  code: string;
  name: string;
}
