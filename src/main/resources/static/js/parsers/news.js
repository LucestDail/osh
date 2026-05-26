/**
 * 뉴스 파서 (연합)
 * 입력 wrapper { yeonhapJson: "{ data: { items: [...] } }" }
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    const INIT_TITLES = new Set([
        '데이터를 불러오는 중입니다...',
        '데이터 로드 중 오류가 발생했습니다',
        '서비스 초기화 중입니다...'
    ]);

    function render(strJson) {
        const tbody = document.getElementById('newsTbody');
        if (!tbody) return;

        const root = H.unwrap(strJson, 'yeonhapJson');
        const items = (root && root.data && Array.isArray(root.data.items)) ? root.data.items : [];

        if (!items.length) {
            tbody.innerHTML = H.emptyRowHtml(3, '뉴스를 불러오는 중입니다', '잠시 후 자동으로 표시됩니다');
            return;
        }

        if (items.length === 1 && INIT_TITLES.has(items[0].title)) {
            tbody.innerHTML = H.emptyRowHtml(3, items[0].title, items[0].content);
            return;
        }

        const max = Math.min(items.length, 10);
        let html = '';
        for (let i = 0; i < max; i++) {
            const it = items[i] || {};
            html += '<tr>' +
                '<td>' + H.formatDateTime(it.createDT) + '</td>' +
                '<td>' + H.esc(it.title || '-') + '</td>' +
                '<td>' + H.esc(H.truncate(it.content, 300)) + '</td>' +
                '</tr>';
        }
        tbody.innerHTML = html;
    }

    (window.OSH = window.OSH || {}).news = { render: render };
})();
