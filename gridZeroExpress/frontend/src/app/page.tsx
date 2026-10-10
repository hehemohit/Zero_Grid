'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';

interface TierDetail {
  title: string;
  runtime: string;
  protocol: string;
  failover: string;
}

const TIER_DATA: Record<string, TierDetail> = {
  t1: {
    title: 'INSPECTOR: TIER 1 // FRONTEND INGRESS & FIELD HARDWARE',
    runtime: 'Kotlin 2.0 Compose / Next.js 16 SSR / Amplify Gen2',
    protocol: 'BLE 5.0 Custom GAP Advertisement & WSS fallback',
    failover: 'Local Encrypted SQLite Cache for fully offline operation',
  },
  t2: {
    title: 'INSPECTOR: TIER 2 // SECURITY & GATEWAY PERIMETER',
    runtime: 'Amazon API Gateway (Regional) / AWS Cognito User Pools',
    protocol: 'REST HTTPS with SigV4 Signing / WSS Duplex Stream',
    failover: 'DDoS Shield Standard with WAF Rate Limiting 10k RPS',
  },
  t3: {
    title: 'INSPECTOR: TIER 3 // SERVERLESS INGESTION & VOICE HOOK',
    runtime: 'Python 3.12 AWS Lambda / FastAPI Mangum Adapter',
    protocol: 'SQS FIFO Queue Buffer (Zero Message Droppage)',
    failover: 'Dead Letter Queue with S3 raw telemetry archiving',
  },
  t4: {
    title: 'INSPECTOR: TIER 4 // ECS FARGATE AGENTZERO ORCHESTRATOR',
    runtime: 'Python 3.12 / Strands SDK 1.19 / FastMCP Server',
    protocol: 'gRPC Internal / WebSocket Outbound / SSE Telemetry',
    failover: 'Multi-AZ ECS Fargate with Local SQLite Offline Fallback',
  },
  t5: {
    title: 'INSPECTOR: TIER 5 // AMAZON BEDROCK LLM COGNITIVE CORE',
    runtime: 'Claude 3.5 Sonnet (Inference) + Claude 3 Haiku (Audio Voice)',
    protocol: 'Bedrock Runtime SDK / JSON Schema Validated Tools',
    failover: 'Cross-Region Inference Profile (ap-south-1 to us-east-1)',
  },
  t6: {
    title: 'INSPECTOR: TIER 6 // PERSISTENCE & MUTEX LOCKING',
    runtime: 'MongoDB Atlas 2dsphere + Redis 7.2 Cluster + DynamoDB',
    protocol: 'Redlock Mutex Algorithm / Geospatial GeoJSON Coordinates',
    failover: 'Atlas Cross-Region Replica Sets with Automatic Leader Failover',
  },
};

interface LogItem {
  time: string;
  text: string;
  color: string;
}

