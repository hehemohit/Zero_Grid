'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { TacticalAgentFlowSimulator } from '@/components/landing/TacticalAgentFlowSimulator';
import { ActiveCasesMiniMap } from '@/components/landing/ActiveCasesMiniMap';
import { MeshNetworkSimulator } from '@/components/landing/MeshNetworkSimulator';

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
    title: 'INSPECTOR: TIER 6 // DYNAMODB SCADA GRAPH & DATA PERSISTENCE',
    runtime: 'Amazon DynamoDB (< 4ms 33kV SCADA Breaker State) + MongoDB Atlas 2dsphere + Redis 7.2',
    protocol: 'Single-Table PK/SK Feeder Breaker Topology / Redlock Mutex / GeoJSON Coordinates',
    failover: 'DynamoDB Global Tables Active-Active + Atlas Replica Sets + Redis Multi-AZ',
  },
};

interface LogItem {
  time: string;
  text: string;
  color: string;
}

export default function LandingPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);
  const [selectedTier, setSelectedTier] = useState<string>('t4');

  const currentInspector = TIER_DATA[selectedTier] || TIER_DATA.t4;

  return (
    <div className="bg-[#f8fafc] text-[#0f172a] antialiased font-sans min-h-screen tactical-grid-bg relative overflow-x-hidden selection:bg-emerald-100 selection:text-emerald-900">
      {/* TOP NAVIGATION BAR */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200 transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Brand Anchor Logo */}
          <div className="flex items-center gap-3 shrink-0">
            <Link className="flex items-center gap-2.5" href="/">
              <div className="w-9 h-9 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800 shadow-sm shrink-0">
                <span className="material-symbols-outlined text-[22px]">hub</span>
              </div>
              <div className="whitespace-nowrap">
                <div className="flex items-center gap-2">
                  <span className="font-display font-bold text-lg text-emerald-950 tracking-tight">ZeroGrid</span>
                  <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
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
          <nav className="hidden md:flex items-center gap-1 lg:gap-2 text-xs font-semibold text-slate-600 whitespace-nowrap">
            <Link className="px-2.5 py-1.5 rounded-lg bg-emerald-50 text-emerald-800 font-bold hover:bg-emerald-100 transition-all flex items-center gap-1.5 border border-emerald-200 mr-1" href="/trip">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
              <span>TRIP Tour</span>
            </Link>
            <a className="px-2.5 py-1.5 rounded-lg hover:text-emerald-900 hover:bg-slate-100 transition-all" href="#mesh-simulator">
              Mesh Simulator
            </a>
            <a className="px-2.5 py-1.5 rounded-lg hover:text-emerald-900 hover:bg-slate-100 transition-all" href="#multi-agent">
              Multi-Agent Engine
            </a>
            <a className="px-2.5 py-1.5 rounded-lg hover:text-emerald-900 hover:bg-slate-100 transition-all" href="#simulation">
              Live Tactical Simulator
            </a>
            <a className="px-2.5 py-1.5 rounded-lg hover:text-emerald-900 hover:bg-slate-100 transition-all" href="#architecture">
              AWS Topology
            </a>
          </nav>

          {/* Action Buttons & Mobile Toggle */}
          <div className="flex items-center gap-2 shrink-0 whitespace-nowrap">
            <Link
              className="hidden sm:inline-flex items-center gap-1.5 px-3 h-9 rounded-lg border border-slate-200 bg-white text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-slate-900 transition-all shadow-xs"
              href="/auth/login"
            >
              <span className="material-symbols-outlined text-sm">lock</span>
              <span>ADMIN SIGN IN</span>
            </Link>
            <Link
              className="inline-flex items-center gap-1.5 px-3.5 h-9 rounded-lg bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-xs active:scale-95"
              href="#simulation"
            >
              <span className="material-symbols-outlined text-base">radar</span>
              <span>LAUNCH OPS CONSOLE</span>
            </Link>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden inline-flex items-center justify-center w-9 h-9 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-all shadow-xs"
              aria-label="Toggle navigation menu"
            >
              <span className="material-symbols-outlined text-xl">
                {mobileMenuOpen ? 'close' : 'menu'}
              </span>
            </button>
          </div>
        </div>

        {/* Mobile Navigation Dropdown Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-slate-200 bg-white px-4 py-3 space-y-1 shadow-md">
            <Link
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center justify-between px-3 py-2 rounded-lg text-xs font-bold text-emerald-800 bg-emerald-50 border border-emerald-200 mb-1.5"
              href="/trip"
            >
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse"></span>
                <span>TRIP: Project Tour Walkthrough</span>
              </div>
              <span className="material-symbols-outlined text-sm">arrow_forward</span>
            </Link>
            <a
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-100 hover:text-emerald-900"
              href="#mesh-simulator"
            >
              P2P Mesh Simulator
            </a>
            <a
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-100 hover:text-emerald-900"
              href="#multi-agent"
            >
              Autonomous Multi-Agent Engine
            </a>
            <a
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-100 hover:text-emerald-900"
              href="#simulation"
            >
              Live Tactical Simulator
            </a>
            <a
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-100 hover:text-emerald-900"
              href="#architecture"
            >
              AWS Cloud Topology
            </a>
            <div className="pt-2 border-t border-slate-100">
              <Link
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-bold text-emerald-800 bg-emerald-50"
                href="/auth/login"
              >
                <span className="material-symbols-outlined text-sm">lock</span>
                <span>ADMIN SIGN IN</span>
              </Link>
            </div>
          </div>
        )}
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
                Saving Lives When <span className="text-emerald-800">Mobile Towers Submerge</span> &amp; Power Grids Die
              </h1>
              <p className="text-base sm:text-lg text-slate-600 leading-relaxed max-w-3xl">
                ZeroGrid is an autonomous off-grid rescue coordination engine engineered for extreme Indian monsoons. Powered by Native Android BLE 5.0 / Wi-Fi Direct P2P Mesh with opportunistic cloud uplink into AWS ECS Fargate running <strong className="text-emerald-950 font-semibold">AgentZero (Strands Agents SDK &amp; Amazon Bedrock Claude 3.5 Sonnet)</strong> to prevent 33kV high-voltage electrocutions and direct rescue boats away from flooded death-traps.
              </p>

              {/* CTAs */}
              <div className="flex flex-wrap items-center gap-3.5 pt-2">
                <Link
                  href="/trip"
                  className="px-5 py-3 rounded-xl bg-gradient-to-r from-emerald-800 via-emerald-900 to-slate-900 hover:from-emerald-700 hover:to-slate-800 text-white font-bold text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2.5 transition-all shadow-md active:scale-95 border border-emerald-500/40 group ring-2 ring-emerald-500/20"
                >
                  <span className="flex h-2.5 w-2.5 relative">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-400"></span>
                  </span>
                  <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 border border-emerald-400/40 text-[10px] font-mono text-emerald-300">
                    TRIP
                  </span>
                  <span>CLICK ME TO LEARN ABOUT THE PROJECT</span>
                  <span className="material-symbols-outlined text-sm text-emerald-400 group-hover:translate-x-1 transition-transform">
                    arrow_forward
                  </span>
                </Link>
                <a
                  className="px-5 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white font-semibold text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2 transition-all shadow-md active:scale-95"
                  href="#simulation"
                >
                  <span className="material-symbols-outlined text-lg">crisis_alert</span>
                  <span>LAUNCH COMMAND SIMULATOR</span>
                </a>
                <a
                  className="px-5 py-3 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 hover:text-emerald-800 font-semibold text-xs sm:text-sm uppercase tracking-wider flex items-center gap-2 transition-all shadow-sm"
                  href="#mesh-simulator"
                >
                  <span className="material-symbols-outlined text-lg text-emerald-800">hub</span>
                  <span>LAUNCH MESH SIMULATOR</span>
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

            {/* Native Android Mobile Client UI Showcase */}
            <div className="lg:col-span-4 flex flex-col items-center">
              <div className="w-full max-w-[350px] bg-white rounded-[32px] border-4 border-slate-200 shadow-xl overflow-hidden flex flex-col min-h-[620px]">
                {/* Mobile Status Bar */}
                <div className="px-5 pt-3 pb-2 flex items-center justify-between text-xs text-slate-600 font-mono bg-white">
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
                <div className="px-5 py-2.5 flex items-center justify-between border-b border-slate-200 bg-white">
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
          </div>

          {/* 32-BIT WORD-ALIGNED PACKET PAYLOAD SPECIFICATION & MESH PROTOCOL */}
          <div className="space-y-6" id="mesh-protocol">
            {/* Packet Payload Architecture Design Card */}
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-4 border-b border-slate-200">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-800">memory</span>
                  <div>
                    <h2 className="text-lg font-bold font-display text-slate-900">
                      32-Bit Word-Aligned Binary Packet Specification (Over-The-Air Payload)
                    </h2>
                    <p className="text-xs text-slate-500">
                      ZeroGrid avoids bloated JSON envelopes on congested radio spectrum. Transmitted via raw BLE 5.0 Manufacturer Specific Data or Wi-Fi Direct frame headers.
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 text-xs font-mono">
                  <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-bold border border-emerald-200">
                    32 BYTES FIXED (0x00 - 0x1F)
                  </span>
                  <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-bold">
                    CRC-32 PROTECTED
                  </span>
                </div>
              </div>

              {/* Binary Word Layout Visualizer */}
              <div className="pt-4 space-y-3">
                <div className="grid grid-cols-4 sm:grid-cols-8 gap-1.5 font-mono text-center text-xs">
                  <div className="bg-emerald-50 border border-emerald-300 p-2.5 rounded-xl text-emerald-900 font-bold shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B0</span>
                    0x5A
                    <span className="block text-[8px] text-emerald-700 font-semibold truncate">MAGIC</span>
                  </div>
                  <div className="bg-emerald-50 border border-emerald-300 p-2.5 rounded-xl text-emerald-900 font-bold shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B1</span>
                    0x01
                    <span className="block text-[8px] text-emerald-700 font-semibold truncate">TYPE SOS</span>
                  </div>
                  <div className="bg-amber-50 border border-amber-300 p-2.5 rounded-xl text-amber-900 font-bold shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B2</span>
                    0x07
                    <span className="block text-[8px] text-amber-700 font-semibold truncate">TTL: 7 HOPS</span>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 p-2.5 rounded-xl text-slate-800 shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B3</span>
                    0x12
                    <span className="block text-[8px] text-slate-500 truncate">BATT: 18%</span>
                  </div>
                  <div className="col-span-2 bg-slate-100 border border-slate-200 p-2.5 rounded-xl text-slate-800 shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B4-B7 // float32</span>
                    19.4560° N
                    <span className="block text-[8px] text-slate-500 truncate">LATITUDE</span>
                  </div>
                  <div className="col-span-2 bg-slate-100 border border-slate-200 p-2.5 rounded-xl text-slate-800 shadow-2xs">
                    <span className="block text-[9px] text-slate-500">B8-B11 // float32</span>
                    72.8120° E
                    <span className="block text-[8px] text-slate-500 truncate">LONGITUDE</span>
                  </div>
                  <div className="col-span-4 bg-slate-50 border border-slate-200 p-2.5 rounded-xl text-slate-800 text-left px-3 shadow-2xs">
                    <div className="flex items-center justify-between">
                      <span className="block text-[9px] text-slate-500 font-mono">B12-B27 // 16 BYTES SENSOR &amp; TRIAGE VECTOR</span>
                      <span className="text-[9px] font-mono text-emerald-700 font-bold">DEPTH: 52cm</span>
                    </div>
                    <span className="text-[11px] font-mono text-slate-700 block truncate">
                      Water: 52cm • Temp: 28°C • Occupants: 1 • Impassable
                    </span>
                  </div>
                  <div className="col-span-4 bg-emerald-50 border border-emerald-200 p-2.5 rounded-xl text-emerald-900 font-bold text-center shadow-2xs">
                    <span className="block text-[9px] text-emerald-600">B28-B31 // CRC-32 INTEGRITY</span>
                    <span className="text-xs font-mono">0x48A2C10F [CHECKSUM VALID]</span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center justify-between pt-2 border-t border-slate-200 text-xs font-mono text-slate-600">
                  <span>Packet Over-The-Air Budget: 32 Bytes payload + 4 Bytes GAP Header = 36 Bytes on BLE 5.0</span>
                  <span className="text-emerald-800 font-semibold">LRU Echo Storm Cache: 500-slot FIFO</span>
                </div>
              </div>
            </div>

            {/* INTERACTIVE MESH NETWORK SIMULATOR COMPONENT */}
            <MeshNetworkSimulator />
          </div>
        </section>

        {/* 2. AUTONOMOUS MULTI-AGENT ENGINE */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="multi-agent">
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-1">
              <span className="material-symbols-outlined text-emerald-800 text-base">schema</span>
              <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">AUTONOMOUS MULTI-AGENT ENGINE</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">
              Strands Agents SDK &amp; AgentZero Autonomous Decision Pipeline
            </h2>
            <p className="text-sm text-slate-600 max-w-2xl mt-1">
              AgentZero eliminates emergency room chaos by orchestrating deterministic multi-modal verification with domain-specialized LLM sub-agents.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Phase 1: Confidence Calculator Agent */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 01</span>
                <span className="material-symbols-outlined text-emerald-800">calculate</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">Confidence Calculator Agent</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Deterministic multi-modal gatekeeper scoring incoming beacon veracity across MongoDB memory, live radar, and OSM topography.
              </p>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <span className="text-[10px] font-mono text-emerald-800 block font-bold">DETERMINISTIC VERACITY FORMULA:</span>
                <div className="text-[11px] font-mono text-slate-700 font-semibold">
                  Score = 50 (Base) + M_hist + M_weather + M_osm
                </div>
                <div className="text-[10px] font-mono text-slate-500 pt-1 space-y-0.5">
                  <div>• Hist Bottleneck: +10 to +25 (Past Flood Frequency)</div>
                  <div>• Live Weather: -20 to +20 (Rainfall mm/hr &amp; Surge)</div>
                  <div>• OSM Culvert: -15 to +15 (Low-Lying Topography)</div>
                </div>
              </div>
              <div className="text-xs text-emerald-700 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">verified</span> Floor &ge; 65% triggers Agent 0; &lt; 65% filtered
              </div>
            </div>

            {/* Phase 2: AgentZero Deduplication & Priority Escalator */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 02</span>
                <span className="material-symbols-outlined text-emerald-800">filter_alt</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">AgentZero Dedup &amp; Escalator</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Spatial-temporal clustering preventing 112 saturation, merging witness reports, and dynamically escalating priority tiers.
              </p>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <span className="text-[10px] font-mono text-emerald-800 block font-bold">SPATIAL-TEMPORAL CLUSTERING:</span>
                <div className="text-[11px] font-mono text-slate-700 font-semibold">
                  350m Radius • 60-Min Window • Domain Match
                </div>
                <div className="text-[10px] font-mono text-slate-500 pt-1 space-y-0.5">
                  <div>• 1 Report: Baseline (LOW / MEDIUM)</div>
                  <div>• 2 Reports: Escalated to HIGH (Score: 78)</div>
                  <div>• 3–4 Reports: CRITICAL (Score: 88)</div>
                  <div>• 5+ Reports: CRITICAL Hotspot (Score: 95)</div>
                </div>
              </div>
              <div className="text-xs text-emerald-800 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">hub</span> Autonomous Crisis Domain Routing
              </div>
            </div>

            {/* Phase 3: 4 Specialized Domain Sub-Agents */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 03</span>
                <span className="material-symbols-outlined text-emerald-800">account_tree</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">4 Domain Sub-Agents</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Parallel domain execution formulating tactical equipment demands, required skill tags, and site precautions.
              </p>
              <div className="space-y-1.5 text-xs">
                <div className="bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-700 font-medium">Flood Agent:</span>
                  <span className="text-emerald-800 font-mono text-[10px] font-bold">Zodiacs &amp; 500HP Pumps</span>
                </div>
                <div className="bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-700 font-medium">Power Grid Agent:</span>
                  <span className="text-amber-700 font-mono text-[10px] font-bold">33kV Trip &amp; Linemen</span>
                </div>
                <div className="bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-700 font-medium">Rescue Agent:</span>
                  <span className="text-blue-700 font-mono text-[10px] font-bold">Structural Shoring</span>
                </div>
                <div className="bg-slate-50 p-2 rounded-lg border border-slate-200 flex items-center justify-between">
                  <span className="text-slate-700 font-medium">Heatwave Agent:</span>
                  <span className="text-red-700 font-mono text-[10px] font-bold">Misting &amp; Saline IV</span>
                </div>
              </div>
              <div className="text-xs text-emerald-700 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">smart_toy</span> Strands SDK &amp; Bedrock Claude 3.5
              </div>
            </div>

            {/* Phase 4: Workforce Allocation & Iterative Fallback */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-mono text-xs font-bold">PHASE 04</span>
                <span className="material-symbols-outlined text-emerald-800">lock_person</span>
              </div>
              <h3 className="text-base font-bold text-slate-900">Workforce Fallback Loop</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Queries 160-Admin roster (40 per domain). Acquires atomic Redis mutex locks and triggers inter-department fallback on shortfall.
              </p>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 space-y-1">
                <span className="text-[10px] font-mono text-emerald-800 block font-bold">DISTRIBUTED ATOMIC MUTEX:</span>
                <div className="text-[11px] font-mono text-slate-700 font-semibold">
                  SET team:&lt;id&gt; incident:&lt;id&gt; NX EX 7200
                </div>
                <div className="text-[10px] font-mono text-slate-500 pt-1 space-y-0.5">
                  <div>• Shortfall: Grid &rarr; Flood Dewatering Squads</div>
                  <div>• Shortfall: Flood &rarr; Rescue Evac Units</div>
                  <div>• Shortfall: Heat &rarr; Medical Triage Teams</div>
                </div>
              </div>
              <div className="text-xs text-emerald-800 font-mono font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span> Zero double-dispatch race conditions
              </div>
            </div>
          </div>
        </section>

        {/* 3. LIVE INTERACTIVE SIMULATION SANDBOX */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200" id="simulation">
          <TacticalAgentFlowSimulator />
        </section>

        {/* 4. ACTIVE CASES TACTICAL MINI-MAP PREVIEW */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200">
          <ActiveCasesMiniMap />
        </section>

        {/* 5. PROBLEM STATEMENT MATRIX */}
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

        {/* 6. 6-TIER AWS CLOUD-TO-EDGE ARCHITECTURE BLUEPRINT */}
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
                { id: 't6', title: 'TIER 6 DATA', name: 'DynamoDB & Data', icon: 'database', items: ['Amazon DynamoDB 33kV SCADA', 'MongoDB Atlas 2dsphere Geo', 'ElastiCache Redlock Mutex'], metric: 'SCADA Latency: < 4ms' },
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

        {/* 7. PRODUCTION TECH STACK MATRIX */}
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

          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3">
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
              <span className="material-symbols-outlined text-emerald-800 text-2xl mb-1">map</span>
              <span className="text-xs font-bold text-slate-900 block">AWS Maps SDK</span>
              <span className="text-[10px] text-slate-500 font-mono">Amazon Location</span>
            </div>
            <div className="bg-white p-4 rounded-xl border border-slate-200 text-center shadow-2xs hover:border-emerald-300 transition-all">
              <span className="material-symbols-outlined text-indigo-600 text-2xl mb-1">bolt</span>
              <span className="text-xs font-bold text-slate-900 block">Amazon DynamoDB</span>
              <span className="text-[10px] text-slate-500 font-mono">33kV SCADA Graph</span>
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
