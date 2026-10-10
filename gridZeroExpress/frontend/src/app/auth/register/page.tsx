'use client';
import { useState, FormEvent } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Shield, Eye, EyeOff } from 'lucide-react';
import { Input, Button } from '@/components/ui';

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();

  const [form, setForm] = useState({
    displayName: '',
    email: '',
    password: '',
  });
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [registeredSuccess, setRegisteredSuccess] = useState(false);

  function set(k: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm(prev => ({ ...prev, [k]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await register({ ...form, role: 'CITIZEN' });
      setRegisteredSuccess(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  if (registeredSuccess) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4 py-12">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_40%_at_50%_0%,rgba(16,185,129,0.1),transparent)]" />
        <div className="relative w-full max-w-md space-y-6 text-center">
          <div className="w-16 h-16 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl flex items-center justify-center mx-auto text-emerald-400">
            <Shield className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-black text-white">Citizen Account Created</h1>
          <div className="glass-card rounded-2xl p-6 text-left space-y-4">
            <p className="text-sm text-gray-300">
              Welcome to ZeroGrid, <strong className="text-white">{form.displayName}</strong>.
            </p>
            <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-xs text-emerald-300 leading-relaxed">
              📱 <strong>Mobile Application Required:</strong> Citizen accounts operate exclusively via the <strong>ZeroGrid Android App</strong> for decentralized BLE mesh routing, offline hazard detection, and SOS dispatch.
            </div>
            <p className="text-xs text-gray-400">
              The web console is strictly reserved for emergency command dispatchers and administrators.
            </p>
            <div className="pt-2">
              <Link
                href="/auth/login"
                className="w-full inline-flex justify-center items-center py-2.5 px-4 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-gray-950 font-bold text-sm transition-colors"
              >
                Return to Sign In
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4 py-12">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_40%_at_50%_0%,rgba(239,68,68,0.1),transparent)]" />

      <div className="relative w-full max-w-md space-y-8 fade-in">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-center justify-center mx-auto">
            <Shield className="w-6 h-6 text-red-400" />
          </div>
          <h1 className="text-2xl font-black text-white">Join ZeroGrid</h1>
          <p className="text-gray-500 text-sm">Create your free emergency network account</p>
        </div>

        <div className="glass-card rounded-2xl p-8 space-y-5">
          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg px-4 py-3 text-sm text-red-400">
              {error}
            </div>
          )}

          <div className="p-3 bg-white/5 border border-white/10 rounded-xl text-xs text-gray-400 flex items-center gap-2">
            <span>ℹ️</span>
            <span>Public registration creates standard Citizen accounts for the mobile mesh app.</span>
          </div>

          <form id="register-form" onSubmit={handleSubmit} className="space-y-5">
            <Input
              id="reg-name"
              label="Display Name"
              type="text"
              placeholder="Alice Smith"
              value={form.displayName}
              onChange={set('displayName')}
              required
            />

            <Input
              id="reg-email"
              label="Email address"
              type="email"
              placeholder="you@example.com"
              value={form.email}
              onChange={set('email')}
              required
              autoComplete="email"
            />

            <div className="space-y-1.5">
              <label htmlFor="reg-password" className="block text-sm font-medium text-gray-300">
                Password <span className="text-gray-600 font-normal">(min. 8 chars)</span>
              </label>
              <div className="relative">
                <input
                  id="reg-password"
                  type={showPass ? 'text' : 'password'}
                  value={form.password}
                  onChange={set('password')}
                  required
                  minLength={8}
                  autoComplete="new-password"
                  placeholder="••••••••"
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2.5 pr-10 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500/50 focus:border-red-500 transition-colors"
                />
                <button type="button" onClick={() => setShowPass(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300">
                  {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" id="register-submit" loading={loading} className="w-full" size="lg">
              Create Account
            </Button>
          </form>
        </div>

        <p className="text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link href="/auth/login" className="text-red-400 hover:text-red-300 font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
