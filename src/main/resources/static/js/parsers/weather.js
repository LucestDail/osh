/**
 * 날씨 파서 — 19도시 weather grid 렌더링.
 * 입력: SSE wrapper { weatherJson: "{ weatherJson1: '{...}', ...}" }
 * 각 city payload 안에 server-side 가 추가한 cityName(한글) 사용.
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function describe(weatherArr) {
        if (!weatherArr || !weatherArr.length) return '-';
        const w = weatherArr[0];
        return (w && (w.description || w.main)) || '-';
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
                   '<span>습도</span>'    + '<b>' + (main.humidity != null ? main.humidity + '%' : '-') + '</b>' +
                   '<span>구름</span>'    + '<b>' + (payload.clouds && payload.clouds.all != null ? payload.clouds.all + '%' : '-') + '</b>' +
                   '<span>바람</span>'    + '<b>' + (payload.wind && payload.wind.speed != null ? payload.wind.speed + ' m/s' : '-') + '</b>' +
                   '<span>일출</span>'    + '<b>' + H.formatHm(payload.sys && payload.sys.sunrise) + '</b>' +
                 '</div>' +
               '</div>';
    }

    function render(strJson) {
        const host = document.getElementById('weatherGrid');
        if (!host) return;
        const map = H.unwrap(strJson, 'weatherJson');
        if (!map) {
            host.innerHTML = '<div class="empty-state">' +
                               '<div class="empty-state__title">날씨 정보를 불러오는 중입니다</div>' +
                               '<div class="empty-state__hint">잠시 후 자동으로 표시됩니다</div>' +
                             '</div>';
            return;
        }
        const keys = Object.keys(map).sort(function (a, b) {
            return parseInt(a.replace(/\D/g, '')) - parseInt(b.replace(/\D/g, ''));
        });
        const cards = [];
        let ok = 0;
        keys.forEach(function (k) {
            const payload = H.safeParse(map[k]);
            if (!payload || !payload.main) return;
            cards.push(cell(payload));
            ok++;
        });
        if (!ok) {
            host.innerHTML = '<div class="empty-state empty-state--danger">' +
                               '<div class="empty-state__title">날씨 데이터를 표시할 수 없습니다</div>' +
                               '<div class="empty-state__hint">서버 응답을 확인해주세요</div>' +
                             '</div>';
            return;
        }
        host.innerHTML = cards.join('');
    }

    (window.OSH = window.OSH || {}).weather = { render: render };
})();
