/**
 * 뉴스 파서 (연합)
 * 입력 wrapper { yeonhapJson: "{ data: { items: [...] } }" }
 * 페이저: 8건/페이지
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    const INIT_TITLES = new Set([
        '데이터를 불러오는 중입니다...',
        '데이터 로드 중 오류가 발생했습니다',
        '서비스 초기화 중입니다...'
    ]);

    function row(it) {
        return '<tr>' +
            '<td>' + H.formatDateTime(it.createDT) + '</td>' +
            '<td>' + H.esc(it.title || '-') + '</td>' +
            '<td>' + H.esc(H.truncate(it.content, 220)) + '</td>' +
            '</tr>';
    }

    const pager = window.OSH.pager.create({
        name: 'news',
        pageSize: 8,
        onRender: function (slice) {
            const tbody = document.getElementById('newsTbody');
            if (!tbody) return;
            if (!slice.length) {
                tbody.innerHTML = H.emptyRowHtml(3, '뉴스를 불러오는 중입니다', '잠시 후 자동으로 표시됩니다');
                return;
            }
            tbody.innerHTML = slice.map(row).join('');
        }
    });

    function render(strJson) {
        const root = H.unwrap(strJson, 'yeonhapJson');
        const items = (root && root.data && Array.isArray(root.data.items)) ? root.data.items.slice() : [];

        if (items.length === 1 && INIT_TITLES.has(items[0].title)) {
            pager.update([]);
            return;
        }
        pager.update(items);
    }

    (window.OSH = window.OSH || {}).news = { render: render };
})();
