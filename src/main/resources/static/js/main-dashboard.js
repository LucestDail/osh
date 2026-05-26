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

    /* ===== 신규 이벤트 30초 알림 ===== */
    var _seen = { emergency: {}, traffic: {}, news: {} };
    var _firstLoad = { emergency: true, traffic: true, news: true };

    function dismissAlert(el) {
        el.classList.add('is-out');
        setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 450);
    }

    function showAlert(type, icon, title, body) {
        var box = document.getElementById('mapAlertBox');
        if (!box) return;

        // 최대 4개 유지
        var items = box.querySelectorAll('.map-alert');
        if (items.length >= 4) dismissAlert(items[0]);

        var el = document.createElement('div');
        el.className = 'map-alert map-alert--' + type;
        el.innerHTML =
            '<div class="map-alert__type">' + icon + ' ' + { emergency: '재난 문자', traffic: '교통 돌발', news: '뉴스' }[type] + '</div>' +
            '<div class="map-alert__title">' + H.esc(title) + '</div>' +
            (body ? '<div class="map-alert__body">' + H.esc(body) + '</div>' : '') +
            '<button class="map-alert__close" aria-label="닫기">×</button>' +
            '<div class="map-alert__bar"></div>';

        el.querySelector('.map-alert__close').addEventListener('click', function () { dismissAlert(el); });
        box.appendChild(el);

        var tid = setTimeout(function () { dismissAlert(el); }, 30000);
        el.addEventListener('mouseenter', function () { clearTimeout(tid); });
        el.addEventListener('mouseleave', function () { tid = setTimeout(function () { dismissAlert(el); }, 8000); });

        // 뱃지 카운트
        var badge = document.getElementById('mapAlertBadge');
        if (badge) {
            var n = (parseInt(badge.dataset.count, 10) || 0) + 1;
            badge.dataset.count = n;
            badge.textContent = n + '건 신규';
            badge.style.display = '';
            clearTimeout(badge._tid);
            badge._tid = setTimeout(function () { badge.textContent = ''; badge.dataset.count = 0; }, 60000);
        }
    }

    function checkEmergency(data) {
        var root = H.unwrap(data, 'emergencyJson');
        var items = (root && Array.isArray(root.items)) ? root.items : [];
        if (_firstLoad.emergency) {
            items.forEach(function (it) { _seen.emergency[(it.CRT_DT || '') + (it.RCPTN_RGN_NM || '')] = true; });
            _firstLoad.emergency = false;
            return;
        }
        items.slice(0, 5).forEach(function (it) {
            var key = (it.CRT_DT || '') + (it.RCPTN_RGN_NM || '');
            if (_seen.emergency[key]) return;
            _seen.emergency[key] = true;
            showAlert('emergency', '🚨', it.RCPTN_RGN_NM || '-', H.truncate(it.MSG_CN || '', 55));
        });
    }

    function checkTraffic(data) {
        var root = H.unwrap(data, 'trafficJson');
        var items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items : [];
        if (_firstLoad.traffic) {
            items.forEach(function (it) { _seen.traffic[(it.roadName || '') + (it.startDate || '')] = true; });
            _firstLoad.traffic = false;
            return;
        }
        items.slice(0, 5).forEach(function (it) {
            var key = (it.roadName || '') + (it.startDate || '');
            if (_seen.traffic[key]) return;
            _seen.traffic[key] = true;
            showAlert('traffic', '⚠', it.roadName || '-', H.truncate(it.message || '', 55));
        });
    }

    function checkNews(data) {
        var root = H.unwrap(data, 'yeonhapJson');
        var list = (root && Array.isArray(root.list)) ? root.list : [];
        if (_firstLoad.news) {
            list.forEach(function (it) { _seen.news[it.link || (it.title || '').slice(0, 40)] = true; });
            _firstLoad.news = false;
            return;
        }
        list.slice(0, 5).forEach(function (it) {
            var key = it.link || (it.title || '').slice(0, 40);
            if (_seen.news[key]) return;
            _seen.news[key] = true;
            showAlert('news', '📰', H.truncate(it.title || '-', 50), '');
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
                    checkEmergency(data);
                }),
                traffic: safeCall(function (data) {
                    window.OSH.traffic && window.OSH.traffic.render(data);
                    window.OSH.map     && window.OSH.map.renderTraffic(data);
                    checkTraffic(data);
                }),
                yeonhap: safeCall(function (data) {
                    window.OSH.news && window.OSH.news.render(data);
                    window.OSH.map  && window.OSH.map.renderNews(data);
                    checkNews(data);
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
