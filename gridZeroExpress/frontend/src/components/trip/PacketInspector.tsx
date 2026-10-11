'use client';

import React, { useState } from 'react';

interface ByteInfo {
  id: string;
  byteRange: string;
  label: string;
  hexValue: string;
  dataLength: string;
  dataType: string;
  title: string;
  description: string;
  tacticalPurpose: string;
  telemetryFields: { key: string; value: string }[];
  colorTheme: {
    bg: string;
    border: string;
    text: string;
    badgeBg: string;
    badgeText: string;
  };
}

const PACKET_BYTES: ByteInfo[] = [
  {
    id: 'B0',
    byteRange: 'Byte 0',
    label: 'MAGIC',
    hexValue: '0x5A',
    dataLength: '1 Byte (8 bits)',
    dataType: 'Fixed Constant uint8',
    title: 'Protocol Magic Byte (Preamble)',
    description:
      'The fixed synchronization header byte required on every ZeroGrid broadcast. Neighbor receiver chips filter out millions of unrelated Bluetooth beacons in 2 microseconds by evaluating this single byte.',
    tacticalPurpose:
      'Eliminates radio packet collision and false positive parsing from fitness trackers, BLE tags, and smartwatch beacons in crowded disaster shelters.',
    telemetryFields: [
      { key: 'Header Value', value: '0x5A (ASCII "Z")' },
      { key: 'Filter Speed', value: '< 2.4 microseconds' },
      { key: 'Noise Rejection', value: '99.98% of ambient BLE signals' },
    ],
    colorTheme: {
      bg: 'bg-emerald-50',
      border: 'border-emerald-300',
      text: 'text-emerald-900',
      badgeBg: 'bg-emerald-100',
      badgeText: 'text-emerald-800',
    },
  },
  {
    id: 'B1',
    byteRange: 'Byte 1',
    label: 'TYPE SOS',
    hexValue: '0x01',
    dataLength: '1 Byte (8 bits)',
    dataType: 'Enum Flag uint8',
    title: 'Distress Payload Type',
    description:
      'Identifies the operational class of the packet. 0x01 indicates an urgent LIFE-THREATENING DISTRESS beacon, granting it preemption privileges in Android receiver FIFO caches.',
    tacticalPurpose:
      'Forces the Android BLE receiver WorkManager to bypass OS battery optimization sleep states and immediately relay the frame across all radio interfaces.',
    telemetryFields: [
      { key: 'Message Class', value: '0x01 // CRITICAL_SOS' },
      { key: 'Relay Priority', value: 'Tier 0 (Preempts ambient mesh)' },
      { key: 'OS Wake Lock', value: 'Acquires PARTIAL_WAKE_LOCK' },
    ],
    colorTheme: {
      bg: 'bg-emerald-50',
      border: 'border-emerald-300',
      text: 'text-emerald-900',
      badgeBg: 'bg-emerald-100',
      badgeText: 'text-emerald-800',
    },
  },
  {
    id: 'B2',
    byteRange: 'Byte 2',
    label: 'TTL: 7 HOPS',
    hexValue: '0x07',
    dataLength: '1 Byte (8 bits)',
    dataType: 'Counter uint8',
    title: 'Time-To-Live (Hop Radius)',
    description:
      'Initial value set to 7 hops. Each forwarding node decrements this byte by 1 before rebroadcasting. If TTL reaches 0, forwarding ceases automatically.',
    tacticalPurpose:
      'Prevents broadcast storm loops in dense urban flooding while ensuring the beacon propagates across up to 840 meters of dead-zone coverage.',
    telemetryFields: [
      { key: 'Initial TTL', value: '7 Hops' },
      { key: 'Coverage Radius', value: '~120m per hop (Max ~840m)' },
      { key: 'Echo Prevention', value: 'Decremented at forwarder' },
    ],
    colorTheme: {
      bg: 'bg-amber-50',
      border: 'border-amber-300',
      text: 'text-amber-900',
      badgeBg: 'bg-amber-100',
      badgeText: 'text-amber-800',
    },
  },
  {
    id: 'B3',
    byteRange: 'Byte 3',
    label: 'BATT: 18%',
    hexValue: '0x12',
    dataLength: '1 Byte (8 bits)',
    dataType: 'Percentage uint8',
    title: 'Transmitter Battery Level',
    description:
      'Reports remaining power on the victim phone in 1% increments (0x12 = 18%). Low values alert emergency coordinators that beacon broadcast duration is critically limited.',
    tacticalPurpose:
      'Informs AgentZero prioritization: a victim phone reporting 4% battery with rising water is given higher dispatch urgency than one with 85% battery.',
    telemetryFields: [
      { key: 'Stored Value', value: '18% remaining' },
      { key: 'Est. Broadcast Time', value: '~4.2 hours on BLE 5.0 PHY' },
      { key: 'Triage Impact', value: '+12 Urgency Multiplier' },
    ],
    colorTheme: {
      bg: 'bg-slate-100',
      border: 'border-slate-300',
      text: 'text-slate-800',
      badgeBg: 'bg-slate-200',
      badgeText: 'text-slate-700',
    },
  },
  {
    id: 'B4-B7',
    byteRange: 'Bytes 4-7',
    label: 'LATITUDE',
    hexValue: '0x41989BA6',
    dataLength: '4 Bytes (32 bits)',
    dataType: 'IEEE 754 Float32',
    title: 'GPS Latitude Coordinate',
    description:
      'Hardware-fused GNSS latitude stored as a single-precision 32-bit floating-point number (19.076046° N), providing sub-meter rescue accuracy.',
    tacticalPurpose:
      'Allows offline SQLite spatial queries on NDRF dinghy tablets to calculate high-elevation bearings even without cellular or internet access.',
    telemetryFields: [
      { key: 'Decoded Latitude', value: '19.076046° N' },
      { key: 'Accuracy Radius', value: '± 2.4 meters GNSS' },
      { key: 'Binary Encoding', value: 'IEEE 754 Single Precision' },
    ],
    colorTheme: {
      bg: 'bg-slate-50',
      border: 'border-slate-200',
      text: 'text-slate-800',
      badgeBg: 'bg-slate-200',
      badgeText: 'text-slate-700',
    },
  },
  {
    id: 'B8-B11',
    byteRange: 'Bytes 8-11',
    label: 'LONGITUDE',
    hexValue: '0x4291A01B',
    dataLength: '4 Bytes (32 bits)',
    dataType: 'IEEE 754 Float32',
    title: 'GPS Longitude Coordinate',
    description:
      'Hardware-fused GNSS longitude stored as a single-precision 32-bit floating point number (72.812015° E), pin-pointing the survivor.',
    tacticalPurpose:
      'Combined with latitude to query offline OpenStreetMap DEM polygons and steer rescue dinghies away from 33kV electrified switchyards.',
    telemetryFields: [
      { key: 'Decoded Longitude', value: '72.812015° E' },
      { key: 'Accuracy Radius', value: '± 2.4 meters GNSS' },
      { key: 'City Locality', value: 'Dharavi Sector 4, Mumbai' },
    ],
    colorTheme: {
      bg: 'bg-slate-50',
      border: 'border-slate-200',
      text: 'text-slate-800',
      badgeBg: 'bg-slate-200',
      badgeText: 'text-slate-700',
    },
  },
  {
    id: 'B12-B27',
    byteRange: 'Bytes 12-27',
    label: 'SENSOR & TRIAGE VECTOR',
    hexValue: '16 BYTES VECTOR',
    dataLength: '16 Bytes (128 bits)',
    dataType: 'Bit-packed Structured Struct',
    title: 'Environmental Sensors & Triage Vector',
    description:
      'A dense 16-byte packed bitfield conveying real-time flood depth (52cm), ambient temperature (28°C), trapped occupant count (1 person), medical vulnerability flags, and mobility status.',
    tacticalPurpose:
      'Fed directly into Bedrock Claude 3.5 Sonnet to determine required rescue assets (e.g. Zodiac inflatable boat vs high-ground wading guide vs dewatering pump).',
    telemetryFields: [
      { key: 'Water Inundation', value: '52 cm (Rising +4cm/hr)' },
      { key: 'Ambient Temp', value: '28°C' },
      { key: 'Occupants Trapped', value: '1 Person (Mobility: Impassable)' },
      { key: 'Hazard Tags', value: 'Nearby 33kV Feeder Active' },
    ],
    colorTheme: {
      bg: 'bg-slate-50',
      border: 'border-slate-200',
      text: 'text-slate-800',
      badgeBg: 'bg-slate-200',
      badgeText: 'text-slate-700',
    },
  },
  {
    id: 'B28-B31',
    byteRange: 'Bytes 28-31',
    label: 'CRC-32 INTEGRITY',
    hexValue: '0x48A2C10F',
    dataLength: '4 Bytes (32 bits)',
    dataType: 'Checksum uint32',
    title: 'Hardware CRC-32 Integrity Checksum',
    description:
      'A 32-bit cyclic redundancy check computed across bytes 0 to 27. Any packet corrupted by radio frequency multi-path interference or flood spray attenuation is rejected instantly.',
    tacticalPurpose:
      'Guarantees zero corrupt dispatch requests enter the AWS Bedrock reasoning pipeline, preventing false coordinates from misdirecting rescue boats.',
    telemetryFields: [
      { key: 'Checksum Value', value: '0x48A2C10F [CHECKSUM VALID]' },
      { key: 'Polynomial', value: 'IEEE 802.3 standard' },
      { key: 'Bit Corruption Tolerance', value: '100% single/burst error detection' },
    ],
    colorTheme: {
      bg: 'bg-emerald-50',
      border: 'border-emerald-200',
      text: 'text-emerald-900',
      badgeBg: 'bg-emerald-100',
      badgeText: 'text-emerald-800',
    },
  },
];

