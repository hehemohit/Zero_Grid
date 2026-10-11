'use client';

import React, { useState, useEffect, useRef } from 'react';

interface PhaseStep {
  phase: number;
  badge: string;
  name: string;
  icon: string;
  summary: string;
  formulaLabel: string;
  formulaValue: string;
  logMessage: string;
  metrics: { label: string; value: string; color: string }[];
}

const PHASES: PhaseStep[] = [
  {
    phase: 1,
    badge: 'PHASE 01 // DETERMINISTIC GATEKEEPER',
    name: 'Confidence Calculator Agent',
    icon: 'calculate',
    summary:
      'Incoming 32-byte beacon arrives from mesh edge gateway. Before invoking costly LLMs, this deterministic gatekeeper scores distress veracity against MongoDB past incidents, real-time Doppler radar rainfall, and OSM culvert topography.',
    formulaLabel: 'VERACITY SCORE FORMULA',
    formulaValue: 'Score = 50 (Base) + M_hist (+20) + M_weather (+15) + M_osm (+9) = 94%',
    logMessage: '[GATEKEEPER] Beacon 0x5A validated. Veracity 94% >= 65% floor. Triggering AgentZero Orchestrator.',
    metrics: [
      { label: 'Veracity Score', value: '94% (PASS)', color: 'text-emerald-400' },
      { label: 'Gatekeeper Latency', value: '18ms', color: 'text-emerald-400' },
      { label: 'Filter Action', value: 'PROCEED TO AGENTZERO', color: 'text-emerald-400' },
    ],
  },
  {
    phase: 2,
    badge: 'PHASE 02 // SPATIAL-TEMPORAL CLUSTERING',
    name: 'AgentZero Deduplication & Priority Escalator',
    icon: 'filter_alt',
    summary:
      'Clusters incoming distress beacons within a 350-meter radius and 60-minute window. Merges duplicate reports from panicked neighbors into a single unified incident while escalating priority to prevent 112 system saturation.',
    formulaLabel: 'SPATIAL-TEMPORAL CLUSTERING MATRIX',
    formulaValue: 'Radius: 350m • Window: 60m • 3 Reports Merged -> Priority: CRITICAL HOTSPOT (Score: 88)',
    logMessage: '[DEDUP ENGINE] Merged 3 beacon packets at (19.0760° N, 72.8120° E). Escalated from MEDIUM to CRITICAL.',
    metrics: [
      { label: 'Active Cluster', value: 'INCIDENT #SOS-401', color: 'text-amber-400' },
      { label: 'Reports Merged', value: '3 Beacons Deduped', color: 'text-amber-400' },
      { label: 'Incident Tier', value: 'CRITICAL (Tier 1)', color: 'text-red-400' },
    ],
  },
  {
    phase: 3,
    badge: 'PHASE 03 // PARALLEL MULTI-MODAL REASONING',
    name: '4 Specialized Domain Sub-Agents',
    icon: 'account_tree',
    summary:
      'Strands Agents SDK fans out the incident to 4 parallel LLM sub-agents backed by Amazon Bedrock Claude 3.5 Sonnet. Each agent inspects localized risks, formulating specific equipment and safety precautions.',
    formulaLabel: 'PARALLEL DOMAIN TOOL INVOCATION',
    formulaValue: 'Flood: Zodiac + 500HP Pump • Power: 33kV Trip Feeder #12 • Rescue: Shoring Unit • Heatwave: Standby',
    logMessage: '[STRANDS SDK] Power Agent dispatched 33kV SCADA breaker cutoff. Flood Agent requested motorized inflatable dinghy.',
    metrics: [
      { label: 'LLM Runtime', value: 'Claude 3.5 Sonnet', color: 'text-emerald-400' },
      { label: 'Parallel Agents', value: '4 Domain Workers', color: 'text-blue-400' },
      { label: 'SCADA Action', value: '33kV FEEDER ISOLATED', color: 'text-amber-400' },
    ],
  },
  {
    phase: 4,
    badge: 'PHASE 04 // DISTRIBUTED ATOMIC DISPATCH',
    name: 'Workforce Allocation & Iterative Fallback Loop',
    icon: 'lock_person',
    summary:
      'Queries 160-responder roster (40 per domain). Acquires atomic Redis mutex locks to prevent double-dispatch race conditions. If specialized teams are depleted, automatically triggers inter-department fallback loops.',
    formulaLabel: 'ATOMIC REDIS MUTEX LOCK',
    formulaValue: 'SET team:NDRF-Boat-4 incident:sos-401 NX EX 7200 [ACQUIRED] • Shortfall: Dewatering Squad Assigned',
    logMessage: '[WORKFORCE ENGINE] Acquired lock for NDRF Squad #4. Zero double-dispatch race conditions. Rerouting via safe water channel.',
    metrics: [
      { label: 'Assigned Unit', value: 'NDRF Boat Unit #4', color: 'text-emerald-400' },
      { label: 'Redis Lock TTL', value: '7200s (Mutex Active)', color: 'text-emerald-400' },
      { label: 'Dispatch ETA', value: '4m 12s', color: 'text-emerald-400' },
    ],
  },
];

