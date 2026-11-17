# Sabeel ProGuard / R8 rules for the release build.
#
# Version 1.0 ships with NO project-specific keep rules on purpose. The current
# dependency set already ships the consumer rules it needs:
#
#   * Jetpack Compose (ui, foundation, material3) - AndroidX Compose consumer
#     rules cover R8; no reflection-based Compose API is used here.
#   * Room - room-runtime/room-compiler ship consumer rules that keep generated
#     DAO/entity implementations and the annotation metadata R8 needs.
#   * DataStore, WorkManager, Navigation - explicit class references plus their
#     consumer rules cover them; Workers are declared in the merged manifest or
#     constructed from Class references the compiler can see.
#   * Kotlin stdlib / kotlinx-coroutines - no reflection, no serialization.
#
# Do NOT add blanket rules like "-keep class com.sabeel.steps.** { *; }": they
# would disable shrinking for the whole app and hide the value of R8.
#
# If a future change adds reflection, a serialization library or JNI, add the
# narrowest rule that fixes the observed failure and document why here, e.g.:
#
#   # Example: a JSON @TypeConverter looked up reflectively by Room.
#   -keep class com.sabeel.steps.data.db.JsonConverters { *; }
#
# Verification: build :app:bundleRelease, install the signed AAB on a device or
# emulator, and exercise step counting, walk tracking, CSV export/import and
# reminders. The R8 mapping file is written to
# app/build/outputs/mapping/release/mapping.txt - archive it with the release.
