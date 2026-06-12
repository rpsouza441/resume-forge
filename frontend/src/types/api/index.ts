export interface ApiError {
  error: string;
  message: string;
  details?: { field: string; message: string }[];
  timestamp: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}
