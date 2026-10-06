import axios from 'axios';
import { toastEvents } from './toast';

// Define standard backend error shape
export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldErrorDetail[];
}

export function isApiError(err: unknown): err is { response: { status?: number; data?: ApiError } } {
  return typeof err === 'object' && err !== null && 'response' in err && typeof (err as any).response === 'object' && (err as any).response !== null;
}

// Define standard frontend pagination shapes matching Prompt 6
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string | string[]; // e.g. 'name,asc' or ['name,asc', 'createdAt,desc']
}

let accessToken = '';
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export const setAccessToken = (token: string) => {
  accessToken = token;
};

export const setRefreshTokenInCookie = (token: string) => {
  if (token) {
    document.cookie = `refreshToken=${token}; path=/; max-age=604800; Secure; SameSite=Strict`;
  } else {
    document.cookie = `refreshToken=; path=/; max-age=0`;
  }
};

export const getRefreshTokenFromCookie = (): string => {
  const match = document.cookie.match(new RegExp('(^| )refreshToken=([^;]+)'));
  return match ? match[2] : '';
};

// Configure base URL dynamically per environment
export const api = axios.create({
  baseURL: API_BASE_URL || undefined,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach access token to requests
api.interceptors.request.use(
  (config) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor to handle token refresh and toast errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 1. Check if token needs rotation on 401
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/api/auth/refresh')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const rToken = getRefreshTokenFromCookie();
      if (!rToken) {
        isRefreshing = false;
        return Promise.reject(error);
      }

      try {
        const refreshResponse = await api.post('/api/auth/refresh', {
          refreshToken: rToken,
        });

        const newAccessToken = refreshResponse.data.accessToken;
        setAccessToken(newAccessToken);
        processQueue(null, newAccessToken);
        isRefreshing = false;

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        isRefreshing = false;
        
        setAccessToken('');
        setRefreshTokenInCookie('');
        
        window.location.href = '/login?expired=true';
        return Promise.reject(refreshError);
      }
    }

    // 2. Format and toast errors matching standard backend shape
    if (error.response) {
      const status = error.response.status;
      if (status === 403) {
        // Only show "Access Denied" if the request was authenticated.
        // An unauthenticated 403 means the user simply isn't logged in yet —
        // showing "Access Denied" before they've entered credentials is misleading.
        const hadAuthHeader = !!originalRequest?.headers?.Authorization;
        if (hadAuthHeader) {
          toastEvents.error('Access Denied (403): You do not have permission to perform this action.');
        }
      } else if (status === 404) {
        toastEvents.error('Resource not found (404).');
      } else if (status >= 500) {
        toastEvents.error('Server error (500). Please try again later.');
      } else if (status !== 401) {
        const apiError = error.response.data as ApiError;
        if (apiError && apiError.message) {
          if (apiError.fieldErrors && apiError.fieldErrors.length > 0) {
            const fields = apiError.fieldErrors.map(f => `${f.field}: ${f.message}`).join(', ');
            toastEvents.error(`Validation Failed - ${fields}`);
          } else {
            toastEvents.error(apiError.message);
          }
        } else {
          toastEvents.error(`Request failed with status ${status}`);
        }
      }
    } else if (!error.response) {
      toastEvents.error('Network connection error. Please check your network connection.');
    }

    return Promise.reject(error);
  }
);

// Typed request helper for paginated list endpoints
export const fetchPaginated = async <T>(
  url: string,
  params?: PaginationParams & Record<string, any>
): Promise<PageResponse<T>> => {
  const response = await api.get<PageResponse<T>>(url, {
    params,
    paramsSerializer: {
      indexes: null // Keeps serialization flat (e.g. ?sort=name,asc&sort=createdAt,desc)
    }
  });
  return response.data;
};
