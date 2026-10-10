'use client';
import { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import AppSidebar from '@/components/AppSidebar';
import { PageSpinner } from '@/components/ui';
import { Shield } from 'lucide-react';

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const isAdminPage = pathname?.startsWith('/admin');
  const isApprovedAdmin = user?.role === 'ADMIN' && user?.adminApproved === true;

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/auth/login');
    }
  }, [user, loading, router]);

  if (loading) return <PageSpinner />;
  if (!user) return null;

  // Strict Gate: Citizen accounts have no access to the web dashboard
  // Unapproved admins are kept in pending status
  if (!isApprovedAdmin) {
    return (
      <div className="min-h-screen w-full bg-canvas flex items-center justify-center p-6 text-primaryText">
        <div className="max-w-md w-full glass-card rounded-2xl p-8 border border-hairline shadow-2xl text-center space-y-5 animate-fade-in">
          <div className="w-14 h-14 rounded-2xl bg-brandTealDark border border-brandTeal/30 flex items-center justify-center mx-auto text-brandTeal shadow-glow-teal">
            <Shield className="w-7 h-7" />
          </div>
          <div>
            <h2 className="text-xl font-bold font-display">
              {user.role === 'ADMIN' ? 'Approval Pending' : 'Mobile Application Required'}
            </h2>
            <p className="text-xs text-secondaryText mt-1">
              {user.role === 'ADMIN'
                ? 'Your administrator credentials are confirmed and awaiting approval by an active system administrator.'
                : 'ZeroGrid Citizen accounts do not have access to the web operations dashboard.'}
            </p>
          </div>
          <div className="p-4 bg-surfaceElevated border border-hairline rounded-xl text-xs text-mutedGray leading-relaxed text-left">
            {user.role === 'ADMIN' ? (
              <span>⏳ Please contact your command supervisor to approve your account in the Admin Directory. Once approved, you can access the emergency console.</span>
            ) : (
              <span>📱 Please open the <strong>ZeroGrid Android App</strong> on your mobile phone to broadcast SOS, connect to decentralized BLE meshes, and monitor local telemetry.</span>
            )}
          </div>
          <button
            onClick={() => {
              logout();
              router.replace('/auth/login');
            }}
            className="w-full py-2.5 px-4 rounded-xl bg-surfaceCard hover:bg-surfaceElevated border border-hairline text-xs font-semibold text-secondaryText hover:text-primaryText transition-colors"
          >
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col md:flex-row h-screen w-full bg-canvas text-primaryText transition-colors duration-200 overflow-hidden select-none">
      <AppSidebar />
      <main
        className={`flex-1 min-w-0 h-full ${
          isAdminPage
            ? 'overflow-hidden flex flex-col'
            : 'overflow-y-auto'
        }`}
      >
        {children}
      </main>
    </div>
  );
}

