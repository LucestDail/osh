/**
 * 날씨 파서 — 도시별 weather row 렌더링.
 * 입력: SSE wrapper { weatherJson: "{ weatherJson1: '{...}', ...}" }
 *
 * 2026-10 개편: 31개 도시를 4페이지(pageSize:10)로 넘기던 것을 스크롤 전체보기로
 * 바꾸고, 정렬(기온↓ · 대기질 나쁨순 · 가나다)을 추가했다(D5). 단기예보 TMN/TMX가
 * 비어도(02/11시 발표분만 실림) "-/-°" 빈 줄을 그대로 보여주던 것도 고쳤다(D6) —
 * 비어 있으면 그 기간은 강수확률+체감온도로 대체한다.
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function describe(weatherArr) {
        if (!weatherArr || !weatherArr.length) return '-';
        const w = weatherArr[0];
        return (w && (w.description || w.main)) || '-';
    }

    function skyLabel(sky, pty) {
        const p = Number(pty);
        if (p === 1) return '비';
        if (p === 2) return '비/눈';
        if (p === 3) return '눈';
        if (p === 4) return '소나기';
        switch (Number(sky)) {
            case 1: return '맑음';
            case 3: return '구름많음';
            case 4: return '흐림';
            default: return '-';
        }
    }

    // D6: tmn/tmx 가 둘 다 없으면(저녁 시간대 정상 동작) "-/-°" 대신 강수확률+체감온도를 보여준다.
    function fcstStrip(fcstStr, feelsC) {
        if (!fcstStr) return '';
        const f = H.safeParse(fcstStr);
        if (!f) return '';
        const t = f.today || {};
        const m = f.tomorrow || {};
        const seg = function (label, b) {
            const hasRange = b.tmn != null || b.tmx != null;
            if (hasRange) {
                const range = ((b.tmn != null ? b.tmn : '-') + '/' + (b.tmx != null ? b.tmx : '-')) + '°';
                return label + ' ' + range + '·' + skyLabel(b.sky, b.pty) + (b.pop != null ? ' ' + b.pop + '%' : '');
            }
            const feels = feelsC != null && isFinite(feelsC) ? '체감' + feelsC.toFixed(0) + '°·' : '';
            return label + ' ' + feels + skyLabel(b.sky, b.pty) + (b.pop != null ? ' 강수' + b.pop + '%' : '');
        };
        return seg('오늘', t) + ' · ' + seg('내일', m);
    }

    function row(payload) {
        const name = payload.cityName || payload.name || '-';
        const main = payload.main || {};
        const desc = describe(payload.weather);
        const feelsC = main.feels_like != null ? Number(main.feels_like) - 273.15 : null;
        const humidity = main.humidity != null ? main.humidity + '%' : '-';
        const wind = payload.wind && payload.wind.speed != null ? payload.wind.speed + 'm/s' : '-';
        const fcst = fcstStrip(payload.fcst, feelsC);
        return '<div class="weather-row">' +
                 '<span class="weather-row__name">' + H.esc(name) + '</span>' +
                 '<span class="weather-row__temp">' + H.kToC(main.temp) + '°</span>' +
                 '<span class="weather-row__desc">' + H.esc(desc) + '</span>' +
                 '<span class="weather-row__meta">습도 ' + humidity + ' · 바람 ' + wind + '</span>' +
                 (fcst ? '<span class="weather-row__fcst">' + H.esc(fcst) + '</span>' : '') +
               '</div>';
    }

    const host = function () { return document.getElementById('weatherGrid'); };

    let sortMode = 'temp'; // temp | air | name
    let latestItems = [];

    function sortedItems() {
        const items = latestItems.slice();
        if (sortMode === 'name') {
            items.sort(function (a, b) { return String(a.cityName || a.name).localeCompare(String(b.cityName || b.name), 'ko'); });
        } else if (sortMode === 'air') {
            const grades = (window.OSH.air && window.OSH.air.gradesByCity) ? window.OSH.air.gradesByCity() : {};
            items.sort(function (a, b) {
                const ga = Number(grades[a.cityName || a.name]) || 0;
                const gb = Number(grades[b.cityName || b.name]) || 0;
                if (gb !== ga) return gb - ga;
                return (b.main.temp || 0) - (a.main.temp || 0);
            });
        } else {
            items.sort(function (a, b) { return (b.main.temp || 0) - (a.main.temp || 0); });
        }
        return items;
    }

    function renderSortedInto(h) {
        const items = sortedItems();
        if (!items.length) {
            h.innerHTML = '<div class="empty-state"><div class="empty-state__title">날씨 정보를 불러오는 중입니다</div></div>';
            return;
        }
        h.innerHTML = items.map(row).join('');
    }

    function ensureSortbar() {
        const h = host();
        if (!h || !h.parentElement) return null;
        let bar = h.parentElement.querySelector('.sortbar[data-for="weather"]');
        if (bar) return bar;
        bar = document.createElement('div');
        bar.className = 'sortbar';
        bar.setAttribute('data-for', 'weather');
        bar.innerHTML =
            '정렬 ' +
            '<button type="button" data-sort="temp" class="is-active">기온↓</button>' +
            '<button type="button" data-sort="air">대기질 나쁨순</button>' +
            '<button type="button" data-sort="name">가나다</button>';
        h.parentElement.insertBefore(bar, h);
        bar.addEventListener('click', function (e) {
            const btn = e.target.closest('button[data-sort]');
            if (!btn) return;
            sortMode = btn.getAttribute('data-sort');
            bar.querySelectorAll('button').forEach(function (b) { b.classList.toggle('is-active', b === btn); });
            renderSortedInto(h);
        });
        return bar;
    }

    const pager = window.OSH.pager.create({
        name: 'weather',
        pageSize: 40, // 31개 도시 전부 1페이지 — 페이저는 자동으로 숨는다(D5)
        onRender: function (slice) {
            const h = host();
            if (!h) return;
            ensureSortbar();
            latestItems = slice;
            renderSortedInto(h);
        }
    });

    function render(strJson) {
        const map = H.unwrap(strJson, 'weatherJson');
        if (!map) { pager.update([]); return; }
        const keys = Object.keys(map).sort(function (a, b) {
            return parseInt(a.replace(/\D/g, '')) - parseInt(b.replace(/\D/g, ''));
        });
        const items = [];
        keys.forEach(function (k) {
            const p = H.safeParse(map[k]);
            if (p && p.main) items.push(p);
        });
        pager.update(items);
    }

    (window.OSH = window.OSH || {}).weather = { render: render };
})();
