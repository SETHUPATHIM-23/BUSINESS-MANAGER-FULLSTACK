type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

type ToastCallback = (toast: ToastMessage) => void;

const listeners = new Set<ToastCallback>();

export const toastEvents = {
  subscribe(listener: ToastCallback) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  
  emit(message: string, type: ToastType = 'error', duration = 4000) {
    const id = Math.random().toString(36).substring(2, 9);
    listeners.forEach((listener) => listener({ id, message, type, duration }));
  },
  
  success(message: string, duration?: number) {
    this.emit(message, 'success', duration);
  },
  
  error(message: string, duration?: number) {
    this.emit(message, 'error', duration);
  },
  
  warning(message: string, duration?: number) {
    this.emit(message, 'warning', duration);
  },
  
  info(message: string, duration?: number) {
    this.emit(message, 'info', duration);
  }
};
