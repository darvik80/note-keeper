/**
 * @module Login
 * @category Pages
 * @description Login / registration page with local and Google OAuth authentication.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {api} from '../utils/api';
import {AuthRequest} from '../types';
import {CogitorMark} from '../components/CogitorMark';

export default function Login() {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [oauth, setOauth] = useState({ google: true, yandex: true });
  const navigate = useNavigate();

  useEffect(() => {
    fetch('/api/v1/config')
      .then(res => res.ok ? res.json() : null)
      .then(cfg => {
        if (cfg?.oauth) {
          setOauth({
            google: cfg.oauth.google !== false,
            yandex: cfg.oauth.yandex !== false,
          });
        }
      })
      .catch(() => { /* keep default buttons */ });
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.split('?')[1] || '');
    const tokenFromUrl = params.get('token');
    if (tokenFromUrl) {
      localStorage.setItem('token', tokenFromUrl);
      fetch('/api/v1/auth/me', {
        headers: { Authorization: `Bearer ${tokenFromUrl}` }
      })
        .then(res => res.ok ? res.json() : null)
        .then(user => {
          if (user) localStorage.setItem('user', JSON.stringify(user));
          navigate('/', { replace: true });
        })
        .catch(() => navigate('/', { replace: true }));
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const request: AuthRequest = {
        email,
        password,
        name: isLogin ? undefined : name,
      };

      const response = await api.auth[isLogin ? 'login' : 'register'](request);

      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify(response.user));

      navigate('/');
    } catch (err: any) {
      let errorMessage = 'Authentication failed';
      let errorDetails = '';

      try {
        const errorData = JSON.parse(err.message);
        errorMessage = errorData.message || errorMessage;
        if (errorData.details) {
          if (typeof errorData.details === 'object') {
            errorDetails = Object.entries(errorData.details)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join(', ');
          } else {
            errorDetails = errorData.details;
          }
        }
      } catch {
        errorMessage = err.message || errorMessage;
      }

      setError(errorMessage + (errorDetails ? ` (${errorDetails})` : ''));
    } finally {
      setLoading(false);
    }
  };

  const oauthBase = window.location.port === '5173' || window.location.port === '3001'
    ? 'http://localhost:8080'
    : '';

  const handleOAuthLogin = (provider: 'google' | 'yandex') => {
    window.location.href = `${oauthBase}/oauth2/authorization/${provider}`;
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="relative max-w-md w-full">
        <div className="mb-10">
          <div className="w-14 h-14 rounded-[18px] bg-[#0B0E11] flex items-center justify-center text-[#EDEFF2] mb-6">
            <CogitorMark size={32} />
          </div>
          <h1 className="text-display text-text">NoteKeeper</h1>
          <p className="text-[17px] text-text-secondary mt-2">
            {isLogin ? 'Welcome back.' : 'Create your account.'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLogin && (
            <div>
              <label className="block text-[13px] font-medium text-text-secondary mb-1.5">Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="input-field"
                required={!isLogin}
              />
            </div>
          )}

          <div>
            <label className="block text-[13px] font-medium text-text-secondary mb-1.5">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input-field"
              required
            />
          </div>

          <div>
            <label className="block text-[13px] font-medium text-text-secondary mb-1.5">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input-field"
              required
            />
          </div>

          {error && (
            <div role="alert" className="text-danger text-sm bg-danger/10 p-3 rounded-xl">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full nk-btn nk-btn-primary py-3 mt-2"
          >
            {loading ? 'Loading…' : isLogin ? 'Sign in' : 'Sign up'}
          </button>
        </form>

        {(oauth.google || oauth.yandex) && (
        <div className="mt-8">
          <p className="text-[13px] text-text-secondary mb-3">Or continue with</p>

          {oauth.google && (
            <button
              type="button"
              onClick={() => handleOAuthLogin('google')}
              className="w-full flex items-center justify-center gap-2 nk-btn nk-btn-ghost border border-border py-2.5"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span className="text-text font-medium">Google</span>
            </button>
          )}
          {oauth.yandex && (
            <button
              type="button"
              onClick={() => handleOAuthLogin('yandex')}
              className="mt-2 w-full flex items-center justify-center gap-2 nk-btn nk-btn-ghost border border-border py-2.5"
            >
              <span className="inline-flex items-center justify-center w-5 h-5 rounded-sm bg-[#FC3F1D] text-white text-xs font-bold">Я</span>
              <span className="text-text font-medium">Yandex</span>
            </button>
          )}
        </div>
        )}

        <div className="mt-8">
          <button
            onClick={() => setIsLogin(!isLogin)}
            className="text-secondary hover:opacity-80 text-[15px]"
          >
            {isLogin ? "Don't have an account? Sign up" : 'Already have an account? Sign in'}
          </button>
        </div>
      </div>
    </div>
  );
}
