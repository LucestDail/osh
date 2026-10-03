/**
 * 상황 타임라인 — 재난·교통·뉴스를 하나의 시간순 스트림으로 합친다(신규, D2가 비운 자리).
 * 예전엔 세 탭을 번갈아 눌러야 "최근에 무슨 일이 있었나"를 알 수 있었다.
 * main-dashboard.js의 기존 SSE 디스패치에 올라타 호출된다(새 연결 없음).
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;
    const MAX_PER_SOURCE = 30;
    const MAX_RENDER = 60;

    let emergencyItems = [];
    let trafficItems = [];
    let newsItems = [];
    let activeFilter = 'all';
    let _maxSeenTs = 0;
    let _ready = false;

    function stepTag(step) {
        const s = String(step || '').toLowerCase();
        if (s.includes('심각') || s.includes('위기') || s.includes('경계')) return 'danger';
        if (s.includes('주의')) return 'warning';
        if (s.includes('관심')) return 'info';
        return 'muted';
    }

    function timeHm(d) {
        if (!d) return '--:--';
        return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Asia/Seoul' });
    }

    function onEmergency(strJson) {
        const root = H.unwrap(strJson, 'emergencyJson');
        const items = (root && Array.isArray(root.items)) ? root.items : [];
        emergencyItems = items.slice(0, MAX_PER_SOURCE).map(function (it) {
            const d = H.parseToDate(it.CRT_DT);
            const variant = stepTag(it.EMRG_STEP_NM);
            return {
                ts: d ? d.getTime() : 0,
                type: 'emergency',
                variant: variant,
                tagCls: 'tl-tag--' + variant,
                tagLabel: it.EMRG_STEP_NM || '안내',
                html: '<b>' + H.esc((it.RCPTN_RGN_NM || '-').trim()) + '</b> ' + H.esc(H.truncate(it.MSG_CN || '-', 70))
            };
        });
        render();
    }

    function onTraffic(strJson) {
        const root = H.unwrap(strJson, 'trafficJson');
        const items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items : [];
        trafficItems = items.slice(0, MAX_PER_SOURCE).map(function (it) {
            const d = H.parseToDate(it.startDate);
            const road = (it.roadName || '-') + (it.roadDrcType ? ' (' + it.roadDrcType + ')' : '');
            return {
                ts: d ? d.getTime() : 0,
                type: 'traffic',
                variant: 'warning',
                tagCls: 'tl-tag--warning',
                tagLabel: it.eventType || '돌발',
                html: '<b>' + H.esc(road) + '</b> ' + H.esc(H.truncate(it.message || '-', 70))
            };
        });
        render();
    }

    function onNews(strJson) {
        const root = H.unwrap(strJson, 'yeonhapJson');
        const items = (root && root.data && Array.isArray(root.data.items)) ? root.data.items : [];
        newsItems = items.slice(0, MAX_PER_SOURCE).map(function (it) {
            const d = H.parseToDate(it.createDT);
            return {
                ts: d ? d.getTime() : 0,
                type: 'news',
                variant: 'info',
                tagCls: 'tl-tag--info',
                tagLabel: '뉴스',
                html: '<b>' + H.esc(H.truncate(it.title || '-', 70)) + '</b>'
            };
        });
        render();
    }

    function merged() {
        return emergencyItems.concat(trafficItems, newsItems)
            .filter(function (it) { return it.ts > 0; })
            .sort(function (a, b) { return b.ts - a.ts; })
            .slice(0, MAX_RENDER);
    }

    function render() {
        const host = document.getElementById('timelineList');
        if (!host) return;
        const all = merged();
        const list = activeFilter === 'all' ? all : all.filter(function (it) { return it.type === activeFilter; });

        if (!list.length) {
            host.innerHTML = '<div class="empty-state"><div class="empty-state__title">표시할 상황이 없습니다</div><div class="empty-state__hint">재난·교통·뉴스가 들어오면 여기 모입니다</div></div>';
            return;
        }

        const newMax = all.length ? all[0].ts : 0;
        host.innerHTML = list.map(function (it) {
            const isNew = _ready && it.ts > _maxSeenTs;
            const d = new Date(it.ts);
            // 아이콘 칸(⚠/🚧/📰)을 없앴다 — 바로 옆 태그가 같은 말을 하고 있었고,
            // 이모지마다 폭·색이 달라 줄이 들쭉날쭉했다. 심각도는 왼쪽 레일로만 말한다.
            return '<div class="tl-row tl-row--' + it.variant + (isNew ? ' is-new' : '') + '">' +
                '<span class="tl-row__rail" aria-hidden="true"></span>' +
                '<span class="tl-row__t">' + timeHm(d) + '</span>' +
                '<span class="tl-row__x"><span class="tl-tag ' + it.tagCls + '">' + H.esc(it.tagLabel) + '</span>' + it.html + '</span>' +
                '</div>';
        }).join('');

        if (newMax > _maxSeenTs) _maxSeenTs = newMax;
        _ready = true;
    }

    function bindFilters() {
        const bar = document.getElementById('timelineFilters');
        if (!bar) return;
        bar.addEventListener('click', function (e) {
            const btn = e.target.closest('button[data-filter]');
            if (!btn) return;
            activeFilter = btn.getAttribute('data-filter');
            bar.querySelectorAll('button').forEach(function (b) { b.classList.toggle('is-active', b === btn); });
            render();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindFilters);
    } else {
        bindFilters();
    }

    (window.OSH = window.OSH || {}).timeline = {
        onEmergency: onEmergency,
        onTraffic: onTraffic,
        onNews: onNews
    };
})();
