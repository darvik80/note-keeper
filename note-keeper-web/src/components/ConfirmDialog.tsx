import React from 'react';
import { Modal } from './Modal';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'default';
  loading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'default',
  loading = false,
}) => (
  <Modal
    isOpen={isOpen}
    onClose={onClose}
    title={title}
    footer={
      <>
        <button
          onClick={onClose}
          disabled={loading}
          className="flex-1 px-4 py-2 border border-border rounded-lg hover:bg-hover transition-colors text-text disabled:opacity-50"
        >
          {cancelLabel}
        </button>
        <button
          onClick={onConfirm}
          disabled={loading}
          className={`flex-1 px-4 py-2 rounded-lg text-white transition-colors disabled:opacity-50 ${
            variant === 'danger'
              ? 'bg-red-500 hover:bg-red-600'
              : 'bg-primary hover:bg-primary/90'
          }`}
        >
          {loading ? 'Processing...' : confirmLabel}
        </button>
      </>
    }
  >
    <p className="text-text-secondary text-sm leading-relaxed">{message}</p>
  </Modal>
);
