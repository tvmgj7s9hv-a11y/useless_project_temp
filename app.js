/**
 * ScrollMeter — Web Companion & Interactive Background Tracking Simulator
 * Implements physical screen DPI calculation, Instagram session state transitions,
 * witty real-world comparisons, milestone unlocks, and local persistence.
 */

(function () {
  'use strict';

  // Physical calculation constants
  const DEVICE_DPI = 420.0;
  const METERS_PER_INCH = 0.0254;
  const FEET_PER_METER = 3.28084;
  const MILES_PER_METER = 0.000621371;

  // Milestone landmarks definitions
  const MILESTONES = [
    { id: 'm_25', threshold: 25, title: 'Olympic Pool', desc: 'Swam an Olympic pool with your thumb.', emoji: '🏊' },
    { id: 'm_100', threshold: 100, title: 'Crossed The Street', desc: 'Crossed a busy city street without looking.', emoji: '🚶' },
    { id: 'm_300', threshold: 300, title: 'Eiffel Tower', desc: 'Scaled the vertical height of the Eiffel Tower.', emoji: '🗼' },
    { id: 'm_828', threshold: 828, title: 'Burj Khalifa', desc: 'Reached the summit of the tallest skyscraper.', emoji: '🏙️' },
    { id: 'm_1000', threshold: 1000, title: '1 Kilometer Club', desc: 'Walked an entire kilometer without moving a leg.', emoji: '🦥' },
    { id: 'm_2500', threshold: 2500, title: 'Coffee Run', desc: 'Distance from your home to the nearest coffee shop.', emoji: '☕' },
    { id: 'm_5000', threshold: 5000, title: '5K Park Run', desc: 'A solid 5K jog... purely on your sofa.', emoji: '🏃' },
    { id: 'm_8848', threshold: 8848, title: 'Mount Everest', desc: 'Summitted Mount Everest vertically from bed.', emoji: '🏔️' },
    { id: 'm_10000', threshold: 10000, title: 'Touch Some Grass', desc: 'You scrolled 10 km. Please go outside and touch some grass.', emoji: '🌱' },
    { id: 'm_21097', threshold: 21097.5, title: 'Half Marathon', desc: 'Completed a Half Marathon on social media.', emoji: '🎽' },
    { id: 'm_42195', threshold: 42195, title: 'Full Marathon', desc: 'You just scrolled 42.195 km! An Olympic Marathon!', emoji: '🏅' }
  ];

  // Initial State with sensible defaults or loaded from localStorage
  const defaultState = {
    todayMeters: 2470.0,
    todaySeconds: 6120, // 1h 42m
    todaySessions: 7,
    weekMeters: 14800.0,
    monthMeters: 48200.0,
    lifetimeMeters: 142500.0,
    longestSessionDuration: 8100, // 2h 15m
    longestSessionMeters: 3800.0,
    highestDayMeters: 6400.0,
    highestDayDate: 'Sep 08',
    history: [
      { date: 'Yesterday (Sep 10)', meters: 3120, duration: 4500, sessions: 5 },
      { date: 'Tue (Sep 09)', meters: 2100, duration: 3200, sessions: 4 },
      { date: 'Mon (Sep 08)', meters: 6400, duration: 8900, sessions: 9 },
      { date: 'Sun (Sep 07)', meters: 1800, duration: 2400, sessions: 3 },
      { date: 'Sat (Sep 06)', meters: 4200, duration: 5800, sessions: 8 }
    ],
    settings: {
      trackingEnabled: true,
      notificationsEnabled: true,
      dailyGoalMeters: 3000.0,
      unit: 'metric' // 'metric' or 'imperial'
    }
  };

  let state = loadState();

  // Runtime session state
  let isInstagramForeground = true;
  let currentSessionMeters = 0.0;
  let currentSessionSeconds = 0;
  let sessionTimer = null;
  let liveDeltaDebounce = null;

  // DOM Elements
  const elTodayDist = document.getElementById('uiTodayDistance');
  const elTodayDur = document.getElementById('uiTodayDuration');
  const elGoalProgressFill = document.getElementById('uiGoalProgressFill');
  const elGoalPercentText = document.getElementById('uiGoalPercentText');
  const elWittyQuoteText = document.getElementById('uiWittyQuoteText');
  const elLiveDataStream = document.getElementById('uiLiveDataStream');
  const elLiveStreamCard = document.getElementById('uiLiveStreamCard');
  const elWeekDist = document.getElementById('uiWeekDistance');
  const elMonthDist = document.getElementById('uiMonthDistance');
  const elTodaySessions = document.getElementById('uiTodaySessions');
  const elDailyAvg = document.getElementById('uiDailyAverage');

  const elHistoryList = document.getElementById('uiHistoryList');
  const elLifetimeDist = document.getElementById('uiLifetimeDistance');
  const elLongestSession = document.getElementById('uiLongestSession');
  const elHighestDay = document.getElementById('uiHighestDay');
  const elMilestonesList = document.getElementById('uiMilestonesList');

  const elSettingTracking = document.getElementById('settingTrackingToggle');
  const elSettingNotif = document.getElementById('settingNotifToggle');
  const elSettingGoalSlider = document.getElementById('settingGoalSlider');
  const elSettingGoalVal = document.getElementById('settingGoalVal');
  const elBtnMetric = document.getElementById('btnUnitMetric');
  const elBtnImperial = document.getElementById('btnUnitImperial');

  const elIgFeed = document.getElementById('igScrollFeed');
  const elAppStateBtn = document.getElementById('btnToggleForeground');
  const elAppStateLabel = document.getElementById('appStateLabel');
  const elInappTrackingBadge = document.getElementById('inappTrackingBadge');
  const elGlobalStatusBadge = document.getElementById('globalStatusBadge');
  const elGlobalStatusText = document.getElementById('globalStatusText');
  const toastContainer = document.getElementById('toastContainer');

  // Initialize
  function init() {
    updateClock();
    setInterval(updateClock, 30000);

    setupNavigation();
    setupSimulatorControls();
    setupSettingsEvents();
    attachInstagramScrollListener();

    renderDashboard();
    renderHistory();
    renderStatistics();
    renderSettings();
  }

  function updateClock() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const mins = String(now.getMinutes()).padStart(2, '0');
    const clockEl = document.getElementById('statusClock');
    if (clockEl) clockEl.textContent = `${hours}:${mins}`;
  }

  // Navigation Tabs
  function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetTab = btn.getAttribute('data-tab');
        navItems.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        document.querySelectorAll('.tab-pane').forEach(pane => {
          pane.classList.remove('active');
        });
        const activePane = document.getElementById(targetTab);
        if (activePane) activePane.classList.add('active');
      });
    });

    // View mode toggle
    const toggleSimBtn = document.getElementById('btnToggleSimMode');
    if (toggleSimBtn) {
      toggleSimBtn.addEventListener('click', () => {
        const showcase = document.querySelector('.showcase-container');
        showcase.classList.toggle('stacked-mode');
        toggleSimBtn.innerHTML = showcase.classList.contains('stacked-mode')
          ? '<span class="icon">📱</span> View Mode: Stacked'
          : '<span class="icon">📱</span> View Mode: Side-by-Side';
      });
    }

    // Like buttons in Instagram simulator
    document.querySelectorAll('.ig-like-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        btn.textContent = btn.textContent === '❤️' ? '💖' : '❤️';
      });
    });

    // Scroll to top button in IG feed
    const resetScrollBtn = document.getElementById('btnResetScrollPosition');
    if (resetScrollBtn) {
      resetScrollBtn.addEventListener('click', () => {
        elIgFeed.scrollTo({ top: 0, behavior: 'smooth' });
      });
    }
  }

  // Instagram Scroll Gesture Detection & Distance Accumulation
  function attachInstagramScrollListener() {
    let lastScrollTop = elIgFeed.scrollTop;
    let isTouching = false;
    let touchStartY = 0;

    // Wheel event (Desktop mouse/touchpad)
    elIgFeed.addEventListener('wheel', (e) => {
      if (!isInstagramForeground || !state.settings.trackingEnabled) return;
      const deltaY = Math.abs(e.deltaY);
      if (deltaY > 4) {
        processScrollPixels(deltaY);
      }
    }, { passive: true });

    // Touch events (Mobile simulation)
    elIgFeed.addEventListener('touchstart', (e) => {
      isTouching = true;
      touchStartY = e.touches[0].clientY;
    }, { passive: true });

    elIgFeed.addEventListener('touchmove', (e) => {
      if (!isTouching || !isInstagramForeground || !state.settings.trackingEnabled) return;
      const currentY = e.touches[0].clientY;
      const deltaY = Math.abs(currentY - touchStartY);
      if (deltaY > 6) {
        processScrollPixels(deltaY);
        touchStartY = currentY;
      }
    }, { passive: true });

    elIgFeed.addEventListener('touchend', () => {
      isTouching = false;
    });

    // Native scroll event fallback
    elIgFeed.addEventListener('scroll', () => {
      if (!isInstagramForeground || !state.settings.trackingEnabled) return;
      const currentScrollTop = elIgFeed.scrollTop;
      const deltaY = Math.abs(currentScrollTop - lastScrollTop);
      if (deltaY > 6) {
        processScrollPixels(deltaY);
      }
      lastScrollTop = currentScrollTop;
    }, { passive: true });
  }

  // Convert raw pixels into real physical meters via Screen DPI
  function processScrollPixels(pixelDelta) {
    // Inches = pixels / DPI
    const inches = pixelDelta / DEVICE_DPI;
    // Meters = inches * 0.0254
    const addedMeters = inches * METERS_PER_INCH;

    if (addedMeters <= 0.0) return;

    recordDistanceIncrement(addedMeters);
  }

  function recordDistanceIncrement(addedMeters) {
    const prevToday = state.todayMeters;
    state.todayMeters += addedMeters;
    state.weekMeters += addedMeters;
    state.monthMeters += addedMeters;
    state.lifetimeMeters += addedMeters;

    currentSessionMeters += addedMeters;

    // Start session timer if not ticking
    if (!sessionTimer && isInstagramForeground) {
      sessionTimer = setInterval(() => {
        currentSessionSeconds++;
        state.todaySeconds++;
        elTodayDur.textContent = formatDuration(state.todaySeconds) + ' active';
      }, 1000);
    }

    // Update live streaming card
    elLiveStreamCard.style.display = 'flex';
    elLiveDataStream.textContent = `Streaming scroll events: +${formatDistance(currentSessionMeters)} (${formatDuration(currentSessionSeconds)})`;

    clearTimeout(liveDeltaDebounce);
    liveDeltaDebounce = setTimeout(() => {
      saveState();
    }, 1500);

    // Check milestones
    checkMilestones(prevToday, state.todayMeters);

    renderDashboard();
  }

  // Fast-Forward Simulator Buttons
  function setupSimulatorControls() {
    document.getElementById('btnSim100m').addEventListener('click', () => {
      recordDistanceIncrement(100.0);
      showToast('⚡ Fast-Forward Simulation', 'Simulated 100m vertical scroll on Instagram.', '🚶');
    });

    document.getElementById('btnSim1km').addEventListener('click', () => {
      recordDistanceIncrement(1000.0);
      showToast('⚡ Fast-Forward Simulation', 'Simulated 1.0 km scroll distance.', '🦥');
    });

    document.getElementById('btnSim5km').addEventListener('click', () => {
      recordDistanceIncrement(5000.0);
      showToast('⚡ Fast-Forward Simulation', 'Simulated 5.0 km couch jog on Instagram.', '🏃');
    });

    document.getElementById('btnSimMarathon').addEventListener('click', () => {
      recordDistanceIncrement(42195.0);
      showToast('⚡ Fast-Forward Simulation', 'Simulated 42.2 km full marathon on Instagram!', '🏅');
    });

    // Foreground / Background Toggle (tests tracking boundary requirements)
    elAppStateBtn.addEventListener('click', () => {
      isInstagramForeground = !isInstagramForeground;

      if (isInstagramForeground) {
        elAppStateLabel.textContent = 'FOREGROUND (Active)';
        elAppStateLabel.parentElement.querySelector('.dot-live').style.background = 'var(--accent-green)';
        elInappTrackingBadge.style.opacity = '1';
        elLiveStreamCard.style.display = 'flex';
        showToast('Instagram Opened', 'ScrollMeter is now tracking Instagram in the background.', '📱');
      } else {
        elAppStateLabel.textContent = 'BACKGROUND (Paused)';
        elAppStateLabel.parentElement.querySelector('.dot-live').style.background = '#94a3b8';
        elInappTrackingBadge.style.opacity = '0.5';
        elLiveStreamCard.style.display = 'none';

        if (sessionTimer) {
          clearInterval(sessionTimer);
          sessionTimer = null;
        }
        showToast('Instagram Paused', 'User left Instagram. Scroll tracking stopped.', '⏸️');
      }
    });
  }

  // Milestone Detection & Notification Dispatcher
  function checkMilestones(previousMeters, currentMeters) {
    if (!state.settings.notificationsEnabled) return;

    for (const milestone of MILESTONES) {
      if (previousMeters < milestone.threshold && currentMeters >= milestone.threshold) {
        showToast(
          `🏆 Milestone Unlocked: ${milestone.title}`,
          milestone.desc,
          milestone.emoji
        );
        renderStatistics(); // Refresh unlocked milestones UI
      }
    }
  }

  // Toast Notification System
  function showToast(title, message, icon = '🔔') {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `
      <div class="toast-icon">${icon}</div>
      <div>
        <div class="toast-title">${title}</div>
        <div class="toast-msg">${message}</div>
      </div>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('fade-out');
      setTimeout(() => {
        toast.remove();
      }, 350);
    }, 4500);
  }

  // Render Dashboard
  function renderDashboard() {
    const isMetric = state.settings.unit === 'metric';

    elTodayDist.textContent = formatDistance(state.todayMeters, isMetric);
    elTodayDur.textContent = `${formatDuration(state.todaySeconds)} active`;

    // Goal progress
    const goal = state.settings.dailyGoalMeters;
    const pct = Math.min(100, Math.round((state.todayMeters / goal) * 100));
    elGoalProgressFill.style.width = `${pct}%`;
    elGoalPercentText.textContent = `${pct}% of ${formatDistance(goal, isMetric)} daily goal`;

    // Dynamic Witty Quote
    elWittyQuoteText.textContent = getWittyQuote(state.todayMeters);

    // Quick Stats
    elWeekDist.textContent = formatDistance(state.weekMeters, isMetric);
    elMonthDist.textContent = formatDistance(state.monthMeters, isMetric);
    elTodaySessions.textContent = `${state.todaySessions} opens`;
    const avgMeters = (state.weekMeters / 7);
    elDailyAvg.textContent = `${formatDistance(avgMeters, isMetric)}/day`;
  }

  // Witty real-world comparisons
  function getWittyQuote(meters) {
    if (meters < 50) return "Just warming up your scrolling thumb...";
    if (meters < 150) return "That's roughly the distance to cross the street.";
    if (meters < 500) return "You've scrolled higher than the Eiffel Tower.";
    if (meters < 1200) return "You walked an entire kilometer without moving your legs.";
    if (meters < 3000) return "That's roughly the distance from your home to the nearest coffee shop.";
    if (meters < 6000) return "That's a solid 5K park jog... but on your couch.";
    if (meters < 9500) return "You've scrolled higher than Mount Everest's death zone.";
    if (meters < 15000) return "10+ kilometers! Seriously, go outside and touch some grass 🌱";
    if (meters < 30000) return "Half marathon territory. Your thumb has six-pack abs.";
    return "You've scrolled farther than the length of a marathon... eventually.";
  }

  // Render History Tab
  function renderHistory() {
    const isMetric = state.settings.unit === 'metric';
    elHistoryList.innerHTML = '';

    state.history.forEach(item => {
      const card = document.createElement('div');
      card.className = 'history-item';
      const pct = Math.min(100, Math.round((item.meters / state.settings.dailyGoalMeters) * 100));

      card.innerHTML = `
        <div class="history-item-top">
          <span class="history-date">${item.date}</span>
          <span class="history-dist">${formatDistance(item.meters, isMetric)}</span>
        </div>
        <div class="history-bar-wrap">
          <div class="history-bar-fill" style="width: ${pct}%;"></div>
        </div>
        <div class="history-meta">
          <span>${formatDuration(item.duration)} active</span>
          <span>${item.sessions} sessions</span>
        </div>
      `;
      elHistoryList.appendChild(card);
    });
  }

  // Render Statistics & Milestones Tab
  function renderStatistics() {
    const isMetric = state.settings.unit === 'metric';

    elLifetimeDist.textContent = formatDistance(state.lifetimeMeters, isMetric);
    elLongestSession.textContent = `${formatDuration(state.longestSessionDuration)} (${formatDistance(state.longestSessionMeters, isMetric)})`;
    elHighestDay.textContent = `${formatDistance(state.highestDayMeters, isMetric)} (${state.highestDayDate})`;

    elMilestonesList.innerHTML = '';
    MILESTONES.forEach(m => {
      const isUnlocked = state.lifetimeMeters >= m.threshold;
      const card = document.createElement('div');
      card.className = `milestone-card ${isUnlocked ? 'unlocked' : 'locked'}`;
      card.innerHTML = `
        <div class="milestone-emoji">${m.emoji}</div>
        <div class="milestone-info">
          <div class="milestone-header">
            <span class="milestone-title">${m.title} ${isUnlocked ? '✓' : '🔒'}</span>
            <span class="milestone-dist">${formatDistance(m.threshold, isMetric)}</span>
          </div>
          <div class="milestone-desc">${m.desc}</div>
        </div>
      `;
      elMilestonesList.appendChild(card);
    });
  }

  // Settings Events
  function setupSettingsEvents() {
    elSettingTracking.addEventListener('change', (e) => {
      state.settings.trackingEnabled = e.target.checked;
      elInappTrackingBadge.style.display = e.target.checked ? 'flex' : 'none';
      elGlobalStatusBadge.style.opacity = e.target.checked ? '1' : '0.4';
      elGlobalStatusText.textContent = e.target.checked ? 'ACCESSIBILITY TRACKER ACTIVE' : 'TRACKER PAUSED';
      saveState();
    });

    elSettingNotif.addEventListener('change', (e) => {
      state.settings.notificationsEnabled = e.target.checked;
      saveState();
    });

    elSettingGoalSlider.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      state.settings.dailyGoalMeters = val;
      const isMetric = state.settings.unit === 'metric';
      elSettingGoalVal.textContent = formatDistance(val, isMetric);
      renderDashboard();
      saveState();
    });

    elBtnMetric.addEventListener('click', () => {
      state.settings.unit = 'metric';
      elBtnMetric.classList.add('active');
      elBtnImperial.classList.remove('active');
      renderDashboard();
      renderHistory();
      renderStatistics();
      saveState();
    });

    elBtnImperial.addEventListener('click', () => {
      state.settings.unit = 'imperial';
      elBtnImperial.classList.add('active');
      elBtnMetric.classList.remove('active');
      renderDashboard();
      renderHistory();
      renderStatistics();
      saveState();
    });

    // Reset today
    document.getElementById('btnResetTodayStats').addEventListener('click', () => {
      if (confirm("Reset today's Instagram scroll distance to 0?")) {
        state.todayMeters = 0.0;
        state.todaySeconds = 0;
        state.todaySessions = 0;
        currentSessionMeters = 0.0;
        currentSessionSeconds = 0;
        renderDashboard();
        saveState();
        showToast('Today Reset', 'Your scroll distance for today was reset to 0 m.', '🔄');
      }
    });

    // Delete all data
    document.getElementById('btnDeleteAllData').addEventListener('click', () => {
      if (confirm('Delete all stored history and statistics? This cannot be undone.')) {
        state.todayMeters = 0.0;
        state.todaySeconds = 0;
        state.todaySessions = 0;
        state.weekMeters = 0.0;
        state.monthMeters = 0.0;
        state.lifetimeMeters = 0.0;
        state.history = [];
        currentSessionMeters = 0.0;
        currentSessionSeconds = 0;
        renderDashboard();
        renderHistory();
        renderStatistics();
        saveState();
        showToast('Data Cleared', 'All stored statistics have been erased from device.', '🗑️');
      }
    });

    // Quick reset button on the Today hero card
    const btnQuickReset = document.getElementById('btnQuickResetWeb');
    if (btnQuickReset) {
      btnQuickReset.addEventListener('click', () => {
        if (confirm("Reset today's Instagram scroll distance back to 0?")) {
          state.todayMeters = 0.0;
          state.todaySeconds = 0;
          state.todaySessions = 0;
          currentSessionMeters = 0.0;
          currentSessionSeconds = 0;
          renderDashboard();
          saveState();
          showToast('ScrollMeter Reset', "Today's distance has been reset to 0 m.", '🔄');
        }
      });
    }
  }

  function renderSettings() {
    elSettingTracking.checked = state.settings.trackingEnabled;
    elSettingNotif.checked = state.settings.notificationsEnabled;
    elSettingGoalSlider.value = state.settings.dailyGoalMeters;
    const isMetric = state.settings.unit === 'metric';
    elSettingGoalVal.textContent = formatDistance(state.settings.dailyGoalMeters, isMetric);

    if (isMetric) {
      elBtnMetric.classList.add('active');
      elBtnImperial.classList.remove('active');
    } else {
      elBtnImperial.classList.add('active');
      elBtnMetric.classList.remove('active');
    }
  }

  // Formatters
  function formatDistance(meters, isMetric = true) {
    if (isMetric) {
      if (meters < 1000) {
        return `${Math.round(meters)} m`;
      }
      return `${(meters / 1000).toFixed(2)} km`;
    } else {
      const miles = meters * MILES_PER_METER;
      if (miles < 0.1) {
        return `${Math.round(meters * FEET_PER_METER)} ft`;
      }
      return `${miles.toFixed(2)} mi`;
    }
  }

  function formatDuration(totalSeconds) {
    const hours = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    if (hours > 0) return `${hours}h ${mins}m`;
    if (mins > 0) return `${mins}m`;
    return `${totalSeconds}s`;
  }

  // State Persistence
  function saveState() {
    try {
      localStorage.setItem('scrollmeter_state_v1', JSON.stringify(state));
    } catch (e) {
      // LocalStorage quota or restricted
    }
  }

  function loadState() {
    try {
      const stored = localStorage.getItem('scrollmeter_state_v1');
      if (stored) {
        return Object.assign({}, defaultState, JSON.parse(stored));
      }
    } catch (e) {
      // Fallback
    }
    return JSON.parse(JSON.stringify(defaultState));
  }

  // Start on DOM Ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