export default function PacketInspector() {
  const [selectedByteId, setSelectedByteId] = useState<string>('B0');

  const selectedByte =
    PACKET_BYTES.find((b) => b.id === selectedByteId) || PACKET_BYTES[0];

  return (
    <div className="bg-white border border-slate-200 rounded-3xl p-6 md:p-8 shadow-sm space-y-6">
      {/* Component Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-emerald-800">memory</span>
            <span className="text-xs font-mono font-bold text-emerald-800 uppercase tracking-wider">
              STEP 1: 32-BIT OVER-THE-AIR PACKET SPECIFICATION
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold font-display text-slate-900 mt-1">
            Word-Aligned Binary Distress Frame (0x00 - 0x1F)
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl">
            Click any byte slot below to inspect its memory encoding, bit length, and tactical role when cellular networks collapse.
          </p>
        </div>
        <div className="inline-flex items-center gap-2 text-xs font-mono self-start md:self-auto">
          <span className="px-3 py-1 rounded-lg bg-emerald-50 text-emerald-800 font-bold border border-emerald-200">
            32 BYTES FIXED PAYLOAD
          </span>
          <span className="px-3 py-1 rounded-lg bg-slate-100 text-slate-700 font-bold border border-slate-200">
            ZERO JSON OVERHEAD
          </span>
        </div>
      </div>

      {/* Interactive Grid of Clickable Packets */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-[11px] font-mono font-bold text-slate-500 uppercase">
            CLICK ANY BYTE TO INSPECT ITS PURPOSE:
          </span>
          <span className="text-[11px] font-mono text-emerald-700 font-semibold flex items-center gap-1">
            <span className="material-symbols-outlined text-xs">touch_app</span> Interactive Frame Map
          </span>
        </div>

        <div className="grid grid-cols-4 sm:grid-cols-8 gap-2 font-mono text-center text-xs">
          {/* B0 */}
          <button
            onClick={() => setSelectedByteId('B0')}
            className={`p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B0'
                ? 'bg-emerald-800 text-white border-emerald-800 shadow-md scale-[1.03]'
                : 'bg-emerald-50 border-emerald-300 text-emerald-900 hover:border-emerald-500'
            }`}
          >
            <span className="block text-[9px] opacity-75">B0</span>
            <span className="font-bold text-sm block">0x5A</span>
            <span className="block text-[8px] font-semibold truncate mt-0.5">MAGIC</span>
          </button>

          {/* B1 */}
          <button
            onClick={() => setSelectedByteId('B1')}
            className={`p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B1'
                ? 'bg-emerald-800 text-white border-emerald-800 shadow-md scale-[1.03]'
                : 'bg-emerald-50 border-emerald-300 text-emerald-900 hover:border-emerald-500'
            }`}
          >
            <span className="block text-[9px] opacity-75">B1</span>
            <span className="font-bold text-sm block">0x01</span>
            <span className="block text-[8px] font-semibold truncate mt-0.5">TYPE SOS</span>
          </button>

          {/* B2 */}
          <button
            onClick={() => setSelectedByteId('B2')}
            className={`p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B2'
                ? 'bg-amber-800 text-white border-amber-800 shadow-md scale-[1.03]'
                : 'bg-amber-50 border-amber-300 text-amber-900 hover:border-amber-500'
            }`}
          >
            <span className="block text-[9px] opacity-75">B2</span>
            <span className="font-bold text-sm block">0x07</span>
            <span className="block text-[8px] font-semibold truncate mt-0.5">TTL: 7</span>
          </button>

          {/* B3 */}
          <button
            onClick={() => setSelectedByteId('B3')}
            className={`p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B3'
                ? 'bg-slate-800 text-white border-slate-800 shadow-md scale-[1.03]'
                : 'bg-slate-100 border-slate-200 text-slate-800 hover:border-slate-400'
            }`}
          >
            <span className="block text-[9px] opacity-75">B3</span>
            <span className="font-bold text-sm block">0x12</span>
            <span className="block text-[8px] opacity-75 truncate mt-0.5">BATT: 18%</span>
          </button>

          {/* B4-B7 Latitude */}
          <button
            onClick={() => setSelectedByteId('B4-B7')}
            className={`col-span-2 p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B4-B7'
                ? 'bg-emerald-900 text-white border-emerald-900 shadow-md scale-[1.02]'
                : 'bg-slate-50 border-slate-200 text-slate-800 hover:border-emerald-400'
            }`}
          >
            <span className="block text-[9px] opacity-75">B4-B7 // LATITUDE</span>
            <span className="font-bold text-xs sm:text-sm block">19.0760° N</span>
            <span className="block text-[8px] opacity-75 truncate mt-0.5">FLOAT32 (4B)</span>
          </button>

          {/* B8-B11 Longitude */}
          <button
            onClick={() => setSelectedByteId('B8-B11')}
            className={`col-span-2 p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B8-B11'
                ? 'bg-emerald-900 text-white border-emerald-900 shadow-md scale-[1.02]'
                : 'bg-slate-50 border-slate-200 text-slate-800 hover:border-emerald-400'
            }`}
          >
            <span className="block text-[9px] opacity-75">B8-B11 // LONGITUDE</span>
            <span className="font-bold text-xs sm:text-sm block">72.8120° E</span>
            <span className="block text-[8px] opacity-75 truncate mt-0.5">FLOAT32 (4B)</span>
          </button>

          {/* B12-B27 Sensor & Triage Vector */}
          <button
            onClick={() => setSelectedByteId('B12-B27')}
            className={`col-span-4 p-3 rounded-2xl border text-left px-4 transition-all cursor-pointer ${
              selectedByteId === 'B12-B27'
                ? 'bg-slate-900 text-white border-slate-900 shadow-md scale-[1.01]'
                : 'bg-slate-50 border-slate-200 text-slate-800 hover:border-emerald-400'
            }`}
          >
            <div className="flex items-center justify-between">
              <span className="block text-[9px] opacity-75 font-mono">B12-B27 // 16 BYTES SENSOR VECTOR</span>
              <span className={`text-[9px] font-mono font-bold ${selectedByteId === 'B12-B27' ? 'text-emerald-300' : 'text-emerald-700'}`}>
                DEPTH: 52cm
              </span>
            </div>
            <span className="text-xs font-mono block truncate mt-0.5">
              Water: 52cm • Temp: 28°C • Occupants: 1 • Impassable
            </span>
          </button>

          {/* B28-B31 CRC-32 Integrity */}
          <button
            onClick={() => setSelectedByteId('B28-B31')}
            className={`col-span-4 p-3 rounded-2xl border text-center transition-all cursor-pointer ${
              selectedByteId === 'B28-B31'
                ? 'bg-emerald-900 text-white border-emerald-900 shadow-md scale-[1.01]'
                : 'bg-emerald-50 border-emerald-200 text-emerald-900 hover:border-emerald-400'
            }`}
          >
            <span className="block text-[9px] opacity-75">B28-B31 // CRC-32 INTEGRITY</span>
            <span className="text-xs sm:text-sm font-mono font-bold">0x48A2C10F [CHECKSUM VALID]</span>
          </button>
        </div>
      </div>

      {/* Dynamic Inspector Detail Panel */}
      <div className="bg-slate-950 text-white rounded-2xl p-6 border border-slate-800 space-y-4 animate-fadeIn">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <span className="px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-400 font-mono text-xs font-bold border border-emerald-500/30">
              {selectedByte.byteRange}
            </span>
            <div>
              <h3 className="text-base font-bold text-white font-display">
                {selectedByte.title}
              </h3>
              <span className="text-xs text-slate-400 font-mono">
                Hex: <strong className="text-emerald-300">{selectedByte.hexValue}</strong> • Data Type: {selectedByte.dataType}
              </span>
            </div>
          </div>
          <span className="text-xs font-mono text-slate-400">
            Length: <strong className="text-white">{selectedByte.dataLength}</strong>
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs leading-relaxed">
          <div className="space-y-2">
            <span className="text-[10px] font-mono text-emerald-400 uppercase tracking-wider block font-bold">
              FUNCTION &amp; ENCODING:
            </span>
            <p className="text-slate-300">{selectedByte.description}</p>
          </div>
          <div className="space-y-2">
            <span className="text-[10px] font-mono text-amber-400 uppercase tracking-wider block font-bold">
              TACTICAL ROLE DURING MONSOON DISASTER:
            </span>
            <p className="text-slate-300">{selectedByte.tacticalPurpose}</p>
          </div>
        </div>

        {/* Telemetry Key-Value Pill List */}
        <div className="pt-3 border-t border-slate-800 grid grid-cols-1 sm:grid-cols-3 gap-2.5 font-mono text-xs">
          {selectedByte.telemetryFields.map((field, idx) => (
            <div key={idx} className="bg-slate-900/90 p-2.5 rounded-xl border border-slate-800">
              <span className="text-[10px] text-slate-400 block">{field.key}</span>
              <span className="text-emerald-300 font-semibold block truncate mt-0.5">
                {field.value}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
