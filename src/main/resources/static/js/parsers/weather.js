/**
 * 날씨 파서 — 도시별 weather grid 렌더링.
 * 입력: SSE wrapper { weatherJson: "{ weatherJson1: '{...}', ...}" }
 * 페이저: 6도시/페이지 (3행 x 2열)
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

    function fcstStrip(fcstStr) {
        if (!fcstStr) return '';
        const f = H.safeParse(fcstStr);
        if (!f) return '';
        const t = f.today || {};
        const m = f.tomorrow || {};
        const seg = function (label, b) {
            const range = ((b.tmn != null ? b.tmn : '-') + '/' + (b.tmx != null ? b.tmx : '-')) + '°';
            return '<span>' + label + ' <b>' + range + '</b> · ' + skyLabel(b.sky, b.pty) +
                   (b.pop != null ? ' <b>' + b.pop + '%</b>' : '') + '</span>';
        };
        return '<div class="weather-cell__fcst">' + seg('오늘', t) + seg('내일', m) + '</div>';
    }

    function cell(payload) {
        const name = payload.cityName || payload.name || '-';
        const main = payload.main || {};
        const desc = describe(payload.weather);
        return '<div class="weather-cell">' +
                 '<div class="weather-cell__head">' +
                   '<span class="weather-cell__name">' + H.esc(name) + '</span>' +
                   '<span class="weather-cell__temp">' + H.kToC(main.temp) + '°</span>' +
                 '</div>' +
                 '<div class="weather-cell__desc">' + H.esc(desc) + '</div>' +
                 '<div class="weather-cell__meta">' +
                   '<span>습도</span>' + '<b>' + (main.humidity != null ? main.humidity + '%' : '-') + '</b>' +
                   '<span>구름</span>' + '<b>' + (payload.clouds && payload.clouds.all != null ? payload.clouds.all + '%' : '-') + '</b>' +
                   '<span>바람</span>' + '<b>' + (payload.wind && payload.wind.speed != null ? payload.wind.speed + ' m/s' : '-') + '</b>' +
                   '<span>일출</span>' + '<b>' + H.formatHm(payload.sys && payload.sys.sunrise) + '</b>' +
                 '</div>' +
                 fcstStrip(payload.fcst) +
               '</div>';
    }

    const host = function () { return document.getElementById('weatherGrid'); };

    const pager = window.OSH.pager.create({
        name: 'weather',
        pageSize: 10,
        onRender: function (slice) {
            const h = host();
            if (!h) return;
            if (!slice.length) {
                h.innerHTML = '<div class="empty-state"><div class="empty-state__title">날씨 정보를 불러오는 중입니다</div></div>';
                return;
            }
            h.innerHTML = slice.map(cell).join('');
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
