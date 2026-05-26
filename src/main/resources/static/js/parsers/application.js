/**
 * 서버 정보 파서 — 시계, 메모리, 시스템 부하 등 + Chart.js doughnut.
 * 입력 wrapper { applicationJson: "{ currentTime, memory, useMemory, freeMemory, ... }" }
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    let chart = null;

    function setText(id, value) {
        const el = document.getElementById(id);
        if (el && value != null) el.textContent = value;
    }

    function memoryMB(s) {
        if (s == null) return 0;
        const m = String(s).match(/[\d.]+/);
        return m ? parseFloat(m[0]) : 0;
    }

    function ensureChart(used, free) {
        const canvas = document.getElementById('memoryChart');
        if (!canvas || typeof Chart === 'undefined') return;
        const data = {
            labels: ['사용 중', '여유'],
            datasets: [{
                data: [used, free],
                backgroundColor: [
                    getComputedStyle(document.documentElement).getPropertyValue('--color-primary').trim() || '#3b6cff',
                    getComputedStyle(document.documentElement).getPropertyValue('--color-border-strong').trim() || '#c8cfdf'
                ],
                borderWidth: 0
            }]
        };
        if (chart) {
            chart.data = data;
            chart.update('none');
            return;
        }
        chart = new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '70%',
                animation: { duration: 600 },
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                const total = used + free;
                                const pct = total > 0 ? (ctx.raw / total * 100).toFixed(1) : '0.0';
                                return ctx.label + ': ' + ctx.raw + ' MB (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    function render(strJson) {
        const app = H.unwrap(strJson, 'applicationJson');
        if (!app) return;
        setText('currentTime', app.currentTime || '-');
        setText('systemArchitecture', app.systemArchitecture || '-');
        setText('systemName', app.systemName || '-');
        setText('systemLoad', app.systemLoadAverage != null ? Number(app.systemLoadAverage).toFixed(2) : '-');
        setText('totalMemory', app.memory || '-');
        setText('usedMemory', app.useMemory || '-');
        setText('freeMemory', app.freeMemory || '-');

        ensureChart(memoryMB(app.useMemory), memoryMB(app.freeMemory));
    }

    (window.OSH = window.OSH || {}).application = { render: render };
})();
