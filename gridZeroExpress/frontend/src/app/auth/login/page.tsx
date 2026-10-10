'use client';

import { useState, FormEvent } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Eye, EyeOff, Clock, Smartphone, AlertTriangle, ArrowLeft } from 'lucide-react';

export default function LoginPage() {
  const { login, logout } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [isPendingApproval, setIsPendingApproval] = useState(false);
  const [isCitizenBlocked, setIsCitizenBlocked] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setIsPendingApproval(false);
    setIsCitizenBlocked(false);
    setLoading(true);

    try {
      const loggedUser = await login(email, password);

      // Block Citizen accounts from web dashboard
      if (loggedUser.role === 'CITIZEN') {
        logout();
        setIsCitizenBlocked(true);
        return;
      }

      // Check Admin approval status
      if (loggedUser.role === 'ADMIN') {
        if (loggedUser.adminApproved !== true) {
          logout();
          setIsPendingApproval(true);
          return;
        }
        router.push('/admin');
        return;
      }

      // Default fallback
      router.push('/admin');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Login failed';
      if (msg.toLowerCase().includes('pending approval')) {
        setIsPendingApproval(true);
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
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
            AP-SOUTH-1 SECURE GATE
          </span>
        </div>

        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-center text-emerald-800 shadow-sm mx-auto">
            <span className="material-symbols-outlined text-[24px]">hub</span>
          </div>
          <h1 className="text-2xl font-display font-bold text-slate-950 tracking-tight">
            Authority Command Portal
          </h1>
          <p className="text-xs text-slate-500 font-mono">
            ZeroGrid Dispatched Responders &amp; Super-Admin Authentication
          </p>
        </div>

        {/* Form Card */}
        <div className="bg-white border border-slate-200 rounded-2xl p-7 shadow-sm space-y-5">
          {/* Pending Approval Banner */}
          {isPendingApproval && (
            <div className="bg-amber-50/80 border border-amber-200 rounded-xl p-4 text-xs space-y-2 text-amber-950">
              <div className="flex items-center gap-2 font-bold text-xs text-amber-800 uppercase tracking-wider">
                <Clock className="w-4 h-4 shrink-0 text-amber-700" />
                <span>Admin Account Pending Approval</span>
              </div>
              <p className="leading-relaxed text-slate-700">
                Your credentials are valid, but your access requires activation by an active ZeroGrid system administrator before command features are unlocked.
              </p>
              <p className="text-[11px] text-amber-800 font-mono">
                Contact your dispatch lead or command supervisor to approve your account in the Admin Directory.
              </p>
            </div>
          )}

          {/* Citizen Mobile Notice */}
          {isCitizenBlocked && (
            <div className="bg-emerald-50/80 border border-emerald-200 rounded-xl p-4 text-xs space-y-2 text-emerald-950">
              <div className="flex items-center gap-2 font-bold text-xs text-emerald-800 uppercase tracking-wider">
                <Smartphone className="w-4 h-4 shrink-0 text-emerald-700" />
                <span>Mobile Application Required</span>
              </div>
              <p className="leading-relaxed text-slate-700">
                This web console is strictly reserved for emergency dispatchers and rescue teams. Citizen accounts operate exclusively through the mobile application.
              </p>
              <p className="text-[11px] text-emerald-800 font-mono">
                Please open the <strong>ZeroGrid Android App</strong> on your mobile device to broadcast SOS and connect to local BLE meshes.
              </p>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 text-xs text-red-700 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{error}</span>
            </div>
          )}

          <form id="login-form" onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="login-email" className="block text-xs font-mono font-bold uppercase tracking-wider text-slate-700">
                Authorized Email
              </label>
              <input
                id="login-email"
                type="email"
                placeholder="dispatcher.name@zerogrid.org"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                autoComplete="email"
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-emerald-700 focus:bg-white transition-all font-mono"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="login-password" className="block text-xs font-mono font-bold uppercase tracking-wider text-slate-700">
                Secure Password
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
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
              id="login-submit"
              disabled={loading}
              className={`w-full py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-sm flex items-center justify-center gap-2 active:scale-95 ${
                loading ? 'opacity-60 cursor-not-allowed' : ''
              }`}
            >
              <span className="material-symbols-outlined text-sm">security</span>
              <span>{loading ? 'Authenticating Credentials...' : 'Sign In to Operations Console'}</span>
            </button>
          </form>

          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 font-mono">
            <span>Registering citizen?</span>
            <Link href="/auth/register" className="text-emerald-800 hover:text-emerald-950 font-bold hover:underline">
              Create Citizen Account &gt;
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
