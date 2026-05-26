/**
 * 메인 대시보드 진입점.
 *  - 단일 SSE (/dashboard/main/stream) 다중 event type 디스패치
 *  - 정보 탭 5종 전환
 *  - AI 한줄 브리핑 10분 갱신 + 수동 새로고침
 */
(function () {
    'use strict';
    const SSE = window.OSH.sse;
    const ctx = window.CTX || '/';

    function statusBinder(id, label) {
        const el = document.getElementById(id);
        if (!el) return function () {};
        return function (state) {
            el.classList.remove('live-pill--stale');
            switch (state) {
                case 'open':
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + '</span>'; return;
                case 'connecting':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 연결 중</span>'; return;
                case 'reconnecting':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 재연결 중</span>'; return;
                case 'closed':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 종료</span>'; return;
            }
        };
    }

    function safeCall(fn) {
        return function (data) { try { fn(data); } catch (e) { console.error(e); } };
    }

    function bindInfoTabs() {
        const tabs = document.querySelectorAll('.info-tab');
        const panels = document.querySelectorAll('.info-panel');
        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                const name = tab.getAttribute('data-tab');
                tabs.forEach(function (t) { t.classList.toggle('is-active', t === tab); });
                panels.forEach(function (p) {
                    const active = (p.id === 'panel-' + name);
                    p.hidden = !active;
                    p.classList.toggle('is-active', active);
                });
            });
        });
    }

    function bindSplitBriefing() {
        const ids = ['Weather', 'Air', 'Emergency', 'Traffic', 'News'];
        const keys = ['weather', 'air', 'emergency', 'traffic', 'news'];
        const updated = document.getElementById('splitUpdated');

        function paint(state) {
            ids.forEach(function (id) {
                const el = document.getElementById('split' + id);
                if (el) el.textContent = state;
            });
        }
        function refresh() {
            if (updated) updated.textContent = '생성 중…';
            fetch(ctx + 'api/gemini/dashboard-split', { headers: { 'Accept': 'application/json' }})
                .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(function (data) {
                    keys.forEach(function (k, i) {
                        const el = document.getElementById('split' + ids[i]);
                        if (el) el.textContent = data[k] || '-';
                    });
                    if (updated) updated.textContent = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
                })
                .catch(function (e) {
                    console.error('split summary', e);
                    paint('요약 실패');
                    if (updated) updated.textContent = '실패';
                });
        }
        const btn = document.getElementById('splitRefreshBtn');
        if (btn) btn.addEventListener('click', refresh);

        refresh();
        setInterval(refresh, 10 * 60 * 1000);
    }

    function start() {
        bindInfoTabs();

        const allStatus = statusBinder('liveAll', '실시간');

        SSE.subscribeMulti(ctx + 'dashboard/main/stream', {
            handlers: {
                dashboard: safeCall(function (data) { window.OSH.application && window.OSH.application.render(data); }),
                weather: safeCall(function (data) {
                    window.OSH.weather && window.OSH.weather.render(data);
                    window.OSH.map     && window.OSH.map.renderWeather(data);
                }),
                emergency: safeCall(function (data) {
                    window.OSH.emergency && window.OSH.emergency.render(data);
                    window.OSH.map       && window.OSH.map.renderEmergency(data);
                }),
                traffic: safeCall(function (data) {
                    window.OSH.traffic && window.OSH.traffic.render(data);
                    window.OSH.map     && window.OSH.map.renderTraffic(data);
                }),
                yeonhap: safeCall(function (data) { window.OSH.news && window.OSH.news.render(data); }),
                air: safeCall(function (data) {
                    if (window.OSH.air) {
                        window.OSH.air.render(data);
                        if (window.OSH.map && window.OSH.air.gradesByCity) {
                            window.OSH.map.setAirGrades(window.OSH.air.gradesByCity());
                        }
                    }
                })
            },
            onStatus: allStatus
        });

        bindSplitBriefing();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
