'use client';

import React, { useState, useEffect, useRef } from 'react';

interface MeshNode {
  id: string;
  name: string;
  type: 'VICTIM' | 'BLE_RELAY' | 'ESP32_REPEATER' | 'DATA_MULE' | 'GATEWAY' | 'CLOUD_HQ';
  role: string;
  x: number; // percentage 0-100
  y: number; // percentage 0-100
  battery: number;
  isOnline: boolean;
  statusText: string;
  icon: string;
}

interface PacketHop {
  fromNodeId: string;
  toNodeId: string;
  ttlRemaining: number;
  channel: 'BLE_5_0' | 'WIFI_DIRECT' | 'CELLULAR_UPLINK';
  distanceM: number;
  rssi: number;
  timestamp: string;
}

const INITIAL_NODES: MeshNode[] = [
  {
    id: 'node-victim',
    name: 'Victim Phone (A34)',
    type: 'VICTIM',
    role: 'Distress Origin // Cell Dead',
    x: 8,
    y: 54,
    battery: 18,
    isOnline: true,
    statusText: 'Broadcasting 0x5A Beacon',
    icon: 'phone_android',
  },
  {
    id: 'node-relay-1',
    name: 'Neighbor Phone (X68)',
    type: 'BLE_RELAY',
    role: 'Peer Mesh Hop 1',
    x: 28,
    y: 32,
    battery: 64,
    isOnline: true,
    statusText: '500-Slot LRU Forwarder',
    icon: 'smartphone',
  },
  {
    id: 'node-esp32',
    name: 'Solar ESP32 Relay',
    type: 'ESP32_REPEATER',
    role: 'Rooftop Fixed Mesh Node',
    x: 48,
    y: 68,
    battery: 92,
    isOnline: true,
    statusText: 'LoRa/BLE Dual Radio',
    icon: 'solar_power',
  },
  {
    id: 'node-relay-2',
    name: 'Volunteer Phone',
    type: 'BLE_RELAY',
    role: 'Alternate Sector Hop',
    x: 44,
    y: 22,
    battery: 48,
    isOnline: true,
    statusText: 'Idle Standby Relay',
    icon: 'person_pin_circle',
  },
  {
    id: 'node-mule',
    name: 'NDRF Boat Data Mule',
    type: 'DATA_MULE',
    role: 'Store & Forward Ferry',
    x: 68,
    y: 45,
    battery: 88,
    isOnline: true,
    statusText: 'Buffer & Transport',
    icon: 'sailing',
  },
  {
    id: 'node-gateway',
    name: 'Cell Edge Gateway',
    type: 'GATEWAY',
    role: '4G/Starlink Edge Node',
    x: 88,
    y: 28,
    battery: 99,
    isOnline: true,
    statusText: 'Restored Internet Link',
    icon: 'cell_tower',
  },
  {
    id: 'node-cloud',
    name: 'AWS AgentZero HQ',
    type: 'CLOUD_HQ',
    role: 'Bedrock Multi-Agent Core',
    x: 92,
    y: 76,
    battery: 100,
    isOnline: true,
    statusText: 'Autonomous Dispatch Active',
    icon: 'psychology',
  },
];

