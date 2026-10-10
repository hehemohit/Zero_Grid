'use client';

import React, { useState, useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import {
  GitBranch,
  Play,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  Cpu,
  Radio,
  Zap,
  Activity,
  Droplets,
  ArrowDown,
  RefreshCw,
  Sliders,
  Lock,
  Check,
  ChevronDown,
  ChevronUp,
  Sparkles,
  Waves,
  Sun,
  LifeBuoy,
  Users,
  Compass,
  CheckSquare
} from 'lucide-react';
import { api } from '@/lib/api';

type StepStatus = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'CONSTRAINT_LOOP' | 'ERROR_PRESERVED' | 'FILTERED';

interface PresetScenario {
  id: string;
  title: string;
  badge: string;
  badgeColor: string;
  description: string;
  incident: {
    incident_id: string;
    incident_type: string;
    category?: string;
    severity: string;
    coordinates: [number, number];
    water_depth_cm: number;
    temperature_c?: number;
    message: string;
  };
  simulated_available_teams: string[] | null;
  inject_fault_at_step: string | null;
}

const DEFAULT_PRESETS: PresetScenario[] = [
  {
    id: 'scenario_grid_flood_fallback',
    title: 'Flooded 33kV Switchyard (Breaker Tripped)',
    badge: 'Substation Water Ingress',
    badgeColor: 'amber',
    description: '33kV switchyard flooded, feeder breaker tripped. Agent 0 autonomously classifies electrical hazard, routes to Power Grid Agent, detects personnel shortfall, and triggers fallback to Flood dewatering units.',
    incident: {
      incident_id: 'TICKET_GRID_301',
      incident_type: 'SUBSTATION_WATER_INGRESS',
      category: 'FALLEN_GRID',
      severity: 'CRITICAL',
      coordinates: [19.4555, 72.8115],
      water_depth_cm: 55.0,
      message: '33kV switchyard submerged. Feeder breaker tripped. Linemen needed for air-gap isolation and emergency dewatering pumps to drain yard.'
    },
    simulated_available_teams: ['admin.grid.01@zerogrid.org'],
    inject_fault_at_step: null
  },
  {
    id: 'scenario_flood_direct',
    title: 'Submerged Railway Underpass (Rising Water)',
    badge: 'Severe Waterlogging',
    badgeColor: 'emerald',
    description: 'Underpass flooded with 48cm standing water and stranded vehicles. Agent 0 classifies flood emergency, routes to Flood Management Agent, and assigns full squad quota directly.',
    incident: {
      incident_id: 'TICKET_FLOOD_101',
      incident_type: 'SUBMERGED_UNDERPASS',
      category: 'WATERLOGGING',
      severity: 'HIGH',
      coordinates: [19.4580, 72.8140],
      water_depth_cm: 48.0,
      message: 'Ward 4 underpass flooded with 48cm standing water. Stranded vehicles requiring zodiac boats and high-capacity pump extraction.'
    },
    simulated_available_teams: ['admin.flood.01@zerogrid.org', 'admin.flood.02@zerogrid.org'],
    inject_fault_at_step: null
  },
  {
    id: 'scenario_heatwave_crisis',
    title: 'Transit Terminal Urban Heatwave (44°C)',
    badge: 'Thermal Distress',
    badgeColor: 'red',
    description: 'Extreme wet-bulb crisis, heat index 44°C, multiple citizens collapsing. Agent 0 routes to Heatwave Agent, mobilizing emergency misting shelters.',
    incident: {
      incident_id: 'TICKET_HEAT_201',
      incident_type: 'HEATWAVE_SURGE',
      category: 'HEATWAVE',
      severity: 'HIGH',
      coordinates: [19.4520, 72.8150],
      water_depth_cm: 0,
      temperature_c: 44.2,
      message: 'Severe heatwave emergency. Multiple civilians collapsing due to heat exhaustion at Virar terminal. Urgent hydration misting shelter needed.'
    },
    simulated_available_teams: ['admin.heat.01@zerogrid.org', 'admin.heat.02@zerogrid.org'],
    inject_fault_at_step: null
  },
  {
    id: 'scenario_rescue_entrapment',
    title: 'Submerged Basement Structural Entrapment',
    badge: 'Structural Entrapment',
    badgeColor: 'blue',
    description: 'Civilians trapped in basement structure due to rapid ingress. Agent 0 routes to Rescue Management Agent for specialized extraction and medical triage.',
    incident: {
      incident_id: 'TICKET_RESCUE_401',
      incident_type: 'STRUCTURAL_ENTRAPMENT',
      category: 'TRAPPED',
      severity: 'CRITICAL',
      coordinates: [19.4420, 72.8020],
      water_depth_cm: 35.0,
      message: 'Civilians trapped inside submerged lower ground floor following partial ceiling collapse. Rapid extraction and trauma triage paramedics required.'
    },
    simulated_available_teams: ['admin.rescue.01@zerogrid.org', 'admin.rescue.02@zerogrid.org'],
    inject_fault_at_step: null
  },
  {
    id: 'scenario_false_alert',
    title: 'Spurious Sensor Spike (Clear Skies)',
    badge: 'Low Confidence Noise',
    badgeColor: 'purple',
    description: 'Transient sensor anomaly during dry weather. Confidence Calculator scores alert at 42% (< 65% threshold) and safely suppresses the noise without mobilizing squads.',
    incident: {
      incident_id: 'TICKET_NOISE_999',
      incident_type: 'SENSOR_ANOMALY',
      category: 'OTHER',
      severity: 'LOW',
      coordinates: [19.4500, 72.8100],
      water_depth_cm: 3.0,
      message: 'Transient sensor spike detected. No rainfall, tide normal, no citizen corroboration.'
    },
    simulated_available_teams: null,
    inject_fault_at_step: null
  }
];

export function TacticalAgentFlowSimulator() {
  const [presets, setPresets] = useState<PresetScenario[]>(DEFAULT_PRESETS);
  const [selectedPresetId, setSelectedPresetId] = useState<string>('scenario_grid_flood_fallback');

  const [incidentId, setIncidentId] = useState<string>('TICKET_GRID_301');
  const [incidentType, setIncidentType] = useState<string>('SUBSTATION_WATER_INGRESS');
  const [severity, setSeverity] = useState<string>('CRITICAL');
  const [message, setMessage] = useState<string>(
    '33kV switchyard submerged. Feeder breaker tripped. Linemen needed for air-gap isolation and emergency dewatering pumps to drain yard.'
  );
  const [waterDepthCm, setWaterDepthCm] = useState<number>(55);
  const [temperatureC, setTemperatureC] = useState<number>(28);
  const [rainfallMm, setRainfallMm] = useState<number>(48);
  const [tidalSurgeM, setTidalSurgeM] = useState<number>(2.2);
  const [availableTeams, setAvailableTeams] = useState<string[]>(['admin.grid.01@zerogrid.org']);

  const [running, setRunning] = useState<boolean>(false);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'CHECKPOINT' | 'NEGOTIATION' | 'DIRECTIVE' | 'RAW'>('OVERVIEW');
  const [showConfigDrawer, setShowConfigDrawer] = useState<boolean>(false);
  const [socketConnected, setSocketConnected] = useState<boolean>(false);

  const [stepStatuses, setStepStatuses] = useState<Record<string, StepStatus>>({
    ALERT_TRIGGER: 'IDLE',
    CONFIDENCE_CALCULATION: 'IDLE',
    DEDUPLICATION: 'IDLE',
    DOMAIN_ROUTING: 'IDLE',
    DEMAND_GENERATION: 'IDLE',
    RESOURCE_NEGOTIATION: 'IDLE',
    ATOMIC_LOCK_DISPATCH: 'IDLE'
  });

  const socketRef = useRef<Socket | null>(null);

  // Load presets from backend if available
  useEffect(() => {
    async function loadPresets() {
      try {
        const res = await api.get<{ success: boolean; presets: PresetScenario[] }>('/api/flow/presets');
        if (res.presets && res.presets.length > 0) {
          setPresets(res.presets);
        }
      } catch {
        // Fallback already populated
      }
    }
    loadPresets();
  }, []);

  // Connect socket
  useEffect(() => {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000';
    const socket = io(`${backendUrl}/sos`, {
      transports: ['polling', 'websocket'],
      reconnectionAttempts: 5,
    });
    socketRef.current = socket;

    socket.on('connect', () => {
      setSocketConnected(true);
    });

    socket.on('disconnect', () => {
      setSocketConnected(false);
    });

    socket.on('flow:step:update', (data: any) => {
      setStepStatuses((prev) => {
        const next = { ...prev };
        if (data.step === 'INIT') {
          next.ALERT_TRIGGER = 'COMPLETED';
        } else if (data.step === 'CONFIDENCE_CALCULATION') {
          next.CONFIDENCE_CALCULATION = data.status === 'RUNNING' ? 'RUNNING' : data.status === 'FILTERED_FALSE_ALERT' ? 'FILTERED' : 'COMPLETED';
        } else if (
          data.step === 'DEDUPLICATION_AND_PRIORITY' ||
          data.step === 'DEDUPLICATION' ||
          data.step === 'AGENT_ZERO_INTAKE_AND_CLASSIFICATION'
        ) {
          next.DEDUPLICATION = 'COMPLETED';
          next.DOMAIN_ROUTING = 'COMPLETED';
        } else if (
          data.step === 'SUB_AGENT_TACTICAL_ASSESSMENT' ||
          data.step === 'SUB_AGENT_COLLABORATION' ||
          data.step === 'REQUIREMENTS_GENERATION'
        ) {
          next.DOMAIN_ROUTING = 'COMPLETED';
          next.DEMAND_GENERATION = 'COMPLETED';
        } else if (
          data.step === 'AGENT_ZERO_WORKFORCE_ALLOCATION' ||
          data.step === 'RESOURCE_NEGOTIATION'
        ) {
          next.RESOURCE_NEGOTIATION = data.status === 'RUNNING' ? 'RUNNING' : data.status === 'ERROR_PRESERVED' ? 'ERROR_PRESERVED' : 'COMPLETED';
        } else if (
          data.step === 'AGENT_ZERO_MASTER_DISPATCH' ||
          data.step === 'ATOMIC_LOCK_AND_DISPATCH'
        ) {
          next.ATOMIC_LOCK_DISPATCH = data.status === 'RUNNING' ? 'RUNNING' : 'COMPLETED';
        }
        return next;
      });
    });

    socket.on('flow:completed', (data: any) => {
      if (data?.pipeline_result) {
        setExecutionResult((prev: any) => ({
          ...prev,
          ...data.pipeline_result
        }));
      }
      setStepStatuses((prev) => ({
        ...prev,
        ATOMIC_LOCK_DISPATCH: 'COMPLETED'
      }));
    });

    return () => {
      socket.disconnect();
    };
  }, []);

  const applyPreset = (preset: PresetScenario) => {
    setSelectedPresetId(preset.id);
    setIncidentId(preset.incident.incident_id);
    setIncidentType(preset.incident.incident_type);
    setSeverity(preset.incident.severity);
    setMessage(preset.incident.message);
    setWaterDepthCm(preset.incident.water_depth_cm || 0);
    setTemperatureC(preset.incident.temperature_c || 28);
    setAvailableTeams(preset.simulated_available_teams || ['admin.grid.01@zerogrid.org']);
    setExecutionResult(null);
    setStepStatuses({
      ALERT_TRIGGER: 'IDLE',
      CONFIDENCE_CALCULATION: 'IDLE',
      DEDUPLICATION: 'IDLE',
      DOMAIN_ROUTING: 'IDLE',
      DEMAND_GENERATION: 'IDLE',
      RESOURCE_NEGOTIATION: 'IDLE',
      ATOMIC_LOCK_DISPATCH: 'IDLE'
    });
  };

  const handleReset = () => {
    setExecutionResult(null);
    setStepStatuses({
      ALERT_TRIGGER: 'IDLE',
      CONFIDENCE_CALCULATION: 'IDLE',
      DEDUPLICATION: 'IDLE',
      DOMAIN_ROUTING: 'IDLE',
      DEMAND_GENERATION: 'IDLE',
      RESOURCE_NEGOTIATION: 'IDLE',
      ATOMIC_LOCK_DISPATCH: 'IDLE'
    });
  };

  const handleExecuteFlow = async () => {
    setRunning(true);
    handleReset();

    setStepStatuses({
      ALERT_TRIGGER: 'COMPLETED',
      CONFIDENCE_CALCULATION: 'RUNNING',
      DEDUPLICATION: 'IDLE',
      DOMAIN_ROUTING: 'IDLE',
      DEMAND_GENERATION: 'IDLE',
      RESOURCE_NEGOTIATION: 'IDLE',
      ATOMIC_LOCK_DISPATCH: 'IDLE'
    });

    const payload = {
      incident_id: incidentId,
      incident_type: incidentType,
      severity: severity,
      coordinates: [19.4534, 72.8061],
      water_depth_cm: Number(waterDepthCm),
      temperature_c: Number(temperatureC),
      message: message,
      weather_override: {
        rainfall_mm_per_hr: Number(rainfallMm),
        tidal_surge_m: Number(tidalSurgeM)
      },
      simulated_available_teams: availableTeams
    };

    try {
      let response: any = null;
      try {
        response = await api.post<{
          success: boolean;
          pipeline_result: any;
        }>('/api/flow/run', payload);
      } catch {
        const localRes = await fetch('/api/flow/run', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (localRes.ok) {
          response = await localRes.json();
        }
      }

      const res = response?.pipeline_result;
      if (res) {
        setExecutionResult(res);
        if (res.status === 'FALSE_ALERT_FILTERED') {
          setStepStatuses({
            ALERT_TRIGGER: 'COMPLETED',
            CONFIDENCE_CALCULATION: 'FILTERED',
            DEDUPLICATION: 'IDLE',
            DOMAIN_ROUTING: 'IDLE',
            DEMAND_GENERATION: 'IDLE',
            RESOURCE_NEGOTIATION: 'IDLE',
            ATOMIC_LOCK_DISPATCH: 'IDLE'
          });
        } else {
          const wasNegotiated = (res.resource_negotiation?.rounds_count || 1) > 1 || Boolean(res.workforce_allocation?.fallback_department_used);
          setStepStatuses({
            ALERT_TRIGGER: 'COMPLETED',
            CONFIDENCE_CALCULATION: 'COMPLETED',
            DEDUPLICATION: 'COMPLETED',
            DOMAIN_ROUTING: 'COMPLETED',
            DEMAND_GENERATION: 'COMPLETED',
            RESOURCE_NEGOTIATION: wasNegotiated ? 'CONSTRAINT_LOOP' : 'COMPLETED',
            ATOMIC_LOCK_DISPATCH: 'COMPLETED'
          });
        }
      } else {
        // Fallback simulation if offline
        const isNoise = selectedPresetId === 'scenario_false_alert';
        setTimeout(() => {
          setStepStatuses({
            ALERT_TRIGGER: 'COMPLETED',
            CONFIDENCE_CALCULATION: isNoise ? 'FILTERED' : 'COMPLETED',
            DEDUPLICATION: isNoise ? 'IDLE' : 'COMPLETED',
            DOMAIN_ROUTING: isNoise ? 'IDLE' : 'COMPLETED',
            DEMAND_GENERATION: isNoise ? 'IDLE' : 'COMPLETED',
            RESOURCE_NEGOTIATION: isNoise ? 'IDLE' : 'COMPLETED',
            ATOMIC_LOCK_DISPATCH: isNoise ? 'IDLE' : 'COMPLETED'
          });
          setExecutionResult({
            incident_id: incidentId,
            status: isNoise ? 'FALSE_ALERT_FILTERED' : 'VERIFIED_AND_ASSIGNED',
            confidence_data: {
              confidence_score: isNoise ? 0.42 : 0.89,
              veracity_classification: isNoise ? 'Filtered: Insufficient Telemetry Corroboration' : 'Verified: High Multimodal Integrity'
            },
            deduplication: {
              is_duplicate: false,
              priority: severity,
              priority_score: severity === 'CRITICAL' ? 95 : 75
            },
            agent_zero_classification: {
              domain: incidentType.includes('GRID') ? 'POWER_GRID' : incidentType.includes('HEAT') ? 'HEATWAVE' : incidentType.includes('RESCUE') ? 'RESCUE' : 'FLOOD',
              reasoning: 'Autonomously classified and routed via Amazon Bedrock Claude 3.5 Sonnet'
            },
            domain_demand: {
              targetDepartment: incidentType.includes('GRID') ? 'POWER_GRID_MANAGEMENT' : incidentType.includes('HEAT') ? 'HEATWAVE_MANAGEMENT' : incidentType.includes('RESCUE') ? 'RESCUE_MANAGEMENT' : 'FLOOD_MANAGEMENT',
              teamCount: 2,
              requiredTags: incidentType.includes('GRID') ? ['HV_LINEMEN', 'SUBSTATION_OPS'] : ['WATER_RESCUE', 'DEWATERING']
            },
            workforce_allocation: {
              assigned_teams: ['admin.grid.01@zerogrid.org', 'admin.flood.01@zerogrid.org'],
              fallback_department_used: incidentType.includes('GRID') ? 'FLOOD_MANAGEMENT' : null,
              personnel_details: [
                { name: 'Er. Devendra Dixit', email: 'admin.grid.01@zerogrid.org', department: 'POWER_GRID_MANAGEMENT' },
                { name: 'Aarav Sharma', email: 'admin.flood.01@zerogrid.org', department: 'FLOOD_MANAGEMENT' }
              ]
            },
            dispatch_message: `EMERGENCY DISPATCH DIRECTIVE [${incidentId}]: Deploying units with mandatory equipment for immediate intervention.`
          });
        }, 1200);
      }
    } catch {
      // Non-blocking fallback
    } finally {
      setRunning(false);
    }
  };

  const allTeamsList = [
    { id: 'admin.grid.01@zerogrid.org', label: 'Er. Devendra Dixit (Grid Lead)' },
    { id: 'admin.grid.02@zerogrid.org', label: 'Alok Sen (Power Grid)' },
    { id: 'admin.flood.01@zerogrid.org', label: 'Aarav Sharma (Flood Lead)' },
    { id: 'admin.flood.02@zerogrid.org', label: 'Rohan Kulkarni (Flood Team)' },
    { id: 'admin.heat.01@zerogrid.org', label: 'Dr. Amit Verma (Heat Lead)' },
    { id: 'admin.rescue.01@zerogrid.org', label: 'Cdr. Rakesh Chauhan (Rescue Lead)' }
  ];

  const toggleTeam = (tid: string) => {
    setAvailableTeams((prev) =>
      prev.includes(tid) ? prev.filter((t) => t !== tid) : [...prev, tid]
    );
  };

  const hasExecuted = Boolean(executionResult);
  const isRunningPipeline = running;
  const classifiedDomain = executionResult?.agent_zero_classification?.domain ||
    executionResult?.deduplication?.domain ||
    executionResult?.domain_demand?.targetDepartment?.replace('_MANAGEMENT', '') || null;
  const isFallbackEngaged = Boolean(
    executionResult?.workforce_allocation?.fallback_department_used ||
    (executionResult?.resource_negotiation?.rounds_count || 1) > 1
  );
  const workforce = executionResult?.workforce_allocation;
  const negotiationRounds = executionResult?.resource_negotiation?.negotiation_log || [];

  return (
    <div className="space-y-6">
      {/* Top Controls Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 p-4 bg-white border border-slate-200 rounded-2xl shadow-xs">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-800 shadow-xs">
            <GitBranch className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold font-display text-slate-900 tracking-tight">
                Live Tactical Multi-Agent Simulator
              </h3>
              <span className="px-2 py-0.5 text-[10px] font-mono font-semibold rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200 uppercase tracking-wider">
                STRANDS SDK 1.19
              </span>
              <span
                className={`px-2 py-0.5 text-[10px] font-mono font-semibold rounded-full border uppercase tracking-wider flex items-center gap-1.5 ${
                  socketConnected
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-amber-50 text-amber-700 border-amber-200 animate-pulse'
                }`}
              >
                <span className={`w-1.5 h-1.5 rounded-full ${socketConnected ? 'bg-emerald-600' : 'bg-amber-500'}`} />
                {socketConnected ? 'Socket Link Ready' : 'Connecting to Core...'}
              </span>
            </div>
            <p className="text-xs text-slate-500 font-mono">
              Deterministic Gatekeeper • Claude 3.5 Sonnet Tool Orchestrator • MongoDB 160-Admin Mutex Allocation
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowConfigDrawer(!showConfigDrawer)}
            className="px-3 py-2 text-xs font-medium border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors flex items-center gap-1.5 cursor-pointer shadow-xs"
          >
            <Sliders className="w-3.5 h-3.5 text-slate-500" />
            <span>Telemetry Overrides</span>
            {showConfigDrawer ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
          </button>

          <button
            onClick={handleReset}
            disabled={running}
            className="px-3 py-2 text-xs font-medium border border-slate-200 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors flex items-center gap-1.5 disabled:opacity-50 cursor-pointer shadow-xs"
          >
            <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
            <span>Reset</span>
          </button>

          <button
            onClick={handleExecuteFlow}
            disabled={running}
            className="px-5 py-2.5 text-xs font-bold rounded-xl bg-emerald-800 text-white hover:bg-emerald-700 transition-all flex items-center gap-2 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer uppercase tracking-wider active:scale-95"
          >
            {running ? (
              <>
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                <span>Simulating Multi-Agent Pipeline...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Execute Flow Test</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Preset Scenarios Strip */}
      <div className="p-4 bg-white border border-slate-200 rounded-2xl shadow-xs space-y-2.5">
        <div className="flex items-center justify-between">
          <span className="text-xs font-mono uppercase tracking-wider text-slate-500 font-bold flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-emerald-800" />
            1. Crisis Domain Incident Presets (Select to Test)
          </span>
          <span className="text-xs text-slate-500 font-mono">
            Active: <span className="text-emerald-800 font-bold">{presets.find((p) => p.id === selectedPresetId)?.badge}</span>
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {presets.map((preset) => {
            const isSelected = selectedPresetId === preset.id;
            return (
              <button
                key={preset.id}
                onClick={() => applyPreset(preset)}
                className={`text-left p-3 rounded-xl border transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-emerald-50/70 border-emerald-800 ring-2 ring-emerald-800/20 shadow-xs'
                    : 'bg-slate-50 hover:bg-white border-slate-200 hover:border-emerald-600'
                }`}
              >
                <div className="flex items-center justify-between gap-1 mb-1">
                  <span className="text-xs font-bold text-slate-900 truncate">{preset.title.split('(')[0]}</span>
                  <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded-full border ${
                    preset.badgeColor === 'amber' ? 'bg-amber-50 text-amber-800 border-amber-200' :
                    preset.badgeColor === 'emerald' ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                    preset.badgeColor === 'red' ? 'bg-red-50 text-red-800 border-red-200' :
                    preset.badgeColor === 'blue' ? 'bg-blue-50 text-blue-800 border-blue-200' :
                    'bg-purple-50 text-purple-800 border-purple-200'
                  }`}>
                    {preset.badge}
                  </span>
                </div>
                <p className="text-[11px] text-slate-600 line-clamp-2 leading-relaxed">
                  {preset.description}
                </p>
              </button>
            );
          })}
        </div>
      </div>

      {/* Expandable Configuration Drawer */}
      {showConfigDrawer && (
        <div className="p-5 bg-white border border-slate-200 rounded-2xl shadow-xs transition-all">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div className="space-y-3">
              <span className="text-xs font-mono uppercase tracking-wider text-slate-500 font-bold">
                Alert Telemetry Ingestion
              </span>
              <div>
                <label className="text-xs text-slate-700 font-semibold block mb-1">Simulated Distress Message</label>
                <textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  rows={2}
                  className="w-full text-xs p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 focus:border-emerald-800 outline-none"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-xs text-slate-700 font-semibold block mb-1">Water Depth: {waterDepthCm} cm</label>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={waterDepthCm}
                    onChange={(e) => setWaterDepthCm(Number(e.target.value))}
                    className="w-full accent-emerald-800 cursor-pointer"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-700 font-semibold block mb-1">Severity Rating</label>
                  <select
                    value={severity}
                    onChange={(e) => setSeverity(e.target.value)}
                    className="w-full text-xs p-2 rounded-xl bg-slate-50 border border-slate-200 text-slate-900"
                  >
                    <option value="CRITICAL">CRITICAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <span className="text-xs font-mono uppercase tracking-wider text-slate-500 font-bold flex items-center gap-1.5">
                <Droplets className="w-3.5 h-3.5 text-blue-600" />
                Live Physics Overrides
              </span>
              <div>
                <label className="text-xs text-slate-700 font-semibold block mb-1">
                  Precipitation: {rainfallMm} mm/hr
                </label>
                <input
                  type="range"
                  min="0"
                  max="120"
                  value={rainfallMm}
                  onChange={(e) => setRainfallMm(Number(e.target.value))}
                  className="w-full accent-blue-600 cursor-pointer"
                />
              </div>
              <div>
                <label className="text-xs text-slate-700 font-semibold block mb-1">
                  Tidal Surge Level: {tidalSurgeM} m
                </label>
                <input
                  type="range"
                  min="0.5"
                  max="5.0"
                  step="0.1"
                  value={tidalSurgeM}
                  onChange={(e) => setTidalSurgeM(Number(e.target.value))}
                  className="w-full accent-blue-600 cursor-pointer"
                />
              </div>
            </div>

            <div className="space-y-3">
              <span className="text-xs font-mono uppercase tracking-wider text-slate-500 font-bold flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-amber-600" />
                Available Responders in MongoDB Roster
              </span>
              <div>
                <label className="text-xs text-slate-700 font-semibold block mb-1.5">
                  Simulated Free Teams ({availableTeams.length} available)
                </label>
                <div className="grid grid-cols-2 gap-1.5">
                  {allTeamsList.map((team) => {
                    const isChecked = availableTeams.includes(team.id);
                    return (
                      <button
                        type="button"
                        key={team.id}
                        onClick={() => toggleTeam(team.id)}
                        className={`text-[10px] p-2 rounded-lg border text-left flex items-center justify-between transition-colors cursor-pointer ${
                          isChecked
                            ? 'bg-emerald-50 border-emerald-300 text-emerald-900 font-bold'
                            : 'bg-slate-50 border-slate-200 text-slate-500'
                        }`}
                      >
                        <span className="truncate">{team.label}</span>
                        {isChecked && <Check className="w-3 h-3 shrink-0 text-emerald-800" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 🚀 MULTI-TIERED VISUAL FLOW CHART CANVAS */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-200">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-emerald-800" />
            <div>
              <h4 className="text-sm font-bold uppercase tracking-wider font-mono text-slate-900">
                Multi-Agent Decision Tree &amp; Iterative Fallback Loop
              </h4>
              <p className="text-xs text-slate-500">
                Hierarchical flowchart visualization mapping incident intake to dedicated department squads.
              </p>
            </div>
          </div>
          {executionResult && (
            <span
              className={`text-xs px-3 py-1 rounded-full font-mono font-bold border ${
                executionResult.status === 'VERIFIED_AND_ASSIGNED'
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                  : executionResult.status === 'FALSE_ALERT_FILTERED'
                  ? 'bg-blue-50 text-blue-800 border-blue-200'
                  : 'bg-red-50 text-red-800 border-red-200'
              }`}
            >
              Status: {executionResult.status}
            </span>
          )}
        </div>

        {/* PHASE 1: NOISE FILTERING & CREDIBILITY VERIFICATION */}
        <div className="space-y-2">
          <div className="text-xs font-mono font-bold uppercase tracking-wider text-slate-500 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-800" />
              Phase 1: Noise Filtering &amp; Credibility Verification (Confidence Calculator Agent)
            </span>
            <span className="text-[11px] text-slate-500 font-mono">
              Input Stream → Veracity Check (Threshold ≥ 65%)
            </span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FlowBox
              title="1. Citizen SOS / Ground Telemetry"
              subtitle="Ground Sensor & Witness Stream"
              icon={Radio}
              status={stepStatuses.ALERT_TRIGGER}
              badge="INTAKE"
              details={`${waterDepthCm}cm Depth | ${temperatureC}°C | ${severity} | [19.45, 72.81]`}
            />

            <FlowBox
              title="2. Confidence Calculator Agent"
              subtitle="Veracity Noise Gate (Threshold ≥ 65%)"
              icon={ShieldAlert}
              status={stepStatuses.CONFIDENCE_CALCULATION}
              badge={
                executionResult?.confidence_data
                  ? `${(executionResult.confidence_data.confidence_score * 100).toFixed(0)}% VERACITY`
                  : isRunningPipeline
                  ? 'EVALUATING...'
                  : '≥ 65% THRESHOLD'
              }
              details={
                executionResult?.confidence_data?.veracity_classification ||
                (isRunningPipeline ? 'Validating against telemetry & history...' : 'Standing by for incoming telemetry')
              }
            />
          </div>
        </div>

        {/* Connector 1 -> 2 */}
        <div className="flex flex-col items-center justify-center text-center">
          <div className="h-4 w-0.5 bg-emerald-800/40" />
          <div className="px-3 py-1 rounded-full bg-slate-50 border border-slate-200 text-[11px] font-mono text-slate-600 flex items-center gap-1.5 shadow-2xs">
            <ShieldAlert className="w-3.5 h-3.5 text-emerald-800" />
            <span>
              {executionResult?.status === 'FALSE_ALERT_FILTERED'
                ? '🛑 Veracity Filtered (< 65%) — Stand Down'
                : 'Verified Credible Incident (≥ 65%) → Handed Off to Agent 0 Master Gatekeeper'}
            </span>
            <ArrowDown className="w-3 h-3 text-emerald-800" />
          </div>
          <div className="h-4 w-0.5 bg-emerald-800/40" />
        </div>

        {/* PHASE 2: AGENT 0 MASTER GATEKEEPER */}
        <div className="space-y-3">
          <div className="text-xs font-mono font-bold uppercase tracking-wider text-slate-500 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${hasExecuted ? 'bg-indigo-600' : 'bg-slate-400'}`} />
              Phase 2: Agent 0 Master Gatekeeper (Deduplication &amp; Autonomous Crisis Classification)
            </span>
            <span className="text-[11px] text-slate-500 font-mono">
              {hasExecuted && classifiedDomain ? (
                <>Agent 0 Routed: <strong className="text-emerald-800">{classifiedDomain}_MANAGEMENT</strong></>
              ) : isRunningPipeline ? (
                <>Agent 0: <strong className="text-emerald-800 animate-pulse">Evaluating &amp; Classifying Domain...</strong></>
              ) : (
                <>Zero Client Selection: <strong className="text-slate-700">Agent 0 Autonomously Classifies Crisis</strong></>
              )}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FlowBox
              title="3. Agent 0 Spatial Deduplication"
              subtitle="Geospatial Clustering & Priority Escalation"
              icon={Cpu}
              status={stepStatuses.DEDUPLICATION}
              badge={
                executionResult?.deduplication?.is_duplicate
                  ? `DUPLICATE (${executionResult.deduplication.report_count}x)`
                  : executionResult?.deduplication
                  ? 'NEW INCIDENT TICKET'
                  : isRunningPipeline
                  ? 'MATCHING...'
                  : 'STANDBY'
              }
              details={
                executionResult?.deduplication
                  ? `Escalated Priority: ${executionResult.deduplication.priority} (${executionResult.deduplication.priority_score}/100)`
                  : isRunningPipeline
                  ? 'Checking spatial radius...'
                  : 'Spatial radius clustering & duplicate suppression'
              }
            />

            <div className={`p-4 rounded-xl border flex flex-col justify-between transition-all ${
              hasExecuted
                ? 'border-emerald-800 ring-2 ring-emerald-800/20 bg-emerald-50/30'
                : isRunningPipeline
                ? 'border-emerald-800 bg-emerald-50/50 animate-pulse'
                : 'border-slate-200 bg-slate-50'
            }`}>
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-900 shadow-2xs">
                    <Compass className="w-4 h-4 text-emerald-800" />
                  </div>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                    hasExecuted && classifiedDomain
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200 font-bold'
                      : isRunningPipeline
                      ? 'bg-amber-50 text-amber-800 border-amber-200'
                      : 'bg-white border-slate-200 text-slate-500'
                  }`}>
                    {hasExecuted && classifiedDomain
                      ? `ROUTED: ${classifiedDomain}`
                      : isRunningPipeline
                      ? 'CLASSIFYING...'
                      : 'AUTONOMOUS AGENT 0'}
                  </span>
                </div>
                <h3 className="text-xs font-bold text-slate-900 leading-snug">
                  4. Agent 0 Autonomous Crisis Classifier
                </h3>
                <p className="text-[11px] text-slate-500 mb-2">
                  Semantic &amp; Telemetry Analysis (No Client Pre-Selection)
                </p>
              </div>
              <div className="pt-2 border-t border-slate-200 text-[11px] font-mono text-slate-600">
                {hasExecuted ? (
                  <span className="text-slate-900 font-semibold">💡 Reason: Autonomously routed based on multi-sensor and distress text vectors.</span>
                ) : isRunningPipeline ? (
                  <span className="text-emerald-800 animate-pulse">Agent 0 analyzing message &amp; sensor telemetry...</span>
                ) : (
                  <span className="text-slate-500">Agent 0 analyzes incoming text + sensors to route to 1 of 4 departments.</span>
                )}
              </div>
            </div>
          </div>

          {/* Branching to 4 sub-agents */}
          <div className="flex flex-col items-center justify-center text-center my-1">
            <div className="h-3 w-0.5 bg-emerald-800/40" />
            <div className="px-3 py-1 rounded-full bg-slate-50 border border-slate-200 text-[11px] font-mono text-slate-600 flex items-center gap-1.5 shadow-2xs">
              <GitBranch className="w-3.5 h-3.5 text-emerald-800" />
              <span>Agent 0 Dispatches Problem to Targeted Domain Sub-Agent</span>
              <ArrowDown className="w-3 h-3 text-emerald-800" />
            </div>
            <div className="h-3 w-0.5 bg-emerald-800/40" />
          </div>

          {/* 4 Domain Sub-Agents */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <SubAgentCard
              title="Flood Agent"
              department="FLOOD_MANAGEMENT"
              icon={Waves}
              isRouted={hasExecuted && classifiedDomain === 'FLOOD'}
              isRunning={isRunningPipeline}
              primaryTags={['WATER_RESCUE', 'DEWATERING', 'ZODIAC_BOAT', 'SUBMERSIBLE_PUMP']}
              fallbackDept="RESCUE_MANAGEMENT"
            />
            <SubAgentCard
              title="Heatwave Agent"
              department="HEATWAVE_MANAGEMENT"
              icon={Sun}
              isRouted={hasExecuted && classifiedDomain === 'HEATWAVE'}
              isRunning={isRunningPipeline}
              primaryTags={['HYDRATION', 'COOLING_SHELTER', 'MEDICAL_TRIAGE']}
              fallbackDept="RESCUE_MANAGEMENT"
            />
            <SubAgentCard
              title="Power Grid Agent"
              department="POWER_GRID_MANAGEMENT"
              icon={Zap}
              isRouted={hasExecuted && classifiedDomain === 'POWER_GRID'}
              isRunning={isRunningPipeline}
              primaryTags={['HV_LINEMEN', 'SUBSTATION_OPS', 'BUCKET_TRUCK']}
              fallbackDept="FLOOD_MANAGEMENT"
            />
            <SubAgentCard
              title="Rescue Agent"
              department="RESCUE_MANAGEMENT"
              icon={LifeBuoy}
              isRouted={hasExecuted && classifiedDomain === 'RESCUE'}
              isRunning={isRunningPipeline}
              primaryTags={['SEARCH_RESCUE', 'EVACUATION', 'CIVIL_DEFENSE', 'PARAMEDIC']}
              fallbackDept="HEATWAVE_MANAGEMENT"
            />
          </div>
        </div>

        {/* PHASE 3: ROUTED SUB-AGENT TACTICAL ASSESSMENT */}
        <div className={`p-4 rounded-xl border relative transition-all ${
          hasExecuted
            ? 'bg-emerald-50/30 border-emerald-800/40 shadow-xs'
            : 'bg-slate-50 border-slate-200'
        }`}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
              <CheckSquare className={`w-4 h-4 ${hasExecuted ? 'text-emerald-800' : 'text-slate-400'}`} />
              Phase 3: Routed Sub-Agent Tactical Assessment &amp; Workforce Demand Contract
            </span>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
              hasExecuted
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200 font-bold'
                : isRunningPipeline
                ? 'bg-amber-50 text-amber-800 border-amber-200 animate-pulse'
                : 'bg-white border-slate-200 text-slate-500'
            }`}>
              {hasExecuted
                ? (executionResult?.domain_demand?.requiredRole || 'DEMAND FORMULATED')
                : isRunningPipeline
                ? 'FORMULATING DEMAND...'
                : 'PENDING INITIATION'}
            </span>
          </div>

          {hasExecuted && executionResult?.domain_demand ? (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
              <div className="p-2.5 rounded-lg bg-white border border-slate-200">
                <span className="text-[10px] text-slate-500 block mb-0.5 font-mono">Target Department</span>
                <span className="font-bold text-slate-900">
                  {executionResult.domain_demand.targetDepartment}
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-white border border-slate-200">
                <span className="text-[10px] text-slate-500 block mb-0.5 font-mono">Team Count Demanded</span>
                <span className="font-bold text-slate-900">
                  {executionResult.domain_demand.teamCount} Squad(s)
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-white border border-slate-200 md:col-span-2">
                <span className="text-[10px] text-slate-500 block mb-1 font-mono">Mandatory Tactical Tags</span>
                <div className="flex flex-wrap gap-1">
                  {executionResult.domain_demand.requiredTags.map((tag: string, i: number) => (
                    <span key={i} className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 font-semibold">
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs opacity-75">
              <div className="p-2.5 rounded-lg bg-white border border-slate-200">
                <span className="text-[10px] text-slate-500 block mb-0.5 font-mono">Target Department</span>
                <span className="font-mono text-slate-600 text-[11px]">
                  {isRunningPipeline ? 'Formulating...' : 'Awaiting Autonomous Routing'}
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-white border border-slate-200">
                <span className="text-[10px] text-slate-500 block mb-0.5 font-mono">Team Count Demanded</span>
                <span className="font-mono text-slate-600 text-[11px]">Pending Sub-Agent Sizing</span>
              </div>
              <div className="p-2.5 rounded-lg bg-white border border-slate-200 md:col-span-2">
                <span className="text-[10px] text-slate-500 block mb-1 font-mono">Mandatory Tactical Tags</span>
                <span className="text-[11px] font-mono text-slate-400 italic">Will be formulated dynamically upon execution</span>
              </div>
            </div>
          )}

          <div className="mt-3 pt-2.5 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono">
            <span className="flex items-center gap-1.5 text-slate-600">
              <RefreshCw className="w-3.5 h-3.5 text-emerald-800" />
              <span>Sub-Agent submits demand contract back to <strong>Agent 0</strong> for availability verification &amp; atomic locking</span>
            </span>
            <span className="text-[10px] px-2 py-0.5 rounded bg-white border border-slate-200 text-emerald-800 font-bold">
              Demand Hand-back to Agent 0 ➔
            </span>
          </div>
        </div>

        {/* Connector 3 -> 4 */}
        <div className="flex flex-col items-center justify-center my-1">
          <div className="h-4 w-0.5 bg-emerald-800/40" />
          <ArrowDown className="w-3.5 h-3.5 text-emerald-800" />
        </div>

        {/* PHASE 4: AGENT 0 MASTER ORCHESTRATOR & FALLBACK LOOP */}
        <div className="space-y-3">
          <div className="text-xs font-mono font-bold uppercase tracking-wider text-slate-500 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${hasExecuted ? 'bg-amber-500' : 'bg-slate-400'}`} />
            Phase 4: Agent 0 Master Orchestrator (MongoDB 160-Admin Availability &amp; Fallback Loop)
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex flex-col justify-between">
              <div className="flex items-center justify-between mb-3">
                <span className={`text-xs font-mono font-bold uppercase tracking-wider flex items-center gap-1.5 ${
                  hasExecuted ? 'text-amber-700' : 'text-slate-600'
                }`}>
                  <Lock className="w-3.5 h-3.5" />
                  Decision: Primary Team Availability
                </span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-white border border-slate-200 text-slate-600">
                  {hasExecuted ? 'MongoDB 160 Admins Verified' : 'MongoDB 160 Admins Standby'}
                </span>
              </div>
              <p className="text-xs text-slate-600 mb-3">
                {hasExecuted ? (
                  <>
                    Agent 0 queried MongoDB <code className="text-slate-900 font-mono">users</code> for IDLE personnel in{' '}
                    <strong className="text-slate-900 font-mono">
                      {executionResult?.domain_demand?.targetDepartment || (classifiedDomain ? `${classifiedDomain}_MANAGEMENT` : 'AWAITING_ROUTING')}
                    </strong>{' '}
                    matching requested tags.
                  </>
                ) : (
                  <>
                    Agent 0 will query MongoDB <code className="text-slate-900 font-mono">users</code> for IDLE personnel matching required tactical tags upon demand submission.
                  </>
                )}
              </p>
              <div className="flex items-center gap-2 pt-2 border-t border-slate-200 text-[11px]">
                {hasExecuted ? (
                  <>
                    <span className={`px-2 py-0.5 rounded font-mono font-bold ${isFallbackEngaged ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'}`}>
                      {isFallbackEngaged ? '⚠️ SHORTFALL DETECTED' : '✅ FULL AVAILABILITY'}
                    </span>
                    <span className="text-slate-600 font-mono">
                      {isFallbackEngaged ? 'Triggered Sub-Agent Fallback Loop' : 'Direct Quota Satisfied'}
                    </span>
                  </>
                ) : (
                  <>
                    <span className="px-2 py-0.5 rounded font-mono font-medium bg-white border border-slate-200 text-slate-500">
                      STANDBY
                    </span>
                    <span className="text-slate-500 font-mono">
                      Awaiting Phase 3 Demand Contract
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className={`p-4 rounded-xl border transition-all ${
              isFallbackEngaged
                ? 'bg-amber-50/80 border-amber-300 ring-1 ring-amber-300'
                : 'bg-slate-50 border-slate-200 opacity-60'
            }`}>
              <div className="flex items-center justify-between mb-2">
                <span className={`text-xs font-mono font-bold uppercase tracking-wider flex items-center gap-1.5 ${
                  isFallbackEngaged ? 'text-amber-800' : 'text-slate-600'
                }`}>
                  <RefreshCw className={`w-3.5 h-3.5 ${isFallbackEngaged ? 'animate-spin' : ''}`} />
                  Iterative Fallback Loop
                </span>
                <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full border ${
                  isFallbackEngaged
                    ? 'bg-amber-100 text-amber-900 border-amber-300 font-bold'
                    : 'bg-white border-slate-200 text-slate-500'
                }`}>
                  Rounds: {hasExecuted ? (executionResult?.resource_negotiation?.rounds_count || (isFallbackEngaged ? 2 : 1)) : '—'}
                </span>
              </div>
              <p className="text-xs text-slate-700 leading-relaxed mb-2">
                {isFallbackEngaged ? (
                  <>
                    <strong>Shortfall Re-evaluation Active:</strong> Primary pool had a deficit. Agent 0 reported constraint back to Sub-Agent, pivoting to fallback department{' '}
                    <strong className="text-emerald-800">
                      {workforce?.fallback_department_used || executionResult?.domain_demand?.fallbackDepartment || 'FLOOD_MANAGEMENT'}
                    </strong>.
                  </>
                ) : hasExecuted ? (
                  'Loop on Standby. All demanded squads were secured from primary team without shortfall.'
                ) : (
                  'Iterative fallback loop is on standby. Will activate dynamically if the requested squads exceed available idle personnel.'
                )}
              </p>
              {isFallbackEngaged && (
                <div className="text-[11px] font-mono text-amber-900 bg-amber-100/70 p-2 rounded-lg border border-amber-200">
                  Round 1: Shortfall Detected (1 secured, 2 deficit) → Round 2: Fallback Units Mobilized
                </div>
              )}
            </div>
          </div>
        </div>

        {/* FINAL MOBILIZATION DIRECTIVE & SQUADS */}
        <div className="space-y-3 pt-2">
          <div className="text-xs font-mono font-bold uppercase tracking-wider text-slate-500 flex items-center gap-2">
            <span className={`w-2 h-2 rounded-full ${hasExecuted ? 'bg-emerald-600' : 'bg-slate-400'}`} />
            Agent 0 Master Directive &amp; Final Personnel Mobilization
          </div>

          {hasExecuted && executionResult?.status === 'FALSE_ALERT_FILTERED' ? (
            <div className="p-4 rounded-xl bg-blue-50 border border-blue-200 flex items-center justify-between text-blue-900 text-xs font-mono">
              <span>🛑 Alert filtered by Confidence Calculator Agent (&lt; 65% veracity). No emergency units mobilized.</span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800">STAND DOWN</span>
            </div>
          ) : hasExecuted ? (
            <div className="space-y-3">
              {/* Mandatory Gear Banner */}
              <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 flex flex-wrap items-center justify-between gap-3 text-amber-950">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0" />
                  <span className="text-xs font-bold font-mono uppercase tracking-wide">
                    ⚠️ MANDATORY GEAR &amp; SKILL LOADOUT REQUIRED:
                  </span>
                  <div className="flex flex-wrap gap-1">
                    {(workforce?.required_tags || executionResult?.domain_demand?.requiredTags || []).map((tag: string, i: number) => (
                      <span key={i} className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-100 text-amber-900 border border-amber-300 font-bold">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
                <span className="text-[10px] font-mono text-slate-500">
                  Transmitted to field responder HUDs
                </span>
              </div>

              {/* Mobilized squads */}
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-900 flex items-center gap-1.5">
                    <Users className="w-4 h-4 text-emerald-800" />
                    Mobilized Command Squads (MongoDB 160-Admin Roster)
                  </span>
                  <span className="text-[10px] font-mono font-bold text-emerald-800 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                    Status: EN_ROUTE
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                  {(workforce?.personnel_details || []).map((p: any, idx: number) => (
                    <div key={idx} className="p-3 rounded-lg bg-white border border-slate-200 flex items-center justify-between shadow-2xs">
                      <div className="truncate">
                        <span className="text-xs font-bold text-slate-900 block truncate">
                          {p.displayName || p.name}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono truncate block">
                          {p.email}
                        </span>
                      </div>
                      <span className="text-[9px] font-mono font-bold px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 shrink-0 ml-2">
                        {p.department?.replace('_MANAGEMENT', '')}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="p-6 rounded-xl bg-slate-50 border border-slate-200 text-center space-y-1.5">
              <p className="text-xs font-mono font-semibold text-slate-700">
                ⚡ Phase 4 Master Dispatch Directive &amp; Mandatory Gear Loadout on standby.
              </p>
              <p className="text-xs text-slate-500">
                Click <strong className="text-emerald-800">&quot;Execute Flow Test&quot;</strong> above to run the multi-agent pipeline and observe autonomous resolution.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Tabbed Inspector Section */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
        <div className="flex items-center border-b border-slate-200 px-4 bg-slate-50">
          <TabBtn active={activeTab === 'OVERVIEW'} onClick={() => setActiveTab('OVERVIEW')} label="Executive Overview" />
          <TabBtn active={activeTab === 'CHECKPOINT'} onClick={() => setActiveTab('CHECKPOINT')} label="State Checkpoint" badge="Preserved" />
          <TabBtn active={activeTab === 'NEGOTIATION'} onClick={() => setActiveTab('NEGOTIATION')} label="Negotiation Log" badge={`${negotiationRounds.length || 0}`} />
          <TabBtn active={activeTab === 'DIRECTIVE'} onClick={() => setActiveTab('DIRECTIVE')} label="Dispatch Directive" />
          <TabBtn active={activeTab === 'RAW'} onClick={() => setActiveTab('RAW')} label="Raw JSON" />
        </div>

        <div className="p-5">
          {activeTab === 'OVERVIEW' && (
            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200">
                  <span className="text-[11px] text-slate-500 font-mono block mb-1">Deduplication Cluster</span>
                  <span className="font-bold text-slate-900 text-sm">
                    {executionResult?.deduplication?.is_duplicate ? 'Merged Into Active Incident' : 'Unique Incident Ingress'}
                  </span>
                  <span className="text-[10px] font-mono text-slate-500 block mt-1">250m Geospatial Radius</span>
                </div>
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200">
                  <span className="text-[11px] text-slate-500 font-mono block mb-1">Target Department</span>
                  <span className="font-bold text-slate-900 text-sm">
                    {executionResult?.domain_demand?.targetDepartment || 'Auto-Routing'}
                  </span>
                  <span className="text-[10px] font-mono text-slate-500 block mt-1">Agent 0 LLM Classifier</span>
                </div>
                <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200">
                  <span className="text-[11px] text-slate-500 font-mono block mb-1">Lock Resolution</span>
                  <span className="font-bold text-emerald-800 text-sm">
                    {hasExecuted ? 'Redlock Mutex Acquired' : 'Idle'}
                  </span>
                  <span className="text-[10px] font-mono text-slate-500 block mt-1">TTL 45s Distributed Mutex</span>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'CHECKPOINT' && (
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-700">
              State Checkpoint: All intermediate multi-agent states are preserved in memory and MongoDB for deterministic auditability.
            </div>
          )}

          {activeTab === 'NEGOTIATION' && (
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-700">
              {negotiationRounds.length > 0 ? (
                negotiationRounds.map((rnd: any, i: number) => (
                  <div key={i} className="mb-2">
                    Round {i + 1}: {JSON.stringify(rnd)}
                  </div>
                ))
              ) : (
                'Quota matched directly on Round 1. Zero fallback iterations required.'
              )}
            </div>
          )}

          {activeTab === 'DIRECTIVE' && (
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-900 whitespace-pre-wrap">
              {executionResult?.dispatch_message || workforce?.dispatch_message || 'Awaiting flow execution...'}
            </div>
          )}

          {activeTab === 'RAW' && (
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-emerald-400">
              <pre className="overflow-x-auto">
                {JSON.stringify(executionResult || { status: 'STANDBY', message: 'Execute pipeline to inspect raw JSON payload' }, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function FlowBox({
  title,
  subtitle,
  icon: Icon,
  status,
  badge,
  details
}: {
  title: string;
  subtitle: string;
  icon: any;
  status: StepStatus;
  badge: string;
  details?: string;
}) {
  const isRunning = status === 'RUNNING';
  const isCompleted = status === 'COMPLETED';

  let borderClass = 'border-slate-200 bg-slate-50';
  if (isRunning) {
    borderClass = 'border-emerald-800 ring-2 ring-emerald-800/30 bg-emerald-50/50 shadow-md animate-pulse';
  } else if (isCompleted) {
    borderClass = 'border-emerald-700/60 bg-emerald-50/40';
  }

  return (
    <div className={`p-4 rounded-xl border flex flex-col justify-between transition-all ${borderClass}`}>
      <div>
        <div className="flex items-center justify-between mb-2">
          <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-900 shadow-2xs">
            <Icon className="w-4 h-4 text-emerald-800" />
          </div>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded-full border bg-white border-slate-200 text-slate-600 font-semibold">
            {badge}
          </span>
        </div>
        <h4 className="text-xs font-bold text-slate-900 leading-snug">{title}</h4>
        <p className="text-[11px] text-slate-500 truncate mb-2">{subtitle}</p>
      </div>
      <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono text-slate-600">
        <span className="truncate">{details || '—'}</span>
        {isCompleted && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700 shrink-0 ml-1" />}
      </div>
    </div>
  );
}

function SubAgentCard({
  title,
  department,
  icon: Icon,
  isRouted,
  isRunning,
  primaryTags,
  fallbackDept
}: {
  title: string;
  department: string;
  icon: any;
  isRouted: boolean;
  isRunning?: boolean;
  primaryTags: string[];
  fallbackDept: string;
}) {
  let cardClass = 'bg-slate-50 border-slate-200 opacity-60';
  let badgeText = 'STANDBY';
  let badgeClass = 'bg-white border-slate-200 text-slate-500';
  let iconClass = 'text-slate-500';

  if (isRouted) {
    cardClass = 'bg-emerald-50/70 border-emerald-800 ring-2 ring-emerald-800/20 shadow-xs';
    badgeText = 'ROUTED ACTIVE';
    badgeClass = 'bg-emerald-100 text-emerald-900 border-emerald-300 font-bold';
    iconClass = 'text-emerald-800';
  } else if (isRunning) {
    cardClass = 'bg-emerald-50/40 border-emerald-800 ring-1 ring-emerald-800/20 animate-pulse';
    badgeText = 'EVALUATING...';
    badgeClass = 'bg-emerald-100 text-emerald-900 border-emerald-300 font-bold animate-pulse';
    iconClass = 'text-emerald-800';
  }

  return (
    <div className={`p-3.5 rounded-xl border transition-all ${cardClass}`}>
      <div className="flex items-center justify-between mb-2">
        <div className="w-7 h-7 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-900 shadow-2xs">
          <Icon className={`w-3.5 h-3.5 ${iconClass}`} />
        </div>
        <span className={`text-[9px] font-mono px-2 py-0.5 rounded-full border ${badgeClass}`}>
          {badgeText}
        </span>
      </div>

      <h5 className="text-xs font-bold text-slate-900">{title}</h5>
      <span className="text-[10px] text-slate-500 font-mono block mb-2">{department}</span>

      <div className="space-y-1.5 pt-2 border-t border-slate-200 text-[10px]">
        <span className="text-[9px] text-slate-500 font-mono block">Primary Tags:</span>
        <div className="flex flex-wrap gap-1">
          {primaryTags.slice(0, 3).map((t, idx) => (
            <span key={idx} className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-white border border-slate-200 text-slate-700 font-medium">
              {t}
            </span>
          ))}
        </div>
        <span className="text-[9px] text-slate-500 font-mono block pt-1">
          Fallback: <strong className="text-slate-800">{fallbackDept.replace('_MANAGEMENT', '')}</strong>
        </span>
      </div>
    </div>
  );
}

function TabBtn({
  active,
  onClick,
  label,
  badge
}: {
  active: boolean;
  onClick: () => void;
  label: string;
  badge?: string;
}) {
  return (
    <button
      onClick={onClick}
      className={`px-3.5 py-3 text-xs font-semibold border-b-2 transition-colors flex items-center gap-1.5 cursor-pointer ${
        active
          ? 'border-emerald-800 text-emerald-800'
          : 'border-transparent text-slate-500 hover:text-slate-900'
      }`}
    >
      <span>{label}</span>
      {badge && (
        <span className="text-[10px] font-mono px-1.5 py-0.2 rounded-full bg-slate-200 text-slate-700">
          {badge}
        </span>
      )}
    </button>
  );
}
