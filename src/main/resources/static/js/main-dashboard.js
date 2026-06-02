/**
 * 메인 대시보드 진입점.
 *  - 단일 SSE (/dashboard/main/stream) 다중 event type 디스패치
 *  - 정보 탭 5종 전환
 *  - AI 한줄 브리핑 10분 갱신 + 수동 새로고침
 *  - 지도 전체화면 토글
 *  - 신규 재난/교통/뉴스 30초 오버레이 알림
 */
(function () {
    'use strict';
    const SSE = window.OSH.sse;
    const H   = window.OSH.helpers;
    const ctx = window.CTX || '/';

    /* ===== 상태 pill ===== */
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

    /* ===== 정보 탭 ===== */
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

    /* ===== AI 브리핑 ===== */
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
        function applyBriefingMeta(res) {
            if (!updated || !res) return;
            var cached = res.headers.get('X-Briefing-Cached') === 'true';
            var genAt = res.headers.get('X-Briefing-Generated-At') || '';
            if (cached && genAt) {
                updated.textContent = '캐시 ' + genAt.slice(11, 16);
                updated.title = '서버 캐시 (' + genAt + ') · 1시간마다 자동 갱신';
            } else if (genAt) {
                updated.textContent = '생성 ' + genAt.slice(11, 16);
                updated.title = '방금 생성 (' + genAt + ')';
            } else {
                updated.textContent = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Seoul' });
            }
        }

        function refresh(userAccess) {
            const icon = document.getElementById('splitRefreshIcon');
            if (icon) icon.classList.add('is-spinning');
            if (updated) updated.textContent = userAccess ? '생성 중…' : '불러오는 중…';
            var url = ctx + 'api/gemini/dashboard-split' + (userAccess ? '?access=true' : '');
            fetch(url, { headers: { 'Accept': 'application/json' }})
                .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json().then(function (data) { return { data: data, res: r }; }); })
                .then(function (wrap) {
                    var data = wrap.data;
                    keys.forEach(function (k, i) {
                        const el = document.getElementById('split' + ids[i]);
                        if (el) el.textContent = data[k] || '-';
                    });
                    applyBriefingMeta(wrap.res);
                })
                .catch(function (e) {
                    console.error('split summary', e);
                    paint('요약 실패');
                    if (updated) updated.textContent = '실패';
                })
                .finally(function () {
                    if (icon) icon.classList.remove('is-spinning');
                });
        }
        const btn = document.getElementById('splitRefreshBtn');
        if (btn) btn.addEventListener('click', function () { refresh(true); });
        refresh(true);
    }

    /* ===== 지도 전체화면 ===== */
    function bindMapFullscreen() {
        const card = document.getElementById('mapCard');
        const btn  = document.getElementById('mapFsBtn');
        if (!card || !btn) return;

        function toggle(on) {
            card.classList.toggle('map-fs', on);
            btn.textContent = card.classList.contains('map-fs') ? '✕' : '⛶';
            btn.title = card.classList.contains('map-fs') ? '전체화면 해제 (ESC)' : '전체화면 (ESC 해제)';
            setTimeout(function () {
                window.OSH.map && window.OSH.map.invalidateSize && window.OSH.map.invalidateSize();
            }, 80);
        }

        btn.addEventListener('click', function () { toggle(!card.classList.contains('map-fs')); });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && card.classList.contains('map-fs')) toggle(false);
        });
    }

    /* ===== 시작 ===== */
    function start() {
        bindInfoTabs();
        bindMapFullscreen();

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
                yeonhap: safeCall(function (data) {
                    window.OSH.news && window.OSH.news.render(data);
                    window.OSH.map  && window.OSH.map.renderNews(data);
                }),
                air: safeCall(function (data) {
                    if (!window.OSH.air) return;
                    window.OSH.air.render(data);
                    if (window.OSH.map) {
                        if (window.OSH.air.gradesByCity) {
                            window.OSH.map.setAirGrades(window.OSH.air.gradesByCity());
                        }
                        if (window.OSH.air.infoByCity && window.OSH.map.setAirInfo) {
                            window.OSH.map.setAirInfo(window.OSH.air.infoByCity());
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