export function MeshNetworkSimulator() {
  const [nodes, setNodes] = useState<MeshNode[]>(INITIAL_NODES);
  const [activeStep, setActiveStep] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [radioMode, setRadioMode] = useState<'BLE' | 'WIFI_DIRECT'>('BLE');
  const [hopHistory, setHopHistory] = useState<PacketHop[]>([]);
  const [killedNodeId, setKilledNodeId] = useState<string | null>(null);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Compute active multi-hop path based on whether relay 1 is killed
  const activePath = killedNodeId === 'node-relay-1'
    ? ['node-victim', 'node-esp32', 'node-mule', 'node-gateway', 'node-cloud']
    : killedNodeId === 'node-esp32'
    ? ['node-victim', 'node-relay-1', 'node-relay-2', 'node-mule', 'node-gateway', 'node-cloud']
    : ['node-victim', 'node-relay-1', 'node-mule', 'node-gateway', 'node-cloud'];

  const maxSteps = activePath.length;

  const currentActiveNodeId = activeStep > 0 ? activePath[Math.min(activeStep - 1, activePath.length - 1)] : null;
  const currentTargetNodeId = activeStep < activePath.length ? activePath[activeStep] : null;

  // Progress simulation steps
  const nextStep = () => {
    setActiveStep((prev) => {
      if (prev >= maxSteps) {
        setIsPlaying(false);
        return prev;
      }
      const newStep = prev + 1;
      const fromId = activePath[prev];
      const toId = activePath[newStep];
      if (fromId && toId) {
        const hop: PacketHop = {
          fromNodeId: fromId,
          toNodeId: toId,
          ttlRemaining: 7 - prev,
          channel: toId === 'node-cloud' ? 'CELLULAR_UPLINK' : radioMode === 'BLE' ? 'BLE_5_0' : 'WIFI_DIRECT',
          distanceM: Math.floor(65 + Math.random() * 45),
          rssi: -65 - prev * 4,
          timestamp: new Date().toLocaleTimeString(),
        };
        setHopHistory((h) => [...h, hop]);
      }
      return newStep;
    });
  };

  const resetSimulation = () => {
    setActiveStep(0);
    setIsPlaying(false);
    setHopHistory([]);
    if (timerRef.current) clearInterval(timerRef.current);
  };

  const toggleNodeKill = (nodeId: string) => {
    if (nodeId === 'node-victim' || nodeId === 'node-cloud' || nodeId === 'node-gateway') return;
    setKilledNodeId((prev) => (prev === nodeId ? null : nodeId));
    setNodes((prev) =>
      prev.map((n) => (n.id === nodeId ? { ...n, isOnline: !n.isOnline } : n))
    );
    resetSimulation();
  };

  // Auto-play loop
  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        setActiveStep((step) => {
          if (step >= maxSteps) {
            setIsPlaying(false);
            if (timerRef.current) clearInterval(timerRef.current);
            return step;
          }
          const next = step + 1;
          const fromId = activePath[step];
          const toId = activePath[next];
          if (fromId && toId) {
            const hop: PacketHop = {
              fromNodeId: fromId,
              toNodeId: toId,
              ttlRemaining: 7 - step,
              channel: toId === 'node-cloud' ? 'CELLULAR_UPLINK' : radioMode === 'BLE' ? 'BLE_5_0' : 'WIFI_DIRECT',
              distanceM: Math.floor(70 + Math.random() * 35),
              rssi: -62 - step * 5,
              timestamp: new Date().toLocaleTimeString(),
            };
            setHopHistory((h) => [...h, hop]);
          }
          return next;
        });
      }, 1600);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPlaying, maxSteps, activePath, radioMode]);

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden" id="mesh-simulator">
      {/* Simulator Header */}
      <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/70 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-emerald-800 text-lg">hub</span>
            <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">
              REAL-TIME P2P MESH SIMULATOR // MULTI-HOP PROPAGATION
            </span>
          </div>
          <h3 className="text-lg font-display font-bold text-slate-900 mt-0.5">
            Decentralized RF Gossip &amp; Data Mule Uplink
          </h3>
          <p className="text-xs text-slate-500">
            Simulate offline BLE 5.0 beacon flooding, hop-by-hop TTL decrementing, and store-and-forward relay to AWS cloud.
          </p>
        </div>

        {/* Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Radio Switcher */}
          <div className="inline-flex p-1 bg-white rounded-xl border border-slate-200 text-xs font-semibold">
            <button
              onClick={() => { setRadioMode('BLE'); resetSimulation(); }}
              className={`px-3 py-1 rounded-lg transition-all ${
                radioMode === 'BLE'
                  ? 'bg-emerald-800 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              BLE 5.0 (~120m)
            </button>
            <button
              onClick={() => { setRadioMode('WIFI_DIRECT'); resetSimulation(); }}
              className={`px-3 py-1 rounded-lg transition-all ${
                radioMode === 'WIFI_DIRECT'
                  ? 'bg-emerald-800 text-white shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Wi-Fi Direct (~400m)
            </button>
          </div>

          {/* Action Trigger Buttons */}
          <button
            onClick={() => {
              if (activeStep >= maxSteps) resetSimulation();
              setIsPlaying((p) => !p);
            }}
            className="px-4 py-2 rounded-xl bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 shadow-sm active:scale-95 transition-all"
          >
            <span className="material-symbols-outlined text-sm">
              {isPlaying ? 'pause' : activeStep >= maxSteps ? 'replay' : 'play_arrow'}
            </span>
            <span>{isPlaying ? 'PAUSE' : activeStep >= maxSteps ? 'REPLAY SOS' : activeStep === 0 ? 'START BEACON' : 'RESUME'}</span>
          </button>

          <button
            onClick={nextStep}
            disabled={activeStep >= maxSteps || isPlaying}
            className="px-3 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 text-xs font-semibold text-slate-700 flex items-center gap-1 transition-all"
          >
            <span className="material-symbols-outlined text-sm">skip_next</span>
            <span>STEP HOP</span>
          </button>

          <button
            onClick={resetSimulation}
            className="p-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-500 hover:text-slate-800 transition-all"
            title="Reset Simulator"
          >
            <span className="material-symbols-outlined text-sm">restart_alt</span>
          </button>
        </div>
      </div>

      {/* Simulator Interactive Visual Canvas */}
      <div className="relative h-[380px] bg-slate-950 overflow-hidden select-none">
        {/* Tactical Background Grid */}
        <div className="absolute inset-0 opacity-15 bg-[radial-gradient(#10b981_1px,transparent_1px)] [background-size:24px_24px]"></div>
        
        {/* Simulated Flood Hazard Contours in Canvas */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none">
          <defs>
            <linearGradient id="floodGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#0284c7" stopOpacity="0.15" />
              <stop offset="100%" stopColor="#0369a1" stopOpacity="0.05" />
            </linearGradient>
            <linearGradient id="hopLineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#10b981" />
              <stop offset="100%" stopColor="#34d399" />
            </linearGradient>
          </defs>

          {/* Inundated Zone Polygon */}
          <polygon
            points="20,120 280,70 420,240 260,350 40,320"
            fill="url(#floodGrad)"
            stroke="#0284c7"
            strokeWidth="1.5"
            strokeDasharray="4 4"
            opacity="0.7"
          />

          {/* Safe Corridor Guide Line */}
          <line
            x1="8%" y1="54%" x2="88%" y2="28%"
            stroke="#334155" strokeWidth="1" strokeDasharray="3 3" opacity="0.3"
          />

          {/* Render Active Mesh Topology Lines */}
          {activePath.map((nodeId, idx) => {
            if (idx === 0) return null;
            const prevNode = nodes.find((n) => n.id === activePath[idx - 1]);
            const currNode = nodes.find((n) => n.id === nodeId);
            if (!prevNode || !currNode) return null;

            const isPassedHop = activeStep >= idx;
            const isCurrentHop = activeStep === idx;

            return (
              <g key={`hop-line-${idx}`}>
                <line
                  x1={`${prevNode.x}%`}
                  y1={`${prevNode.y}%`}
                  x2={`${currNode.x}%`}
                  y2={`${currNode.y}%`}
                  stroke={isPassedHop ? '#10b981' : '#334155'}
                  strokeWidth={isPassedHop ? 2.5 : 1.5}
                  strokeDasharray={isPassedHop ? 'none' : '4 4'}
                  opacity={isPassedHop ? 0.9 : 0.4}
                />
                {/* Animated transmission pulse on active hop */}
                {isCurrentHop && (
                  <circle
                    cx={`${(prevNode.x + currNode.x) / 2}%`}
                    cy={`${(prevNode.y + currNode.y) / 2}%`}
                    r="6"
                    fill="#34d399"
                    className="animate-ping"
                  />
                )}
              </g>
            );
          })}
        </svg>

        {/* Flood Hazard Badge in Canvas */}
        <div className="absolute top-3 left-4 z-10 bg-slate-900/90 border border-slate-700/80 px-2.5 py-1 rounded-md text-[10px] font-mono text-cyan-400 flex items-center gap-1.5 shadow-sm">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse"></span>
          <span>SECTOR 4 FLOOD INUNDATION (+1.8m) // CELL INFRA DEAD</span>
        </div>

        {/* Dynamic Topology Reroute Notice */}
        {killedNodeId && (
          <div className="absolute top-3 right-4 z-10 bg-amber-950/80 border border-amber-600/60 px-3 py-1 rounded-md text-[11px] font-mono text-amber-300 flex items-center gap-1.5 shadow-sm">
            <span className="material-symbols-outlined text-xs text-amber-400">alt_route</span>
            <span>NODE FAULT INJECTED: Rerouted via {killedNodeId === 'node-relay-1' ? 'ESP32 Rooftop Node' : 'Alternate Relay'}</span>
          </div>
        )}

        {/* Render Mesh Nodes */}
        {nodes.map((node) => {
          const isCurrentOrigin = currentActiveNodeId === node.id;
          const isCurrentTarget = currentTargetNodeId === node.id;
          const hasReceived = activePath.slice(0, activeStep).includes(node.id);
          const isDead = !node.isOnline;

          return (
            <div
              key={node.id}
              style={{ left: `${node.x}%`, top: `${node.y}%` }}
              onClick={() => toggleNodeKill(node.id)}
              className="absolute -translate-x-1/2 -translate-y-1/2 z-20 cursor-pointer group flex flex-col items-center"
            >
              {/* Pulsing Radio Propagation Ring */}
              {(isCurrentOrigin || (hasReceived && node.type === 'VICTIM')) && !isDead && (
                <div className="absolute -inset-4 rounded-full border border-emerald-500/40 animate-ping [animation-duration:2.5s] pointer-events-none"></div>
              )}
              {isCurrentTarget && !isDead && (
                <div className="absolute -inset-3 rounded-full border border-cyan-400/50 animate-pulse pointer-events-none"></div>
              )}

              {/* Node Circle */}
              <div
                className={`w-11 h-11 rounded-2xl flex items-center justify-center transition-all shadow-lg ${
                  isDead
                    ? 'bg-red-950 border-2 border-red-500 text-red-400'
                    : hasReceived
                    ? 'bg-emerald-900 border-2 border-emerald-400 text-white shadow-emerald-500/30'
                    : isCurrentTarget
                    ? 'bg-cyan-950 border-2 border-cyan-400 text-cyan-300 scale-110 shadow-cyan-500/30'
                    : 'bg-slate-900 border border-slate-700 text-slate-300 group-hover:border-slate-500'
                }`}
              >
                <span className="material-symbols-outlined text-lg">{node.icon}</span>
              </div>

              {/* Node Title & Role Tooltip */}
              <div className="mt-1.5 flex flex-col items-center text-center whitespace-nowrap pointer-events-none">
                <span className={`text-[11px] font-bold tracking-tight ${isDead ? 'text-red-400 line-through' : 'text-slate-200'}`}>
                  {node.name}
                </span>
                <span className="text-[9px] font-mono text-slate-400">
                  {isDead ? 'OFFLINE (CLICK TO RESTORE)' : node.role}
                </span>
              </div>

              {/* Battery Badge */}
              {!isDead && (
                <span
                  className={`mt-0.5 px-1 py-0.2 rounded text-[8px] font-mono font-bold ${
                    node.battery < 20
                      ? 'bg-red-900/70 text-red-300 border border-red-700'
                      : 'bg-slate-800 text-slate-300'
                  }`}
                >
                  🔋 {node.battery}%
                </span>
              )}
            </div>
          );
        })}

        {/* Simulation State Floating Banner */}
        <div className="absolute bottom-3 left-4 z-10 flex items-center gap-2 text-xs font-mono">
          <div className="px-2.5 py-1 rounded bg-slate-900/90 border border-slate-700 text-slate-300">
            PROPAGATION STEP: <span className="font-bold text-emerald-400">{activeStep} / {maxSteps}</span>
          </div>
          {activeStep >= maxSteps && (
            <div className="px-2.5 py-1 rounded bg-emerald-950/90 border border-emerald-600 text-emerald-300 font-bold flex items-center gap-1.5 animate-bounce">
              <span className="material-symbols-outlined text-xs">check_circle</span>
              <span>UPLINK CONFIRMED: AWS AGENTZERO INGESTION COMPLETE</span>
            </div>
          )}
        </div>
      </div>

      {/* Live 32-Byte Packet Inspector HUD & Hop Log */}
      <div className="p-5 border-t border-slate-200 grid grid-cols-1 lg:grid-cols-12 gap-5 bg-white">
        {/* Left: Active Binary Word Inspector */}
        <div className="lg:col-span-7 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold text-slate-700 uppercase flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse"></span>
              ACTIVE 32-BYTE PACKET BUFFER (OVER-THE-AIR BLE 5.0)
            </span>
            <span className="text-[10px] font-mono text-slate-500 bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
              CRC-32: 0x48A2C10F [VERIFIED]
            </span>
          </div>

          <div className="grid grid-cols-4 sm:grid-cols-8 gap-1.5 font-mono text-center text-xs">
            <div className="bg-emerald-50 border border-emerald-300 p-2 rounded-lg text-emerald-900 font-bold">
              <span className="block text-[9px] text-slate-500">B0</span>
              0x5A
              <span className="block text-[8px] truncate">MAGIC</span>
            </div>
            <div className="bg-emerald-50 border border-emerald-300 p-2 rounded-lg text-emerald-900 font-bold">
              <span className="block text-[9px] text-slate-500">B1</span>
              0x01
              <span className="block text-[8px] truncate">SOS</span>
            </div>
            <div className="bg-amber-50 border border-amber-300 p-2 rounded-lg text-amber-900 font-bold">
              <span className="block text-[9px] text-slate-500">B2</span>
              0x0{Math.max(0, 7 - activeStep)}
              <span className="block text-[8px] truncate">TTL: {Math.max(0, 7 - activeStep)}</span>
            </div>
            <div className="bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800">
              <span className="block text-[9px] text-slate-500">B3</span>
              0x12
              <span className="block text-[8px] truncate">BATT: 18%</span>
            </div>
            <div className="col-span-2 bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800">
              <span className="block text-[9px] text-slate-500">B4-B7</span>
              19.4560° N
              <span className="block text-[8px] truncate">LAT float32</span>
            </div>
            <div className="col-span-2 bg-slate-100 border border-slate-200 p-2 rounded-lg text-slate-800">
              <span className="block text-[9px] text-slate-500">B8-B11</span>
              72.8120° E
              <span className="block text-[8px] truncate">LNG float32</span>
            </div>
            <div className="col-span-4 bg-slate-50 border border-slate-200 p-2 rounded-lg text-slate-700 text-left px-2.5">
              <span className="block text-[9px] text-slate-500">B12-B27 // 16-BYTE SENSOR VECTOR</span>
              <span className="text-[11px] font-semibold text-emerald-900">DEPTH: 52cm • TEMP: 28°C • TRAPPED: 1</span>
            </div>
            <div className="col-span-4 bg-emerald-50 border border-emerald-200 p-2 rounded-lg text-emerald-800 font-bold text-center">
              <span className="block text-[9px] text-emerald-600">B28-B31 // CRC-32</span>
              <span className="text-[11px]">0x48A2C10F [VALID]</span>
            </div>
          </div>
        </div>

        {/* Right: Chronological Hop Execution Log */}
        <div className="lg:col-span-5 bg-slate-50 rounded-xl p-3 border border-slate-200 flex flex-col justify-between">
          <div className="flex items-center justify-between pb-2 border-b border-slate-200 text-xs">
            <span className="font-mono font-bold text-slate-800 uppercase flex items-center gap-1">
              <span className="material-symbols-outlined text-emerald-700 text-sm">timeline</span>
              MULTI-HOP AUDIT LOG
            </span>
            <span className="text-[10px] font-mono text-slate-500">
              {hopHistory.length} Recorded Hops
            </span>
          </div>

          <div className="space-y-1.5 my-2 max-h-[110px] overflow-y-auto pr-1">
            {hopHistory.length === 0 ? (
              <div className="text-[11px] text-slate-400 font-mono py-4 text-center">
                Click &quot;START BEACON&quot; to begin mesh gossip simulation...
              </div>
            ) : (
              hopHistory.map((h, i) => {
                const from = nodes.find((n) => n.id === h.fromNodeId)?.name || h.fromNodeId;
                const to = nodes.find((n) => n.id === h.toNodeId)?.name || h.toNodeId;
                return (
                  <div key={i} className="text-[10px] font-mono p-1.5 rounded bg-white border border-slate-200 flex items-center justify-between">
                    <span className="font-bold text-emerald-800">HOP {i + 1}: {from} &rarr; {to}</span>
                    <span className="text-slate-500">{h.distanceM}m ({h.rssi} dBm) // TTL: {h.ttlRemaining}</span>
                  </div>
                );
              })
            )}
          </div>

          <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-[11px] font-mono">
            <span className="text-slate-500">Interactive Tip:</span>
            <span className="text-emerald-800 font-semibold">Click any relay node above to simulate failure</span>
          </div>
        </div>
      </div>
    </div>
  );
}
export default MeshNetworkSimulator;
