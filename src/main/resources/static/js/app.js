/**
 * Smart River Management System - Day 15: Modern Interactive Web Dashboard
 * Pure Vanilla JavaScript Client (Canvas 2D Gauge + REST API Integration)
 */

// ==========================================================================
// 1. Application State & Configuration
// ==========================================================================
const AppState = {
  stations: [],
  selectedStation: null,
  stats: null,
  readings: [],
  alerts: [],
  filterStatus: 'ALL',
  searchQuery: '',
  autoRefresh: true,
  autoRefreshInterval: 5000,
  countdownTimer: 5,
  
  // Animation state for Water Level Gauge
  gauge: {
    currentLevel: 7.5,
    targetLevel: 7.5,
    maxLevel: 25.0,
    wavePhase: 0,
    lerpSpeed: 0.06
  }
};

// ==========================================================================
// 2. REST API Service Wrapper
// ==========================================================================
const ApiService = {
  async getStations() {
    const res = await fetch('/api/stations');
    if (!res.ok) throw new Error(`Failed to load stations (${res.status})`);
    return await res.json();
  },

  async getStats() {
    const res = await fetch('/api/stats');
    if (!res.ok) throw new Error(`Failed to load basin stats (${res.status})`);
    return await res.json();
  },

  async getReadings(limit = 40) {
    const res = await fetch(`/api/readings?limit=${limit}`);
    if (!res.ok) throw new Error(`Failed to load readings (${res.status})`);
    return await res.json();
  },

  async getAlerts() {
    const res = await fetch('/api/readings/alerts');
    if (!res.ok) throw new Error(`Failed to load alerts (${res.status})`);
    return await res.json();
  },

  async registerStation(stationData) {
    const res = await fetch('/api/stations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(stationData)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to register station');
    return data;
  },

  async recordReading(stationId, level) {
    const params = new URLSearchParams({
      stationId: stationId,
      level: level.toString(),
      timestamp: new Date().toLocaleTimeString('en-US', { hour12: false })
    });
    const res = await fetch(`/api/readings?${params.toString()}`, {
      method: 'POST'
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to record measurement');
    return data;
  },

  async triggerFloodSurge(stationId = null) {
    const url = stationId 
      ? `/api/simulation/surge?stationId=${encodeURIComponent(stationId)}`
      : '/api/simulation/surge';
    const res = await fetch(url, { method: 'POST' });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Surge simulation failed');
    return data;
  }
};

// ==========================================================================
// 3. Canvas 2D River Staff Gauge Visualizer
// ==========================================================================
class RiverGaugeRenderer {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    this.ctx = this.canvas.getContext('2d');
    this.resizeCanvas();
    window.addEventListener('resize', () => this.resizeCanvas());
    this.animate();
  }

  resizeCanvas() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.ctx.resetTransform();
    this.ctx.scale(dpr, dpr);
    this.width = rect.width;
    this.height = rect.height;
  }

  animate() {
    // Smooth interpolation (lerp) toward target water level
    const g = AppState.gauge;
    g.currentLevel += (g.targetLevel - g.currentLevel) * g.lerpSpeed;
    g.wavePhase += 0.04;

    this.render();
    requestAnimationFrame(() => this.animate());
  }

  render() {
    const ctx = this.ctx;
    const w = this.width;
    const h = this.height;
    if (!w || !h) return;

    ctx.clearRect(0, 0, w, h);

    const station = AppState.selectedStation;
    const currentLevel = AppState.gauge.currentLevel;
    const maxLevel = AppState.gauge.maxLevel;
    const normalLevel = station ? station.normalLevelMeters : 7.0;
    const dangerLevel = station ? station.dangerLevelMeters : 16.0;

    // Staff Gauge Geometry
    const staffX = w * 0.22;
    const staffW = w * 0.56;
    const topY = 35;
    const botY = h - 35;
    const usableH = botY - topY;

    const levelToY = (lvl) => {
      const clamped = Math.max(0, Math.min(maxLevel, lvl));
      return botY - (clamped / maxLevel) * usableH;
    };

    const currentY = levelToY(currentLevel);
    const normalY = levelToY(normalLevel);
    const dangerY = levelToY(dangerLevel);

    // 1. Draw Gauge Column Background
    ctx.save();
    ctx.fillStyle = 'rgba(10, 16, 35, 0.7)';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.12)';
    ctx.lineWidth = 2;
    this.roundRect(ctx, staffX, topY, staffW, usableH, 12);
    ctx.fill();
    ctx.stroke();
    ctx.restore();

    // 2. Draw Graduated Metric Ticks (0 to 25 meters)
    ctx.save();
    ctx.font = '10px "JetBrains Mono", monospace';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';

    for (let m = 0; m <= maxLevel; m += 1) {
      const y = levelToY(m);
      const isMajor = m % 5 === 0;
      const tickLen = isMajor ? 14 : (m % 1 === 0 ? 8 : 4);

      // Left tick marks
      ctx.beginPath();
      ctx.moveTo(staffX, y);
      ctx.lineTo(staffX + tickLen, y);
      ctx.strokeStyle = isMajor ? 'rgba(0, 242, 254, 0.8)' : 'rgba(255, 255, 255, 0.25)';
      ctx.lineWidth = isMajor ? 2 : 1;
      ctx.stroke();

      // Right tick marks
      ctx.beginPath();
      ctx.moveTo(staffX + staffW, y);
      ctx.lineTo(staffX + staffW - tickLen, y);
      ctx.stroke();

      // Major numerical labels on the left of staff
      if (isMajor) {
        ctx.fillStyle = 'rgba(148, 163, 184, 0.9)';
        ctx.fillText(`${m}m`, staffX - 8, y);
      }
    }
    ctx.restore();

    // 3. Draw Threshold Reference Lines
    // Danger Threshold Line (Red Dashed)
    ctx.save();
    ctx.setLineDash([6, 4]);
    ctx.strokeStyle = '#ef4444';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(staffX - 10, dangerY);
    ctx.lineTo(staffX + staffW + 10, dangerY);
    ctx.stroke();
    ctx.fillStyle = '#ef4444';
    ctx.font = 'bold 10px "Inter", sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`CRITICAL FLOOD (${dangerLevel.toFixed(1)}m)`, staffX + staffW + 14, dangerY);

    // Normal Threshold Line (Green/Amber Dashed)
    ctx.setLineDash([4, 4]);
    ctx.strokeStyle = '#10b981';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(staffX - 10, normalY);
    ctx.lineTo(staffX + staffW + 10, normalY);
    ctx.stroke();
    ctx.fillStyle = '#10b981';
    ctx.fillText(`NORMAL BASE (${normalLevel.toFixed(1)}m)`, staffX + staffW + 14, normalY);
    ctx.restore();

    // 4. Clip to Staff Gauge Area for Fluid Water Rendering
    ctx.save();
    this.roundRect(ctx, staffX + 2, topY + 2, staffW - 4, usableH - 4, 10);
    ctx.clip();

    // Water Column Color Gradient (Changes hue if critical flood)
    const isCritical = currentLevel >= dangerLevel;
    const isWarning = currentLevel >= normalLevel * 1.2 && !isCritical;
    
    let waterGrad = ctx.createLinearGradient(0, currentY, 0, botY);
    if (isCritical) {
      waterGrad.addColorStop(0, 'rgba(239, 68, 68, 0.85)');
      waterGrad.addColorStop(1, 'rgba(153, 27, 27, 0.95)');
    } else if (isWarning) {
      waterGrad.addColorStop(0, 'rgba(245, 158, 11, 0.8)');
      waterGrad.addColorStop(1, 'rgba(180, 83, 9, 0.9)');
    } else {
      waterGrad.addColorStop(0, 'rgba(0, 242, 254, 0.8)');
      waterGrad.addColorStop(0.5, 'rgba(59, 130, 246, 0.85)');
      waterGrad.addColorStop(1, 'rgba(30, 58, 138, 0.95)');
    }

    // Secondary deep wave (background ripple)
    ctx.fillStyle = isCritical ? 'rgba(220, 38, 38, 0.4)' : 'rgba(56, 189, 248, 0.35)';
    ctx.beginPath();
    ctx.moveTo(staffX, botY);
    for (let x = staffX; x <= staffX + staffW; x += 4) {
      const wave = Math.sin((x - staffX) * 0.04 + AppState.gauge.wavePhase * 1.3) * 4;
      ctx.lineTo(x, currentY + 3 + wave);
    }
    ctx.lineTo(staffX + staffW, botY);
    ctx.closePath();
    ctx.fill();

    // Primary oscillating wave
    ctx.fillStyle = waterGrad;
    ctx.beginPath();
    ctx.moveTo(staffX, botY);
    for (let x = staffX; x <= staffX + staffW; x += 4) {
      const wave = Math.sin((x - staffX) * 0.035 + AppState.gauge.wavePhase) * 6;
      ctx.lineTo(x, currentY + wave);
    }
    ctx.lineTo(staffX + staffW, botY);
    ctx.closePath();
    ctx.fill();

    ctx.restore(); // End clipping

    // 5. Draw Waterline Pill and Glow Tag
    ctx.save();
    const pillW = 105;
    const pillH = 26;
    const pillX = staffX + (staffW - pillW) / 2;
    const pillY = Math.max(topY + 14, Math.min(botY - 20, currentY - 13));

    ctx.fillStyle = isCritical ? '#dc2626' : (isWarning ? '#d97706' : '#0284c7');
    ctx.strokeStyle = isCritical ? '#fecaca' : '#bae6fd';
    ctx.lineWidth = 1.5;
    ctx.shadowColor = isCritical ? 'rgba(239, 68, 68, 0.6)' : 'rgba(0, 242, 254, 0.4)';
    ctx.shadowBlur = 12;
    this.roundRect(ctx, pillX, pillY, pillW, pillH, 13);
    ctx.fill();
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 12px "JetBrains Mono", monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(`${currentLevel.toFixed(2)} m`, pillX + pillW / 2, pillY + pillH / 2);
    ctx.restore();
  }

  roundRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
  }
}

