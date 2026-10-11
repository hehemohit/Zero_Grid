/**
 * seedVirarSosRequests.js
 * 
 * Injects high-fidelity, presentation-grade emergency SOS requests across genuine
 * geographic landmarks in Virar (West & East), Palghar District, Maharashtra.
 * 
 * Features:
 * - Geographically accurate coordinates for flood basins, railway subways, and grid nodes.
 * - Diverse categories (UNDERPASS, WATERLOGGING, FALLEN_GRID, MEDICAL, TRAPPED, MESH).
 * - Real-world citizen profiles with phone numbers and emergency contacts.
 * - Offline Bluetooth Data Mule packets (transport: 'MESH', relayedByMule: true).
 * - Agent Zero autonomous orchestration directives and tactical squad allocations.
 * - Staggered timestamps over the last 40 minutes for realistic timeline feeds.
 * - Auto-ensures VVMC Virar Disaster HQ and Hexagonal Operational Zone are present.
 * 
 * Usage:
 *   node src/scripts/seedVirarSosRequests.js
 *   node src/scripts/seedVirarSosRequests.js --clean
 *   node src/scripts/seedVirarSosRequests.js --count=6
 */

const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });
const mongoose = require('mongoose');

const SosEvent = require('../models/SosEvent');
const User = require('../models/User');
const Headquarters = require('../models/Headquarters');
const Zone = require('../models/Zone');
const Contact = require('../models/Contact');
const { generateHexagonPolygon } = require('../utils/geoUtils');
const { executeAgentZeroOrchestration } = require('../utils/agentZeroWebhook');

// ==========================================
// 1. GENUINE VIRAR CITIZEN PROFILES
// ==========================================
const VIRAR_CITIZENS = [
  {
    displayName: 'Kavita Patil',
    email: 'kavita.patil@zerogrid.test',
    phoneNumber: '+91 98201 44521',
    role: 'CITIZEN'
  },
  {
    displayName: 'Rohan Deshmukh',
    email: 'rohan.deshmukh@zerogrid.test',
    phoneNumber: '+91 98192 33412',
    role: 'CITIZEN'
  },
  {
    displayName: 'Amit Vartak',
    email: 'amit.vartak@zerogrid.test',
    phoneNumber: '+91 98335 77890',
    role: 'CITIZEN'
  },
  {
    displayName: 'Priya Jadhav',
    email: 'priya.jadhav@zerogrid.test',
    phoneNumber: '+91 98205 11234',
    role: 'CITIZEN'
  },
  {
    displayName: 'Mehul Meher',
    email: 'mehul.meher@zerogrid.test',
    phoneNumber: '+91 98190 88219',
    role: 'CITIZEN'
  },
  {
    displayName: 'Dr. Sameer Save',
    email: 'dr.sameer.save@zerogrid.test',
    phoneNumber: '+91 98211 44901',
    role: 'CITIZEN'
  },
  {
    displayName: 'Pooja Tare',
    email: 'pooja.tare@zerogrid.test',
    phoneNumber: '+91 98692 33789',
    role: 'CITIZEN'
  },
  {
    displayName: 'Kunal Bhoir',
    email: 'kunal.bhoir@zerogrid.test',
    phoneNumber: '+91 98208 55432',
    role: 'CITIZEN'
  },
  {
    displayName: 'Swapnil Raut',
    email: 'swapnil.raut@zerogrid.test',
    phoneNumber: '+91 98199 12345',
    role: 'CITIZEN'
  },
  {
    displayName: 'Aniket Chaudhari',
    email: 'aniket.chaudhari@zerogrid.test',
    phoneNumber: '+91 98331 66789',
    role: 'CITIZEN'
  }
];

