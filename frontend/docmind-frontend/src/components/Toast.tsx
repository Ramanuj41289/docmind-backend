
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastData {
  message: string;
  type?: ToastType;
}

interface ToastProps {
  toast: ToastData | null;
  onClose: () => void;
}

export default function Toast({ toast, onClose }: ToastProps) {
  if (!toast) {
    return null;
  }

  const { message, type = 'success' } = toast;

  const renderIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle2 className="w-4 h-4 text-emerald-400" />;

      case 'error':
        return <AlertCircle className="w-4 h-4 text-red-400" />;

      default:
        return <Info className="w-4 h-4 text-blue-400" />;
    }
  };

  const getBackgroundBorder = (): string => {
    switch (type) {
      case 'success':
        return 'bg-[#0f291e] border-emerald-500/40 text-emerald-200';

      case 'error':
        return 'bg-[#2b1111] border-red-500/40 text-red-200';

      default:
        return 'bg-[#111e33] border-blue-500/40 text-blue-200';
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-slideUp">
      <div
        className={`flex items-center gap-3 px-4 py-3 rounded-xl border shadow-xl text-xs font-medium ${getBackgroundBorder()}`}
      >
        {renderIcon()}

        <span>{message}</span>

        <button
          type="button"
          onClick={onClose}
          aria-label="Close notification"
          className="ml-2 text-slate-400 hover:text-white transition"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
