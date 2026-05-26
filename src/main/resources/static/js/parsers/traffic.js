/**
 * 교통돌발 파서
 * 입력 wrapper { trafficJson: "{ body: { items: [...] } }" }
 * 페이저: 10건/페이지
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function row(it) {
        const road = (it.roadName || '-') + (it.roadDrcType ? ' (' + it.roadDrcType + ')' : '');
        return '<tr>' +
            '<td>' + H.formatDateTime(it.startDate) + '</td>' +
            '<td>' + H.esc(road) + '</td>' +
            '<td>' + H.esc(it.message || '-') + '</td>' +
            '<td><span class="badge">' + H.esc(it.eventType || '-') + '</span></td>' +
            '</tr>';
    }

    const pager = window.OSH.pager.create({
        name: 'traffic',
        pageSize: 20,
        onRender: function (slice) {
            const tbody = document.getElementById('trafficTbody');
            if (!tbody) return;
            if (!slice.length) {
                tbody.innerHTML = H.emptyRowHtml(4, '교통 정보를 수집 중입니다', '데이터가 없거나 잠시 후 다시 시도해주세요');
                return;
            }
            tbody.innerHTML = slice.map(row).join('');
        }
    });

    function parseDate(s) {
        var str = String(s || '');
        if (str.length >= 14) {
            // yyyyMMddHHmmss → ISO string
            var iso = str.slice(0,4) + '-' + str.slice(4,6) + '-' + str.slice(6,8)
                    + 'T' + str.slice(8,10) + ':' + str.slice(10,12) + ':' + str.slice(12,14);
            return new Date(iso).getTime();
        }
        return new Date(str).getTime() || 0;
    }

    function render(strJson) {
        const root = H.unwrap(strJson, 'trafficJson');
        const items = (root && root.body && Array.isArray(root.body.items)) ? root.body.items.slice() : [];
        items.sort(function (a, b) {
            return parseDate(b.startDate) - parseDate(a.startDate);
        });
        pager.update(items);
    }

    (window.OSH = window.OSH || {}).traffic = { render: render };
})();
