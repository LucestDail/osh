/**
 * 서버 정보 파서 — navbar 시계 + 상단 status bar 갱신.
 * 입력 wrapper { applicationJson: "{ currentTime, memory, useMemory, freeMemory, systemLoadAverage, ... }" }
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function setText(id, value) {
        const el = document.getElementById(id);
        if (el && value != null) el.textContent = value;
    }

    function memoryMB(s) {
        if (s == null) return 0;
        const m = String(s).match(/[\d.]+/);
        return m ? parseFloat(m[0]) : 0;
    }

    function render(strJson) {
        const app = H.unwrap(strJson, 'applicationJson');
        if (!app) return;

        setText('currentTime', app.currentTime || '-');
        if (app.serverNowMs != null) {
            window.OSH = window.OSH || {};
            window.OSH.serverNowMs = Number(app.serverNowMs);
        }
        setText('systemArchitecture', app.systemArchitecture || '-');
        setText('systemName', app.systemName || '-');
        setText('systemLoad', app.systemLoadAverage != null ? Number(app.systemLoadAverage).toFixed(2) : '-');

        const used  = memoryMB(app.useMemory);
        const total = memoryMB(app.memory);
        const pct   = total > 0 ? Math.min(100, (used / total) * 100) : 0;

        const fill = document.getElementById('memBarFill');
        if (fill) {
            fill.style.width = pct.toFixed(1) + '%';
            fill.classList.toggle('is-warn', pct >= 80);
        }
        setText('memText', used.toFixed(0) + ' / ' + total.toFixed(0) + ' MB (' + pct.toFixed(0) + '%)');
    }

    (window.OSH = window.OSH || {}).application = { render: render };
})();
