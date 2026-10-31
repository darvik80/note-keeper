import React from 'react';
import type { AutosaveStatus } from '../hooks/useAutosave';

type SaveButtonProps = {
  dirty: boolean;
  status: AutosaveStatus;
  failed?: boolean;
  onClick: () => void;
};

/** Primary save control. Amber while dirty, mint after a successful write, pressed scale on click. */
export const SaveButton: React.FC<SaveButtonProps> = ({ dirty, status, failed, onClick }) => {
  const saving = status === 'saving';
  const error = failed || status === 'error';
  // Clean note (loaded or just written) shows Saved — do not wait for autosave status.
  const saved = !dirty && !error && !saving;

  const label = saving ? 'Saving…' : error ? 'Retry' : saved ? 'Saved' : 'Save';
  const icon = saving ? 'fa-spinner fa-spin' : error ? 'fa-exclamation' : saved ? 'fa-check' : 'fa-save';
  const kind = saved ? 'nk-btn-saved' : error ? 'nk-btn-danger' : 'nk-btn-primary';

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={saving}
      className={`nk-btn ${kind} shrink-0 min-w-[6.25rem]`}
      title={label}
      aria-live="polite"
      aria-busy={saving}
    >
      <i className={`fas ${icon}`} aria-hidden="true"></i>
      <span className="hidden sm:inline">{label}</span>
    </button>
  );
};