// ==========================================================================
// 4. UI Dashboard Controller
// ==========================================================================
const DashboardController = {
  gaugeRenderer: null,

  async init() {
    this.gaugeRenderer = new RiverGaugeRenderer('gaugeCanvas');
    this.setupEventListeners();
    await this.refreshAll();
    this.startAutoRefresh();
  },

  setupEventListeners() {
    // Station Select Dropdown
    const stationSelect = document.getElementById('stationSelect');
    stationSelect.addEventListener('change', (e) => {
      this.selectStationById(e.target.value);
    });

    // Quick Simulation Flood Surge Button
    document.getElementById('btnSurge').addEventListener('click', async () => {
      await this.handleSurgeSimulation();
    });

    // Refresh Data Button
    document.getElementById('btnRefresh').addEventListener('click', async () => {
      await this.refreshAll();
      this.showToast('Telemetry data synchronized.', 'success');
    });

    // Modals
    document.getElementById('btnOpenAddStation').addEventListener('click', () => {
      this.openModal('modalAddStation');
    });
    document.getElementById('btnOpenRecordReading').addEventListener('click', () => {
      this.populateRecordModalStations();
      this.openModal('modalRecordReading');
    });

    // Close Modals
    document.querySelectorAll('.btn-close-modal, .btn-cancel').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const modal = e.target.closest('.modal-overlay');
        if (modal) modal.classList.remove('active');
      });
    });

    // Form Submissions
    document.getElementById('formAddStation').addEventListener('submit', async (e) => {
      e.preventDefault();
      await this.handleAddStationSubmit();
    });

    document.getElementById('formRecordReading').addEventListener('submit', async (e) => {
      e.preventDefault();
      await this.handleRecordReadingSubmit();
    });

    // Search Station Input
    const searchInput = document.getElementById('searchStationInput');
    searchInput.addEventListener('input', (e) => {
      AppState.searchQuery = e.target.value.toLowerCase();
      this.renderStationGrid();
    });

    // Telemetry Filter Chips
    document.querySelectorAll('.filter-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
        e.target.classList.add('active');
        AppState.filterStatus = e.target.dataset.filter;
        this.renderTelemetryFeed();
      });
    });
  },

  async refreshAll() {
    try {
      const [stations, stats, readings, alerts] = await Promise.all([
        ApiService.getStations(),
        ApiService.getStats(),
        ApiService.getReadings(50),
        ApiService.getAlerts()
      ]);

      AppState.stations = stations;
      AppState.stats = stats;
      AppState.readings = readings;
      AppState.alerts = alerts;

      // Select initial or maintain current station
      if (!AppState.selectedStation && stations.length > 0) {
        AppState.selectedStation = stations[0];
      } else if (AppState.selectedStation) {
        // Re-sync selected station instance
        const found = stations.find(s => s.stationId === AppState.selectedStation.stationId);
        if (found) AppState.selectedStation = found;
      }

      this.updateStationDropdown();
      this.updateGaugeTargetLevel();
      this.renderKPIs();
      this.renderAlertBanner();
      this.renderTelemetryFeed();
      this.renderStationGrid();
    } catch (err) {
      console.error('Refresh Error:', err);
      this.showToast('Network error updating telemetry: ' + err.message, 'error');
    }
  },

  updateStationDropdown() {
    const sel = document.getElementById('stationSelect');
    sel.innerHTML = '';
    AppState.stations.forEach(stn => {
      const opt = document.createElement('option');
      opt.value = stn.stationId;
      opt.textContent = `${stn.stationName} (${stn.stationId}) - ${stn.riverName}`;
      if (AppState.selectedStation && AppState.selectedStation.stationId === stn.stationId) {
        opt.selected = true;
      }
      sel.appendChild(opt);
    });
  },

  selectStationById(stationId) {
    const stn = AppState.stations.find(s => s.stationId === stationId);
    if (stn) {
      AppState.selectedStation = stn;
      this.updateGaugeTargetLevel();
      this.renderStationDetailPills();
    }
  },

  updateGaugeTargetLevel() {
    if (!AppState.selectedStation) return;
    const stn = AppState.selectedStation;

    // Find the latest reading for this station
    const stnReadings = AppState.readings.filter(r => 
      r.stationLocation.includes(stn.stationId) || r.stationLocation.includes(stn.stationName)
    );

    let target = stn.normalLevelMeters;
    if (stnReadings.length > 0) {
      target = stnReadings[stnReadings.length - 1].waterLevelMeters;
    }

    AppState.gauge.targetLevel = target;
    this.renderStationDetailPills(target);
  },

  renderStationDetailPills(currentLevel = null) {
    const stn = AppState.selectedStation;
    if (!stn) return;
    const lvl = currentLevel !== null ? currentLevel : AppState.gauge.targetLevel;

    document.getElementById('statNormalLevel').textContent = `${stn.normalLevelMeters.toFixed(1)} m`;
    document.getElementById('statDangerLevel').textContent = `${stn.dangerLevelMeters.toFixed(1)} m`;
    
    const margin = stn.dangerLevelMeters - lvl;
    const marginEl = document.getElementById('statFreeboardMargin');
    marginEl.textContent = `${margin.toFixed(1)} m`;
    marginEl.style.color = margin <= 0 ? '#ef4444' : (margin <= 2 ? '#f59e0b' : '#10b981');
  },

  renderKPIs() {
    const stats = AppState.stats;
    if (!stats) return;

    document.getElementById('kpiTotalStations').textContent = stats.totalStations;
    document.getElementById('kpiTotalReadings').textContent = stats.totalReadings;
    document.getElementById('kpiAvgLevel').textContent = `${stats.averageWaterLevel.toFixed(1)} m`;
    document.getElementById('kpiPeakLevel').textContent = `${stats.peakWaterLevel.toFixed(1)} m`;
    
    const alertCountEl = document.getElementById('kpiAlertCount');
    alertCountEl.textContent = stats.criticalAlertsCount;
    alertCountEl.style.color = stats.criticalAlertsCount > 0 ? '#ef4444' : '#10b981';
  },

  renderAlertBanner() {
    const banner = document.getElementById('emergencyAlertBanner');
    const alerts = AppState.alerts || [];

    if (alerts.length > 0) {
      const latest = alerts[alerts.length - 1];
      document.getElementById('alertBannerText').textContent = 
        `CRITICAL FLOOD WARNING: ${latest.stationLocation} has exceeded danger threshold! Recorded level: ${latest.waterLevelMeters.toFixed(1)}m. Alert status: ${latest.alertStatus}`;
      banner.classList.add('active');
    } else {
      banner.classList.remove('active');
    }
  },

  renderTelemetryFeed() {
    const feedContainer = document.getElementById('telemetryList');
    feedContainer.innerHTML = '';

    let items = [...AppState.readings].reverse(); // latest first
    if (AppState.filterStatus !== 'ALL') {
      items = items.filter(r => r.alertStatus.toUpperCase().includes(AppState.filterStatus));
    }

    if (items.length === 0) {
      feedContainer.innerHTML = '<div style="text-align: center; color: var(--color-text-dim); padding: 40px 0;">No telemetry records matching filter.</div>';
      return;
    }

    items.slice(0, 30).forEach(record => {
      const itemEl = document.createElement('div');
      itemEl.className = 'feed-item';

      let badgeClass = 'badge-normal';
      let statusText = 'NORMAL';
      if (record.alertStatus.includes('CRITICAL')) {
        badgeClass = 'badge-critical';
        statusText = 'CRITICAL';
      } else if (record.alertStatus.includes('WARNING')) {
        badgeClass = 'badge-warning';
        statusText = 'WARNING';
      }

      itemEl.innerHTML = `
        <div class="feed-item-left">
          <span class="feed-status-badge ${badgeClass}">${statusText}</span>
          <div class="feed-meta">
            <h5>${this.escapeHtml(record.stationLocation)}</h5>
            <span>${this.escapeHtml(record.timestamp)} &bull; ${this.escapeHtml(record.riverName || 'Basin')}</span>
          </div>
        </div>
        <div class="feed-level">
          <div class="feed-level-val">${record.waterLevelMeters.toFixed(2)} m</div>
        </div>
      `;
      feedContainer.appendChild(itemEl);
    });
  },

  renderStationGrid() {
    const grid = document.getElementById('stationsGrid');
    grid.innerHTML = '';

    let filtered = AppState.stations;
    if (AppState.searchQuery) {
      filtered = filtered.filter(s => 
        s.stationName.toLowerCase().includes(AppState.searchQuery) ||
        s.stationId.toLowerCase().includes(AppState.searchQuery) ||
        s.riverName.toLowerCase().includes(AppState.searchQuery)
      );
    }

    if (filtered.length === 0) {
      grid.innerHTML = '<div style="color: var(--color-text-dim); padding: 24px;">No stations found matching search.</div>';
      return;
    }

    filtered.forEach(stn => {
      const card = document.createElement('div');
      card.className = 'station-card';

      // Determine station latest reading status
      const stnReadings = AppState.readings.filter(r => 
        r.stationLocation.includes(stn.stationId) || r.stationLocation.includes(stn.stationName)
      );
      let latestLevel = stn.normalLevelMeters;
      let statusBadge = '<span class="feed-status-badge badge-normal">NORMAL</span>';
      if (stnReadings.length > 0) {
        latestLevel = stnReadings[stnReadings.length - 1].waterLevelMeters;
        if (latestLevel >= stn.dangerLevelMeters) {
          statusBadge = '<span class="feed-status-badge badge-critical">CRITICAL</span>';
        } else if (latestLevel >= stn.normalLevelMeters * 1.2) {
          statusBadge = '<span class="feed-status-badge badge-warning">WARNING</span>';
        }
      }

      card.innerHTML = `
        <div class="card-top">
          <div>
            <span class="card-stn-id">${this.escapeHtml(stn.stationId)}</span>
            <span class="card-river-tag">${this.escapeHtml(stn.riverName)}</span>
            <h4 class="card-name">${this.escapeHtml(stn.stationName)}</h4>
          </div>
          <div>${statusBadge}</div>
        </div>
        <div class="card-thresholds">
          <div class="thresh-item">
            <span class="thresh-label">Normal Level</span>
            <span class="thresh-val">${stn.normalLevelMeters.toFixed(1)} m</span>
          </div>
          <div class="thresh-item">
            <span class="thresh-label">Danger Level</span>
            <span class="thresh-val" style="color: #ef4444;">${stn.dangerLevelMeters.toFixed(1)} m</span>
          </div>
        </div>
        <div class="card-footer">
          <span style="font-size: 0.8rem; color: var(--color-text-muted);">
            Latest: <strong style="color: #ffffff;">${latestLevel.toFixed(1)} m</strong>
          </span>
          <button class="btn btn-secondary btn-sm btn-inspect" data-id="${stn.stationId}">
            Inspect Gauge
          </button>
        </div>
      `;

      card.querySelector('.btn-inspect').addEventListener('click', () => {
        document.getElementById('stationSelect').value = stn.stationId;
        this.selectStationById(stn.stationId);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      });

      grid.appendChild(card);
    });
  },

  populateRecordModalStations() {
    const sel = document.getElementById('recordStationSelect');
    sel.innerHTML = '';
    AppState.stations.forEach(stn => {
      const opt = document.createElement('option');
      opt.value = stn.stationId;
      opt.textContent = `${stn.stationName} (${stn.stationId})`;
      if (AppState.selectedStation && AppState.selectedStation.stationId === stn.stationId) {
        opt.selected = true;
      }
      sel.appendChild(opt);
    });
  },

  async handleAddStationSubmit() {
    const id = document.getElementById('addStationId').value.trim();
    const name = document.getElementById('addStationName').value.trim();
    const river = document.getElementById('addRiverName').value.trim();
    const normal = parseFloat(document.getElementById('addNormalLevel').value);
    const danger = parseFloat(document.getElementById('addDangerLevel').value);

    if (!id || !name || !river || isNaN(normal) || isNaN(danger)) {
      this.showToast('Please fill in all station fields with valid values.', 'warning');
      return;
    }

    try {
      await ApiService.registerStation({
        stationId: id,
        stationName: name,
        riverName: river,
        normalLevelMeters: normal,
        dangerLevelMeters: danger
      });
      this.closeModal('modalAddStation');
      document.getElementById('formAddStation').reset();
      this.showToast(`Station '${name}' registered and persisted to JPA/H2 database.`, 'success');
      await this.refreshAll();
    } catch (err) {
      this.showToast(`Registration failed: ${err.message}`, 'error');
    }
  },

  async handleRecordReadingSubmit() {
    const stnId = document.getElementById('recordStationSelect').value;
    const level = parseFloat(document.getElementById('recordWaterLevel').value);

    if (!stnId || isNaN(level) || level < 0 || level > 50) {
      this.showToast('Please specify a valid water level between 0.0m and 50.0m', 'warning');
      return;
    }

    try {
      const res = await ApiService.recordReading(stnId, level);
      this.closeModal('modalRecordReading');
      document.getElementById('formRecordReading').reset();
      this.showToast(`Measurement recorded (${level}m): ${res.alertStatus}`, 'success');
      await this.refreshAll();
    } catch (err) {
      this.showToast(`Recording failed: ${err.message}`, 'error');
    }
  },

  async handleSurgeSimulation() {
    try {
      const activeStnId = AppState.selectedStation ? AppState.selectedStation.stationId : null;
      this.showToast('Simulating emergency flash flood cloudburst surge (+4.5m)...', 'warning');
      const res = await ApiService.triggerFloodSurge(activeStnId);
      this.showToast(`${res.message} Level: ${res.surgeLevelMeters}m (${res.alertStatus})`, 'error');
      await this.refreshAll();
    } catch (err) {
      this.showToast(`Surge error: ${err.message}`, 'error');
    }
  },

  openModal(modalId) {
    document.getElementById(modalId).classList.add('active');
  },

  closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
  },

  startAutoRefresh() {
    setInterval(async () => {
      if (!AppState.autoRefresh) return;
      await this.refreshAll();
    }, AppState.autoRefreshInterval);
  },

  showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <span>${this.escapeHtml(message)}</span>
      <span style="cursor:pointer;opacity:0.6;" onclick="this.parentElement.remove()">&times;</span>
    `;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(30px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4500);
  },

  escapeHtml(str) {
    if (!str) return '';
    return str.toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
};

// Bootstrap application on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  DashboardController.init();
});
