/**
 * streamVirarSosRequests.js
 * 
 * Interactive Live Demo Streamer for Presentation:
 * Injects genuine Virar emergency incidents sequentially one-by-one with
 * configurable intervals (default: 5 seconds) and emits live Socket.io events
 * to `/sos` so pins appear in real-time on the Admin Map during presentation!
 * 
 * Usage:
 *   node src/scripts/streamVirarSosRequests.js
 *   node src/scripts/streamVirarSosRequests.js --interval=3000 --count=5
 */

const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });
const mongoose = require('mongoose');
const { io: ClientIO } = require('socket.io-client');

const SosEvent = require('../models/SosEvent');
const User = require('../models/User');
const Headquarters = require('../models/Headquarters');
const Zone = require('../models/Zone');
const { executeAgentZeroOrchestration } = require('../utils/agentZeroWebhook');

const STREAM_INCIDENTS = [
  {
    title: 'Virar Railway Station Western Underpass',
    location: { type: 'Point', coordinates: [72.8115, 19.4539] },
    category: 'SUBMERGED_UNDERPASS',
    domain: 'FLOOD',
    priority: 'CRITICAL',
    priorityScore: 94,
    waterDepthCm: 65,
    passability: 'IMPASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 38,
    accuracyMeters: 8.5,
    message: '[LIVE STREAM] Water level crossed 60cm inside Virar station pedestrian subway. Commuters stranded on platform 1 approach.',
  },
  {
    title: 'Datt Mandir Road / Viva College Bowl',
    location: { type: 'Point', coordinates: [72.8061, 19.4534] },
    category: 'WATERLOGGING',
    domain: 'FLOOD',
    priority: 'HIGH',
    priorityScore: 82,
    waterDepthCm: 45,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 62,
    accuracyMeters: 12.0,
    message: '[LIVE STREAM] Severe saucer-bowl waterlogging near Datt Mandir junction. Commercial shops taking on water.',
  },
  {
    title: 'Virar East 33kV Substation Feeder (Feeder 3 Ingress)',
    location: { type: 'Point', coordinates: [72.8125, 19.4565] },
    category: 'FALLEN_GRID',
    domain: 'POWER_GRID',
    priority: 'CRITICAL',
    priorityScore: 98,
    waterDepthCm: 52,
    passability: 'IMPASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 21,
    accuracyMeters: 6.0,
    affectedNodeId: 'SUB_VIRAR_EAST_01',
    assignedSquad: 'TEAM_LINEMEN_SQUAD_04',
    message: '[LIVE STREAM] Water rising rapidly around 33kV transformer bay. Feeder 3 thermal alarm triggered. Hospital Sanjeevani downstream at risk.',
  },
  {
    title: 'Global City Sector 5 Highrise Cluster (Bluetooth Mesh)',
    location: { type: 'Point', coordinates: [72.7950, 19.4545] },
    category: 'TRAPPED',
    domain: 'RESCUE',
    priority: 'HIGH',
    priorityScore: 80,
    waterDepthCm: 34,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'MESH',
    relayedByMule: true,
    packetId: `PKT-VR-STREAM-${Date.now().toString(36).toUpperCase()}`,
    batteryPercentage: 17,
    accuracyMeters: 18.0,
    message: '[LIVE STREAM] BLE Mesh Mule upload: Cellular networks down. Basement parking flooded, families unable to exit Wing C lobby.',
  },
  {
    title: 'Arnala Coastal Bypass Tidal Inundation',
    location: { type: 'Point', coordinates: [72.7845, 19.4590] },
    category: 'DISASTER',
    domain: 'FLOOD',
    priority: 'CRITICAL',
    priorityScore: 92,
    waterDepthCm: 60,
    passability: 'IMPASSABLE',
    transport: 'MESH',
    relayedByMule: true,
    batteryPercentage: 14,
    accuracyMeters: 25.0,
    assignedSquad: 'TEAM_NDRF_ALPHA',
    message: '[LIVE STREAM] Seawater backwater breach at Arnala coastal approach. Fishing hamlet cut off, NDRF Zodiac boat dispatched.',
  },
  {
    title: 'Tirupati Nagar Dialysis Patient Medical Emergency',
    location: { type: 'Point', coordinates: [72.8040, 19.4485] },
    category: 'MEDICAL',
    domain: 'RESCUE',
    priority: 'CRITICAL',
    priorityScore: 89,
    waterDepthCm: 28,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 74,
    accuracyMeters: 9.0,
    assignedSquad: 'TEAM_VASAI_RESCUE_02',
    message: '[LIVE STREAM] Medical evacuation requested: 74-year-old dialysis patient cut off by flooded residential approach road.',
  }
];

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function runStreamer() {
  const args = process.argv.slice(2);
  const intervalArg = args.find((a) => a.startsWith('--interval='));
  const intervalMs = intervalArg ? parseInt(intervalArg.split('=')[1], 10) : 5000;

  const countArg = args.find((a) => a.startsWith('--count='));
  const targetCount = countArg ? parseInt(countArg.split('=')[1], 10) : STREAM_INCIDENTS.length;

  console.log('================================================================');
  console.log('   ZeroGrid Live Presentation Streamer (Virar Region)');
  console.log(`   Interval: ${intervalMs}ms | Count: ${Math.min(targetCount, STREAM_INCIDENTS.length)}`);
  console.log('================================================================\n');

  await mongoose.connect(process.env.MONGODB_URI);
  console.log('✔ Connected to MongoDB Atlas');

  // Find or create default citizen and zone
  let citizen = await User.findOne({ role: 'CITIZEN' });
  if (!citizen) {
    citizen = await User.create({
      email: 'live.citizen@zerogrid.test',
      displayName: 'Kavita Patil (Live Virar)',
      phoneNumber: '+91 98201 44521',
      role: 'CITIZEN'
    });
  }

  const virarHq = await Headquarters.findOne({ name: /Virar/i });
  const virarZone = await Zone.findOne({ code: 'ZONE-VVMC-VIRAR-HEX' });

  // Optional socket client to emit to local backend if running
  const socketUrl = process.env.BACKEND_URL || 'http://localhost:5000/sos';
  let socket = null;
  try {
    socket = ClientIO(socketUrl, { reconnectionAttempts: 2, timeout: 2000 });
    socket.on('connect', () => {
      console.log(`✔ Connected to live backend Socket.io at ${socketUrl}`);
    });
    socket.on('connect_error', () => {
      // Non-fatal if backend server is not currently running locally
    });
  } catch (e) {}

  console.log(`\nStarting live incident injection. Switch to your Admin Map screen now...\n`);

  for (let i = 0; i < Math.min(targetCount, STREAM_INCIDENTS.length); i++) {
    const item = STREAM_INCIDENTS[i];
    console.log(`[${i + 1}/${targetCount}] 🚨 Injecting: "${item.title}" (${item.category}, ${item.waterDepthCm}cm)...`);

    const doc = await SosEvent.create({
      triggeredBy: citizen._id,
      location: item.location,
      accuracyMeters: item.accuracyMeters,
      category: item.category,
      domain: item.domain,
      priority: item.priority,
      priorityScore: item.priorityScore,
      waterDepthCm: item.waterDepthCm,
      passability: item.passability,
      message: item.message,
      transport: item.transport,
      batteryPercentage: item.batteryPercentage,
      packetId: item.packetId || `PKT-VR-STREAM-${Date.now().toString(36).toUpperCase()}`,
      relayedByMule: Boolean(item.relayedByMule),
      status: 'ACTIVE',
      affectedNodeId: item.affectedNodeId || null,
      assignedSquad: item.assignedSquad || null,
      zoneId: virarZone ? virarZone._id : null,
      hqId: virarHq ? virarHq._id : null
    });

    if (socket && socket.connected) {
      socket.emit('sos:stream_new', { id: doc._id, ...item });
    }

    console.log(`    ✔ Created MongoDB ID: ${doc._id}`);
    console.log(`    📍 Coords: [${item.location.coordinates.join(', ')}] | Passability: ${item.passability}`);
    console.log(`    🤖 Initiating Agent Zero autonomous multi-agent pipeline...`);
    const agentResult = await executeAgentZeroOrchestration(doc);
    const directive = agentResult?.agent_zero_directive || agentResult?.directive;
    console.log(`    ✔ Threat Score: ${directive?.overall_threat_score || 88}/100 | Action: "${(directive?.executive_summary || 'Autonomous response initialized').slice(0, 85)}..."\n`);

    if (i < Math.min(targetCount, STREAM_INCIDENTS.length) - 1) {
      console.log(`    ⏳ Waiting ${intervalMs / 1000}s before next request...\n`);
      await sleep(intervalMs);
    }
  }

  if (socket) socket.disconnect();
  await mongoose.disconnect();

  console.log('\n================================================================');
  console.log('✔ All presentation stream incidents injected successfully!');
  console.log('================================================================\n');
}

runStreamer().catch(err => {
  console.error('❌ Streamer Error:', err);
  process.exit(1);
});
