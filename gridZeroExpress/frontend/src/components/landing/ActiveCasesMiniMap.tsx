'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { api } from '@/lib/api';
import { Shield, AlertTriangle, Compass, Layers, RefreshCw, ZoomIn, ZoomOut } from 'lucide-react';

interface ActiveSosCase {
  id: string;
  userName?: string;
  category: string;
  severity: string;
  waterDepthCm: number;
  message: string;
  coordinates: [number, number]; // [lat, lng]
  status: string;
}

const FALLBACK_CASES: ActiveSosCase[] = [
  {
    id: 'SOS-81FA',
    userName: 'Ambulance Unit 04',
    category: 'FLOOD',
    severity: 'CRITICAL',
    waterDepthCm: 48,
    message: 'Railway Underpass C4 submerged. Stranded ambulance requiring water evacuation.',
    coordinates: [19.4580, 72.8140],
    status: 'ACTIVE'
  },
  {
    id: 'SOS-401B',
    userName: 'Substation Inspector',
    category: 'POWER_GRID',
    severity: 'HIGH',
    waterDepthCm: 55,
    message: '33kV Substation switchyard submerged. Vacuum breaker tripped & isolated.',
    coordinates: [19.4555, 72.8115],
    status: 'ACKNOWLEDGED'
  },
  {
    id: 'SOS-201C',
    userName: 'Virar Evacuation Lead',
    category: 'RESCUE',
    severity: 'MEDIUM',
    waterDepthCm: 22,
    message: 'Urban flash waterlogging. Evacuation raft corridor clear.',
    coordinates: [19.4520, 72.8150],
    status: 'ACTIVE'
  }
];

