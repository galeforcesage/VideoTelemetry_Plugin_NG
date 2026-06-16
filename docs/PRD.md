# Video Telemetry Overlay Playback Plugin — PRD

## 1. Overview

Implement the first **SageTV‑NG** native plugin: a **Video Telemetry Overlay Playback Plugin**.

Adds:
- Telemetry-aware playback for video content
- Real-time overlay rendering (DJI is the initial target)
- Extensible telemetry ingestion framework

Integrated into: **Video → Playback UI** (NG clients).

---

## 2. Platform Scope

### ✅ Required

| Component | Requirement |
|-----------|-------------|
| Server    | SageTV‑NG **only** |
| Clients   | NG clients (Android, Windows, etc.) and legacy clients (legacy may have limited functionality) |
| Runtime   | Java 21 LTS |

### ❌ Not Required
- Legacy SageTV server support

**Constraint:** No architectural compromises for NG due to legacy compatibility.

---

## 3. Plugin Architecture (NG-Focused)

### Design Requirements
Follow standard SageTV plugin structure for install / distribution, but:
- ✅ Behavior optimized for NG server + NG clients
- ✅ Transparent to the user (auto-detect telemetry based on sidecar)
- ✅ Rewire setup menu: replace **"program guide marks non-Tribune provided channels"** with **"Telemetry available to display"** and wire it to the video display
- ✅ Support legacy clients (best-effort)

### Structure

```
/JARs/
  telemetry-overlay.jar

/plugin/
  telemetry-overlay/plugin.xml
```

### 3.1 `plugin.xml`

```xml
<plugin>
  <name>Telemetry Overlay Playback</name>
  <identifier>com.sagetv.ng.telemetry</identifier>
  <version>1.0.0</version>
  <type>Standard</type>
  <author>YourName</author>
  <description>Telemetry-aware playback for drone/action video</description>
</plugin>
```

### 3.2 Responsibilities

**Server (NG)**
- File detection
- Telemetry parsing
- Unit preference resolution
- Telemetry cache

**Client (NG)**
- Overlay rendering
- Sync with playback clock

---

## 4. Functional Requirements

### 4.1 Video Playback
Must support:
- H.264
- H.265 (HEVC, hardware dependent)

**Constraint:** Use the existing NG playback pipeline. **Do not fork or replace the player.**

### 4.2 Telemetry Detection
Auto-detect:
- `video.mp4`
- `video.srt`
- Embedded subtitle extraction for display

### 4.3 Telemetry Parsing (Phase 1)

**Source:** DJI SRT

**Output Model**
```java
class TimedMetadata {
    double timestamp;
    Map<String, Object> fields;
}
```

### 4.4 Telemetry Sync
Telemetry must:
- Track playback time precisely
- Work with: pause, seek, FF / RW

### 4.5 Overlay Rendering (Core UX — Required)

**Rendering mode:** ✅ Overlay **on** video (mandatory).

**Visual requirements:**
- Semi-transparent background (20–40% black)
- Edge placement
  - Default: top-left
  - Optional: bottom-left

**UX justification:**
- ✅ Always readable
- ✅ Works across aspect ratios
- ✅ Matches DJI / GoPro overlay conventions

**Example layout**
```
ALT: 120 m     SPD: 8 m/s     GPS: 41.87, -87.62
```

**Controls — Playback Options popup**

The playback popup / menu must be context-aware and shared with the existing standard playback popup video menu:
- When the current video **has telemetry metadata**, show Telemetry Overlay controls:
  - **All Telemetry ON/OFF**
  - Individually selectable fields: Height/Altitude, Speed, GPS/location, Heading, Distance, Camera/Gimbal data, and any future parsed telemetry fields
- When the current video is **OTA or normal video with caption/subtitle tracks** (SRT/CC), the same popup area must instead show caption controls mapped to available tracks (e.g. CC1, CC2, SRT track selection, Off)
- Telemetry options **must not** appear when no telemetry source is available
- CC/SRT options **must not** be displaced when the content is a normal captioned video

### 4.6 Units System (Required)

Telemetry **must** respect the global user unit preference (Metric vs Imperial).

**Integration strategy**

*Step 1 — Detect system preference.* Search for:
- Weather plugin settings
- Locale settings
- Units preference

Likely sources: Weather plugin settings, regional / locale config.

*Step 2 — Fallback.* If no global unit setting exists, add plugin setting:
```
telemetry_units = metric | imperial | auto
```

**Conversion rules**
```
alt_ft = alt_m * 3.28084
mph    = mps   * 2.23694
kmh    = mps   * 3.6
```

**Display examples**

Metric:
```
ALT: 120 m
SPD: 8 m/s   (or 28.8 km/h later)
```

Imperial:
```
ALT: 394 ft
SPD: 18 mph
```

---

## 5. Data Model

### 5.1 Telemetry Track
```java
class TelemetryTrack {
    List<TimedMetadata> entries;
    TimedMetadata getAtTime(double time);
}
```

### 5.2 Adapter Interface
```java
interface TelemetryAdapter {
    TelemetryTrack parse(File input);
}
```

### 5.3 Phase 2 Cache
```
video.mp4.telemetry.json
```

---

## 6. Implementation Phases

### 🚀 Phase 1 (MVP)
- ✅ NG plugin works end-to-end
- ✅ DJI SRT parsing
- ✅ Overlay rendering
- ✅ Units conversion
- JSON cache
- UI polish (opacity, placement)
- Improved seek sync

### 🚀 Phase 2
- Map overlay (value to be discussed)
- Widget system
- Adapter expansion
  - GoPro cameras
  - Garmin / cycling devices
  - Race telemetry (OBD / CAN)

### Design Constraint
Core system must remain **telemetry-agnostic**. Adapters only:
- DJI SRT → adapter
- OBD/CAN → adapter
- GPX → adapter
- MAVLink → adapter

---

## 8. Implementation Checklist (Grep-Friendly)

**Plugin setup** — search: `plugin.xml`, `SageTVPlugin`

**Playback hook** — search: `VideoPlayback`, `MediaPlayer`, `MiniClient`

**File detection** — search: `import scan`, `media import`; add: `*.srt` detection

**Telemetry module** — create:
```
/telemetry/
  DjiSrtParser.java
  TelemetryParser.java
  TelemetryTrack.java
```

**Units integration** — search: `weather`, `units`, `locale`, `preferences`; add if missing: `UnitsProvider.java`

**Overlay UI** — search: `OSD`, `overlay`, `render`; create: `TelemetryOverlayView`

**Settings** — add:
- `telemetry_overlay_enabled`
- `telemetry_units_override`

---

## 9. Risks & Constraints

### NG vs Legacy Risk

| Risk | Decision |
|------|----------|
| Legacy client incompatibility | Accepted |
| STV UI mismatch | Not supported |
| NG-only features breaking legacy | Allowed |

### Technical Risks

| Risk | Mitigation |
|------|------------|
| HEVC unsupported | Client fallback / transcode |
| 10-bit overload | Capability detection |
| SRT format drift | Tolerant parser |
| UI performance | Lightweight rendering |

---

## 10. Key Architecture Rule

✅ **Optimize for SageTV‑NG.**
