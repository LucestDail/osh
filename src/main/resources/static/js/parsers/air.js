/**
 * 대기질(AirKorea) 파서.
 *  입력 wrapper { airJson: "{ sido: [{name,pm10,pm25,khaiGrade,station,dataTime}, ...] }" }
 *  - 페이저: 8개/페이지 (2열 x 4행)
 *  - map.js 외곽선 등급 매핑 노출
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function gradeMeta(g) {
        switch (Number(g)) {
            case 1: return { label: '좋음',     cls: 'air-cell--good' };
            case 2: return { label: '보통',     cls: 'air-cell--moderate' };
            case 3: return { label: '나쁨',     cls: 'air-cell--bad' };
            case 4: return { label: '매우나쁨', cls: 'air-cell--severe' };
            default: return { label: '관측중',  cls: '' };
        }
    }

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

    function renderCell(it) {
        const m = gradeMeta(it.khaiGrade);
        const pm10 = (it.pm10 != null && it.pm10 >= 0) ? it.pm10 + 'µg' : '-';
        const pm25 = (it.pm25 != null && it.pm25 >= 0) ? it.pm25 + 'µg' : '-';
        return '<div class="air-cell ' + m.cls + '">' +
                  '<div class="air-cell__name">' + H.esc(it.name || '-') + '</div>' +
                  '<div class="air-cell__grade">' + H.esc(m.label) + '</div>' +
                  '<div class="air-cell__nums">' +
                    '<span>PM10 <b>' + pm10 + '</b></span>' +
                    '<span>PM2.5 <b>' + pm25 + '</b></span>' +
                  '</div>' +
                '</div>';
    }

    const pager = window.OSH.pager.create({
        name: 'air',
        pageSize: 18,   // AirKorea 시도 17개 → 한 페이지에 다 보이게
        onRender: function (slice) {
            const host = document.getElementById('airGrid');
            if (!host) return;
            if (!slice.length) {
                host.innerHTML = '<div class="empty-state"><div class="empty-state__title">대기질 정보를 불러오는 중입니다</div></div>';
                return;
            }
            host.innerHTML = slice.map(renderCell).join('');
        }
    });

    function render(strJson) {
        const root = H.unwrap(strJson, 'airJson');
        const items = (root && Array.isArray(root.sido)) ? root.sido.slice() : [];

        latestGradesBySido = {};
        items.forEach(function (it) { if (it && it.name) latestGradesBySido[it.name] = it.khaiGrade; });

        // 나쁜 등급 우선
        items.sort(function (a, b) { return (Number(b.khaiGrade) || 0) - (Number(a.khaiGrade) || 0); });
        pager.update(items);
    }

    (window.OSH = window.OSH || {}).air = {
        render: render,
        gradesByCity: gradesByCity
    };
})();
