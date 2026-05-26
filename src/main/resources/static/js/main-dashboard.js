/**
 * 메인 대시보드 진입점 — 4개 SSE 구독 + 연결 상태 표시.
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
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 실시간</span>';
                    return;
                case 'connecting':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 연결 중</span>';
                    return;
                case 'reconnecting':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 재연결 중</span>';
                    return;
                case 'closed':
                    el.classList.add('live-pill--stale');
                    el.innerHTML = '<span class="live-pill__dot"></span><span>' + label + ' · 종료</span>';
                    return;
            }
        };
    }

    /**
     * 단일 SSE 채널 — 한 connection 안에서 event type 별로 디스패치.
     *
     * 이전: 5개 EventSource → 브라우저 HTTP/1.1 origin-per-host(6) 풀을 다 차지
     *       → 같은 도메인의 myapi/simpleStock/my-computer/aim 호출이 connection 대기.
     * 현재: 1개 EventSource 만 사용 → 다른 서비스 호출에 영향 없음.
     */
    function safeCall(fn) {
        return function (data) { try { fn(data); } catch (e) { console.error(e); } };
    }

    function bindSplitBriefing() {
        const ids = ['Weather', 'Air', 'Emergency', 'Traffic', 'News'];
        const keys = ['weather', 'air', 'emergency', 'traffic', 'news'];
        function paint(state) {
            ids.forEach(function (id) {
                const el = document.getElementById('split' + id);
                if (el) el.textContent = state;
            });
        }
        function refresh() {
            paint('생성 중...');
            fetch(ctx + 'api/gemini/dashboard-split', { headers: { 'Accept': 'application/json' }})
                .then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                })
                .then(function (data) {
                    keys.forEach(function (k, i) {
                        const el = document.getElementById('split' + ids[i]);
                        if (el) el.textContent = data[k] || '-';
                    });
                })
                .catch(function (e) {
                    console.error('split summary', e);
                    paint('요약 실패');
                });
        }
        const btn = document.getElementById('splitRefreshBtn');
        if (btn) btn.addEventListener('click', refresh);

        // 초기 1회 + 10분 주기
        refresh();
        setInterval(refresh, 10 * 60 * 1000);
    }

    function start() {
        const mainStatus      = statusBinder('liveMain',      '메인');
        const emergencyStatus = statusBinder('liveEmergency', '재난문자');
        const trafficStatus   = statusBinder('liveTraffic',   '교통');
        const newsStatus      = statusBinder('liveNews',      '뉴스');
        const airStatus       = statusBinder('liveAir',       '대기질');

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
            // 단일 연결의 상태를 5개 라이브 인디케이터에 동시 반영
            onStatus: function (state) {
                mainStatus(state); emergencyStatus(state); trafficStatus(state); newsStatus(state); airStatus(state);
            }
        });

        bindSplitBriefing();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
