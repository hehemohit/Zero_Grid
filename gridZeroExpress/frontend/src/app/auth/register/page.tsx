'use client';

import { useState, FormEvent } from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';
import { Eye, EyeOff, AlertTriangle, ArrowLeft } from 'lucide-react';

export default function RegisterPage() {
  const { register } = useAuth();

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
      <div className="min-h-screen bg-[#f8fafc] text-slate-900 tactical-grid-bg flex items-center justify-center px-4 py-12 selection:bg-emerald-100 selection:text-emerald-900 font-sans">
        <div className="w-full max-w-md space-y-6 animate-fade-in text-center">
          <div className="w-14 h-14 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-center mx-auto text-emerald-800 shadow-sm">
            <span className="material-symbols-outlined text-[28px]">check_circle</span>
          </div>
          <h1 className="text-2xl font-display font-bold text-slate-950">Citizen Node Enrolled</h1>
          <div className="bg-white border border-slate-200 rounded-2xl p-6 text-left space-y-4 shadow-sm">
            <p className="text-xs text-slate-600">
              Welcome to ZeroGrid, <strong className="text-slate-900">{form.displayName}</strong>.
            </p>
            <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-950 leading-relaxed space-y-1">
              <div className="font-bold flex items-center gap-1.5 text-emerald-800 uppercase tracking-wider text-[11px]">
                <span className="material-symbols-outlined text-sm">phone_android</span>
                <span>Mobile Client Operation Required</span>
              </div>
              <p className="text-slate-700">
                Citizen nodes operate exclusively via the <strong>ZeroGrid Android App</strong> for offline Bluetooth LE 5.0 mesh routing, hazard radar, and localized emergency alerts.
              </p>
            </div>
            <p className="text-[11px] text-slate-400 font-mono">
              The web operations console is strictly gated for emergency command personnel and rescue teams.
            </p>
            <div className="pt-2">
              <Link
                href="/auth/login"
                className="w-full inline-flex justify-center items-center py-2.5 px-4 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-xs uppercase tracking-wider transition-colors shadow-sm"
              >
                Sign In as Administrator
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-900 tactical-grid-bg flex items-center justify-center px-4 py-12 selection:bg-emerald-100 selection:text-emerald-900 font-sans">
      <div className="w-full max-w-md space-y-6 animate-fade-in">
        {/* Top Back Nav */}
        <div className="flex items-center justify-between">
          <Link
            href="/"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-emerald-800 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Main Overview</span>
          </Link>
          <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200">
            CITIZEN MESH NETWORK
          </span>
        </div>

        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-center text-emerald-800 shadow-sm mx-auto">
            <span className="material-symbols-outlined text-[24px]">hub</span>
          </div>
          <h1 className="text-2xl font-display font-bold text-slate-950 tracking-tight">
            Create Citizen Node
          </h1>
          <p className="text-xs text-slate-500 font-mono">
            Enrolls your device in the decentralized emergency peer mesh
          </p>
        </div>

        {/* Form Card */}
        <div className="bg-white border border-slate-200 rounded-2xl p-7 shadow-sm space-y-5">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 text-xs text-red-700 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600 flex items-center gap-2">
            <span className="material-symbols-outlined text-base text-emerald-800">info</span>
            <span>Public registration generates standard Citizen accounts for the Android mobile application.</span>
          </div>

          <form id="register-form" onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="reg-name" className="block text-xs font-mono font-bold uppercase tracking-wider text-slate-700">
                Display Name
              </label>
              <input
                id="reg-name"
                type="text"
                placeholder="Alice Smith"
                value={form.displayName}
                onChange={set('displayName')}
                required
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-emerald-700 focus:bg-white transition-all font-mono"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="reg-email" className="block text-xs font-mono font-bold uppercase tracking-wider text-slate-700">
                Email Address
              </label>
              <input
                id="reg-email"
                type="email"
                placeholder="alice@example.com"
                value={form.email}
                onChange={set('email')}
                required
                autoComplete="email"
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-emerald-700 focus:bg-white transition-all font-mono"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="reg-password" className="block text-xs font-mono font-bold uppercase tracking-wider text-slate-700">
                Password <span className="text-slate-400 font-normal lowercase">(min. 8 chars)</span>
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
                  placeholder="••••••••••••"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 pr-10 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-emerald-700 focus:bg-white transition-all font-mono"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              id="register-submit"
              disabled={loading}
              className={`w-full py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-sm flex items-center justify-center gap-2 active:scale-95 ${
                loading ? 'opacity-60 cursor-not-allowed' : ''
              }`}
            >
              <span className="material-symbols-outlined text-sm">how_to_reg</span>
              <span>{loading ? 'Creating Citizen Profile...' : 'Enroll Account'}</span>
            </button>
          </form>

          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 font-mono">
            <span>Already have an account?</span>
            <Link href="/auth/login" className="text-emerald-800 hover:text-emerald-950 font-bold hover:underline">
              Sign In &gt;
            </Link>
          </div>
        </div>

        {/* Footer info */}
        <div className="text-center text-[11px] text-slate-400 font-mono space-y-1">
          <div>ZeroGrid Tactical Operations // AWS Bedrock Strands v2.4</div>
          <div>MIT License • AP-SOUTH-1 Mumbai Deployment</div>
        </div>
      </div>
    </div>
  );
}