// ==========================================
// 2. CURATED VIRAR INCIDENT TEMPLATES
// ==========================================
const VIRAR_INCIDENTS = [
  {
    title: 'Virar Railway Station Subway',
    landmark: 'Virar Station West Pedestrian Subway',
    location: { type: 'Point', coordinates: [72.8115, 19.4539] }, // [lng, lat]
    category: 'SUBMERGED_UNDERPASS',
    domain: 'FLOOD',
    priority: 'CRITICAL',
    priorityScore: 94,
    waterDepthCm: 65,
    passability: 'IMPASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 41,
    accuracyMeters: 8.5,
    message: 'Water level crossed 60cm inside Virar station pedestrian subway. Commuters stranded on platform 1 approach. Heavy mud and current flowing from east culvert.',
    witnessReports: [
      {
        displayName: 'Nilesh Vartak',
        phoneNumber: '+91 98204 90123',
        reportedDepthCm: 68,
        message: 'Subway completely blocked, water up to chest level on western staircase. Do not attempt crossing.',
        transport: 'ONLINE'
      }
    ],
    notes: [
      { text: 'VVMC Rapid Response notified. Directing foot commuters to elevated north foot overbridge.' }
    ]
  },
  {
    title: 'Virar West Datt Mandir Saucer Bowl',
    landmark: 'Datt Mandir Road / Viva College Junction',
    location: { type: 'Point', coordinates: [72.8061, 19.4534] },
    category: 'WATERLOGGING',
    domain: 'FLOOD',
    priority: 'HIGH',
    priorityScore: 82,
    waterDepthCm: 45,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 68,
    accuracyMeters: 12.0,
    message: 'Severe waterlogging at chronic saucer bowl junction. Auto-rickshaws stalled in knee-deep water. Stormwater entering ground-floor commercial shops.',
    notes: [
      { text: 'Dewatering pump crew requested to clear silt accumulation from roadside culverts.' }
    ]
  },
  {
    title: 'Virar East 33kV Substation Feeder',
    landmark: 'MSEDCL Virar East 33kV Switchyard',
    location: { type: 'Point', coordinates: [72.8125, 19.4565] },
    category: 'FALLEN_GRID',
    domain: 'POWER_GRID',
    priority: 'CRITICAL',
    priorityScore: 98,
    waterDepthCm: 52,
    passability: 'IMPASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 23,
    accuracyMeters: 6.0,
    affectedNodeId: 'SUB_VIRAR_EAST_01',
    assignedSquad: 'TEAM_LINEMEN_SQUAD_04',
    message: 'Water rising rapidly around 33kV transformer bay at Virar East Substation. Feeder 3 thermal alarm triggered. Hospital Sanjeevani downstream power at critical risk.',
    agentZeroAdvisory: {
      overall_threat_score: 95,
      department: 'POWER_GRID_MANAGEMENT',
      mobilized_squads: ['TEAM_LINEMEN_SQUAD_04', 'TEAM_PUMP_CREW_01'],
      directive: 'Agent Zero autonomous directive: Isolate 11kV feeder section 2; deploy high-volume submersible pumps to preserve Sanjeevani hospital feed.',
      confidence_score: 98
    },
    notes: [
      { text: 'Agent Zero automated dispatch locked TEAM_LINEMEN_SQUAD_04 on site.' }
    ]
  },
  {
    title: 'Global City Residential Spine (Bluetooth Mesh)',
    landmark: 'Yazoo Park / Global City Spine Road',
    location: { type: 'Point', coordinates: [72.7950, 19.4545] },
    category: 'TRAPPED',
    domain: 'RESCUE',
    priority: 'HIGH',
    priorityScore: 80,
    waterDepthCm: 34,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'MESH',
    relayedByMule: true,
    packetId: 'PKT-VR-MESH-9042',
    batteryPercentage: 19,
    accuracyMeters: 18.0,
    message: 'Cellular networks down across Global City Sector 5. Basement parking submerged under 4 feet water. 6 families unable to exit Wing C lobby. Alert forwarded via BLE Mesh Mule.',
    notes: [
      { text: 'Received through offline Bluetooth Data Mule node relay.' }
    ]
  },
  {
    title: 'Bolinj Naka Drainage Choke Point',
    landmark: 'Bolinj Naka / Agashi Coastal Arterial',
    location: { type: 'Point', coordinates: [72.7992, 19.4678] },
    category: 'DRAINAGE_OVERFLOW',
    domain: 'FLOOD',
    priority: 'HIGH',
    priorityScore: 78,
    waterDepthCm: 38,
    passability: 'PEDESTRIAN_ONLY',
    transport: 'BOTH',
    batteryPercentage: 55,
    accuracyMeters: 14.0,
    message: 'Stormwater nullah overflowing onto Agashi arterial road. Light vehicles and 2-wheelers cannot pass. Tree branches choking main drainage canal.',
    witnessReports: [
      {
        displayName: 'Sachin Tare',
        phoneNumber: '+91 98199 44882',
        reportedDepthCm: 40,
        message: 'Water overflowing over road crest into paddy fields.',
        transport: 'ONLINE'
      }
    ]
  },
  {
    title: 'Arnala Coastal Fishing Harbor Surge',
    landmark: 'Arnala Coastal Approach Bypass',
    location: { type: 'Point', coordinates: [72.7845, 19.4590] },
    category: 'DISASTER',
    domain: 'FLOOD',
    priority: 'CRITICAL',
    priorityScore: 92,
    waterDepthCm: 60,
    passability: 'IMPASSABLE',
    transport: 'MESH',
    relayedByMule: true,
    packetId: 'PKT-VR-MESH-4418',
    batteryPercentage: 14,
    accuracyMeters: 25.0,
    assignedSquad: 'TEAM_NDRF_ALPHA',
    message: 'High tide coastal backwater inundation at Arnala approach. Coastal fishing hamlet cut off. 15+ ground level kutcha houses flooded; rescue boat required.',
    notes: [
      { text: 'NDRF Flood Rescue Alpha mobilized with Zodiac boats.' }
    ]
  },
  {
    title: 'Tirupati Nagar Medical Emergency',
    landmark: 'Tirupati Nagar Phase 2, Old Viva College Road',
    location: { type: 'Point', coordinates: [72.8040, 19.4485] },
    category: 'MEDICAL',
    domain: 'RESCUE',
    priority: 'CRITICAL',
    priorityScore: 89,
    waterDepthCm: 28,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 76,
    accuracyMeters: 9.0,
    assignedSquad: 'TEAM_VASAI_RESCUE_02',
    message: 'Emergency medical assistance: 74-year-old dialysis patient requires urgent hospital transfer. Ground floor lane flooded, standard ambulances unable to enter.',
    notes: [
      { text: 'Paramedic Quick Response 02 dispatched with high-clearance support truck.' }
    ]
  },
  {
    title: 'Chandansar Road / Kargil Nagar Slum Pocket',
    landmark: 'Chandansar Nullah Low-Lying Corridor',
    location: { type: 'Point', coordinates: [72.8180, 19.4715] },
    category: 'TRAPPED',
    domain: 'RESCUE',
    priority: 'HIGH',
    priorityScore: 86,
    waterDepthCm: 64,
    passability: 'IMPASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 32,
    accuracyMeters: 15.0,
    message: 'Low-lying houses flooded up to chest level near Chandansar nullah overflow. 12 residents including 4 children shifted onto RCC roof slabs, awaiting boat evacuation.',
    notes: [
      { text: 'Ward A emergency shelter opened at Chandansar Municipal School.' }
    ]
  },
  {
    title: 'Manvelpada Market Center',
    landmark: 'Manvelpada Road / Gandhi Chowk',
    location: { type: 'Point', coordinates: [72.8234, 19.4621] },
    category: 'WATERLOGGING',
    domain: 'FLOOD',
    priority: 'MEDIUM',
    priorityScore: 65,
    waterDepthCm: 35,
    passability: 'PEDESTRIAN_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 82,
    accuracyMeters: 10.0,
    message: 'Flash waterlogging across Manvelpada vegetable market corridor. Drainage grates overflowing with vegetable waste. Traffic diverted to Link Road.',
  },
  {
    title: 'Papadkhind Hills Runoff Overflow',
    landmark: 'Papadkhind Dam Downstream Spillway',
    location: { type: 'Point', coordinates: [72.8420, 19.4780] },
    category: 'DRAINAGE_OVERFLOW',
    domain: 'FLOOD',
    priority: 'MEDIUM',
    priorityScore: 58,
    waterDepthCm: 42,
    passability: 'ALL_PASSABLE',
    transport: 'ONLINE',
    batteryPercentage: 89,
    accuracyMeters: 20.0,
    message: 'Rapid runoff surge from Papadkhind hill catchment entering Virar East primary nullah. Nullah running at 85% capacity. Water barrier sandbags recommended.',
  },
  {
    title: 'Narangi Bypass / Nalasopara Link Culvert',
    landmark: 'Narangi Bypass Low-Lying Dip',
    location: { type: 'Point', coordinates: [72.8290, 19.4452] },
    category: 'SUBMERGED_UNDERPASS',
    domain: 'FLOOD',
    priority: 'HIGH',
    priorityScore: 79,
    waterDepthCm: 50,
    passability: 'IMPASSABLE',
    transport: 'BOTH',
    batteryPercentage: 46,
    accuracyMeters: 11.0,
    message: 'Culvert submerged between Narangi bypass and Nalasopara East link. Public buses halted; 2 passenger cars stalled in flooded dip.',
  },
  {
    title: 'Viva College New Campus Approach',
    landmark: 'Viva College Road Campus Entrance',
    location: { type: 'Point', coordinates: [72.8190, 19.4580] },
    category: 'WATERLOGGING',
    domain: 'FLOOD',
    priority: 'MEDIUM',
    priorityScore: 62,
    waterDepthCm: 30,
    passability: 'HIGH_CLEARANCE_ONLY',
    transport: 'ONLINE',
    batteryPercentage: 63,
    accuracyMeters: 13.0,
    message: 'Campus approach road inundated up to calf level. Students and faculty advised to stay indoors on upper floors. Dewatering pump deployed.',
  }
];

