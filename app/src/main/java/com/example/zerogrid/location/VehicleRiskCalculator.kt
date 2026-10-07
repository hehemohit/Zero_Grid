package com.example.zerogrid.location

/**
 * Pure rule-based vehicle risk calculator. No network calls, no AI.
 *
 * Thresholds are conservative and based on real-world vehicle ground clearances:
 * ─────────────────────────────────────────────────────────────────────────────
 * Vehicle            Safe up to    Notes
 * ─────────────────  ───────────   ──────────────────────────────────────────
 * Two-Wheeler        15 cm         Engine intake ~20–25 cm; stalls quickly
 * Sedan / Hatchback  30 cm         Exhaust ~25–30 cm; ignition kills at 30+
 * SUV / MUV          50 cm         Ground clearance ~20 cm; air intake ~50 cm
 * Truck / Bus        70 cm         High air intake; commercial build
 * ─────────────────────────────────────────────────────────────────────────────
 */
object VehicleRiskCalculator {

    enum class VehicleType(val label: String, val emoji: String, val safeDepthCm: Int) {
        TWO_WHEELER(   "Two-Wheeler",   "🛵", 15),
        SEDAN(         "Sedan / Hatchback", "🚗", 30),
        SUV(           "SUV / MUV",     "🚙", 50),
        TRUCK(         "Truck / Bus",   "🚌", 70)
    }

    enum class RiskLevel(val color: Long) {
        SAFE(    0xFF2E7D32L),   // Dark green
        CAUTION( 0xFFF9A825L),  // Amber
        HIGH(    0xFFE65100L),  // Deep orange
        CRITICAL(0xFFB71C1CL)   // Dark red
    }

    data class RiskAssessment(
        val vehicleType: VehicleType,
        val riskLevel: RiskLevel,
        val headline: String,       // Short bold line
        val detail: String          // Full explanation
    )

    /**
     * Calculates risk for [vehicleType] at the given [waterDepthCm].
     */
    fun assess(vehicleType: VehicleType, waterDepthCm: Int): RiskAssessment {
        val safe    = vehicleType.safeDepthCm
        val diff    = waterDepthCm - safe

        return when {
            waterDepthCm <= safe - 5 -> RiskAssessment(
                vehicleType = vehicleType,
                riskLevel   = RiskLevel.SAFE,
                headline    = "Likely passable",
                detail      = "Current water depth ($waterDepthCm cm) is within safe limits for a ${vehicleType.label}. Proceed with caution and watch for hidden drains."
            )
            waterDepthCm <= safe -> RiskAssessment(
                vehicleType = vehicleType,
                riskLevel   = RiskLevel.CAUTION,
                headline    = "Borderline — use caution",
                detail      = "Water depth ($waterDepthCm cm) is at the upper safe limit for a ${vehicleType.label}. Go slow, avoid sudden stops, and be ready to reverse."
            )
            diff <= 15 -> RiskAssessment(
                vehicleType = vehicleType,
                riskLevel   = RiskLevel.HIGH,
                headline    = "High risk — rerouting recommended",
                detail      = "Water depth ($waterDepthCm cm) exceeds the safe limit for a ${vehicleType.label} by ${diff} cm. Engine stall or water damage is probable."
            )
            else -> RiskAssessment(
                vehicleType = vehicleType,
                riskLevel   = RiskLevel.CRITICAL,
                headline    = "Critical — do NOT enter",
                detail      = "Water depth ($waterDepthCm cm) is ${diff} cm above the danger threshold for a ${vehicleType.label}. Entering will likely result in engine failure and potential entrapment."
            )
        }
    }
}
