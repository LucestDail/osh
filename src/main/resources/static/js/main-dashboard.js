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
    function start() {
        const mainStatus      = statusBinder('liveMain',      '메인');
        const emergencyStatus = statusBinder('liveEmergency', '재난문자');
        const trafficStatus   = statusBinder('liveTraffic',   '교통');
        const newsStatus      = statusBinder('liveNews',      '뉴스');

        SSE.subscribeMulti(ctx + 'dashboard/main/stream', {
            handlers: {
                dashboard: function (data) {
                    try { window.OSH.application.render(data); } catch (e) { console.error('application render', e); }
                },
                weather: function (data) {
                    try { window.OSH.weather.render(data); } catch (e) { console.error('weather render', e); }
                },
                emergency: function (data) {
                    try { window.OSH.emergency.render(data); } catch (e) { console.error('emergency render', e); }
                },
                traffic: function (data) {
                    try { window.OSH.traffic.render(data); } catch (e) { console.error('traffic render', e); }
                },
                yeonhap: function (data) {
                    try { window.OSH.news.render(data); } catch (e) { console.error('news render', e); }
                }
            },
            // 단일 연결의 상태를 4개 라이브 인디케이터에 동시에 반영
            onStatus: function (state) {
                mainStatus(state); emergencyStatus(state); trafficStatus(state); newsStatus(state);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