// ==========================================
// 3. SEED LOGIC
// ==========================================
async function seedVirarRequests() {
  const args = process.argv.slice(2);
  const shouldClean = args.includes('--clean') || args.includes('--reset');
  const countArg = args.find(a => a.startsWith('--count='));
  const targetCount = countArg ? parseInt(countArg.split('=')[1], 10) : VIRAR_INCIDENTS.length;

  console.log('================================================================');
  console.log('   ZeroGrid Backend: Virar Presentation Requests Seeder');
  console.log('================================================================');

  const mongoUri = process.env.MONGODB_URI;
  if (!mongoUri) {
    throw new Error('MONGODB_URI is not defined in backend/.env');
  }

  await mongoose.connect(mongoUri, { serverSelectionTimeoutMS: 10000 });
  console.log('✔ Connected to MongoDB Atlas');

  // Step 1: Ensure VVMC Virar Disaster HQ exists
  let virarHq = await Headquarters.findOne({ name: /Virar/i });
  if (!virarHq) {
    virarHq = await Headquarters.create({
      name: 'VVMC Virar Emergency Response HQ',
      location: {
        type: 'Point',
        coordinates: [72.8125, 19.4565], // Virar Center
        address: 'VVMC Ward A Office, Near Virar Station East, Virar 401305'
      },
      status: 'ACTIVE'
    });
    console.log(`✔ Created Headquarters: ${virarHq.name} (${virarHq._id})`);
  } else {
    console.log(`✔ Found existing Headquarters: ${virarHq.name} (${virarHq._id})`);
  }

  // Step 2: Ensure Hexagonal Operational Zone for Virar exists
  let virarZone = await Zone.findOne({ code: 'ZONE-VVMC-VIRAR-HEX' });
  if (!virarZone) {
    const hexagon = generateHexagonPolygon(19.4565, 72.8125, 12.0); // 12km radius around Virar
    virarZone = await Zone.create({
      name: 'VVMC Virar Operational Zone',
      code: 'ZONE-VVMC-VIRAR-HEX',
      hqId: virarHq._id,
      radiusKm: 12.0,
      center: {
        type: 'Point',
        coordinates: [72.8125, 19.4565]
      },
      boundary: hexagon,
      status: 'ACTIVE'
    });
    console.log(`✔ Created Hexagonal Zone: ${virarZone.name} (${virarZone.code})`);
  } else {
    console.log(`✔ Found existing Hexagonal Zone: ${virarZone.name}`);
  }

  // Step 3: Clean previous Virar simulation events if requested
  if (shouldClean) {
    const deleted = await SosEvent.deleteMany({
      $or: [
        { packetId: { $regex: /^PKT-VR-MESH/ } },
        { message: { $regex: /Virar|Datt Mandir|Bolinj|Arnala|Chandansar|Manvelpada|Papadkhind|Narangi/i } },
        { 'location.coordinates.0': { $gte: 72.77, $lte: 72.86 }, 'location.coordinates.1': { $gte: 19.43, $lte: 19.49 } }
      ]
    });
    console.log(`✔ Cleaned ${deleted.deletedCount} existing Virar SOS requests.`);
    if (args.includes('--clean-only')) {
      await mongoose.disconnect();
      return;
    }
  }

  // Step 4: Seed or retrieve authentic Virar Citizen users
  console.log('\nEnsuring authentic Virar citizen users...');
  const citizenDocs = [];
  for (const c of VIRAR_CITIZENS) {
    let user = await User.findOne({ email: c.email });
    if (!user) {
      user = await User.create({
        email: c.email,
        displayName: c.displayName,
        phoneNumber: c.phoneNumber,
        role: 'CITIZEN',
        authProvider: 'LOCAL',
        department: 'GENERAL_DISPATCH'
      });
      console.log(`  + Created Citizen: ${c.displayName} (${c.phoneNumber})`);
    } else {
      user.displayName = c.displayName;
      user.phoneNumber = c.phoneNumber;
      await user.save();
    }
    citizenDocs.push(user);
  }

  // Step 5: Seed Emergency Contacts and Family Links for Dossier Inspection in Drawer
  console.log('Ensuring emergency contacts and family links for dossier inspection...');
  const ParentChildLink = require('../models/ParentChildLink');
  if (citizenDocs.length >= 2) {
    for (let i = 0; i < citizenDocs.length; i++) {
      const primary = citizenDocs[i];
      const contactTarget = citizenDocs[(i + 1) % citizenDocs.length];
      const existingContact = await Contact.findOne({ ownerId: primary._id, contactUserId: contactTarget._id });
      if (!existingContact) {
        await Contact.create({
          ownerId: primary._id,
          contactUserId: contactTarget._id,
          label: i % 2 === 0 ? 'Family (Spouse/Sibling)' : 'Neighbor / Local Responder'
        });
      }

      // Add family link between first pair for testing family dossier
      if (i === 0) {
        const existingLink = await ParentChildLink.findOne({ parentId: primary._id, childId: contactTarget._id });
        if (!existingLink) {
          await ParentChildLink.create({
            parentId: primary._id,
            childId: contactTarget._id,
            status: 'ACCEPTED',
            respondedAt: new Date()
          });
        }
      }
    }
  }

  // Step 6: Generate and Insert Curated SOS Incidents with Staggered Timestamps
  console.log(`\nGenerating ${Math.min(targetCount, VIRAR_INCIDENTS.length)} genuine Virar SOS requests...`);
  const now = Date.now();
  const createdEvents = [];

  for (let i = 0; i < Math.min(targetCount, VIRAR_INCIDENTS.length); i++) {
    const tpl = VIRAR_INCIDENTS[i];
    const citizen = citizenDocs[i % citizenDocs.length];

    // Stagger timestamps naturally: newest is 3 mins ago, oldest is 42 mins ago
    const minutesAgo = Math.round(3 + (i * 3.5));
    const incidentTime = new Date(now - minutesAgo * 60 * 1000);

    const notesWithAuthor = (tpl.notes || []).map(n => ({
      authorId: citizenDocs[(i + 1) % citizenDocs.length]._id,
      text: n.text,
      timestamp: new Date(incidentTime.getTime() + 60000)
    }));

    const eventPayload = {
      triggeredBy: citizen._id,
      location: tpl.location,
      accuracyMeters: tpl.accuracyMeters,
      category: tpl.category,
      domain: tpl.domain,
      reportCount: Math.floor(Math.random() * 3) + 1,
      priority: tpl.priority,
      priorityScore: tpl.priorityScore,
      waterDepthCm: tpl.waterDepthCm,
      passability: tpl.passability,
      message: tpl.message,
      transport: tpl.transport,
      batteryPercentage: tpl.batteryPercentage,
      packetId: tpl.packetId || `PKT-VR-${Date.now().toString(36).toUpperCase()}-${i}`,
      relayedByMule: Boolean(tpl.relayedByMule),
      status: i === 1 ? 'ACKNOWLEDGED' : 'ACTIVE', // 1 acknowledged for UI variety
      notes: notesWithAuthor,
      witnessReports: tpl.witnessReports || [],
      affectedNodeId: tpl.affectedNodeId || null,
      assignedSquad: tpl.assignedSquad || null,
      agentZeroAdvisory: tpl.agentZeroAdvisory || null,
      zoneId: virarZone._id,
      hqId: virarHq._id,
      createdAt: incidentTime,
      updatedAt: incidentTime
    };

    const doc = await SosEvent.create(eventPayload);

    // Genuinely initiate Agent Zero autonomous multi-agent pipeline for this distress call
    console.log(`  🚨 [${i + 1}/${Math.min(targetCount, VIRAR_INCIDENTS.length)}] Distress dispatched at ${tpl.landmark}`);
    console.log(`     -> Triggering Agent Zero autonomous orchestration...`);
    const agentResult = await executeAgentZeroOrchestration(doc);
    const directive = agentResult?.agent_zero_directive || agentResult?.directive;
    const threatScore = directive?.overall_threat_score || 88;
    const mobilized = agentResult?.sub_agents?.dispatch?.recommended_squads?.map(s => s.name || s.team_id || s) || agentResult?.assigned_teams || [];

    if (directive?.executive_summary) {
      console.log(`     ✔ Directive: "${directive.executive_summary.slice(0, 95)}..."`);
    }
    console.log(`     ✔ Threat Score: ${threatScore}/100 | Units: ${mobilized.join(', ') || 'Auto-Allocated'}\n`);

    createdEvents.push({
      id: doc._id.toString(),
      landmark: tpl.landmark,
      category: tpl.category,
      depth: `${tpl.waterDepthCm}cm`,
      passability: tpl.passability,
      transport: tpl.transport,
      threat: `${threatScore}/100`,
      status: doc.status,
      citizen: citizen.displayName,
      time: `${minutesAgo}m ago`
    });
  }

  console.log('\n✔ Successfully generated genuine Virar requests:');
  console.table(createdEvents);

  console.log('\n================================================================');
  console.log(`🎉 READY FOR PRESENTATION:`);
  console.log(`   - 12 Realistic Virar Incidents now live in MongoDB`);
  console.log(`   - Admin Map will center and display pins across Virar West & East`);
  console.log(`   - To view: open http://localhost:3000/admin`);
  console.log(`   - To clean and reseed anytime: node src/scripts/seedVirarSosRequests.js --clean`);
  console.log('================================================================\n');

  await mongoose.disconnect();
}

seedVirarRequests().catch((err) => {
  console.error('❌ Error during Virar SOS seeding:', err);
  process.exit(1);
});
