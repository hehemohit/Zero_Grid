'use client';
import { useState, FormEvent } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Shield, Eye, EyeOff, Clock, Smartphone, AlertTriangle } from 'lucide-react';
import { Input, Button } from '@/components/ui';

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

      // Default fallback for any other role
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
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_40%_at_50%_0%,rgba(239,68,68,0.1),transparent)]" />

      <div className="relative w-full max-w-md space-y-8 fade-in">
        {/* Logo */}
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-center justify-center mx-auto">
            <Shield className="w-6 h-6 text-red-400" />
          </div>
          <h1 className="text-2xl font-black text-white">Emergency Command Portal</h1>
          <p className="text-gray-500 text-sm">Sign in to your ZeroGrid authority account</p>
        </div>

        {/* Form card */}
        <div className="glass-card rounded-2xl p-8 space-y-5">
          {isPendingApproval && (
            <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 text-xs space-y-2 text-amber-300">
              <div className="flex items-center gap-2 font-bold text-sm text-amber-400">
                <Clock className="w-4 h-4 shrink-0" />
                <span>Admin Account Pending Approval</span>
              </div>
              <p className="leading-relaxed">
                Your administrator credentials are confirmed but require active approval from an existing ZeroGrid system administrator before access is granted.
              </p>
              <p className="text-[11px] text-amber-400/80">
                Please contact your dispatch lead or command supervisor to approve your account in the Admin Directory.
              </p>
            </div>
          )}

          {isCitizenBlocked && (
            <div className="bg-sky-500/10 border border-sky-500/30 rounded-xl p-4 text-xs space-y-2 text-sky-200">
              <div className="flex items-center gap-2 font-bold text-sm text-sky-300">
                <Smartphone className="w-4 h-4 shrink-0" />
                <span>Mobile Application Required</span>
              </div>
              <p className="leading-relaxed">
                This web console is strictly reserved for emergency command and rescue personnel. Citizen accounts do not have access to the web dashboard.
              </p>
              <p className="text-[11px] text-sky-300/80">
                Please open the <strong>ZeroGrid Android App</strong> on your mobile device to broadcast SOS, sync with family links, and access local mesh services.
              </p>
            </div>
          )}

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg px-4 py-3 text-sm text-red-400 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form id="login-form" onSubmit={handleSubmit} className="space-y-5">
            <Input
              id="login-email"
              label="Email address"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              autoComplete="email"
            />

            <div className="space-y-1.5">
              <label htmlFor="login-password" className="block text-sm font-medium text-gray-300">
                Password
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2.5 pr-10 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500/50 focus:border-red-500 transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                >
                  {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              id="login-submit"
              loading={loading}
              className="w-full"
              size="lg"
            >
              Sign In
            </Button>
          </form>
        </div>

        <p className="text-center text-sm text-gray-500">
          Don&apos;t have an account?{' '}
          <Link href="/auth/register" className="text-red-400 hover:text-red-300 font-medium">
            Create one free
          </Link>
        </p>
      </div>
    </div>
  );
}