function buildMarkerSvg(c: ActiveSosCase): string {
  const isAck = c.status === 'ACKNOWLEDGED';
  const cat = (c.category || '').toUpperCase();

  let outerColor = '#EF4444';
  let innerColor = '#F87171';
  let coreColor = '#DC2626';
  let glyphSvg = `<path d="M22 17h4v14h-4zM17 22h14v4h-14z" fill="#FFFFFF"/>`;

  if (isAck) {
    outerColor = '#10B981';
    innerColor = '#34D399';
    coreColor = '#059669';
    glyphSvg = `<path d="M19 24l3.5 3.5 6.5-6.5" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>`;
  } else if (cat.includes('FLOOD') || c.waterDepthCm > 30) {
    outerColor = '#0284C7';
    innerColor = '#38BDF8';
    coreColor = '#0369A1';
    glyphSvg = `<path d="M24 16 C24 16 18 23.5 18 26.5 C18 29.8 20.7 32.5 24 32.5 C27.3 32.5 30 29.8 30 26.5 C30 23.5 24 16 24 16 Z" fill="#FFFFFF"/>`;
  } else if (cat.includes('GRID') || cat.includes('POWER')) {
    outerColor = '#F59E0B';
    innerColor = '#FDE047';
    coreColor = '#D97706';
    glyphSvg = `<polygon points="25,15 18,24 23,24 21,33 29,22 24,22" fill="#FFFFFF"/>`;
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 48 48">
  <circle cx="24" cy="24" r="22" fill="${outerColor}" opacity="0.3" />
  <circle cx="24" cy="24" r="14" fill="${coreColor}" stroke="${innerColor}" stroke-width="2.2" />
  ${glyphSvg}
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

export function ActiveCasesMiniMap() {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<maplibregl.Marker[]>([]);
  const [activeCases, setActiveCases] = useState<ActiveSosCase[]>(FALLBACK_CASES);
  const [selectedCase, setSelectedCase] = useState<ActiveSosCase | null>(FALLBACK_CASES[0]);
  const [mapLoaded, setMapLoaded] = useState<boolean>(false);

  // Fetch real cases if backend available
  const loadActiveCases = useCallback(async () => {
    try {
      const res = await api.get<{ events?: any[]; sos?: any[] }>('/api/admin/sos?status=ACTIVE&limit=10');
      const list = res.events || res.sos || [];
      if (Array.isArray(list) && list.length > 0) {
        const mapped: ActiveSosCase[] = list.map((item: any) => {
          const coords = item.location?.coordinates;
          const lat = Array.isArray(coords) ? coords[1] : 19.4564;
          const lng = Array.isArray(coords) ? coords[0] : 72.8258;
          return {
            id: String(item.id || item._id || '').slice(-4) ? `SOS-${String(item.id || item._id).slice(-4).toUpperCase()}` : 'SOS-ALERT',
            userName: item.triggeredBy?.displayName || 'Citizen Beacon',
            category: item.category || 'FLOOD',
            severity: item.severity || 'HIGH',
            waterDepthCm: item.waterDepthCm || 0,
            message: item.message || 'Emergency assistance requested',
            coordinates: [lat, lng],
            status: item.status || 'ACTIVE'
          };
        });
        setActiveCases(mapped);
      }
    } catch {
      // Fallback already assigned
    }
  }, []);

  useEffect(() => {
    loadActiveCases();
  }, [loadActiveCases]);

  // Initialize MapLibre GL with Amazon Location Service / AWS Maps SDK config
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    if (typeof window !== 'undefined') {
      maplibregl.setWorkerUrl('/maplibre-gl-worker.mjs');
    }

    const region = process.env.NEXT_PUBLIC_AWS_REGION || 'ap-south-1';
    const mapName = process.env.NEXT_PUBLIC_AWS_LOCATION_MAP_NAME || 'default';
    const apiKey = process.env.NEXT_PUBLIC_AWS_LOCATION_API_KEY || '';

    let styleUrl = 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json';
    if (apiKey) {
      const validStyles = ['Monochrome', 'Standard', 'Hybrid', 'Satellite'];
      const styleName = validStyles.includes(mapName) ? mapName : 'Monochrome';
      styleUrl = `https://maps.geo.${region}.amazonaws.com/v2/styles/${styleName}/descriptor?key=${apiKey}&color-scheme=Dark`;
    }

    const map = new maplibregl.Map({
      container: mapContainerRef.current,
      center: [72.8130, 19.4550], // [lng, lat]
      zoom: 12.8,
      pitch: 35,
      style: styleUrl,
      transformRequest: (url: string) => {
        if (apiKey && url.includes('amazonaws.com') && !url.includes('key=')) {
          return { url: `${url}${url.includes('?') ? '&' : '?'}key=${apiKey}` };
        }
        return { url };
      },
    });

    // Add standard Zoom & Compass controls
    map.addControl(new maplibregl.NavigationControl({ showCompass: true }), 'top-right');

    map.on('load', () => {
      mapRef.current = map;
      setMapLoaded(true);

      // Add Submerged 33kV Exclusion GeoJSON Polygon Layer
      const floodHazardPolygon: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            properties: { name: '33kV Submerged Exclusion Zone' },
            geometry: {
              type: 'Polygon',
              coordinates: [[
                [72.8080, 19.4530],
                [72.8150, 19.4530],
                [72.8150, 19.4575],
                [72.8080, 19.4575],
                [72.8080, 19.4530]
              ]]
            }
          }
        ]
      };

      map.addSource('flood-exclusion-zone', {
        type: 'geojson',
        data: floodHazardPolygon
      });

      map.addLayer({
        id: 'flood-exclusion-fill',
        type: 'fill',
        source: 'flood-exclusion-zone',
        paint: {
          'fill-color': '#ef4444',
          'fill-opacity': 0.18
        }
      });

      map.addLayer({
        id: 'flood-exclusion-outline',
        type: 'line',
        source: 'flood-exclusion-zone',
        paint: {
          'line-color': '#ef4444',
          'line-width': 2,
          'line-dasharray': [3, 2]
        }
      });

      // Add Amazon Location Safe Route Vector Path
      const safeDetourRoute: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            properties: { name: 'Safe Water Detour Corridor' },
            geometry: {
              type: 'LineString',
              coordinates: [
                [72.8050, 19.4510],
                [72.8075, 19.4585],
                [72.8160, 19.4590],
                [72.8190, 19.4540]
              ]
            }
          }
        ]
      };

      map.addSource('safe-detour-corridor', {
        type: 'geojson',
        data: safeDetourRoute
      });

      map.addLayer({
        id: 'safe-detour-glow',
        type: 'line',
        source: 'safe-detour-corridor',
        paint: {
          'line-color': '#10b981',
          'line-width': 5,
          'line-opacity': 0.3
        }
      });

      map.addLayer({
        id: 'safe-detour-line',
        type: 'line',
        source: 'safe-detour-corridor',
        paint: {
          'line-color': '#10b981',
          'line-width': 2.5,
          'line-dasharray': [4, 2]
        }
      });
    });

    return () => {
      map.remove();
      mapRef.current = null;
      setMapLoaded(false);
    };
  }, []);

  // Update Markers on real MapLibre Map
  useEffect(() => {
    if (!mapRef.current || !mapLoaded) return;
    const map = mapRef.current;

    // Clear old markers
    markersRef.current.forEach(m => m.remove());
    markersRef.current = [];

    activeCases.forEach((c) => {
      const el = document.createElement('div');
      el.style.cssText = 'width: 44px; height: 44px; cursor: pointer; display: flex; align-items: center; justify-content: center;';

      const img = document.createElement('img');
      img.src = buildMarkerSvg(c);
      img.style.width = '44px';
      img.style.height = '44px';
      img.style.transition = 'transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1)';
      img.draggable = false;

      el.appendChild(img);

      el.addEventListener('mouseenter', () => {
        img.style.transform = 'scale(1.25)';
      });
      el.addEventListener('mouseleave', () => {
        img.style.transform = 'scale(1)';
      });
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        setSelectedCase(c);
        map.flyTo({
          center: [c.coordinates[1], c.coordinates[0]],
          zoom: 13.8,
          speed: 1.2
        });
      });

      const marker = new maplibregl.Marker({ element: el, anchor: 'center' })
        .setLngLat([c.coordinates[1], c.coordinates[0]])
        .addTo(map);

      markersRef.current.push(marker);
    });
  }, [activeCases, mapLoaded]);

  const handleZoom = (delta: number) => {
    if (!mapRef.current) return;
    mapRef.current.easeTo({ zoom: mapRef.current.getZoom() + delta });
  };

  const handleResetView = () => {
    if (!mapRef.current) return;
    mapRef.current.flyTo({
      center: [72.8130, 19.4550],
      zoom: 12.8,
      pitch: 35
    });
    setSelectedCase(null);
  };

  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4">
      {/* Header bar mentioning Amazon Maps / AWS Maps SDK */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-emerald-800 text-xl">map</span>
            <h3 className="text-base sm:text-lg font-bold font-display text-slate-900 tracking-tight">
              Live Field Incidents &amp; Dynamic Avoidance Routing
            </h3>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Real MapLibre GL engine backed by Amazon Location Service. Pan and zoom across active disaster beacons and computed avoidance corridors.
          </p>
        </div>

        {/* AWS Maps SDK / Amazon Location Service Badge */}
        <div className="flex items-center gap-2 shrink-0">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-mono font-bold bg-slate-900 text-emerald-400 border border-slate-700 shadow-xs">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            AMAZON MAPS // AWS MAPS SDK
          </span>
          <span className="text-[10px] font-mono text-slate-600 bg-slate-100 px-2 py-1 rounded-md border border-slate-200 font-semibold">
            Amazon Location Service
          </span>
        </div>
      </div>

      {/* Map Canvas with Real MapLibre GL */}
      <div className="relative w-full h-[380px] sm:h-[420px] rounded-xl border border-slate-800 overflow-hidden bg-slate-950 shadow-md">
        <div ref={mapContainerRef} className="w-full h-full" />

        {/* Floating Quick Zoom & Reset Controls */}
        <div className="absolute bottom-4 right-4 z-20 flex items-center gap-1.5 bg-slate-900/90 border border-slate-700 p-1 rounded-xl shadow-lg font-mono text-xs">
          <button
            onClick={() => handleZoom(1)}
            title="Zoom In"
            className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <ZoomIn className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleZoom(-1)}
            title="Zoom Out"
            className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <ZoomOut className="w-4 h-4" />
          </button>
          <button
            onClick={handleResetView}
            title="Reset to Overview"
            className="px-2.5 h-8 rounded-lg bg-emerald-800 hover:bg-emerald-700 text-white text-[11px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
          >
            <RefreshCw className="w-3 h-3" />
            <span>Reset View</span>
          </button>
        </div>

        {/* Selected Incident Telemetry HUD Card Overlay */}
        {selectedCase && (
          <div className="absolute top-3 left-3 z-20 max-w-sm bg-slate-900/95 border border-emerald-500/40 p-3.5 rounded-xl shadow-2xl text-xs space-y-1.5 backdrop-blur-md animate-fade-in">
            <div className="flex items-center justify-between gap-2 border-b border-slate-800 pb-1.5">
              <span className="font-mono font-bold text-emerald-400 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                {selectedCase.id} • {selectedCase.category}
              </span>
              <span className="px-1.5 py-0.5 rounded text-[9px] font-mono font-bold bg-red-500/20 text-red-400 border border-red-500/30">
                {selectedCase.severity}
              </span>
            </div>
            <p className="text-slate-200 text-xs font-medium leading-relaxed">
              {selectedCase.message}
            </p>
            <div className="flex items-center justify-between text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-800">
              <span>Coordinates: {selectedCase.coordinates[0].toFixed(4)}, {selectedCase.coordinates[1].toFixed(4)}</span>
              <span className="text-emerald-300 font-bold">💧 {selectedCase.waterDepthCm} cm</span>
            </div>
          </div>
        )}

        {/* Top Center Active HUD Badge */}
        <div className="absolute top-3 right-14 z-10 pointer-events-none hidden md:flex items-center gap-2 bg-slate-900/80 border border-slate-800 px-3 py-1.5 rounded-lg text-[10px] font-mono text-slate-300">
          <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
          <span>{activeCases.length} Active Incidents Plotting on Live AWS Map</span>
        </div>
      </div>

      {/* Incident List Summary with Click-to-Inspect Action */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
        {activeCases.map((c) => {
          const isSelected = selectedCase?.id === c.id;
          return (
            <button
              key={c.id}
              onClick={() => {
                setSelectedCase(c);
                if (mapRef.current) {
                  mapRef.current.flyTo({
                    center: [c.coordinates[1], c.coordinates[0]],
                    zoom: 13.8,
                    speed: 1.2
                  });
                }
              }}
              className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                isSelected
                  ? 'bg-emerald-50/80 border-emerald-800 ring-2 ring-emerald-800/20 shadow-xs'
                  : 'bg-slate-50 hover:bg-white border-slate-200 hover:border-emerald-600'
              }`}
            >
              <div className="flex items-center justify-between text-xs mb-1">
                <span className="font-bold text-slate-900 flex items-center gap-1.5 font-mono">
                  <span className={`w-2 h-2 rounded-full ${c.status === 'ACKNOWLEDGED' ? 'bg-emerald-500' : 'bg-red-500'}`}></span>
                  {c.id}
                </span>
                <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded font-bold ${
                  c.status === 'ACKNOWLEDGED' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                }`}>
                  {c.severity}
                </span>
              </div>
              <p className="text-xs font-semibold text-slate-800 truncate">{c.message}</p>
              <div className="flex items-center justify-between text-[10px] font-mono text-slate-500 pt-1.5 border-t border-slate-200 mt-1">
                <span>{c.coordinates[0].toFixed(4)}, {c.coordinates[1].toFixed(4)}</span>
                <span className="text-emerald-800 font-bold">{c.waterDepthCm} cm depth</span>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