export default function LandingPage() {
  const [selectedPath, setSelectedPath] = useState<'A' | 'B'>('A');
  const [selectedTier, setSelectedTier] = useState<string>('t4');
  const [simScenario, setSimScenario] = useState<'underpass' | 'substation'>('underpass');
  const [isSimRunning, setIsSimRunning] = useState(false);
  const [mutexStatus, setMutexStatus] = useState<'IDLE' | 'ACQUIRED'>('IDLE');
  const [logs, setLogs] = useState<LogItem[]>([
    { time: '00:00:00', text: 'ZeroGrid Runtime initialized. Waiting for mesh beacon ingestion...', color: 'text-slate-500' },
    { time: '00:00:01', text: 'Strands Agents SDK 1.19 standby. Amazon Bedrock Claude 3.5 Sonnet connected.', color: 'text-slate-500' },
    { time: '00:00:02', text: 'ElastiCache Redis cluster online. 160 NDRF rescuer pool registered in Atlas.', color: 'text-slate-500' },
  ]);

  const logsEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  const executeSimulation = () => {
    setIsSimRunning(true);
    setMutexStatus('IDLE');

    const getTime = () => new Date().toISOString().substring(11, 19);

    setLogs(prev => [
      ...prev,
      {
        time: getTime(),
        text: `>>> RAW BLE BEACON INGESTED: 0x5A 0x01 TTL:05 RSSI:-82dBm (${simScenario.toUpperCase()})`,
        color: 'text-emerald-400 font-bold',
      },
    ]);

    setTimeout(() => {
      setLogs(prev => [
        ...prev,
        {
          time: getTime(),
          text: '[CONFIDENCE AGENT] Calculating sensor integrity: Audio=0.82, Visual=0.91, IoT Probes=0.95 -> OVERALL: 89.2% (PASS >=65%)',
          color: 'text-slate-200',
        },
      ]);
    }, 500);

    setTimeout(() => {
      setLogs(prev => [
        ...prev,
        {
          time: getTime(),
          text: '[GATEKEEPER] Running MongoDB $geoWithin query... Deduplicated 3 nearby duplicate beacons into Sector 4 incident.',
          color: 'text-emerald-300',
        },
      ]);
    }, 1100);

    setTimeout(() => {
      setLogs(prev => [
        ...prev,
        {
          time: getTime(),
          text: '[POWER SUB-AGENT] Critical Check: Substation Feeder 12 within 180m flood boundary. Triggering SCADA Busbar Standby!',
          color: 'text-amber-400 font-bold',
        },
        {
          time: getTime(),
          text: '[SCADA ACTION] 0ms Hospital ICU transfer complete. Feeder 12 vacuum circuit breaker TRIPPED & ISOLATED.',
          color: 'text-emerald-400 font-bold',
        },
      ]);
    }, 1800);

    setTimeout(() => {
      setMutexStatus('ACQUIRED');
      setLogs(prev => [
        ...prev,
        {
          time: getTime(),
          text: '[REDLOCK MUTEX] Acquired distributed lock on NDRF Rescue Boat Unit #4 (TTL: 45s). Safe water channel vector transmitted to Mobile HUD.',
          color: 'text-teal-300 font-semibold',
        },
      ]);
      setIsSimRunning(false);
    }, 2500);
  };

  const resetSimulation = () => {
    setLogs([
      { time: '00:00:00', text: 'ZeroGrid Runtime initialized. Waiting for mesh beacon ingestion...', color: 'text-slate-500' },
      { time: '00:00:01', text: 'Strands Agents SDK 1.19 standby. Amazon Bedrock Claude 3.5 Sonnet connected.', color: 'text-slate-500' },
      { time: '00:00:02', text: 'ElastiCache Redis cluster online. 160 NDRF rescuer pool registered in Atlas.', color: 'text-slate-500' },
    ]);
    setMutexStatus('IDLE');
    setIsSimRunning(false);
  };

  const currentInspector = TIER_DATA[selectedTier] || TIER_DATA.t4;

  return (
    <div className="bg-[#f8fafc] text-[#0f172a] antialiased font-sans min-h-screen tactical-grid-bg relative overflow-x-hidden selection:bg-emerald-100 selection:text-emerald-900">
      {/* TOP NAVIGATION BAR */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200 transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Brand Anchor Logo */}
          <div className="flex items-center gap-3">
            <Link className="flex items-center gap-2.5" href="/">
              <div className="w-9 h-9 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800 shadow-sm">
                <span className="material-symbols-outlined text-[22px]">hub</span>
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-display font-bold text-lg text-emerald-950 tracking-tight">ZeroGrid</span>
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                    Online • 2 Nearby
                  </span>
                </div>
                <div className="text-[10px] font-mono text-slate-500 -mt-0.5">
                  BHARAT BUILDS // AWS DISASTER TRACK v2.4
                </div>
              </div>
            </Link>
          </div>

          {/* Navigation Links */}
          <nav className="hidden lg:flex items-center gap-6 text-sm font-medium text-slate-600">
            <a className="hover:text-emerald-800 transition-colors" href="#dual-path">Dual-Path RF Mesh</a>
            <a className="hover:text-emerald-800 transition-colors" href="#architecture">AWS Topology</a>
            <a className="hover:text-emerald-800 transition-colors" href="#multi-agent">Strands Agents SDK</a>
            <a className="hover:text-emerald-800 transition-colors" href="#interfaces">Live Interfaces</a>
            <a className="hover:text-emerald-800 transition-colors" href="#hardware-rf">Binary Spec</a>
            <a className="hover:text-emerald-800 transition-colors" href="#simulation">Simulation Sandbox</a>
          </nav>

          {/* Action Buttons */}
          <div className="flex items-center gap-2.5">
            <Link
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-slate-900 transition-all shadow-sm"
              href="/auth/login"
            >
              <span className="material-symbols-outlined text-sm">lock</span>
              <span>ADMIN SIGN IN</span>
            </Link>
            <Link
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-sm active:scale-95"
              href="#simulation"
            >
              <span className="material-symbols-outlined text-base">radar</span>
              <span>LAUNCH OPS CONSOLE</span>
            </Link>
          </div>
        </div>
      </header>

      {/* MAIN CANVAS */}
      <main className="pt-24 pb-20">
        {/* 1. HERO SECTION */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 pb-12">
          {/* Hackathon Track Pill / Banner */}
          <div className="flex flex-wrap items-center gap-2 mb-5">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">
              <span className="w-1.5 h-1.5 rounded-full bg-red-600 animate-pulse"></span>
              CRITICAL DEPLOYMENT STATE: ACTIVE
            </span>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold">
              BHARAT BUILDS HACKATHON // ENVIRONMENT: MONSOON FLOODS &amp; GRID BLACKOUT
            </span>
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-500 text-xs font-medium">
              ORG: WEMAKEDEVS &amp; AWS
            </span>
          </div>

          {/* Hero Header & Telemetry Card */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start mb-10">
            <div className="lg:col-span-8 space-y-5">
              <h1 className="text-3xl sm:text-5xl lg:text-[54px] font-display font-bold text-slate-900 tracking-tight leading-[1.12]">
                Saving Lives When <span className="text-emerald-800 underline decoration-emerald-200 decoration-wavy underline-offset-4">Mobile Towers Submerge</span> &amp; Power Grids Die
              </h1>
              <p className="text-base sm:text-lg text-slate-600 leading-relaxed max-w-3xl">
                ZeroGrid is an autonomous off-grid rescue coordination engine engineered for extreme Indian monsoons. Powered by Native Android BLE 5.0 / Wi-Fi Direct P2P Mesh with opportunistic cloud uplink into AWS ECS Fargate running <strong className="text-emerald-950 font-semibold">AgentZero (Strands Agents SDK &amp; Amazon Bedrock Claude 3.5 Sonnet)</strong> to prevent 33kV high-voltage electrocutions and direct rescue boats away from flooded death-traps.
              </p>

              {/* CTAs */}
              <div className="flex flex-wrap items-center gap-3.5 pt-2">
                <a
                  className="px-5 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-semibold text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2 transition-all shadow-md active:scale-95"
                  href="#simulation"
                >
                  <span className="material-symbols-outlined text-lg">crisis_alert</span>
                  <span>LAUNCH COMMAND SIMULATOR</span>
                </a>
                <a
                  className="px-5 py-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 hover:text-emerald-800 font-semibold text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2 transition-all shadow-sm"
                  href="#interfaces"
                >
                  <span className="material-symbols-outlined text-lg text-emerald-800">devices</span>
                  <span>VIEW REAL MESH APPS</span>
                </a>
                <a
                  className="px-4 py-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 text-xs sm:text-sm font-semibold flex items-center gap-1.5 transition-all"
                  href="#architecture"
                >
                  <span className="material-symbols-outlined text-sm">hub</span>
                  <span>AWS Architecture</span>
                </a>
              </div>
            </div>

            {/* Hero Telemetry Display */}
            <div className="lg:col-span-4 bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-800 text-lg">sensors</span>
                  <span className="text-xs font-mono font-bold uppercase text-slate-800">LIVE DISPATCH HUD</span>
                </div>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span> MESH ONLINE
                </span>
              </div>
              <div className="grid grid-cols-2 gap-2.5">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-[10px] font-mono font-semibold text-slate-500 block uppercase">ACTIVE P2P MESH</span>
                  <span className="text-xl font-bold font-display text-emerald-800">42 NODES</span>
                  <span className="text-[10px] text-slate-500 block">Hop Radius: 7.2 km</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-[10px] font-mono font-semibold text-slate-500 block uppercase">SUBSTATION 33kV</span>
                  <span className="text-xl font-bold font-display text-amber-700">ISOLATED</span>
                  <span className="text-[10px] text-emerald-600 font-medium block">ICU Busbar Safe</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-[10px] font-mono font-semibold text-slate-500 block uppercase">RESOLVED ALERTS</span>
                  <span className="text-xl font-bold font-display text-slate-900">1,248</span>
                  <span className="text-[10px] text-emerald-600 font-medium block">Dedup 99.4%</span>
                </div>
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
                  <span className="text-[10px] font-mono font-semibold text-slate-500 block uppercase">RESCUER LATENCY</span>
                  <span className="text-xl font-bold font-display text-emerald-800">&lt; 18ms</span>
                  <span className="text-[10px] text-slate-500 block">Redis Mutex</span>
                </div>
              </div>

              {/* Radar Mini Widget */}
              <div className="relative h-28 bg-slate-950 rounded-xl border border-slate-800 overflow-hidden flex items-center justify-center p-2">
                <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#10b981_1px,transparent_1px)] [background-size:12px_12px]"></div>
                <div className="absolute w-24 h-24 rounded-full border border-emerald-500/30"></div>
                <div className="absolute w-14 h-14 rounded-full border border-emerald-500/20"></div>
                <div className="absolute w-24 h-0.5 bg-gradient-to-r from-transparent via-emerald-500 to-transparent radar-sweep-anim"></div>
                <div className="relative z-10 text-center">
                  <span className="text-[10px] font-mono font-bold text-emerald-400 block tracking-widest">TACTICAL BEACON ACTIVE</span>
                  <span className="text-xs font-mono text-slate-300">Delhi/Kochi Sector 4 // Water +1.8m</span>
                </div>
              </div>
            </div>
          </div>

          {/* Live Dual-Path Interactive Switcher */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm" id="dual-path">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-200">
              <div>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-800">alt_route</span>
                  <h2 className="text-lg font-bold font-display text-slate-900">Dual-Path Emergency Routing Architecture</h2>
                </div>
                <p className="text-xs sm:text-sm text-slate-500 mt-1">
                  Simulating ZeroGrid packet propagation during complete cellular base-station collapse vs opportunistic AWS cloud restoration.
                </p>
              </div>
              <div className="inline-flex p-1 bg-slate-100 rounded-xl border border-slate-200 self-start md:self-auto">
                <button
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    selectedPath === 'A'
                      ? 'bg-emerald-800 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                  onClick={() => setSelectedPath('A')}
                >
                  PATH A: ZERO-INTERNET RF MESH
                </button>
                <button
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    selectedPath === 'B'
                      ? 'bg-emerald-800 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                  onClick={() => setSelectedPath('B')}
                >
                  PATH B: OPPORTUNISTIC AWS CLOUD
                </button>
              </div>
            </div>

            {/* Path Diagram Render Canvas */}
            <div className="pt-6">
              {selectedPath === 'A' ? (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center animate-fade-in">
                  <div className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-200 relative">
                    <span className="absolute -top-2 left-3 px-2 py-0.5 bg-emerald-800 text-white font-mono text-[9px] rounded font-bold uppercase">ORIGIN</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-emerald-800 text-lg">phone_android</span>
                      <span className="text-sm font-bold text-slate-900">Victim Client</span>
                    </div>
                    <p className="text-xs text-slate-600">Cell towers dead. Android broadcasts raw 32-byte binary BLE beacon with GPS float32 &amp; battery %.</p>
                    <div className="mt-3 text-[11px] font-mono text-emerald-900 bg-white p-1.5 rounded border border-emerald-200 font-semibold">0x5A [SOS_PACKET] TTL: 7</div>
                  </div>
                  <div className="hidden md:flex flex-col items-center justify-center text-emerald-700">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">BLE 5.0 Hop 1</span>
                    <span className="material-symbols-outlined text-2xl animate-pulse">arrow_forward</span>
                    <span className="text-[10px] font-mono font-medium text-emerald-700">~120m Flood Zone</span>
                  </div>
                  <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                    <span className="text-[10px] font-mono text-slate-500 block uppercase font-bold">Mesh Relay Node</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-emerald-600 text-lg">router</span>
                      <span className="text-sm font-bold text-slate-900">Neighbor Android / ESP32</span>
                    </div>
                    <p className="text-xs text-slate-600">500-slot LRU deduplication prevents RF echo storms. Decrements TTL to 6 and forwards.</p>
                    <div className="mt-3 text-[11px] font-mono text-emerald-700 bg-white p-1.5 rounded border border-slate-200">LRU Match: FALSE -&gt; FORWARD</div>
                  </div>
                  <div className="bg-amber-50/60 p-4 rounded-xl border border-amber-200">
                    <span className="text-[10px] font-mono text-amber-800 block uppercase font-bold">FIELD DESTINATION</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-amber-700 text-lg">sailing</span>
                      <span className="text-sm font-bold text-slate-900">NDRF Boat Tablet</span>
                    </div>
                    <p className="text-xs text-slate-600">Offline SQLite spatial query plots bearing &amp; elevation vector bypassing submerged 33kV line.</p>
                    <div className="mt-3 text-[11px] font-mono text-amber-900 bg-white p-1.5 rounded border border-amber-200 font-semibold">ETA 4m 12s // SAFE CHANNEL ACTIVE</div>
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center animate-fade-in">
                  <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                    <span className="text-[10px] font-mono text-slate-500 block uppercase font-bold">Perimeter Gateway</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-emerald-800 text-lg">cell_tower</span>
                      <span className="text-sm font-bold text-slate-900">Relay Device Uplink</span>
                    </div>
                    <p className="text-xs text-slate-600">Device detects sporadic 4G/Satellite edge uplink. Wraps mesh packet into signed JSON.</p>
                    <div className="mt-3 text-[11px] font-mono text-slate-700 bg-white p-1.5 rounded border border-slate-200">POST /v1/ingest/sos (HTTPS)</div>
                  </div>
                  <div className="hidden md:flex flex-col items-center justify-center text-emerald-800">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">AWS APIGW REST/WSS</span>
                    <span className="material-symbols-outlined text-2xl animate-pulse">cloud_sync</span>
                    <span className="text-[10px] font-mono font-medium text-emerald-800">&lt; 24ms Ingestion</span>
                  </div>
                  <div className="bg-emerald-50/60 p-4 rounded-xl border border-emerald-200">
                    <span className="text-[10px] font-mono text-emerald-800 block uppercase font-bold">AWS ECS Fargate</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-emerald-800 text-lg">psychology</span>
                      <span className="text-sm font-bold text-slate-900">AgentZero (Bedrock 3.5)</span>
                    </div>
                    <p className="text-xs text-slate-600">Strands Agents SDK runs multi-modal triage. Coordinates flood model with SCADA 33kV switchboard.</p>
                    <div className="mt-3 text-[11px] font-mono text-emerald-900 bg-white p-1.5 rounded border border-emerald-200 font-semibold">Bedrock Sonnet Confidence: 94.2%</div>
                  </div>
                  <div className="bg-emerald-50/60 p-4 rounded-xl border border-emerald-200">
                    <span className="text-[10px] font-mono text-emerald-800 block uppercase font-bold">Multi-Model Sync</span>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-emerald-700 text-lg">database</span>
                      <span className="text-sm font-bold text-slate-900">Redis Mutex + Atlas Geo</span>
                    </div>
                    <p className="text-xs text-slate-600">Redlock claims atomic assignment to nearest NDRF unit. Breaker isolation broadcast to mesh.</p>
                    <div className="mt-3 text-[11px] font-mono text-emerald-900 bg-white p-1.5 rounded border border-emerald-200 font-semibold">Redlock ACQUIRED // TTL 45s</div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </section>

        {/* 2. REAL APP INTERFACE SPOTLIGHT */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="interfaces">
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-emerald-800 text-base">dashboard_customize</span>
              <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">REAL PRODUCTION INTERFACES</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
              Battle-Tested UI: Native Android Mesh &amp; Web Ops Console
            </h2>
            <p className="text-sm text-slate-600 max-w-2xl mt-1">
              Inspect the identical interfaces utilized by NDRF field operators and community volunteers during blackout emergencies.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Mobile App UI Showcase */}
            <div className="lg:col-span-4 flex flex-col items-center">
              <div className="w-full max-w-[360px] bg-white rounded-[32px] border-4 border-slate-200 shadow-xl overflow-hidden flex flex-col min-h-[640px]">
                {/* Mobile Status Bar */}
                <div className="px-5 pt-3 pb-2 flex items-center justify-between text-xs text-slate-600 font-mono">
                  <span className="font-bold text-slate-800">12:12</span>
                  <div className="flex items-center gap-1.5 text-[11px]">
                    <span>0.28 KB/s</span>
                    <span className="material-symbols-outlined text-xs">bluetooth</span>
                    <span className="material-symbols-outlined text-xs">wifi</span>
                    <span className="material-symbols-outlined text-xs">battery_5_bar</span>
                    <span>53%</span>
                  </div>
                </div>

                {/* Mobile App Header */}
                <div className="px-5 py-2.5 flex items-center justify-between border-b border-slate-200">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800">
                      <span className="material-symbols-outlined text-sm">hub</span>
                    </div>
                    <span className="font-display font-bold text-base text-slate-900">ZeroGrid</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Online • 2 Nearby
                    </span>
                    <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center text-slate-600">
                      <span className="material-symbols-outlined text-sm">person</span>
                    </div>
                  </div>
                </div>

                {/* Mobile App Body */}
                <div className="p-4 flex-1 space-y-3.5 bg-slate-50/50">
                  {/* Mesh Status Banner */}
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-bold text-sm text-slate-900">Mesh Active &amp; Connected</h4>
                      <p className="text-[11px] text-slate-500">Off-grid direct communication</p>
                    </div>
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-emerald-50 text-emerald-700 font-mono text-[11px] font-bold border border-emerald-200">
                      <span className="material-symbols-outlined text-xs">battery_charging_full</span> 54%
                    </span>
                  </div>

                  {/* Metric Pills */}
                  <div className="grid grid-cols-3 gap-2">
                    <div className="bg-white p-2.5 rounded-xl border border-slate-200 text-left shadow-2xs">
                      <span className="text-[10px] text-slate-400 block">Peers</span>
                      <span className="text-xs font-bold text-slate-900">2 Active</span>
                    </div>
                    <div className="bg-white p-2.5 rounded-xl border border-slate-200 text-left shadow-2xs">
                      <span className="text-[10px] text-slate-400 block">Reach</span>
                      <span className="text-xs font-bold text-slate-900">Direct Only</span>
                    </div>
                    <div className="bg-white p-2.5 rounded-xl border border-slate-200 text-left shadow-2xs">
                      <span className="text-[10px] text-slate-400 block">Mode</span>
                      <span className="text-xs font-bold text-emerald-800">BLE</span>
                    </div>
                  </div>

                  {/* Radio Channel Box */}
                  <div className="bg-white p-3 rounded-xl border border-slate-200 flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-700">Radio Channel: BLE</span>
                    <button className="px-2.5 py-1 rounded-lg border border-slate-200 text-[11px] font-semibold text-slate-700 hover:bg-slate-50">
                      Switch to Wi-Fi
                    </button>
                  </div>

                  {/* Primary Action: Scan for Devices */}
                  <button className="w-full py-2.5 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-semibold text-xs flex items-center justify-center gap-1.5 shadow-sm">
                    <span className="material-symbols-outlined text-sm">search</span>
                    <span>Scan for Devices</span>
                  </button>

                  {/* Nearby People & Devices Section */}
                  <div className="pt-1">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-slate-500">Nearby People &amp; Devices</span>
                      <span className="text-[10px] font-bold text-emerald-800 cursor-pointer">View All (2)</span>
                    </div>
                    <div className="space-y-2">
                      <div className="bg-white p-2.5 rounded-xl border border-slate-200 flex items-center justify-between shadow-2xs">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-800 font-bold text-xs flex items-center justify-center">
                            IN
                          </div>
                          <div>
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-bold text-slate-900">Infinix X6871</span>
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            </div>
                            <span className="text-[10px] text-slate-500">Direct • Strong Signal</span>
                          </div>
                        </div>
                        <span className="material-symbols-outlined text-slate-400 text-base">chat_bubble_outline</span>
                      </div>

                      <div className="bg-white p-2.5 rounded-xl border border-slate-200 flex items-center justify-between shadow-2xs">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-800 font-bold text-xs flex items-center justify-center">
                            MO
                          </div>
                          <div>
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-bold text-slate-900">Mohit&apos;s A34</span>
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            </div>
                            <span className="text-[10px] text-slate-500">Direct • Strong Signal</span>
                          </div>
                        </div>
                        <span className="material-symbols-outlined text-slate-400 text-base">chat_bubble_outline</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Mobile Bottom Navigation Bar */}
                <div className="bg-white border-t border-slate-200 px-4 py-2 flex items-center justify-around text-slate-400">
                  <div className="flex flex-col items-center text-emerald-800 font-semibold">
                    <span className="material-symbols-outlined text-lg">hub</span>
                    <span className="text-[9px]">Mesh</span>
                  </div>
                  <div className="flex flex-col items-center relative">
                    <span className="material-symbols-outlined text-lg">chat</span>
                    <span className="absolute -top-1 right-1 px-1 bg-emerald-800 text-white rounded-full text-[8px] font-bold">6</span>
                    <span className="text-[9px]">Messages</span>
                  </div>
                  <div className="flex flex-col items-center">
                    <span className="material-symbols-outlined text-lg">folder</span>
                    <span className="text-[9px]">Files</span>
                  </div>
                  <div className="flex flex-col items-center text-red-600 font-bold">
                    <span className="material-symbols-outlined text-lg">warning</span>
                    <span className="text-[9px]">SOS</span>
                  </div>
                  <div className="flex flex-col items-center">
                    <span className="material-symbols-outlined text-lg">settings</span>
                    <span className="text-[9px]">Settings</span>
                  </div>
                </div>
              </div>
              <span className="text-xs text-slate-500 font-mono mt-3">Native Kotlin BLE Mesh Mobile Client</span>
            </div>

            {/* Web Operations Console Showcase */}
            <div className="lg:col-span-8 bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
              {/* Web Console Header */}
              <div className="px-5 py-3 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3 bg-white">
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1.5 text-xs font-mono font-bold text-slate-800">
                    <span className="material-symbols-outlined text-emerald-800 text-base">map</span>
                    <span>GEO-SPATIAL TELEMETRY CANVAS</span>
                  </div>
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                    1 ACTIVE HQS // 7 ACTIVE SOS
                  </span>
                </div>
                <div className="flex items-center gap-3 text-xs font-mono text-slate-500">
                  <span className="hidden sm:inline">CPU: 0.7%</span>
                  <span className="hidden sm:inline">RAM: 97.8 MB</span>
                  <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-bold">HQS: 1</span>
                  <span className="px-2 py-0.5 rounded bg-red-50 text-red-600 font-bold">Active SOS: 7</span>
                </div>
              </div>

              {/* Simulated Telemetry Map Canvas */}
              <div className="relative h-64 bg-slate-100 overflow-hidden flex items-center justify-center border-b border-slate-200">
                <div className="absolute inset-0 bg-[#e5e9ec] opacity-90"></div>
                <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(#64748b 1px, transparent 1px)', backgroundSize: '20px 20px' }}></div>
                {/* Simulated Flood Danger Polygon */}
                <svg className="absolute inset-0 w-full h-full pointer-events-none">
                  <polygon fill="rgba(245, 158, 11, 0.18)" points="260,80 430,70 510,140 450,220 280,210" stroke="#d97706" strokeDasharray="4,4" strokeWidth="2"></polygon>
                  <circle cx="340" cy="130" fill="rgba(239, 68, 68, 0.3)" r="14" stroke="#ef4444" strokeWidth="2"></circle>
                  <circle cx="430" cy="110" fill="#10b981" r="8"></circle>
                  <circle cx="390" cy="170" fill="#0284c7" r="8"></circle>
                </svg>
                {/* Map Center Pin Indicator */}
                <div className="relative z-10 bg-white/95 px-3 py-1.5 rounded-lg border border-slate-200 shadow-sm text-center">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-slate-800">
                    <span className="w-2 h-2 rounded-full bg-red-600 animate-ping"></span>
                    <span>NCR Delhi Sector // Yamuna Flood Contour</span>
                  </div>
                  <span className="text-[10px] text-slate-500 font-mono">28.6108° N, 77.1019° E • Water Level: +1.8m</span>
                </div>
                <div className="absolute bottom-2 left-3 text-[10px] font-mono text-slate-500 bg-white/90 px-2 py-0.5 rounded border border-slate-200">
                  HQ markers rendered live • Click any marker to view location telemetry
                </div>
              </div>

              {/* Web Console Filter Bar */}
              <div className="p-3 border-b border-slate-200 flex flex-wrap items-center justify-between gap-2 bg-slate-50/70 text-xs">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-slate-800 font-mono flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-red-600"></span> SOS Console
                  </span>
                  <span className="px-2 py-0.5 rounded bg-white border border-slate-200 text-slate-600 font-mono text-[11px]">
                    GET /api/admin/sos (17 Events)
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="px-2.5 py-1 rounded bg-white border border-slate-200 font-semibold text-slate-700">Active Feed</span>
                  <span className="px-2.5 py-1 rounded text-slate-500 hover:text-slate-800">History</span>
                  <Link href="/admin" className="px-2.5 py-1 rounded bg-emerald-50 text-emerald-800 font-bold border border-emerald-200 flex items-center gap-1">
                    <span className="material-symbols-outlined text-xs">navigation</span> Open Console
                  </Link>
                </div>
              </div>

              {/* Active SOS Cards Feed */}
              <div className="p-4 grid grid-cols-1 md:grid-cols-3 gap-3 bg-white">
                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 hover:border-emerald-200 transition-all flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="px-1.5 py-0.2 rounded text-[10px] font-mono font-bold bg-red-50 text-red-600 border border-red-200">
                        ACTIVE #sos-3923
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">05:41 PM</span>
                    </div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-slate-400 text-sm">person</span>
                      <span className="text-xs font-bold text-slate-900">Pokemon</span>
                      <span className="text-[9px] font-mono text-slate-400 bg-white px-1 rounded">REGULAR NODE</span>
                    </div>
                    <div className="text-[10px] font-mono text-slate-500">
                      Coordinates: 28.6108, 77.1019
                    </div>
                  </div>
                  <div className="pt-2 mt-2 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono">
                    <span className="text-slate-600 font-bold">🔋 85% • 2 Peered</span>
                    <Link href="/admin" className="text-emerald-800 font-semibold cursor-pointer hover:underline">Details &gt;</Link>
                  </div>
                </div>

                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 hover:border-emerald-200 transition-all flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="px-1.5 py-0.2 rounded text-[10px] font-mono font-bold bg-red-50 text-red-600 border border-red-200">
                        ACTIVE #sos-3922
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">05:41 PM</span>
                    </div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-slate-400 text-sm">person</span>
                      <span className="text-xs font-bold text-slate-900">aaryan</span>
                      <span className="text-[9px] font-mono text-slate-400 bg-white px-1 rounded">REGULAR NODE</span>
                    </div>
                    <div className="text-[10px] font-mono text-slate-500">
                      Coordinates: 28.6108, 77.1019
                    </div>
                  </div>
                  <div className="pt-2 mt-2 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono">
                    <span className="text-slate-600 font-bold">🔋 53% • 2 Peered</span>
                    <Link href="/admin" className="text-emerald-800 font-semibold cursor-pointer hover:underline">Details &gt;</Link>
                  </div>
                </div>

                <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 hover:border-emerald-200 transition-all flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="px-1.5 py-0.2 rounded text-[10px] font-mono font-bold bg-red-50 text-red-600 border border-red-200">
                        ACTIVE #sos-3921
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">05:37 PM</span>
                    </div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="material-symbols-outlined text-slate-400 text-sm">person</span>
                      <span className="text-xs font-bold text-slate-900">Pokemon</span>
                      <span className="text-[9px] font-mono text-slate-400 bg-white px-1 rounded">REGULAR NODE</span>
                    </div>
                    <div className="text-[10px] font-mono text-slate-500">
                      Coordinates: 28.6108, 77.1019
                    </div>
                  </div>
                  <div className="pt-2 mt-2 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono">
                    <span className="text-slate-600 font-bold">🔋 86% • 2 Peered</span>
                    <Link href="/admin" className="text-emerald-800 font-semibold cursor-pointer hover:underline">Details &gt;</Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 3. PROBLEM STATEMENT MATRIX */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200">
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-red-600 text-base">report_problem</span>
              <span className="text-xs font-mono font-bold text-red-600 uppercase tracking-wider">HACKATHON PROBLEM STATEMENT MATRIX</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
              The Anatomy of Monsoon Cascade Failures
            </h2>
            <p className="text-sm text-slate-600 max-w-2xl mt-1">
              During extreme monsoons in Wayanad, Mumbai, and Chennai, emergency responders confront a four-stage compounding breakdown that paralyzes traditional dispatch systems.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between hover:border-red-300 transition-all shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[11px] font-mono font-bold text-red-600 uppercase">01 // TELECOM BLACKOUT</span>
                  <span className="material-symbols-outlined text-red-600">signal_cellular_connected_no_internet_0_bar</span>
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1.5">Cellular Submersion Horizon</h3>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  Diesel generators in tower basements flood within 45 minutes of heavy inundation. Fiber backhaul lines snap under shifting soil, severing 98% of citizen 112 calls.
                </p>
              </div>
              <div className="pt-3 border-t border-slate-200">
                <span className="text-[10px] font-mono font-bold text-emerald-800 block mb-0.5">ZEROGRID RESOLUTION:</span>
                <p className="text-xs text-slate-800">Autonomous Zero-Infrastructure Bluetooth LE 5.0 mesh hops across survivor phones without towers.</p>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between hover:border-amber-300 transition-all shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[11px] font-mono font-bold text-amber-600 uppercase">02 // GRID HAZARDS</span>
                  <span className="material-symbols-outlined text-amber-600">electric_bolt</span>
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1.5">33kV Substation Electrocution</h3>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  Uncut 11kV/33kV distribution lines contact rising floodwaters, electrifying wading survivors and rescue inflatable dinghies while hospital ICUs blackout.
                </p>
              </div>
              <div className="pt-3 border-t border-slate-200">
                <span className="text-[10px] font-mono font-bold text-emerald-800 block mb-0.5">ZEROGRID RESOLUTION:</span>
                <p className="text-xs text-slate-800">Dynamic SCADA busbar transfer to standby prior to automatic isolation, guaranteeing hospital ICU power.</p>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between hover:border-emerald-300 transition-all shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[11px] font-mono font-bold text-emerald-800 uppercase">03 // DISPATCH COLLAPSE</span>
                  <span className="material-symbols-outlined text-emerald-800">call_missed</span>
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1.5">112 Panic Loop Saturation</h3>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  Hundreds of panicked neighbors call 112 for the same high-rise incident, creating massive backlogs that delay infant and oxygen-dependent triage.
                </p>
              </div>
              <div className="pt-3 border-t border-slate-200">
                <span className="text-[10px] font-mono font-bold text-emerald-800 block mb-0.5">ZEROGRID RESOLUTION:</span>
                <p className="text-xs text-slate-800">AgentZero 250m spatial deduplication clusters identical reports into one authoritative actionable incident.</p>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between hover:border-blue-300 transition-all shadow-sm">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[11px] font-mono font-bold text-blue-600 uppercase">04 // RESCUE TRAPS</span>
                  <span className="material-symbols-outlined text-blue-600">flood</span>
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1.5">Impassable Underpass Traps</h3>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  Google Maps/Apple Maps route rescue vehicles through railway underpasses submerged under 12 feet of water, swamping boat outboard engines.
                </p>
              </div>
              <div className="pt-3 border-t border-slate-200">
                <span className="text-[10px] font-mono font-bold text-emerald-800 block mb-0.5">ZEROGRID RESOLUTION:</span>
                <p className="text-xs text-slate-800">Flood Route Copilot uses real-time IoT water depth probes &amp; elevation DEM models to reroute rescue boats.</p>
              </div>
            </div>
          </div>
        </section>

        {/* 4. 6-TIER AWS CLOUD-TO-EDGE ARCHITECTURE BLUEPRINT */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="architecture">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="material-symbols-outlined text-emerald-800 text-base">cloud</span>
                <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">ENTERPRISE TOPOLOGY // PRODUCTION READY</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
                Cloud-to-Edge Resilience Blueprint
              </h2>
              <p className="text-sm text-slate-600 max-w-2xl mt-1">
                A 6-Tier architecture spanning client mesh nodes, edge lambdas, containerized Bedrock agent orchestrators, and geo-distributed databases.
              </p>
            </div>
            <div className="text-xs font-mono font-bold text-emerald-800 bg-emerald-50 px-3 py-1.5 rounded-lg border border-emerald-200">
              AWS WELL-ARCHITECTED // DISASTER RELIABILITY PILLAR
            </div>
          </div>

          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
              {[
                { id: 't1', title: 'TIER 1', name: 'Frontend Ingress', icon: 'devices', items: ['Amplify Web Next.js 16', 'Kotlin Jetpack Compose', 'NDRF Commander Tablet'], metric: 'Latency: P2P < 8ms' },
                { id: 't2', title: 'TIER 2', name: 'Security & API', icon: 'shield', items: ['API Gateway REST / WSS', 'AWS Cognito JWT Auth', 'AWS Secrets Manager'], metric: 'Throughput: 10k req/s' },
                { id: 't3', title: 'TIER 3', name: 'Serverless Compute', icon: 'bolt', items: ['Lambda Voice Mangum', 'Lambda GenAI Ingest', 'SQS Backpressure Buffers'], metric: 'Cold start: < 45ms' },
                { id: 't4', title: 'TIER 4 CORE', name: 'ECS Fargate Core', icon: 'smart_toy', items: ['Strands Agents SDK 1.19', 'AgentZero Orchestrator', 'MCP Tools Protocol Bridge'], metric: 'Status: Auto-Scale Active', isCore: true },
                { id: 't5', title: 'TIER 5 BRAIN', name: 'Amazon Bedrock', icon: 'psychology', items: ['Claude 3.5 Sonnet', 'Claude 3 Haiku (Voice)', 'Bedrock Knowledge Bases'], metric: 'Confidence Floor: >= 65%' },
                { id: 't6', title: 'TIER 6', name: 'Data & Mutex', icon: 'storage', items: ['MongoDB Atlas 2dsphere', 'DynamoDB 33kV SCADA', 'ElastiCache Redlock Mutex'], metric: 'No Race Conditions' },
              ].map(t => (
                <div
                  key={t.id}
                  onClick={() => setSelectedTier(t.id)}
                  className={`cursor-pointer rounded-xl p-4 transition-all ${
                    selectedTier === t.id
                      ? 'bg-emerald-50/90 border-2 border-emerald-800 shadow-sm'
                      : t.isCore
                      ? 'bg-emerald-50/40 border border-emerald-300 hover:border-emerald-600'
                      : 'bg-slate-50 hover:bg-white border border-slate-200 hover:border-emerald-500'
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[10px] font-mono font-bold text-slate-500 uppercase">{t.title}</span>
                    <span className="material-symbols-outlined text-emerald-800 text-sm">{t.icon}</span>
                  </div>
                  <h4 className="text-xs font-bold text-slate-900 mb-1">{t.name}</h4>
                  <ul className="text-[11px] text-slate-600 space-y-1">
                    {t.items.map((it, idx) => (
                      <li key={idx}>• {it}</li>
                    ))}
                  </ul>
                  <div className="mt-3 pt-2 border-t border-slate-200 text-[10px] font-mono text-emerald-700 font-bold">
                    {t.metric}
                  </div>
                </div>
              ))}
            </div>

            {/* Dynamic Node Telemetry Inspector Subpanel */}
            <div className="mt-6 p-4 rounded-xl bg-slate-950 text-white border border-slate-800">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-800">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-500 text-sm">developer_board</span>
                  <span className="text-xs font-mono font-bold text-emerald-400">
                    {currentInspector.title}
                  </span>
                </div>
                <span className="text-[10px] font-mono text-slate-400">CLICK ANY TIER ABOVE TO INSPECT RUNTIME TELEMETRY</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-3 text-xs">
                <div>
                  <span className="text-slate-400 block text-[11px]">Primary Runtime:</span>
                  <span className="font-mono text-white text-xs">{currentInspector.runtime}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Data Protocol:</span>
                  <span className="font-mono text-white text-xs">{currentInspector.protocol}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Failover Mode:</span>
                  <span className="font-mono text-emerald-400 text-xs">{currentInspector.failover}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 5. 4-PHASE MULTI-AGENT PIPELINE */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="multi-agent">
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-emerald-800 text-base">schema</span>
              <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">AUTONOMOUS MULTI-AGENT ENGINE</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
              4-Phase Decision Pipeline: Strands Agents SDK in Action
            </h2>
            <p className="text-sm text-slate-600 max-w-2xl mt-1">
              AgentZero eliminates emergency room chaos by distributing decision load across deterministic gatekeepers and LLM-specialized sub-agents.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 01</span>
                <span className="material-symbols-outlined text-emerald-800">calculate</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">Confidence Calculator Agent</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Multimodal sensor verification preventing false alarms from drowned microphones or camera lens debris.
              </p>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <span className="text-[10px] font-mono text-emerald-800 block font-bold">WEIGHTED FORMULA:</span>
                <div className="text-[11px] font-mono text-slate-700">
                  0.25 × Audio + 0.35 × Visual + 0.25 × IoT + 0.15 × Surge &gt;= 65%
                </div>
              </div>
              <div className="text-xs text-emerald-700 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span> 0 false alarms recorded
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 02</span>
                <span className="material-symbols-outlined text-emerald-800">filter_alt</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">AgentZero Gatekeeper</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Spatial 250-meter deduplication. Evaluates threat urgency index from 0 to 100 based on water rise velocity.
              </p>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <span className="text-[10px] font-mono text-emerald-800 block font-bold">DEDUPLICATION RADAR:</span>
                <div className="text-[11px] font-mono text-slate-700">
                  $geoWithin: &#123; $centerSphere: [ [lng, lat], 250m ] &#125;
                </div>
              </div>
              <div className="text-xs text-emerald-800 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span> 1,200 req/sec clustering
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 03</span>
                <span className="material-symbols-outlined text-emerald-800">account_tree</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">Sub-Agent Specialization</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Parallel domain execution via Strands SDK tools: Flood, SCADA Power Breaker, and Boat Navigation agents.
              </p>
              <div className="space-y-1.5">
                <div className="text-xs bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-600 font-medium">Flood Sub-Agent:</span>
                  <span className="text-emerald-800 font-mono text-[11px] font-bold">Dynamic Elevation</span>
                </div>
                <div className="text-xs bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-600 font-medium">Grid Sub-Agent:</span>
                  <span className="text-amber-700 font-mono text-[11px] font-bold">33kV SCADA Trip</span>
                </div>
              </div>
              <div className="text-xs text-emerald-700 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span> Bedrock Claude 3.5 Sonnet
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 04</span>
                <span className="material-symbols-outlined text-emerald-800">lock_person</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">Workforce Allocation Loop</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Queries 160-Admin NDRF roster. Acquires atomic Redis Redlock mutex preventing double-dispatch to same sector.
              </p>
              <div className="bg-red-50 p-3 rounded-xl border border-red-200 space-y-1">
                <span className="text-[10px] font-mono text-red-700 block font-bold">HOSPITAL ICU SAFEGUARD:</span>
                <div className="text-[11px] font-mono text-red-900">
                  0ms busbar transfer before flood zone breaker cutoff.
                </div>
              </div>
              <div className="text-xs text-emerald-800 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span> Redlock TTL: 45s fallback
              </div>
            </div>
          </div>
        </section>

        {/* 6. HARDWARE SPEC: 32-BIT WORD-ALIGNED PACKET */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="hardware-rf">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            <div className="lg:col-span-6 space-y-4">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-emerald-800">memory</span>
                <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">HARDWARE &amp; PROTOCOL LAYER</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
                32-Bit Word-Aligned Binary Packet Specification
              </h2>
              <p className="text-sm text-slate-600 leading-relaxed">
                ZeroGrid avoids bloated JSON envelopes on congested radio spectrum. Over-the-air packets are packed into an ultra-dense 32-byte binary payload transmitted via raw BLE 5.0 Manufacturer Specific Data or Wi-Fi Direct frame headers.
              </p>

              {/* Binary Layout Interactive Visualizer */}
              <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm space-y-3">
                <span className="text-xs font-mono font-bold text-slate-500 block uppercase">RAW PACKET BUFFER // 32 BYTES (0x00 - 0x1F)</span>
                <div className="grid grid-cols-4 sm:grid-cols-8 gap-1.5 font-mono text-center text-xs">
                  <div className="bg-emerald-50 border border-emerald-300 p-2 rounded-lg text-emerald-900 font-bold">
                    <span className="block text-[9px] text-slate-500">B0</span>
                    0x5A
                    <span className="block text-[8px] truncate">MAGIC</span>
                  </div>
                  <div className="bg-emerald-50 border border-emerald-300 p-2 rounded-lg text-emerald-900 font-bold">
                    <span className="block text-[9px] text-slate-500">B1</span>
                    0x01
                    <span className="block text-[8px] truncate">TYPE</span>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-700">
                    <span className="block text-[9px] text-slate-500">B2</span>
                    0x07
                    <span className="block text-[8px] truncate">TTL</span>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-700">
                    <span className="block text-[9px] text-slate-500">B3</span>
                    0x0E
                    <span className="block text-[8px] truncate">BATT%</span>
                  </div>
                  <div className="col-span-2 bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800">
                    <span className="block text-[9px] text-slate-500">B4-B7</span>
                    LAT f32
                    <span className="block text-[8px] truncate">28.6108° N</span>
                  </div>
                  <div className="col-span-2 bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800">
                    <span className="block text-[9px] text-slate-500">B8-B11</span>
                    LNG f32
                    <span className="block text-[8px] truncate">77.1019° E</span>
                  </div>
                  <div className="col-span-4 bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800 text-left px-2">
                    <span className="block text-[9px] text-slate-500">B12-B27 // 16 BYTES</span>
                    OCCUPANT PAYLOAD + SENSOR VECTOR
                  </div>
                  <div className="col-span-4 bg-red-50 border border-red-200 p-2 rounded-lg text-red-700 font-bold">
                    <span className="block text-[9px] text-red-500">B28-B31</span>
                    CRC-32 // 0x48A2C10F
                  </div>
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-slate-200 text-xs font-mono">
                  <span className="text-slate-500">Max Range: ~120m/hop BLE // ~400m Wi-Fi Direct</span>
                  <span className="text-emerald-800 font-semibold">Deduplication: 500-slot LRU</span>
                </div>
              </div>
            </div>

            {/* Android Tactical Compass Mockup */}
            <div className="lg:col-span-6 bg-white border border-slate-200 rounded-2xl p-5 shadow-sm">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200 mb-4">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-800">compass_calibration</span>
                  <span className="text-xs font-mono font-bold text-slate-800">TACTICAL FIELD HUD // ANDROID CLIENT</span>
                </div>
                <span className="text-[11px] font-mono font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded">
                  GPS LOCK // SAT: 11
                </span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Compass Radar */}
                <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 flex flex-col items-center justify-center relative min-h-[200px]">
                  <div className="w-32 h-32 rounded-full border border-emerald-200 relative flex items-center justify-center bg-white shadow-2xs">
                    <div className="absolute inset-0 border border-dashed border-emerald-300 rounded-full animate-spin [animation-duration:30s]"></div>
                    <div className="w-20 h-20 rounded-full border border-emerald-100 flex items-center justify-center">
                      <div className="w-2.5 h-2.5 rounded-full bg-emerald-800 animate-ping"></div>
                    </div>
                    <span className="absolute top-1 text-[9px] font-mono font-bold text-emerald-800">N 034°</span>
                    <span className="absolute bottom-1 text-[9px] font-mono text-slate-400">S</span>
                    <span className="absolute left-1 text-[9px] font-mono text-slate-400">W</span>
                    <span className="absolute right-1 text-[9px] font-mono text-slate-400">E</span>
                  </div>
                  <div className="mt-3 text-center">
                    <span className="text-[10px] font-mono text-slate-500 block">BEARING TO HIGHEST GROUND</span>
                    <span className="text-xs font-bold text-emerald-950 font-mono">034° NNE // +14.2m ELEVATION</span>
                  </div>
                </div>

                {/* Avoidance Polygons */}
                <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 flex flex-col justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-red-600 block uppercase font-bold mb-2">AVOIDANCE POLYGONS</span>
                    <div className="space-y-2 mb-3">
                      <div className="p-2.5 bg-white rounded-lg border border-red-200">
                        <div className="flex items-center justify-between text-xs font-bold text-red-600">
                          <span>Railway Underpass C4</span>
                          <span>-4.2m DEPTH</span>
                        </div>
                        <span className="text-[10px] text-slate-500 block mt-0.5">Status: Trapped Outboard Hazard</span>
                      </div>
                      <div className="p-2.5 bg-white rounded-lg border border-emerald-200">
                        <div className="flex items-center justify-between text-xs font-bold text-emerald-700">
                          <span>Substation Feeder 12</span>
                          <span>33kV CUTOFF</span>
                        </div>
                        <span className="text-[10px] text-slate-500 block mt-0.5">Status: Auto-Isolated (Safe Passage)</span>
                      </div>
                    </div>
                  </div>
                  <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-xs font-mono">
                    <span className="text-slate-500">Mesh Battery Saver:</span>
                    <span className="text-emerald-700 font-bold">94h Standby</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 7. LIVE INTERACTIVE SIMULATION SANDBOX */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="simulation">
          <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="material-symbols-outlined text-emerald-800 text-base">terminal</span>
                <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">HACKATHON EVALUATOR CONSOLE</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
                Live Tactical Multi-Agent Simulator
              </h2>
              <p className="text-sm text-slate-600 max-w-2xl mt-1">
                Trigger simulated emergency SOS distress packets into the AWS Bedrock Strands orchestrator. Observe real-time deduplication, automated SCADA 33kV breaker isolation, and Redlock distributed mutex locks.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 text-xs font-mono font-semibold shadow-sm"
                onClick={resetSimulation}
              >
                RESET LOGS
              </button>
            </div>
          </div>

          {/* Simulator Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
            {/* Simulation Control Deck */}
            <div className="lg:col-span-5 space-y-4">
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3">
                <span className="text-xs font-mono font-bold text-slate-800 block">1. SELECT INCIDENT SCENARIO</span>
                <div className="space-y-2">
                  <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-white cursor-pointer hover:border-emerald-500 transition-all">
                    <input
                      checked={simScenario === 'underpass'}
                      onChange={() => setSimScenario('underpass')}
                      className="mt-1 text-emerald-800 focus:ring-emerald-800"
                      name="sim-scenario"
                      type="radio"
                      value="underpass"
                    />
                    <div>
                      <span className="text-xs font-bold text-slate-900 block">Submerged Underpass Trapping Ambulance</span>
                      <span className="text-[11px] text-slate-500 block">Kochi/Delhi North // 4 Patients // Water Rise: 22cm/hr</span>
                    </div>
                  </label>
                  <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-white cursor-pointer hover:border-emerald-500 transition-all">
                    <input
                      checked={simScenario === 'substation'}
                      onChange={() => setSimScenario('substation')}
                      className="mt-1 text-emerald-800 focus:ring-emerald-800"
                      name="sim-scenario"
                      type="radio"
                      value="substation"
                    />
                    <div>
                      <span className="text-xs font-bold text-slate-900 block">33kV Substation Flooded Near ICU Shelter</span>
                      <span className="text-[11px] text-slate-500 block">Vytilla Junction // High Voltage Electrocution Threat</span>
                    </div>
                  </label>
                </div>

                <div className="grid grid-cols-2 gap-2 pt-1 font-mono text-xs">
                  <div>
                    <span className="text-[10px] text-slate-500 block mb-0.5">TRANSMITTER BATTERY</span>
                    <div className="p-2 bg-white rounded border border-slate-200 text-red-600 font-bold">
                      14% (CRITICAL)
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block mb-0.5">PACKET HOP COUNT</span>
                    <div className="p-2 bg-white rounded border border-slate-200 text-emerald-800 font-bold">
                      TTL: 0x05 (3 Hops)
                    </div>
                  </div>
                </div>

                {/* Trigger Button */}
                <button
                  disabled={isSimRunning}
                  onClick={executeSimulation}
                  className={`w-full py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-sm flex items-center justify-center gap-2 active:scale-95 ${
                    isSimRunning ? 'opacity-50 cursor-not-allowed' : ''
                  }`}
                >
                  <span className="material-symbols-outlined text-base">emergency</span>
                  <span>{isSimRunning ? 'PROCESSING MULTI-AGENT INFERENCE...' : 'FIRE SIMULATED BLE SOS PACKET'}</span>
                </button>
              </div>

              {/* Status Box */}
              <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                <span className="text-[10px] font-mono text-slate-500 uppercase block mb-1">DISTRIBUTED MUTEX LOCK</span>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono text-slate-800">Redlock: <code className="text-emerald-800">mutex:sector:4:boat_12</code></span>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold ${
                      mutexStatus === 'ACQUIRED'
                        ? 'bg-emerald-500 text-white'
                        : 'bg-white border border-slate-200 text-slate-600'
                    }`}
                  >
                    {mutexStatus}
                  </span>
                </div>
              </div>
            </div>

            {/* Terminal Stream Deck */}
            <div className="lg:col-span-7 bg-slate-950 border border-slate-800 rounded-xl p-4 font-mono text-xs flex flex-col justify-between min-h-[360px]">
              <div>
                <div className="flex items-center justify-between pb-2 mb-3 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                    <span className="text-slate-400 text-[11px] ml-1">agentzero-telemetry-fargate.log</span>
                  </div>
                  <span className="text-[10px] text-emerald-400">AWS CloudWatch Live Stream</span>
                </div>
                <div className="space-y-1.5 overflow-y-auto max-h-[280px] text-slate-300">
                  {logs.map((lg, idx) => (
                    <div key={idx} className={lg.color}>
                      [{lg.time}] {lg.text}
                    </div>
                  ))}
                  <div ref={logsEndRef} />
                </div>
              </div>
              <div className="pt-3 border-t border-slate-800 flex items-center justify-between text-[11px]">
                <span className="text-emerald-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  ORCHESTRATOR READY FOR EVALUATION
                </span>
                <span className="text-slate-500">AWS Region: ap-south-1</span>
              </div>
            </div>
          </div>
        </section>

        {/* 8. PRODUCTION TECH STACK MATRIX */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200">
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-emerald-800 text-base">layers</span>
              <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">PRODUCTION READY STACK</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
              Engineered With Modern Cloud &amp; Systems Tooling
            </h2>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-emerald-800 text-2xl mb-1">android</span>
              <span className="text-xs font-bold text-slate-900 block">Kotlin 2.0</span>
              <span className="text-[10px] text-slate-500 font-mono">Compose &amp; BLE Mesh</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-emerald-800 text-2xl mb-1">psychology_alt</span>
              <span className="text-xs font-bold text-slate-900 block">Strands SDK</span>
              <span className="text-[10px] text-slate-500 font-mono">Multi-Agent v1.19</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-amber-700 text-2xl mb-1">cloud</span>
              <span className="text-xs font-bold text-slate-900 block">Amazon Bedrock</span>
              <span className="text-[10px] text-slate-500 font-mono">Claude 3.5 Sonnet</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-red-600 text-2xl mb-1">lock</span>
              <span className="text-xs font-bold text-slate-900 block">Redis Redlock</span>
              <span className="text-[10px] text-slate-500 font-mono">Distributed Mutex</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-emerald-700 text-2xl mb-1">database</span>
              <span className="text-xs font-bold text-slate-900 block">MongoDB Atlas</span>
              <span className="text-[10px] text-slate-500 font-mono">2dsphere Geospatial</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-slate-800 text-2xl mb-1">code</span>
              <span className="text-xs font-bold text-slate-900 block">Next.js 16</span>
              <span className="text-[10px] text-slate-500 font-mono">AWS Amplify Gen2</span>
            </div>
          </div>
        </section>

        {/* 9. HACKATHON QUICKSTART TERMINAL */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 border-t border-slate-200">
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-emerald-800">terminal</span>
                <span className="text-xs font-mono font-bold text-slate-900 uppercase">Hackathon Judge Quickstart Terminal</span>
              </div>
              <span className="text-[10px] font-mono text-slate-500">Docker Compose &amp; Gradle</span>
            </div>
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 space-y-1.5">
              <div className="text-emerald-400"># 1. Clone ZeroGrid Monorepo &amp; start local AWS Mock + AgentZero</div>
              <div className="text-slate-200">git clone https://github.com/bharat-builds/zerogrid-emergency.git &amp;&amp; cd zerogrid</div>
              <div className="text-slate-200">docker compose -f docker-compose.local-aws.yml up -d</div>
              <div className="text-emerald-400 pt-1"># 2. Build and launch Native Android APK with BLE Mesh Simulator</div>
              <div className="text-slate-200">./gradlew assembleDebug &amp;&amp; adb install app/build/outputs/apk/debug/app-debug.apk</div>
            </div>
          </div>
        </section>
      </main>

      {/* FOOTER */}
      <footer className="w-full bg-white border-t border-slate-200 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="flex flex-col gap-1 text-center md:text-left">
            <div className="flex items-center justify-center md:justify-start gap-2">
              <div className="w-6 h-6 rounded-md bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800">
                <span className="material-symbols-outlined text-xs">hub</span>
              </div>
              <span className="font-display font-bold text-slate-900 text-sm tracking-tight">ZeroGrid</span>
              <span className="text-xs text-slate-400 font-mono">BHARAT BUILDS HACKATHON 2025</span>
            </div>
            <p className="text-xs text-slate-500 max-w-md">
              Autonomous off-grid emergency response architecture built with WeMakeDevs &amp; AWS.
            </p>
            <div className="flex items-center justify-center md:justify-start gap-3 pt-1 text-[11px] font-mono text-slate-400">
              <span>MIT LICENSE</span>
              <span>•</span>
              <span>AP-SOUTH-1 COMPLIANT</span>
              <span>•</span>
              <span>NDRF FIELD READY</span>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-4 text-xs font-medium text-slate-600">
            <a className="hover:text-emerald-800 transition-colors" href="https://github.com" target="_blank" rel="noreferrer">GitHub Repository</a>
            <a className="hover:text-emerald-800 transition-colors" href="#architecture">Architecture Whitepaper</a>
            <a className="hover:text-emerald-800 transition-colors" href="#multi-agent">Strands Agents SDK</a>
            <a className="hover:text-emerald-800 transition-colors" href="#simulation">NDRF Simulator</a>
            <Link className="text-emerald-800 font-bold hover:underline" href="/admin">Live Telemetry</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
