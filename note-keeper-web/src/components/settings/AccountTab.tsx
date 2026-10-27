import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { User } from '../../types';
import { api } from '../../utils/api';
import { useToast } from '../../contexts/ToastContext';

export const AccountTab: React.FC = () => {
  const { toast } = useToast();
  const location = useLocation();
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [busy, setBusy] = useState<'google' | 'yandex' | null>(null);

  const refresh = async () => {
    const me = await api.auth.getCurrentUser();
    setUser(me);
    localStorage.setItem('user', JSON.stringify(me));
  };

  useEffect(() => {
    refresh().catch(() => setUser(null));
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const linked = params.get('linked');
    const linkError = params.get('linkError');
    if (linked) {
      toast.success(`${linked === 'yandex' ? 'Yandex' : 'Google'} linked`);
      refresh().catch(() => undefined);
      navigate('/settings', { replace: true });
    } else if (linkError) {
      toast.error(linkError);
      navigate('/settings', { replace: true });
    }
  }, [location.search]);

  const connect = async (provider: 'google' | 'yandex') => {
    setBusy(provider);
    try {
      const { url } = await api.auth.startLink(provider);
      window.location.href = url;
    } catch (err) {
      toast.error((err as Error).message || 'Could not start link');
      setBusy(null);
    }
  };

  const disconnect = async (provider: 'google' | 'yandex') => {
    setBusy(provider);
    try {
      const me = await api.auth.unlink(provider);
      setUser(me);
      localStorage.setItem('user', JSON.stringify(me));
      toast.success(`${provider === 'yandex' ? 'Yandex' : 'Google'} unlinked`);
    } catch (err) {
      toast.error((err as Error).message || 'Unlink failed');
    } finally {
      setBusy(null);
    }
  };

  const googleOn = !!user?.googleId;
  const yandexOn = !!user?.yandexId;

  return (
    <div className="nk-card p-6 space-y-5">
      <div>
        <h3 className="text-xl font-bold text-text">Sign-in methods</h3>
        <p className="mt-1 text-[15px] text-text-secondary">
          Link Google and Yandex to the same notes — emails can differ.
        </p>
      </div>

      <ProviderRow
        name="Google"
        linked={googleOn}
        busy={busy === 'google'}
        onConnect={() => connect('google')}
        onDisconnect={() => disconnect('google')}
      />
      <ProviderRow
        name="Yandex"
        linked={yandexOn}
        busy={busy === 'yandex'}
        onConnect={() => connect('yandex')}
        onDisconnect={() => disconnect('yandex')}
      />
    </div>
  );
};

const ProviderRow: React.FC<{
  name: string;
  linked: boolean;
  busy: boolean;
  onConnect: () => void;
  onDisconnect: () => void;
}> = ({ name, linked, busy, onConnect, onDisconnect }) => (
  <div className="flex items-center justify-between gap-3 py-3 border-t border-border first:border-0 first:pt-0">
    <div>
      <div className="font-medium text-text">{name}</div>
      <div className="text-sm text-text-secondary">{linked ? 'Connected' : 'Not connected'}</div>
    </div>
    {linked ? (
      <button type="button" disabled={busy} onClick={onDisconnect} className="nk-btn">
        {busy ? '…' : 'Unlink'}
      </button>
    ) : (
      <button type="button" disabled={busy} onClick={onConnect} className="nk-btn nk-btn-primary">
        {busy ? '…' : 'Connect'}
      </button>
    )}
  </div>
);
