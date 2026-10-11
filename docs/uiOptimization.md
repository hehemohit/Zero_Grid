Implementation Plan: 60/120 FPS UI Rendering, Overdraw Elimination & Performance Engineering
Goal Description
Transform ZeroGrid's UI and data pipeline into a 60/120 FPS buttery-smooth, scalable, and resource-efficient mobile application by enforcing the architectural, coding, and profiling standards used by top-tier product-based engineering organizations (Google, Meta, Uber, Netflix).

This plan tackles the four fundamental performance pillars directly within the ZeroGrid codebase:

60/120 FPS Rendering & Recomposition Optimization (Compose compiler stability, skipping, deferred state reads).
GPU Overdraw Elimination & Hierarchy Flattening (collapsing 4x nested backgrounds, eliminating redundant layout passes).
Main Thread (UI Thread) Protection (moving crypto, JSON parsing, file I/O, and mesh packet processing to dedicated background dispatchers).
Lazy Loading & Virtualization (migrating unvirtualized verticalScroll lists to recycled LazyColumn with stable keys and content types).
User Review Required
IMPORTANT

Key Architectural Upgrades:

Virtualization Migration: Screens currently using Column(Modifier.verticalScroll()) for dynamic content (ChannelChatScreen, ChannelsScreen, NearbyDevicesScreen, SosCenterScreen, AdminPanelScreen) will be converted to LazyColumn with explicit key and contentType.
Compose Stability Enforcement: UI model data classes and DTOs (ContactDto, MeshNode, MeshPacket, StoredMessage) will be annotated with @Immutable / @Stable or mapped into lightweight UI state models to allow the Compose runtime to skip recomposing untouched list rows.
Background Overdraw Stripping: Child screen scaffolds and nested boxes will have redundant background colors removed to let the top-level window/scaffold own background rendering, immediately reducing GPU overdraw from 4x down to 1x.
Proposed Changes
Pillar 1: UI Rendering & 60/120 FPS Recomposition Optimization
[MODIFY]
MessagesScreen.kt
Problem: In items(displayPeerIds, key = { it }) { peerId -> ... }, linear find searches, getPeerDisplayName() lookups, and messages.count { !it.isMine } unread calculations execute repeatedly inside the composable item body on every frame.
Fix: Precompute a stable List<ChatSummaryUiModel> in a remember(conversations, filteredPeers, searchQuery) block (or ViewModel), and pass pre-calculated, immutable items to the list.
Deferred State Reading: Replace direct state reads inside layout modifiers with lambda-based modifiers (Modifier.offset { ... } or Modifier.graphicsLayer { ... }) to avoid re-triggering composition when only visual position changes.
[MODIFY]
MeshDashboardScreen.kt
Problem: The entire screen is wrapped inside a single monolithic item { Box { Column { ... } } }, breaking layout recycling and forcing the entire dashboard to measure and compose as one giant chunk.
Fix: Break into discrete item(key = "hero_card"), item(key = "channels"), item(key = "quick_actions"), items(peers, key = { it.nodeId }) with explicit contentType definitions.
[MODIFY] Model Stability & DTOs
Annotate UI models with @Immutable in:
ContactsApiService.kt
(ContactDto, ContactUserDto)
MeshNode.kt
StoredMessage.kt
Ensure Compose compiler marks composable list item rows (ContactItemCard, PeerChatListItem, ChannelRow) as skippable.
Pillar 2: Overdraw Elimination & Layout Hierarchy Flattening
[MODIFY]
NavGraph.kt
& Sub-Screens
Problem: The app currently draws:
window.setBackgroundDrawable (Window layer)
Surface(color = background) (Theme root in MainActivity)
Scaffold(containerColor = background) (ZeroGridApp in NavGraph)
Scaffold(containerColor = background) (Inside each individual screen: SendFileScreen, ChannelsScreen, SettingsScreen, etc.)
Box(Modifier.background(...)) (Cards and containers) Every pixel is drawn 3–5 times per frame before text/icons render.
Fix:
Keep the primary background at the root (ZeroGridApp / MainActivity window).
Change child Scaffold composables in individual screens to use containerColor = Color.Transparent.
Remove redundant BoxWithConstraints wrappers where responsive layout isn't strictly needed, flattening the layout tree from depth 12+ down to depth 5–6.
Pillar 3: Main Thread Protection & Offloading
[MODIFY]
MeshEngine.kt
& Transport Drivers
Ensure packet serialization/deserialization, AES encryption/decryption, and routing graph calculations run strictly on Dispatchers.Default (CPU-bound) or Dispatchers.IO (I/O bound).
Ensure StateFlow emissions (\_conversations, \_connectedPeers) do not trigger massive copy operations on the Main thread.
[MODIFY]
SendFileScreen.kt
& File Transfers
Offload file URI content resolving, file size metadata extraction, chunking, and SHA-256 hashing to Dispatchers.IO.
Pillar 4: Lazy Loading & List Virtualization
[MODIFY] Screen Migrations from Column(verticalScroll) to LazyColumn:
ChannelChatScreen.kt
:
Convert message thread from Column(Modifier.verticalScroll()) to LazyColumn(reverseLayout = true).
Provide unique keys key = { it.messageId } and contentType = { it.senderType }.
ChannelsScreen.kt
:
Convert channel list from unvirtualized Column(Modifier.verticalScroll()) to LazyColumn.
NearbyDevicesScreen.kt
:
Convert scanned peer list to LazyColumn(items(peers, key = { it.nodeId })).
SosCenterScreen.kt
:
Convert active alerts list and broadcast controls to structured LazyColumn items.
AdminPanelScreen.kt
:
Convert user list, mesh metrics, and audit logs to virtualized LazyColumn.
Verification & Profiling Plan
Automated Build & Compiler Verification
Run ./gradlew.bat compileDebugKotlin to ensure clean build.
(Optional) Enable Compose compiler metrics (-PcomposeCompilerReports=true) to inspect stability and skippability of all composables.
Profiling & Runtime Checks (Top-Tier Standard)
Targeting 60/120 FPS:
Enable HWUI rendering profiling in Android Developer Options ("Profile HWUI rendering" -> "On screen as bars").
Green horizontal line represents 16.6ms (60 FPS) / 8.3ms (120 FPS). All vertical bars must remain below the threshold during fast scrolling in chat and peer lists.
Overdraw Elimination:
Enable "Debug GPU overdraw" in Developer Options ("Show overdraw areas").
The screen should display predominantly original colors (1x) and soft blue (2x), with zero red or pink areas on backgrounds.
Main Thread Jitter:
Inspect Android Studio Profiler CPU Trace (System Trace) during message streaming and peer discovery to confirm zero main thread pauses > 5ms.
Virtualization Memory Check:
Profile memory allocation in Android Studio Memory Profiler before and after opening long chat logs (memory footprint should stay flat regardless of message count).
