/**
 * 전국 지도 파서 (Leaflet)
 *  - 시도별 기온 코로플레스 (GeoJSON 지역 색칠, hover 시 기상+대기질)
 *  - 재난문자 지역 펄스 마커
 *  - 교통돌발 좌표 마커
 *  - 뉴스 지명 키워드 매칭 마커
 *  - 온도 범례 (지도 좌하단)
 *
 * API
 *   window.OSH.map.renderWeather(weatherWrapperStr)
 *   window.OSH.map.renderEmergency(emergencyWrapperStr)
 *   window.OSH.map.renderTraffic(trafficWrapperStr)
 *   window.OSH.map.renderNews(newsWrapperStr)
 *   window.OSH.map.setAirGrades(gradesByCity)
 *   window.OSH.map.setAirInfo(infoByCity)
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
    let choroplethLayer = null;   // 기온 코로플레스 (GeoJSON)
    let cityLayer = null;         // 19도시 원 + 이름 마커
    let emergencyLayer = null;
    let trafficLayer = null;
    let newsLayer = null;
    let _provinceLayerMap = {};   // provinceName → Leaflet layer

    let regionMap = null;
    let regionMapPromise = null;
    let provincesData = null;     // kr-provinces GeoJSON
    let provincesPromise = null;

    // 데이터 캐시
    let _weatherByCity = {};    // cityName → { tempC, humidity, windSpeed, description, lat, lon }
    let _airGradeByCity = {};   // cityName → grade string
    let _airInfoByCity = {};    // cityName → { grade, pm10, pm25, label }

    // 시도 → 대표 도시 매핑 (여러 도시는 온도 평균, 툴팁은 첫번째 도시)
    const PROVINCE_CITIES = {
        '서울특별시':      ['서울'],
        '부산광역시':      ['부산'],
        '대구광역시':      ['대구'],
        '인천광역시':      ['인천'],
        '광주광역시':      ['광주'],
        '대전광역시':      ['대전'],
        '울산광역시':      ['울산'],
        '세종특별자치시':  ['대전'],
        '경기도':         ['수원', '고양', '용인'],
        '강원도':         ['춘천', '원주', '강릉', '속초'],
        '충청북도':       ['대전'],
        '충청남도':       ['대전'],
        '전라북도':       ['광주'],
        '전라남도':       ['광주'],
        '경상북도':       ['포항', '김천'],
        '경상남도':       ['창원', '김해'],
        '제주특별자치도': ['제주']
    };

    function isDark() {
        const explicit = document.documentElement.getAttribute('data-theme');
        if (explicit === 'dark') return true;
        if (explicit === 'light') return false;
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }

    // 본토 중심 (제주 포함 전국) — setView 로 고정 줌 사용
    const KR_CENTER = [36.5, 127.9];
    const KR_ZOOM   = 7.75;   // 꽉 채운 본토뷰

    function fitToKorea() {
        if (!map) return;
        try { map.setView(KR_CENTER, KR_ZOOM, { animate: false }); }
        catch (e) { /* noop */ }
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
            minZoom: 5
        });
        fitToKorea();

        setTimeout(function () {
            try { map.invalidateSize(); fitToKorea(); } catch (e) { /* noop */ }
        }, 250);
        window.addEventListener('resize', function () {
            if (!map) return;
            try { map.invalidateSize(); fitToKorea(); } catch (e) { /* noop */ }
        });

        applyTile();
        cityLayer      = L.layerGroup().addTo(map);
        emergencyLayer = L.layerGroup().addTo(map);
        trafficLayer   = L.layerGroup().addTo(map);
        newsLayer      = L.layerGroup().addTo(map);

        buildLegend();
        buildLocateControl();
        // 페이지 로드 시 조용히 자동 위치 탐지 시도
        setTimeout(tryLocate, 600);
        return map;
    }

    function applyTile() {
        if (!map) return;
        if (tileLayer) { try { map.removeLayer(tileLayer); } catch (e) { /* noop */ } }
        tileLayer = L.tileLayer(isDark() ? TILE_DARK : TILE_LIGHT, {
            attribution: TILE_ATTR,
            subdomains: 'abcd',
            maxZoom: 18
        }).addTo(map);
        // 레이어 순서: tile → choropleth → city → emergency → traffic → news
        if (choroplethLayer) {
            choroplethLayer.bringToBack();
            cityLayer.bringToFront();
            emergencyLayer.bringToFront();
            trafficLayer.bringToFront();
            newsLayer.bringToFront();
        }
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

    function loadProvinces() {
        if (provincesData) return Promise.resolve(provincesData);
        if (provincesPromise) return provincesPromise;
        provincesPromise = fetch(CTX + 'data/kr-provinces.json')
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (j) { provincesData = j; return j; })
            .catch(function (e) { console.warn('kr-provinces.json 로드 실패', e); return null; });
        return provincesPromise;
    }

    /* ========== 온도 색상 ========== */

    function tempToFillColor(tempC) {
        if (tempC == null || isNaN(tempC)) return isDark() ? '#2a2a2a' : '#ddd';
        if (tempC <= 0)   return '#4575b4';
        if (tempC <= 5)   return '#74add1';
        if (tempC <= 10)  return '#abd9e9';
        if (tempC <= 15)  return '#e0f3f8';
        if (tempC <= 20)  return '#a8d990';
        if (tempC <= 25)  return '#fee090';
        if (tempC <= 30)  return '#fdae61';
        return '#d73027';
    }

    function getProvinceTempC(name) {
        const cities = PROVINCE_CITIES[name] || [];
        const temps = [];
        cities.forEach(function (c) {
            const d = _weatherByCity[c];
            if (d && d.tempC != null) temps.push(d.tempC);
        });
        if (!temps.length) return null;
        return temps.reduce(function (a, b) { return a + b; }, 0) / temps.length;
    }

    /* ========== 코로플레스 ========== */

    function provinceBaseStyle(feature) {
        const tempC = getProvinceTempC(feature.properties.name);
        return {
            fillColor: tempToFillColor(tempC),
            fillOpacity: 0.6,
            weight: 1,
            color: isDark() ? '#666' : '#aaa',
            opacity: 0.9
        };
    }

    function buildTooltipHtml(provinceName) {
        const cities = PROVINCE_CITIES[provinceName] || [];
        const primaryCity = cities[0];
        const wd = primaryCity ? _weatherByCity[primaryCity] : null;
        const ai = primaryCity ? _airInfoByCity[primaryCity] : null;

        let html = '<div class="osh-popup"><b>' + H.esc(provinceName) + '</b>';
        if (primaryCity && primaryCity !== provinceName.replace(/(특별시|광역시|특별자치시|특별자치도|도)/, '')) {
            html += '<div class="osh-popup__time">' + H.esc(primaryCity) + ' 기준</div>';
        }
        if (wd) {
            html += '<div>기온 ' + wd.tempC.toFixed(1) + '°C</div>';
            if (wd.humidity != null) html += '<div>습도 ' + wd.humidity + '%</div>';
            if (wd.windSpeed != null) html += '<div>바람 ' + wd.windSpeed.toFixed(1) + ' m/s</div>';
            if (wd.description) html += '<div>' + H.esc(wd.description) + '</div>';
        } else {
            html += '<div>날씨 정보 없음</div>';
        }
        if (ai) {
            const parts = [];
            if (ai.label) parts.push(ai.label);
            if (ai.pm10 != null && ai.pm10 >= 0) parts.push('PM10 ' + ai.pm10 + 'µg');
            if (ai.pm25 != null && ai.pm25 >= 0) parts.push('PM2.5 ' + ai.pm25 + 'µg');
            if (parts.length) html += '<div>대기질 ' + H.esc(parts.join(' · ')) + '</div>';
        }
        html += '</div>';
        return html;
    }

    function updateChoroplethStyles() {
        if (!choroplethLayer) return;
        Object.keys(_provinceLayerMap).forEach(function (name) {
            const layer = _provinceLayerMap[name];
            const tempC = getProvinceTempC(name);
            layer.setStyle({
                fillColor: tempToFillColor(tempC),
                fillOpacity: 0.6,
                weight: 1,
                color: isDark() ? '#666' : '#aaa',
                opacity: 0.9
            });
            // 팝업 내용 갱신 (이미 열려 있으면 닫고 갱신)
            layer.unbindPopup();
            layer.bindPopup(buildTooltipHtml(name), { maxWidth: 240 });
        });
        // 도시 마커 온도색도 갱신
        updateCityLayerStyles();
    }

    function initChoropleth(geojson) {
        if (!map) return;
        if (choroplethLayer) {
            try { map.removeLayer(choroplethLayer); } catch (e) { /* noop */ }
        }
        _provinceLayerMap = {};

        choroplethLayer = L.geoJSON(geojson, {
            style: provinceBaseStyle,
            onEachFeature: function (feature, layer) {
                const name = feature.properties.name;
                _provinceLayerMap[name] = layer;

                // 클릭 시 팝업 (hover 는 테두리 강조만)
                layer.bindPopup(buildTooltipHtml(name), { maxWidth: 240 });

                layer.on('mouseover', function () {
                    layer.setStyle({ weight: 2.5, color: isDark() ? '#fff' : '#222', fillOpacity: 0.82 });
                });
                layer.on('mouseout', function () {
                    choroplethLayer.resetStyle(layer);
                });
            }
        });

        choroplethLayer.addTo(map);
        // 레이어 순서 정리
        cityLayer.bringToFront();
        emergencyLayer.bringToFront();
        trafficLayer.bringToFront();
        newsLayer.bringToFront();
    }

    /* ========== 온도 범례 ========== */

    function buildLegend() {
        if (!map) return;
        const legend = L.control({ position: 'bottomleft' });
        legend.onAdd = function () {
            const div = L.DomUtil.create('div', 'osh-legend');
            const bands = [
                ['#4575b4', '0°C 이하'],
                ['#74add1', '0 – 5°C'],
                ['#abd9e9', '5 – 10°C'],
                ['#e0f3f8', '10 – 15°C'],
                ['#a8d990', '15 – 20°C'],
                ['#fee090', '20 – 25°C'],
                ['#fdae61', '25 – 30°C'],
                ['#d73027', '30°C 이상']
            ];
            div.innerHTML =
                '<div class="osh-legend__title">🌡 기온</div>' +
                bands.map(function (b) {
                    return '<div class="osh-legend__row">' +
                        '<i style="background:' + b[0] + '"></i>' +
                        '<span>' + b[1] + '</span>' +
                        '</div>';
                }).join('');
            return div;
        };
        legend.addTo(map);
    }

    /* ========== 도시 레이어 (circleMarker + permanent tooltip) ========== */

    function buildCityPopupHtml(cityName) {
        const wd = _weatherByCity[cityName];
        const ai = _airInfoByCity[cityName];
        let html = '<div class="osh-popup"><b>' + H.esc(cityName) + '</b>';
        if (wd) {
            html += '<div>기온 ' + (wd.tempC != null ? wd.tempC.toFixed(1) : '-') + '°C</div>';
            if (wd.humidity != null) html += '<div>습도 ' + wd.humidity + '%</div>';
            if (wd.windSpeed != null) html += '<div>바람 ' + wd.windSpeed.toFixed(1) + ' m/s</div>';
            if (wd.description) html += '<div>' + H.esc(wd.description) + '</div>';
        }
        if (ai) {
            const parts = [];
            if (ai.label) parts.push(ai.label);
            if (ai.pm10 != null && ai.pm10 >= 0) parts.push('PM10 ' + ai.pm10 + 'µg');
            if (ai.pm25 != null && ai.pm25 >= 0) parts.push('PM2.5 ' + ai.pm25 + 'µg');
            if (parts.length) html += '<div>대기질 ' + H.esc(parts.join(' · ')) + '</div>';
        }
        html += '</div>';
        return html;
    }

    function renderCityLayer() {
        if (!cityLayer) return;
        cityLayer.clearLayers();
        Object.keys(_weatherByCity).forEach(function (name) {
            const wd = _weatherByCity[name];
            if (wd.lat == null || wd.lon == null) return;

            // circleMarker → 클릭 영역이 반지름 5px 의 원으로 제한돼 하위 마커 클릭 차단 없음
            const c = L.circleMarker([wd.lat, wd.lon], {
                radius: 5,
                fillColor: tempToFillColor(wd.tempC),
                color: '#fff',
                weight: 1.5,
                fillOpacity: 0.95,
                opacity: 1,
                pane: 'markerPane'
            });
            // 도시명 permanent label (non-interactive)
            c.bindTooltip(H.esc(name), {
                permanent: true,
                direction: 'right',
                offset: [6, 0],
                className: 'osh-city-label',
                interactive: false
            });
            c.bindPopup(buildCityPopupHtml(name), { maxWidth: 200 });
            c.addTo(cityLayer);
        });
    }

    function updateCityLayerStyles() {
        // 대기질 업데이트 시 도시 레이어는 색 변경 없음 (온도 기반) — noop
    }

    /* ========== 현재 위치 (GPS → IP 폴백) ========== */

    let _userMarker = null;
    let _locBtn = null;   // 버튼 엘리먼트 참조 (로딩 아이콘 토글용)

    function findNearestCity(lat, lon) {
        let minDist = Infinity;
        let nearest = null;
        Object.keys(_weatherByCity).forEach(function (name) {
            const wd = _weatherByCity[name];
            if (wd.lat == null || wd.lon == null) return;
            const d = (wd.lat - lat) * (wd.lat - lat) + (wd.lon - lon) * (wd.lon - lon);
            if (d < minDist) { minDist = d; nearest = name; }
        });
        return nearest;
    }

    function buildUserPopupHtml(lat, lon, sourceLabel) {
        const city = findNearestCity(lat, lon);
        const wd = city ? _weatherByCity[city] : null;
        const ai = city ? _airInfoByCity[city] : null;
        let html = '<div class="osh-popup"><b>📍 현재 위치</b>';
        if (sourceLabel) html += '<div class="osh-popup__time">위치 방식: ' + H.esc(sourceLabel) + '</div>';
        if (city) html += '<div class="osh-popup__time">가장 가까운 관측지: ' + H.esc(city) + '</div>';
        if (wd) {
            html += '<div>기온 ' + (wd.tempC != null ? wd.tempC.toFixed(1) : '-') + '°C</div>';
            if (wd.humidity != null) html += '<div>습도 ' + wd.humidity + '%</div>';
            if (wd.windSpeed != null) html += '<div>바람 ' + wd.windSpeed.toFixed(1) + ' m/s</div>';
            if (wd.description) html += '<div>' + H.esc(wd.description) + '</div>';
        } else {
            html += '<div>날씨 데이터 로딩 중…</div>';
        }
        if (ai) {
            const parts = [];
            if (ai.label) parts.push(ai.label);
            if (ai.pm10 != null && ai.pm10 >= 0) parts.push('PM10 ' + ai.pm10 + 'µg');
            if (ai.pm25 != null && ai.pm25 >= 0) parts.push('PM2.5 ' + ai.pm25 + 'µg');
            if (parts.length) html += '<div>대기질 ' + H.esc(parts.join(' · ')) + '</div>';
        }
        html += '</div>';
        return html;
    }

    function placeUserMarker(lat, lon, sourceLabel) {
        if (!map) return;
        const icon = L.divIcon({
            className: '',
            html: '<div class="osh-user-loc"><div class="osh-user-loc__pulse"></div></div>',
            iconSize: [14, 14],
            iconAnchor: [7, 7]
        });
        if (_userMarker) { try { map.removeLayer(_userMarker); } catch (e) { /* noop */ } }
        _userMarker = L.marker([lat, lon], { icon: icon, zIndexOffset: 999 });
        _userMarker.on('popupopen', function () {
            _userMarker.getPopup().setContent(buildUserPopupHtml(lat, lon, sourceLabel));
        });
        _userMarker.bindPopup(buildUserPopupHtml(lat, lon, sourceLabel), { maxWidth: 240 });
        _userMarker.addTo(map);
        _userMarker.openPopup();
        if (_locBtn) { _locBtn.innerHTML = '📍'; _locBtn.classList.remove('is-loading'); }
    }

    function tryIpGeolocation(onSuccess, onFail) {
        // HTTP/HTTPS 무관하게 동작하는 공개 IP 위치 API (CORS 지원, 무료)
        fetch('https://ipapi.co/json/')
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.latitude && d.longitude) {
                    onSuccess(d.latitude, d.longitude, 'IP 기반');
                } else {
                    onFail && onFail();
                }
            })
            .catch(function () { onFail && onFail(); });
    }

    function tryLocate(manual) {
        if (!map) return;
        if (_locBtn) { _locBtn.innerHTML = '⟳'; _locBtn.classList.add('is-loading'); }

        // 1) GPS: HTTPS or localhost 에서만 작동
        if (window.isSecureContext && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                function (pos) {
                    placeUserMarker(pos.coords.latitude, pos.coords.longitude, 'GPS');
                },
                function () {
                    // GPS 실패 → IP 폴백
                    tryIpGeolocation(
                        function (lat, lon, src) { placeUserMarker(lat, lon, src); },
                        function () {
                            if (_locBtn) { _locBtn.innerHTML = '📍'; _locBtn.classList.remove('is-loading'); }
                        }
                    );
                },
                { timeout: 8000, maximumAge: 300000 }
            );
        } else {
            // 2) HTTP 환경: GPS 불가 → IP 폴백으로 바로 시도
            tryIpGeolocation(
                function (lat, lon, src) { placeUserMarker(lat, lon, src); },
                function () {
                    if (_locBtn) { _locBtn.innerHTML = '📍'; _locBtn.classList.remove('is-loading'); }
                }
            );
        }
    }

    function buildLocateControl() {
        if (!map) return;
        const ctrl = L.control({ position: 'topleft' });
        ctrl.onAdd = function () {
            const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control osh-loc-ctrl');
            const btn = L.DomUtil.create('a', 'osh-loc-btn', container);
            btn.innerHTML = '📍';
            btn.title = '현재 위치 찾기';
            btn.href = '#';
            _locBtn = btn;
            L.DomEvent.on(btn, 'click', function (e) {
                L.DomEvent.stopPropagation(e);
                L.DomEvent.preventDefault(e);
                tryLocate(true);
            });
            return container;
        };
        ctrl.addTo(map);
    }

    /* ========== 날씨 렌더 (코로플레스 + 도시 레이어 업데이트) ========== */

    function renderWeather(strJson) {
        const wrapper = H.unwrap(strJson, 'weatherJson');
        if (!wrapper) return;

        _weatherByCity = {};
        Object.keys(wrapper).forEach(function (k) {
            const payload = H.safeParse(wrapper[k]);
            if (!payload || !payload.main) return;
            const name = payload.cityName || payload.name || k;
            const tempK = payload.main.temp;
            _weatherByCity[name] = {
                tempC: typeof tempK === 'number' ? tempK - 273.15 : null,
                humidity: payload.main.humidity,
                windSpeed: payload.wind ? payload.wind.speed : null,
                description: payload.weather && payload.weather[0] ? payload.weather[0].description : null,
                lat: payload.coord ? payload.coord.lat : null,
                lon: payload.coord ? payload.coord.lon : null
            };
        });

        if (!ensureMap()) return;

        renderCityLayer();  // 도시 마커 항상 갱신

        if (!provincesData) {
            loadProvinces().then(function (geojson) {
                if (geojson) initChoropleth(geojson);
            });
        } else if (!choroplethLayer) {
            initChoropleth(provincesData);
        } else {
            updateChoroplethStyles();
        }
    }

    function setAirGrades(g) { _airGradeByCity = g || {}; }
    function setAirInfo(info) {
        _airInfoByCity = info || {};
        updateChoroplethStyles(); // 대기질 업데이트 시 즉시 tooltip 갱신
    }

    /* ========== 재난 마커 ========== */

    function lookupRegionCoord(rgnName) {
        if (!rgnName || !regionMap) return null;
        const parts = String(rgnName).split(/\s+|,|\//).filter(Boolean);
        for (let i = parts.length - 1; i >= 0; i--) {
            const p = parts[i];
            if (regionMap.sigungu && regionMap.sigungu[p]) return regionMap.sigungu[p];
            if (regionMap.sido && regionMap.sido[p]) return regionMap.sido[p];
        }
        return null;
    }

    function emergencyLevel(step) {
        const s = String(step || '').toLowerCase();
        if (s.includes('심각') || s.includes('위기')) return 'danger';
        if (s.includes('경계') || s.includes('주의')) return 'warning';
        return 'info';
    }

    function emergencyDivIcon(level) {
        const cls = 'osh-mk osh-mk--emr osh-mk--emr-' + (level || 'info');
        return L.divIcon({
            className: '',
            html: '<div class="' + cls + '"><span class="osh-mk__pulse"></span></div>',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });
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

    /* ========== 교통 마커 ========== */

    function trafficDivIcon() {
        return L.divIcon({
            className: '',
            html: '<div class="osh-mk osh-mk--traffic">⚠</div>',
            iconSize: [22, 22],
            iconAnchor: [11, 11]
        });
    }

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

    /* ========== 뉴스 마커 ========== */

    const SIDO_SHORT_TOKENS = ['서울','부산','대구','인천','광주','대전','울산','세종','경기','강원','충북','충남','전북','전남','경북','경남','제주'];

    function buildRegionIndex() {
        const long = [];
        const shortSido = [];
        if (!regionMap) return { long: long, shortSido: shortSido };
        Object.keys(regionMap.sido || {}).forEach(function (k) {
            if (SIDO_SHORT_TOKENS.indexOf(k) >= 0) {
                shortSido.push({ key: k, coord: regionMap.sido[k] });
            } else {
                long.push({ key: k, coord: regionMap.sido[k] });
            }
        });
        Object.keys(regionMap.sigungu || {}).forEach(function (k) {
            long.push({ key: k, coord: regionMap.sigungu[k] });
        });
        long.sort(function (a, b) { return b.key.length - a.key.length; });
        return { long: long, shortSido: shortSido };
    }

    function shortSidoMatches(text, key) {
        if (!text) return false;
        const re = new RegExp('(?:^|[^가-힣])' + key + '(?:[^가-힣]|$)');
        return re.test(text);
    }

    function findRegionForNews(title, idx) {
        if (!title) return null;
        for (let i = 0; i < idx.long.length; i++) {
            if (title.indexOf(idx.long[i].key) >= 0) return idx.long[i];
        }
        for (let j = 0; j < idx.shortSido.length; j++) {
            if (shortSidoMatches(title, idx.shortSido[j].key)) return idx.shortSido[j];
        }
        return null;
    }

    function newsDivIcon(count) {
        const label = (count && count > 1) ? String(count) : '📰';
        return L.divIcon({
            className: '',
            html: '<div class="osh-mk osh-mk--news">' + H.esc(label) + '</div>',
            iconSize: [18, 18],
            iconAnchor: [9, 9]
        });
    }

    function renderNews(strJson) {
        if (!ensureMap()) return;
        loadRegions().then(function () {
            newsLayer.clearLayers();
            const root = H.unwrap(strJson, 'yeonhapJson');
            const list = (root && Array.isArray(root.list)) ? root.list : [];
            if (!list.length) return;

            const idx = buildRegionIndex();
            const grouped = {};
            const MAX_SCAN = 50;
            const limit = Math.min(list.length, MAX_SCAN);
            for (let i = 0; i < limit; i++) {
                const n = list[i] || {};
                const text = (n.title || '') + ' ' + (n.summary || '');
                const hit = findRegionForNews(text, idx);
                if (!hit) continue;
                if (!grouped[hit.key]) grouped[hit.key] = { coord: hit.coord, items: [] };
                if (grouped[hit.key].items.length < 5) grouped[hit.key].items.push(n);
            }

            Object.keys(grouped).slice(0, 12).forEach(function (k) {
                const g = grouped[k];
                const m = L.marker(g.coord, { icon: newsDivIcon(g.items.length) });
                const rows = g.items.slice(0, 3).map(function (it) {
                    const time = it.pubDate ? H.formatDateTime(it.pubDate) : '';
                    const link = it.link
                        ? '<a href="' + H.esc(it.link) + '" target="_blank" rel="noopener">' + H.esc(it.title || '-') + '</a>'
                        : H.esc(it.title || '-');
                    return '<li>' + link + (time ? ' <span class="osh-popup__time">' + time + '</span>' : '') + '</li>';
                }).join('');
                const popupHtml =
                    '<div class="osh-popup"><b>' + H.esc(k) + ' 관련 뉴스</b>' +
                    '<ul class="osh-popup__list">' + rows + '</ul>' +
                    '</div>';
                m.bindPopup(popupHtml, { maxWidth: 320 });
                m.addTo(newsLayer);
            });
        });
    }

    /* ========== 테마 감지 ========== */

    function watchTheme() {
        const mo = new MutationObserver(function (mutations) {
            for (let i = 0; i < mutations.length; i++) {
                if (mutations[i].attributeName === 'data-theme') {
                    applyTile();
                    updateChoroplethStyles();
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
        renderNews: renderNews,
        setAirGrades: setAirGrades,
        setAirInfo: setAirInfo
    };
})();
