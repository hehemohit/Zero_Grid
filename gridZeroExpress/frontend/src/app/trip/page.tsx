'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import PacketInspector from '@/components/trip/PacketInspector';
import { MeshNetworkSimulator } from '@/components/landing/MeshNetworkSimulator';
import AnimatedAgentPipeline from '@/components/trip/AnimatedAgentPipeline';
import { TacticalAgentFlowSimulator } from '@/components/landing/TacticalAgentFlowSimulator';

type StepNumber = 1 | 2 | 3 | 4;

const STEPS = [
  {
    num: 1,
    title: '32-Bit Distress Packet',
    subtitle: 'Word-Aligned Binary Frame',
    icon: 'memory',
  },
  {
    num: 2,
    title: 'RF Mesh Propagation',
    subtitle: 'P2P Gossip & Edge Uplink',
    icon: 'hub',
  },
  {
    num: 3,
    title: 'Autonomous Multi-Agent',
    subtitle: 'Strands SDK & Bedrock',
    icon: 'psychology',
  },
  {
    num: 4,
    title: 'Live Crisis Orchestration',
    subtitle: 'Tactical Dispatch Simulator',
    icon: 'crisis_alert',
  },
];

export default function TripWalkthroughPage() {
  const [currentStep, setCurrentStep] = useState<StepNumber>(1);

  const nextStep = () => {
    if (currentStep < 4) {
      setCurrentStep((prev) => (prev + 1) as StepNumber);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => (prev - 1) as StepNumber);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <div className="bg-[#f8fafc] text-[#0f172a] antialiased font-sans min-h-screen tactical-grid-bg relative overflow-x-hidden selection:bg-emerald-100 selection:text-emerald-900 pb-20">
      {/* Top Walkthrough Navigation Header */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Brand Anchor Logo */}
          <div className="flex items-center gap-3">
            <Link className="flex items-center gap-2.5" href="/">
              <div className="w-9 h-9 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800 shadow-sm shrink-0">
                <span className="material-symbols-outlined text-[22px]">hub</span>
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-display font-bold text-lg text-emerald-950 tracking-tight">ZeroGrid</span>
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                    PROJECT TRIP
                  </span>
                </div>
                <div className="text-[10px] font-mono text-slate-500 -mt-0.5">
                  END-TO-END RESCUE ARCHITECTURE TOUR
                </div>
              </div>
            </Link>
          </div>

          {/* Step Counter & Exit Action */}
          <div className="flex items-center gap-3">
            <span className="hidden sm:inline-block text-xs font-mono font-bold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-lg border border-slate-200">
              STEP {currentStep} OF 4
            </span>
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 px-3.5 h-9 rounded-lg border border-slate-200 bg-white text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-slate-900 transition-all shadow-xs"
            >
              <span className="material-symbols-outlined text-sm">close</span>
              <span>EXIT TOUR</span>
            </Link>
          </div>
        </div>

        {/* Stepper Progress Bar */}
        <div className="w-full bg-slate-100 h-1">
          <div
            className="bg-emerald-700 h-1 transition-all duration-500 ease-out"
            style={{ width: `${(currentStep / 4) * 100}%` }}
          ></div>
        </div>
      </header>

      {/* Main Tour Canvas */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 space-y-8">
        {/* Stepper Tabs */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
          {STEPS.map((s) => {
            const isActive = s.num === currentStep;
            const isCompleted = s.num < currentStep;
            return (
              <button
                key={s.num}
                onClick={() => setCurrentStep(s.num as StepNumber)}
                className={`p-3 rounded-2xl border text-left transition-all cursor-pointer flex items-center gap-3 ${
                  isActive
                    ? 'bg-emerald-900 text-white border-emerald-900 shadow-md ring-2 ring-emerald-500/20'
                    : isCompleted
                    ? 'bg-emerald-50/70 border-emerald-200 text-slate-800 hover:bg-emerald-100/60'
                    : 'bg-white border-slate-200 text-slate-500 hover:border-slate-300'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-xl flex items-center justify-center font-bold text-xs shrink-0 ${
                    isActive
                      ? 'bg-emerald-700 text-white'
                      : isCompleted
                      ? 'bg-emerald-200 text-emerald-900'
                      : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  {isCompleted ? (
                    <span className="material-symbols-outlined text-sm">check</span>
                  ) : (
                    <span>{s.num}</span>
                  )}
                </div>
                <div className="min-w-0">
                  <span className={`text-xs font-bold block truncate ${isActive ? 'text-white' : 'text-slate-900'}`}>
                    {s.title}
                  </span>
                  <span className={`text-[10px] block truncate font-mono ${isActive ? 'text-emerald-300' : 'text-slate-500'}`}>
                    {s.subtitle}
                  </span>
                </div>
              </button>
            );
          })}
        </div>

        {/* STEP 1: 32-BIT PACKET SPECIFICATION (CLICKABLE BYTES) */}
        {currentStep === 1 && (
          <div className="space-y-6 animate-fadeIn">
            <div className="bg-emerald-900 text-white p-5 rounded-2xl border border-emerald-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-2xl text-emerald-400">info</span>
                <div>
                  <h3 className="text-sm font-bold font-display text-white">
                    Disaster Ground Zero: Mobile Base Stations Submerged
                  </h3>
                  <p className="text-xs text-emerald-200 mt-0.5">
                    Your phone constructs this exact 32-byte raw binary frame. Click any byte below to inspect how it preserves battery, coordinates, and sensor depth.
                  </p>
                </div>
              </div>
              <button
                onClick={nextStep}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white text-emerald-950 hover:bg-emerald-50 text-xs font-bold font-mono uppercase tracking-wider transition-all shadow-sm shrink-0 self-start sm:self-auto"
              >
                <span>NEXT: MESH TRANSMISSION</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </div>

            <PacketInspector />

            {/* Bottom Step Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-200">
              <Link
                href="/"
                className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span>
                <span>Return to Landing Page</span>
              </Link>
              <button
                onClick={nextStep}
                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-md active:scale-95"
              >
                <span>NEXT: WATCH PACKET TRAVEL THROUGH MESH</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </div>
          </div>
        )}

        {/* STEP 2: RF MESH NETWORK PROPAGATION */}
        {currentStep === 2 && (
          <div className="space-y-6 animate-fadeIn">
            {/* Step Narrative Banner */}
            <div className="bg-emerald-900 text-white p-5 rounded-2xl border border-emerald-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-2xl text-emerald-400">sensors</span>
                <div>
                  <h3 className="text-sm font-bold font-display text-white">
                    Step 2: Now this packet is in your phone which travels through the mesh network to reach the cloud
                  </h3>
                  <p className="text-xs text-emerald-200 mt-0.5">
                    With cellular towers dead, the 32-byte beacon hops across neighbor Android phones and solar repeaters until reaching a perimeter Data Mule edge uplink.
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
                <button
                  onClick={prevStep}
                  className="px-3.5 py-2 rounded-xl border border-emerald-700 bg-emerald-950/60 text-white text-xs font-semibold hover:bg-emerald-950 transition-all"
                >
                  &larr; BACK
                </button>
                <button
                  onClick={nextStep}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-white text-emerald-950 hover:bg-emerald-50 text-xs font-bold font-mono uppercase tracking-wider transition-all shadow-sm"
                >
                  <span>NEXT: AI PIPELINE</span>
                  <span className="material-symbols-outlined text-sm">arrow_forward</span>
                </button>
              </div>
            </div>

            {/* Reused Mesh Network Simulator */}
            <MeshNetworkSimulator />

            {/* Bottom Step Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-200">
              <button
                onClick={prevStep}
                className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-semibold transition-all shadow-xs"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span>
                <span>Previous Step (Packet Spec)</span>
              </button>
              <button
                onClick={nextStep}
                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-md active:scale-95"
              >
                <span>NEXT: STRANDS AGENTS SDK DECISION PIPELINE</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </div>
          </div>
        )}

        {/* STEP 3: ANIMATED AGENT PIPELINE */}
        {currentStep === 3 && (
          <div className="space-y-6 animate-fadeIn">
            {/* Step Narrative Banner */}
            <div className="bg-emerald-900 text-white p-5 rounded-2xl border border-emerald-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-2xl text-emerald-400">psychology</span>
                <div>
                  <h3 className="text-sm font-bold font-display text-white">
                    Step 3: Strands Agents SDK &amp; AgentZero Autonomous Decision Pipeline
                  </h3>
                  <p className="text-xs text-emerald-200 mt-0.5">
                    The beacon reaches AWS ECS Fargate. Watch the animated 4-phase reasoning pipeline filter noise, cluster duplicates, invoke specialized LLM sub-agents, and lock responder teams.
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
                <button
                  onClick={prevStep}
                  className="px-3.5 py-2 rounded-xl border border-emerald-700 bg-emerald-950/60 text-white text-xs font-semibold hover:bg-emerald-950 transition-all"
                >
                  &larr; BACK
                </button>
                <button
                  onClick={nextStep}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-white text-emerald-950 hover:bg-emerald-50 text-xs font-bold font-mono uppercase tracking-wider transition-all shadow-sm"
                >
                  <span>NEXT: LIVE SIMULATOR</span>
                  <span className="material-symbols-outlined text-sm">arrow_forward</span>
                </button>
              </div>
            </div>

            {/* Animated Agent Decision Pipeline */}
            <AnimatedAgentPipeline />

            {/* Bottom Step Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-200">
              <button
                onClick={prevStep}
                className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-semibold transition-all shadow-xs"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span>
                <span>Previous Step (Mesh Propagation)</span>
              </button>
              <button
                onClick={nextStep}
                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-md active:scale-95"
              >
                <span>NEXT: LIVE TACTICAL SIMULATOR FOR ORCHESTRATION</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
            </div>
          </div>
        )}

        {/* STEP 4: LIVE TACTICAL AGENT SIMULATOR & FINISH */}
        {currentStep === 4 && (
          <div className="space-y-6 animate-fadeIn">
            {/* Step Narrative Banner */}
            <div className="bg-emerald-900 text-white p-5 rounded-2xl border border-emerald-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-2xl text-emerald-400">radar</span>
                <div>
                  <h3 className="text-sm font-bold font-display text-white">
                    Step 4: Live Tactical Agent Simulator for Orchestration
                  </h3>
                  <p className="text-xs text-emerald-200 mt-0.5">
                    Test live crisis dispatch orchestration against AgentZero. Trigger simulated distress beacons, view SCADA 33kV switchboard cutoffs, and monitor real-time atomic dispatch.
                  </p>
                </div>
              </div>
              <Link
                href="/"
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-400 hover:bg-emerald-300 text-emerald-950 text-xs font-bold uppercase tracking-wider transition-all shadow-md shrink-0 self-start sm:self-auto"
              >
                <span>FINISH TRIP &amp; RETURN HOME</span>
                <span className="material-symbols-outlined text-sm">check_circle</span>
              </Link>
            </div>

            {/* Reused Tactical Agent Flow Simulator */}
            <TacticalAgentFlowSimulator />

            {/* Bottom Tour Completion Bar */}
            <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="space-y-1 text-center sm:text-left">
                <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">
                  TOUR COMPLETE // YOU HAVE MASTERED ZEROGRID
                </span>
                <p className="text-xs text-slate-500">
                  From 32-byte offline radio packets to autonomous cloud multi-agent orchestration.
                </p>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={() => setCurrentStep(1)}
                  className="px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-semibold transition-all shadow-xs"
                >
                  Restart Tour
                </button>
                <Link
                  href="/"
                  className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-md active:scale-95"
                >
                  <span>RETURN TO LANDING PAGE</span>
                  <span className="material-symbols-outlined text-sm">home</span>
                </Link>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
