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

    function start() {
        // /main/info : application 전용 (시계 / 메모리 / 로드) — 1초 tick, 페이로드 ~300B
        SSE.subscribe(ctx + 'dashboard/main/info', {
            onMessage: function (data) {
                try { window.OSH.application.render(data); } catch (e) { console.error('application render', e); }
            },
            onStatus: statusBinder('liveMain', '메인')
        });

        // /main/weather : 19도시 날씨 — 변경 시점에만 (1시간 주기)
        SSE.subscribe(ctx + 'dashboard/main/weather', {
            onMessage: function (data) {
                try { window.OSH.weather.render(data); } catch (e) { console.error('weather render', e); }
            }
        });

        // /main/emergency : 60s
        SSE.subscribe(ctx + 'dashboard/main/emergency', {
            onMessage: function (data) {
                try { window.OSH.emergency.render(data); } catch (e) { console.error('emergency render', e); }
            },
            onStatus: statusBinder('liveEmergency', '재난문자')
        });

        // /main/traffic : 60s
        SSE.subscribe(ctx + 'dashboard/main/traffic', {
            onMessage: function (data) {
                try { window.OSH.traffic.render(data); } catch (e) { console.error('traffic render', e); }
            },
            onStatus: statusBinder('liveTraffic', '교통')
        });

        // /main/yeonhap : 60s
        SSE.subscribe(ctx + 'dashboard/main/yeonhap', {
            onMessage: function (data) {
                try { window.OSH.news.render(data); } catch (e) { console.error('news render', e); }
            },
            onStatus: statusBinder('liveNews', '뉴스')
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
