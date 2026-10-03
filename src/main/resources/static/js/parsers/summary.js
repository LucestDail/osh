/**
 * 상황 요약 띠 — 재난/대기질/교통/기온을 숫자로 집계해 "지금 이상이 있는가"에
 * 1초 안에 답한다(진단 D3). 소스별 마지막 수신 경과도 같이 보여준다(D9) —
 * 서버가 이미 "데이터가 실제로 갱신됐을 때만" push 하므로(DashboardServiceImpl
 * renewX*: 실패 시 push 자체를 안 함), 클라이언트가 이벤트 수신 시각만 기록해도
 * "연결은 됐는데 이 소스만 조용히 안 온다"가 그대로 드러난다 — 새 서버 필드 불필요.
 *
 * 전부 main-dashboard.js의 기존 단일 SSE 디스패치에 올라타 호출된다(새 연결 없음).
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    // 실제 수집 주기(ScheduledTasks.java) 기준 — 주기가 다른 소스에 같은 임계값을 쓰면
    // 날씨/대기질은 항상 "경고"로 보이는 오탐이 난다.
    const FRESHNESS = {
        weather:   { warnMs: 90 * 60000, badMs: 150 * 60000, label: '기상' },   // 1시간 주기
        air:       { warnMs: 90 * 60000, badMs: 150 * 60000, label: '대기' },   // 1시간 주기
        emergency: { warnMs: 20 * 60000, badMs: 40 * 60000,  label: '재난' },   // 10분 주기
        traffic:   { warnMs: 5 * 60000,  badMs: 15 * 60000,  label: '교통' },   // 60초 주기
        news:      { warnMs: 5 * 60000,  badMs: 15 * 60000,  label: '뉴스' }    // 60초 주기
    };

    const lastSeenAt = {}; // sourceKey -> ms

    function nowMs() {
        return (window.OSH && window.OSH.serverNowMs) ? window.OSH.serverNowMs : Date.now();
    }

    function markSeen(key) { lastSeenAt[key] = Date.now(); }

    function ageLabel(ms) {
        if (ms < 1000) return '방금';
        if (ms < 60000) return Math.round(ms / 1000) + '초';
        if (ms < 3600000) return Math.round(ms / 60000) + '분';
        return Math.round(ms / 3600000) + '시간';
    }

    function freshnessState(key) {
        const seen = lastSeenAt[key];
        const cfg = FRESHNESS[key];
        if (!seen) return { cls: 'summary-dot--warn', age: '대기' };
        const age = Date.now() - seen;
        const cls = age >= cfg.badMs ? 'summary-dot--bad' : age >= cfg.warnMs ? 'summary-dot--warn' : 'summary-dot--ok';
        return { cls: cls, age: ageLabel(age) };
    }

    function renderFreshnessChip() {
        const el = document.getElementById('sumFreshness');
        if (!el) return;
        const order = ['weather', 'air', 'traffic', 'news', 'emergency'];
        el.innerHTML = order.map(function (k) {
            const s = freshnessState(k);
            const state = s.cls.replace('summary-dot--', '');
            return '<span class="summary-src summary-src--' + state + '">' +
                     '<span class="summary-dot ' + s.cls + '"></span>' +
                     '<span class="summary-src__label">' + FRESHNESS[k].label + '</span>' +
                     '<span class="summary-src__age">' + s.age + '</span>' +
                   '</span>';
        }).join('');
    }

    /* ===== 재난문자 집계 =====
     * 창을 1시간으로 잡았더니 전국 기준으로도 대부분의 시간대에 0건이 떠서,
     * 상시 "0건"만 보여주는 죽은 칩이 됐다. 6시간이면 교대 한 번 분량이라
     * "내가 보기 전에 무슨 일이 있었나"에 답이 된다. */
    const EMERGENCY_WINDOW_MS = 6 * 3600000;
    let _emergencyCount = 0;
    let _emergencyLevel = '';
    let _emergencyBreakdown = '-';

    function onEmergency(strJson) {
        markSeen('emergency');
        const root = H.unwrap(strJson, 'emergencyJson');
        const items = (root && Array.isArray(root.items)) ? root.items : [];
        const cutoff = nowMs() - EMERGENCY_WINDOW_MS;
        const recent = items.filter(function (it) {
            const d = H.parseToDate(it.CRT_DT);
            return d && d.getTime() >= cutoff;
        });
        _emergencyCount = recent.length;
        const counts = {};
        recent.forEach(function (it) {
            const k = it.EMRG_STEP_NM || '기타';
            counts[k] = (counts[k] || 0) + 1;
        });
        const top = Object.keys(counts).sort(function (a, b) { return counts[b] - counts[a]; }).slice(0, 3);
        _emergencyBreakdown = top.length ? top.map(function (k) { return k + ' ' + counts[k]; }).join(' · ') : '최근 6시간 없음';
        // 건수가 0보다 크다고 전부 빨갛게 하면 "코로나 예방접종 안내" 한 건에도 경보가 뜬다.
        // 색은 건수가 아니라 가장 높은 단계가 정한다.
        _emergencyLevel = '';
        recent.forEach(function (it) {
            const step = String(it.EMRG_STEP_NM || '');
            if (step.indexOf('심각') >= 0 || step.indexOf('경계') >= 0) { _emergencyLevel = 'danger'; }
            else if (step.indexOf('주의') >= 0 && _emergencyLevel !== 'danger') { _emergencyLevel = 'warning'; }
        });
        renderChips();
    }

    /* ===== 대기질 집계 ===== */
    let _airBadCount = 0;
    let _airBadNames = '-';

    function onAir(strJson) {
        markSeen('air');
        const root = H.unwrap(strJson, 'airJson');
        const items = (root && Array.isArray(root.sido)) ? root.sido : [];
        const bad = items.filter(function (it) { return Number(it.khaiGrade) >= 3; });
        _airBadCount = bad.length;
        _airBadNames = bad.length ? bad.map(function (it) { return it.name; }).slice(0, 4).join(' · ') : '전국 양호';
        renderChips();
    }

    /* ===== 교통 집계 ===== */
    let _trafficCount = 0;
    let _trafficBreakdown = '-';

    function onTraffic(strJson) {
        markSeen('traffic');
        const root = H.unwrap(strJson, 'trafficJson');
        const items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items : [];
        _trafficCount = items.length;
        const counts = {};
        items.forEach(function (it) {
            const k = it.eventType || '기타';
            counts[k] = (counts[k] || 0) + 1;
        });
        const top = Object.keys(counts).sort(function (a, b) { return counts[b] - counts[a]; }).slice(0, 3);
        _trafficBreakdown = top.length ? top.map(function (k) { return k + ' ' + counts[k]; }).join(' · ') : '진행 중인 돌발 없음';
        renderChips();
    }

    /* ===== 뉴스 (freshness만, 집계는 없음) ===== */
    function onNews() { markSeen('news'); renderChips(); }

    /* ===== 기온 폭 ===== */
    let _tempRange = '-';
    let _tempRangeSub = '-';

    function onWeather(strJson) {
        markSeen('weather');
        const map = H.unwrap(strJson, 'weatherJson');
        if (!map) { renderChips(); return; }
        let maxT = -Infinity, minT = Infinity, maxCity = '-', minCity = '-';
        Object.keys(map).forEach(function (k) {
            const p = H.safeParse(map[k]);
            if (!p || !p.main || p.main.temp == null) return;
            const c = (Number(p.main.temp) - 273.15);
            const name = p.cityName || p.name || k;
            if (c > maxT) { maxT = c; maxCity = name; }
            if (c < minT) { minT = c; minCity = name; }
        });
        if (maxT > -Infinity) {
            _tempRange = maxT.toFixed(1) + ' / ' + minT.toFixed(1) + '°';
            _tempRangeSub = '최고 ' + maxCity + ' · 최저 ' + minCity;
        }
        renderChips();
    }

    /* ===== 렌더 ===== */
    function chip(id, cls, label, value, sub) {
        const el = document.getElementById(id);
        if (!el) return;
        el.className = 'summary-chip' + (cls ? ' ' + cls : '');
        el.title = label + ' — ' + value + ' (' + sub + ')';
        el.innerHTML =
            '<span class="summary-chip__k">' + H.esc(label) + '</span>' +
            '<span class="summary-chip__line">' +
              '<span class="summary-chip__v">' + H.esc(value) + '</span>' +
              '<span class="summary-chip__s">' + H.esc(sub) + '</span>' +
            '</span>';
    }

    function renderChips() {
        chip('sumEmergency', _emergencyLevel ? 'summary-chip--' + _emergencyLevel : '', '재난문자 · 6시간', _emergencyCount + '건', _emergencyBreakdown);
        chip('sumAir', _airBadCount > 0 ? 'summary-chip--warning' : 'summary-chip--ok', '대기질 나쁨 이상', _airBadCount + '개 시도', _airBadNames);
        chip('sumTraffic', '', '교통 돌발', _trafficCount + '건', _trafficBreakdown);
        chip('sumTemp', '', '기온 폭', _tempRange, _tempRangeSub);
        renderFreshnessChip();
    }

    /* ===== 칩 클릭 → 해당 탭으로 이동 ===== */
    function bindChipNav() {
        const nav = {
            sumEmergency: 'emergency',
            sumAir: 'air',
            sumTraffic: 'traffic',
            sumTemp: 'weather'
        };
        Object.keys(nav).forEach(function (chipId) {
            const el = document.getElementById(chipId);
            if (!el) return;
            el.addEventListener('click', function () {
                const tabBtn = document.querySelector('.info-tab[data-tab="' + nav[chipId] + '"]');
                if (tabBtn) tabBtn.click();
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bindChipNav);
    } else {
        bindChipNav();
    }

    // 데이터가 안 와도 "경과 시간" 표시는 계속 흘러야 하므로 1초마다 갱신.
    setInterval(renderFreshnessChip, 1000);
    renderChips();

    (window.OSH = window.OSH || {}).summary = {
        onEmergency: onEmergency,
        onAir: onAir,
        onTraffic: onTraffic,
        onNews: onNews,
        onWeather: onWeather
    };
})();
