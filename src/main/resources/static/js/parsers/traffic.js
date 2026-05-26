/**
 * 교통돌발 파서
 * 입력 wrapper { trafficJson: "{ body: { items: [...] } }" }
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function render(strJson) {
        const tbody = document.getElementById('trafficTbody');
        if (!tbody) return;

        const root = H.unwrap(strJson, 'trafficJson');
        const items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items : [];

        if (!items.length) {
            tbody.innerHTML = H.emptyRowHtml(6, '교통 정보를 수집 중입니다', '데이터가 없거나 잠시 후 다시 시도해주세요');
            return;
        }

        items.sort(function (a, b) {
            return new Date(b.startDate || 0).getTime() - new Date(a.startDate || 0).getTime();
        });

        const max = Math.min(items.length, 20);
        let html = '';
        for (let i = 0; i < max; i++) {
            const it = items[i] || {};
            html += '<tr>' +
                '<td>' + H.formatDateTime(it.startDate) + '</td>' +
                '<td>' + H.esc(it.roadName || '-') + '</td>' +
                '<td>' + H.esc(it.roadDrcType || '-') + '</td>' +
                '<td>' + H.esc(it.message || '-') + '</td>' +
                '<td><span class="badge">' + H.esc(it.eventType || '-') + '</span></td>' +
                '<td><span class="badge">' + H.esc(it.eventDetailType || '-') + '</span></td>' +
                '</tr>';
        }
        tbody.innerHTML = html;
    }

    (window.OSH = window.OSH || {}).traffic = { render: render };
})();
