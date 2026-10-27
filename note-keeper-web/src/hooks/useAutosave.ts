import { useEffect, useRef, useState } from 'react';

export type AutosaveStatus = 'idle' | 'saving' | 'saved' | 'error';

export function useAutosave(dirty: boolean, save: () => Promise<void>, delay = 800, watch?: unknown) {
  const [status, setStatus] = useState<AutosaveStatus>('idle');
  const saveRef = useRef(save);
  saveRef.current = save;

  useEffect(() => {
    if (!dirty) return;
    setStatus('idle');
    const timer = window.setTimeout(async () => {
      setStatus('saving');
      try {
        await saveRef.current();
        setStatus('saved');
      } catch {
        setStatus('error');
      }
    }, delay);
    return () => window.clearTimeout(timer);
  }, [dirty, delay, watch]);

  return status;
}