export default function AnimatedAgentPipeline() {
  const [activePhaseIndex, setActivePhaseIndex] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(true);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        setActivePhaseIndex((prev) => (prev + 1) % PHASES.length);
      }, 4000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPlaying]);

  const currentPhase = PHASES[activePhaseIndex];

  return (
    <div className="bg-white border border-slate-200 rounded-3xl p-6 md:p-8 shadow-sm space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-emerald-800">psychology</span>
            <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">
              STEP 3: AUTONOMOUS MULTI-AGENT DECISION PIPELINE
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold font-display text-slate-900 mt-1">
            Strands Agents SDK &amp; AgentZero in Motion
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl">
            Watch the 4-phase cognitive flow process the incoming 32-byte beacon from multi-modal gatekeeper verification to atomic emergency crew dispatch.
          </p>
        </div>

        {/* Playback Controls */}
        <div className="flex items-center gap-2 self-start md:self-auto">
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            className={`px-3.5 py-1.5 rounded-xl border text-xs font-bold font-mono flex items-center gap-1.5 transition-all shadow-xs ${
              isPlaying
                ? 'bg-amber-50 text-amber-800 border-amber-300 hover:bg-amber-100'
                : 'bg-emerald-800 text-white border-emerald-800 hover:bg-emerald-700'
            }`}
          >
            <span className="material-symbols-outlined text-sm">
              {isPlaying ? 'pause' : 'play_arrow'}
            </span>
            <span>{isPlaying ? 'PAUSE ANIMATION' : 'PLAY AUTOMATICALLY'}</span>
          </button>
          <button
            onClick={() => {
              setIsPlaying(false);
              setActivePhaseIndex((prev) => (prev + 1) % PHASES.length);
            }}
            className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-bold font-mono transition-all shadow-xs"
          >
            NEXT PHASE &rarr;
          </button>
        </div>
      </div>

      {/* 4 Phase Progress Pills */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {PHASES.map((p, idx) => {
          const isActive = idx === activePhaseIndex;
          const isDone = idx < activePhaseIndex;
          return (
            <button
              key={p.phase}
              onClick={() => {
                setIsPlaying(false);
                setActivePhaseIndex(idx);
              }}
              className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer relative overflow-hidden ${
                isActive
                  ? 'bg-emerald-950 text-white border-emerald-800 shadow-md ring-2 ring-emerald-500/30'
                  : isDone
                  ? 'bg-emerald-50/60 border-emerald-200 text-slate-800 hover:border-emerald-400'
                  : 'bg-slate-50 border-slate-200 text-slate-600 hover:border-slate-300'
              }`}
            >
              {isActive && isPlaying && (
                <div className="absolute top-0 left-0 right-0 h-1 bg-emerald-500 animate-[pulse_1s_infinite]"></div>
              )}
              <div className="flex items-center justify-between mb-1">
                <span
                  className={`text-[10px] font-mono font-bold uppercase ${
                    isActive ? 'text-emerald-400' : 'text-slate-500'
                  }`}
                >
                  PHASE 0{p.phase}
                </span>
                <span
                  className={`material-symbols-outlined text-base ${
                    isActive ? 'text-emerald-400' : 'text-slate-400'
                  }`}
                >
                  {p.icon}
                </span>
              </div>
              <span className={`text-xs font-bold block truncate ${isActive ? 'text-white' : 'text-slate-900'}`}>
                {p.name}
              </span>
            </button>
          );
        })}
      </div>

      {/* Active Phase Animated Stage */}
      <div className="bg-slate-950 text-white rounded-2xl p-6 sm:p-7 border border-slate-800 space-y-5 animate-fadeIn">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <span className="material-symbols-outlined text-xl">{currentPhase.icon}</span>
            </div>
            <div>
              <span className="text-[10px] font-mono font-bold text-emerald-400 uppercase tracking-wider block">
                {currentPhase.badge}
              </span>
              <h3 className="text-lg sm:text-xl font-bold font-display text-white">
                {currentPhase.name}
              </h3>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
            <span className="text-xs font-mono text-emerald-400 font-bold uppercase">
              AGENT ACTIVE &amp; RUNNING
            </span>
          </div>
        </div>

        <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
          {currentPhase.summary}
        </p>

        {/* Dynamic Formula Calculation Box */}
        <div className="bg-slate-900/90 p-4 rounded-xl border border-slate-800 space-y-1.5">
          <span className="text-[10px] font-mono text-emerald-400 font-bold uppercase tracking-wider block">
            {currentPhase.formulaLabel}:
          </span>
          <div className="text-xs sm:text-sm font-mono text-emerald-300 font-semibold break-all">
            {currentPhase.formulaValue}
          </div>
        </div>

        {/* Telemetry Metrics Row */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 font-mono text-xs">
          {currentPhase.metrics.map((m, i) => (
            <div key={i} className="bg-slate-900 p-3 rounded-xl border border-slate-800">
              <span className="text-[10px] text-slate-400 block">{m.label}</span>
              <span className={`text-xs font-bold block mt-0.5 ${m.color}`}>{m.value}</span>
            </div>
          ))}
        </div>

        {/* Live System Log Box */}
        <div className="p-3 bg-black/60 rounded-xl border border-slate-800 font-mono text-[11px] text-slate-300 flex items-center gap-2">
          <span className="material-symbols-outlined text-xs text-emerald-400 shrink-0">terminal</span>
          <span className="truncate">{currentPhase.logMessage}</span>
        </div>
      </div>
    </div>
  );
}
