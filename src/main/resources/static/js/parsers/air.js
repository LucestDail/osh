/**
 * 대기질(AirKorea) 파서.
 *  입력 wrapper { airJson: "{ sido: [{name,pm10,pm25,khaiGrade,station,dataTime}, ...] }" }
 *
 *  - 카드: 시·도별 PM10/PM2.5 + 등급
 *  - map.js 가 19도시 외곽 색상으로 사용할 등급 매핑 노출
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    /** AirKorea khaiGrade: 1=좋음, 2=보통, 3=나쁨, 4=매우나쁨 */
    function gradeMeta(g) {
        switch (Number(g)) {
            case 1: return { label: '좋음',     cls: 'air-cell--good' };
            case 2: return { label: '보통',     cls: 'air-cell--moderate' };
            case 3: return { label: '나쁨',     cls: 'air-cell--bad' };
            case 4: return { label: '매우나쁨', cls: 'air-cell--severe' };
            default: return { label: '관측중',  cls: '' };
        }
    }

    // 19도시 이름 → 시도 매핑 (map.js 가 weather 마커 외곽선 색을 결정할 때 사용)
    const CITY_TO_SIDO = {
        '서울': '서울', '부산': '부산', '인천': '인천', '대구': '대구', '대전': '대전',
        '광주': '광주', '울산': '울산', '수원': '경기', '고양': '경기', '용인': '경기',
        '포항': '경북', '창원': '경남', '김해': '경남', '김천': '경북', '제주': '제주',
        '춘천': '강원', '원주': '강원', '강릉': '강원', '속초': '강원'
    };

    let latestGradesBySido = {};

    function gradesByCity() {
        const out = {};
        Object.keys(CITY_TO_SIDO).forEach(function (city) {
            const sido = CITY_TO_SIDO[city];
            const g = latestGradesBySido[sido];
            if (g) out[city] = String(g);
        });
        return out;
    }

    function render(strJson) {
        const host = document.getElementById('airGrid');
        if (!host) return;

        const root = H.unwrap(strJson, 'airJson');
        const items = (root && Array.isArray(root.sido)) ? root.sido : [];
        if (!items.length) {
            host.innerHTML = '<div class="empty-state">' +
                               '<div class="empty-state__title">대기질 정보를 불러오는 중입니다</div>' +
                               '<div class="empty-state__hint">키 미설정 시 표시되지 않습니다</div>' +
                             '</div>';
            return;
        }

        // grade lookup 갱신 (map.js 용)
        latestGradesBySido = {};
        items.forEach(function (it) { if (it && it.name) latestGradesBySido[it.name] = it.khaiGrade; });

        // 정렬: 나쁜 순
        const sorted = items.slice().sort(function (a, b) {
            return (Number(b.khaiGrade) || 0) - (Number(a.khaiGrade) || 0);
        });

        let html = '';
        for (let i = 0; i < sorted.length; i++) {
            const it = sorted[i] || {};
            const m = gradeMeta(it.khaiGrade);
            const pm10 = (it.pm10 != null && it.pm10 >= 0) ? it.pm10 + 'µg' : '-';
            const pm25 = (it.pm25 != null && it.pm25 >= 0) ? it.pm25 + 'µg' : '-';
            html += '<div class="air-cell ' + m.cls + '">' +
                      '<div class="air-cell__name">' + H.esc(it.name || '-') + '</div>' +
                      '<div class="air-cell__grade">' + H.esc(m.label) + '</div>' +
                      '<div class="air-cell__nums">' +
                        '<span>PM10 <b>' + pm10 + '</b></span>' +
                        '<span>PM2.5 <b>' + pm25 + '</b></span>' +
                      '</div>' +
                    '</div>';
        }
        host.innerHTML = html;
    }

    (window.OSH = window.OSH || {}).air = {
        render: render,
        gradesByCity: gradesByCity
    };
})();
