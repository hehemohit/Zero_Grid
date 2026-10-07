package com.example.zerogrid.mesh.engine

/**
 * Types of packets supported by the ZeroGrid Mesh Protocol.
 */
enum class PacketType {
    HEARTBEAT,
    PEER_DISCOVERY,
    DIRECT_MESSAGE,
    CHANNEL_BROADCAST,
    SOS_BEACON,
    HAZARD_BEACON,
    FILE_CHUNK,
    ACK
}
