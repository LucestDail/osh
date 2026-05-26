/**
 * 전국 지도 파서 (Leaflet)
 *  - 19도시 기온 마커 (색상 = 기온 구간)
 *  - 재난문자 지역 펄스 마커 (regions.json 매핑)
 *  - 교통돌발 좌표 마커 (ITS payload coordX/coordY)
 *
 * 다크/라이트 모드 전환 시 tile 자동 교체.
 *
 * 노출 API
 *   window.OSH.map.renderWeather(weatherWrapperStr)
 *   window.OSH.map.renderEmergency(emergencyWrapperStr)
 *   window.OSH.map.renderTraffic(trafficWrapperStr)
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;
    const CTX = window.CTX || '/';

    const TILE_LIGHT = 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';
    const TILE_DARK  = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
    const TILE_ATTR  = '© OpenStreetMap © CARTO';

    let map = null;
    let tileLayer = null;
    let weatherLayer = null;
    let emergencyLayer = null;
    let trafficLayer = null;
    let regionMap = null; // { sido: {...}, sigungu: {...} }
    let regionMapPromise = null;

    function isDark() {
        const explicit = document.documentElement.getAttribute('data-theme');
        if (explicit === 'dark') return true;
        if (explicit === 'light') return false;
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    // 19도시가 분포한 남한 본토 bbox (제주는 살짝 잘리지만 마커가 보이도록 약간만 포함)
    //   SW=[34.0, 126.4], NE=[38.5, 129.9]
    const KR_BOUNDS = [[34.0, 126.4], [38.5, 129.9]];
    const MAX_FIT_ZOOM = 8.5;

    function fitToKorea() {
        if (!map) return;
        try {
            map.fitBounds(KR_BOUNDS, { padding: [4, 4], maxZoom: MAX_FIT_ZOOM, animate: false });
        } catch (e) { /* noop */ }
    }

    function ensureMap() {
        if (map) return map;
        if (typeof L === 'undefined') return null;
        const host = document.getElementById('mapCanvas');
        if (!host) return null;

        map = L.map(host, {
            zoomControl: true,
            attributionControl: true,
            scrollWheelZoom: false,
            zoomSnap: 0.25,
            maxZoom: 14,
            minZoom: 6
        });
        fitToKorea();

        // 카드 사이즈가 처음 0 으로 잡힐 수 있어 한번 재계산
        setTimeout(function () {
            try { map.invalidateSize(); fitToKorea(); } catch (e) { /* noop */ }
        }, 250);
        // window resize 대응
        window.addEventListener('resize', function () {
            if (!map) return;
            try { map.invalidateSize(); fitToKorea(); } catch (e) { /* noop */ }
        });

        applyTile();
        weatherLayer   = L.layerGroup().addTo(map);
        emergencyLayer = L.layerGroup().addTo(map);
        trafficLayer   = L.layerGroup().addTo(map);
        return map;
    }

    function applyTile() {
        if (!map) return;
        if (tileLayer) {
            try { map.removeLayer(tileLayer); } catch (e) { /* noop */ }
        }
        tileLayer = L.tileLayer(isDark() ? TILE_DARK : TILE_LIGHT, {
            attribution: TILE_ATTR,
            subdomains: 'abcd',
            maxZoom: 18
        }).addTo(map);
    }

    function loadRegions() {
        if (regionMap) return Promise.resolve(regionMap);
        if (regionMapPromise) return regionMapPromise;
        regionMapPromise = fetch(CTX + 'data/regions.json')
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (j) { regionMap = j || { sido: {}, sigungu: {} }; return regionMap; })
            .catch(function () { regionMap = { sido: {}, sigungu: {} }; return regionMap; });
        return regionMapPromise;
    }

    function tempColor(t) {
        if (t == null || isNaN(t)) return '#888';
        if (t <= -10) return '#3b82f6';
        if (t <= 0)   return '#60a5fa';
        if (t <= 10)  return '#22d3ee';
        if (t <= 20)  return '#10b981';
        if (t <= 25)  return '#facc15';
        if (t <= 30)  return '#f97316';
        return '#ef4444';
    }

    function airBorderColor(grade) {
        switch (String(grade || '')) {
            case '1': return '#1aa37a';
            case '2': return '#3b82f6';
            case '3': return '#f97316';
            case '4': return '#ef4444';
            default:  return 'transparent';
        }
    }

    function weatherDivIcon(cityName, tempC, airGrade) {
        const fill = tempColor(tempC);
        const ring = airBorderColor(airGrade);
        const t = (tempC == null || isNaN(tempC)) ? '?' : tempC.toFixed(0) + '°';
        const ringStyle = ring === 'transparent' ? '' : '; box-shadow:0 0 0 3px ' + ring;
        const html =
            '<div class="osh-mk osh-mk--weather" style="background:' + fill + ringStyle + '">' +
                '<span class="osh-mk__t">' + t + '</span>' +
                '<span class="osh-mk__n">' + H.esc(cityName) + '</span>' +
            '</div>';
        return L.divIcon({
            className: '',
            html: html,
            iconSize: [46, 28],
            iconAnchor: [23, 14]
        });
    }

    function emergencyDivIcon(level) {
        const cls = 'osh-mk osh-mk--emr osh-mk--emr-' + (level || 'info');
        return L.divIcon({
            className: '',
            html: '<div class="' + cls + '"><span class="osh-mk__pulse"></span></div>',
            iconSize: [14, 14],
            iconAnchor: [7, 7]
        });
    }

    function trafficDivIcon() {
        return L.divIcon({
            className: '',
            html: '<div class="osh-mk osh-mk--traffic">⚠</div>',
            iconSize: [10, 10],
            iconAnchor: [5, 5]
        });
    }

    /* ---------- weather ---------- */

    // air grade 별도 sink 에서 latest 값 보관 (renderWeather 가 그릴 때 적용)
    let airGradeByCity = {}; // { 서울: '2', 부산: '3', ... }
    function setAirGrades(g) { airGradeByCity = g || {}; }

    function renderWeather(strJson) {
        if (!ensureMap()) return;
        const wrapper = H.unwrap(strJson, 'weatherJson');
        if (!wrapper) return;
        weatherLayer.clearLayers();
        Object.keys(wrapper).forEach(function (k) {
            const payload = H.safeParse(wrapper[k]);
            if (!payload || !payload.coord || !payload.main) return;
            const lat = payload.coord.lat;
            const lon = payload.coord.lon;
            const name = payload.cityName || payload.name || '-';
            const tempK = payload.main.temp;
            const tempC = (typeof tempK === 'number') ? (tempK - 273.15) : null;
            const air = airGradeByCity[name];
            const m = L.marker([lat, lon], { icon: weatherDivIcon(name, tempC, air) });
            const popupHtml =
                '<div class="osh-popup"><b>' + H.esc(name) + '</b>' +
                '<div>기온 ' + (tempC == null ? '-' : tempC.toFixed(1)) + '°C</div>' +
                '<div>습도 ' + (payload.main.humidity || '-') + '%</div>' +
                '<div>바람 ' + (payload.wind && payload.wind.speed != null ? payload.wind.speed + ' m/s' : '-') + '</div>' +
                (air ? '<div>대기 등급 ' + H.esc(air) + '</div>' : '') +
                '</div>';
            m.bindPopup(popupHtml);
            m.addTo(weatherLayer);
        });
    }

    /* ---------- emergency ---------- */

    function lookupRegionCoord(rgnName) {
        if (!rgnName || !regionMap) return null;
        const parts = String(rgnName).split(/\s+|,|\//).filter(Boolean);
        // sigungu 우선 탐색 (더 구체적)
        for (let i = parts.length - 1; i >= 0; i--) {
            const p = parts[i];
            if (regionMap.sigungu && regionMap.sigungu[p]) return regionMap.sigungu[p];
            if (regionMap.sido && regionMap.sido[p]) return regionMap.sido[p];
            // 마지막 글자 단축 보정 ("종로구 일대" -> "종로구")
            const trimmed = p.replace(/(시|구|군|도)$/, function (s) { return s; });
            if (regionMap.sigungu && regionMap.sigungu[trimmed]) return regionMap.sigungu[trimmed];
        }
        return null;
    }

    function emergencyLevel(step) {
        const s = String(step || '').toLowerCase();
        if (s.includes('심각') || s.includes('위기')) return 'danger';
        if (s.includes('경계') || s.includes('주의')) return 'warning';
        return 'info';
    }

    function renderEmergency(strJson) {
        if (!ensureMap()) return;
        loadRegions().then(function () {
            emergencyLayer.clearLayers();
            const root = H.unwrap(strJson, 'emergencyJson');
            const items = (root && Array.isArray(root.items)) ? root.items : [];
            if (!items.length) return;
            const max = Math.min(items.length, 30);
            for (let i = 0; i < max; i++) {
                const it = items[i] || {};
                const c = lookupRegionCoord(it.RCPTN_RGN_NM);
                if (!c) continue;
                const level = emergencyLevel(it.EMRG_STEP_NM);
                const m = L.marker(c, { icon: emergencyDivIcon(level) });
                const popupHtml =
                    '<div class="osh-popup"><b>' + H.esc(it.RCPTN_RGN_NM || '-') + '</b>' +
                    '<div class="osh-popup__time">' + H.formatDateTime(it.CRT_DT) + '</div>' +
                    '<div>' + H.esc(it.MSG_CN || '-') + '</div>' +
                    '</div>';
                m.bindPopup(popupHtml, { maxWidth: 320 });
                m.addTo(emergencyLayer);
            }
        });
    }

    /* ---------- traffic ---------- */

    function renderTraffic(strJson) {
        if (!ensureMap()) return;
        trafficLayer.clearLayers();
        const root = H.unwrap(strJson, 'trafficJson');
        const items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items : [];
        if (!items.length) return;
        const max = Math.min(items.length, 50);
        for (let i = 0; i < max; i++) {
            const it = items[i] || {};
            const x = parseFloat(it.coordX);
            const y = parseFloat(it.coordY);
            if (isNaN(x) || isNaN(y)) continue;
            // ITS coord 형식: (경도, 위도) 또는 (위도, 경도) 케이스 분기
            //  - x > 100 이면 경도 (한반도 124~132)
            const lat = (x > 100 && x < 140) ? y : x;
            const lon = (x > 100 && x < 140) ? x : y;
            if (lat < 33 || lat > 39 || lon < 124 || lon > 132) continue;
            const m = L.marker([lat, lon], { icon: trafficDivIcon() });
            const popupHtml =
                '<div class="osh-popup"><b>' + H.esc(it.roadName || '-') + '</b>' +
                '<div class="osh-popup__time">' + H.formatDateTime(it.startDate) + '</div>' +
                '<div>' + H.esc(it.message || '-') + '</div>' +
                '</div>';
            m.bindPopup(popupHtml, { maxWidth: 320 });
            m.addTo(trafficLayer);
        }
    }

    /* ---------- theme change observer ---------- */

    function watchTheme() {
        const mo = new MutationObserver(function (mutations) {
            for (let i = 0; i < mutations.length; i++) {
                if (mutations[i].attributeName === 'data-theme') {
                    applyTile();
                    return;
                }
            }
        });
        mo.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    }
    watchTheme();

    (window.OSH = window.OSH || {}).map = {
        renderWeather: renderWeather,
        renderEmergency: renderEmergency,
        renderTraffic: renderTraffic,
        setAirGrades: setAirGrades
    };
})();
