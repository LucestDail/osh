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


    /**
     * D6: tmn/tmx 는 기상청 단기예보 특성상 02시·11시 발표분에만 실려서 저녁엔 비는 게
     * 정상이다. 예전엔 그걸 "-/-°" 로 그려 31줄이 전부 빈 칸이었고, 고친 뒤에도
     * "오늘 체감21°·맑음 강수0% · 내일 16/26°·구름많음 20%" 처럼 한 줄에 사실 여섯 개를
     * 욱여넣어 읽히지 않았다. 목록의 보조 컬럼은 **한 가지만** 말한다.
     */
    function shortFcst(fcstStr, feelsC) {
        const f = H.safeParse(fcstStr);
        if (f) {
            const t = f.today || {}, m = f.tomorrow || {};
            if (m.tmn != null && m.tmx != null) return '내일 ' + m.tmn + '/' + m.tmx + '°';
            if (t.tmn != null && t.tmx != null) return '오늘 ' + t.tmn + '/' + t.tmx + '°';
            const pop = m.pop != null ? m.pop : t.pop;
            if (pop != null && Number(pop) > 0) return '강수 ' + pop + '%';
        }
        if (feelsC != null && isFinite(feelsC)) return '체감 ' + feelsC.toFixed(0) + '°';
        return '';
    }

    function row(payload) {
        const name = payload.cityName || payload.name || '-';
        const main = payload.main || {};
        const tempC = main.temp != null ? Number(main.temp) - 273.15 : null;
        const feelsC = main.feels_like != null ? Number(main.feels_like) - 273.15 : null;
        // 지도 코로플레스와 같은 밴드 색 — "지도에서 붉은 곳 = 목록에서 붉은 줄"
        const band = H.tempBandColor(tempC) || 'transparent';
        const sky = H.wxKo(payload.weather && payload.weather[0] && payload.weather[0].main,
                           describe(payload.weather));
        const meta = [
            sky,
            main.humidity != null ? '습도 ' + main.humidity + '%' : null,
            // OWM 풍속은 2.06 / 1.03 처럼 자릿수가 제각각이라 목록에서 열이 지저분해진다
            (payload.wind && payload.wind.speed != null) ? '바람 ' + Number(payload.wind.speed).toFixed(1) + 'm/s' : null
        ].filter(Boolean).join(' · ');
        const fcst = shortFcst(payload.fcst, feelsC);

        return '<div class="weather-row">' +
                 '<span class="weather-row__band" style="background:' + band + '"></span>' +
                 '<span class="weather-row__name">' + H.esc(name) + '</span>' +
                 '<span class="weather-row__temp">' + H.kToC(main.temp) + '°</span>' +
                 '<span class="weather-row__meta">' + H.esc(meta) + '</span>' +
                 '<span class="weather-row__fcst">' + H.esc(fcst) + '</span>' +
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
