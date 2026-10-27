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
          className="flex-1 nk-btn nk-btn-ghost border border-border disabled:opacity-50"
        >
          {cancelLabel}
        </button>
        <button
          onClick={onConfirm}
          disabled={loading}
          className={`flex-1 nk-btn disabled:opacity-50 ${
            variant === 'danger'
              ? 'bg-danger text-white'
              : 'nk-btn-primary'
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
