/**
 * 긴급재난문자 파서
 * 입력 wrapper { emergencyJson: "{ items: [...] }" }
 * 페이저: 8건/페이지
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function stepVariant(step) {
        const s = String(step || '').toLowerCase();
        if (s.includes('경계') || s.includes('주의')) return 'warning';
        if (s.includes('심각') || s.includes('위기')) return 'danger';
        if (s.includes('관심')) return 'info';
        return '';
    }

    function row(it) {
        const v = stepVariant(it.EMRG_STEP_NM);
        const badgeCls = 'badge' + (v ? ' badge--' + v : '');
        return '<tr>' +
            '<td>' + H.formatDateTime(it.CRT_DT) + '</td>' +
            '<td>' + H.esc(it.RCPTN_RGN_NM || '-') + '</td>' +
            '<td>' + H.esc(it.MSG_CN || '-') + '</td>' +
            '<td><span class="' + badgeCls + '">' + H.esc(it.EMRG_STEP_NM || '-') + '</span></td>' +
            '</tr>';
    }

    const pager = window.OSH.pager.create({
        name: 'emergency',
        pageSize: 8,
        onRender: function (slice) {
            const tbody = document.getElementById('emergencyTbody');
            if (!tbody) return;
            if (!slice.length) {
                tbody.innerHTML = H.emptyRowHtml(4, '진행 중인 긴급재난문자가 없습니다', '5분마다 자동 갱신됩니다');
                return;
            }
            tbody.innerHTML = slice.map(row).join('');
        }
    });

    function render(strJson) {
        const root = H.unwrap(strJson, 'emergencyJson');
        const items = (root && Array.isArray(root.items)) ? root.items.slice() : [];
        items.sort(function (a, b) {
            return String(b.CRT_DT || '').localeCompare(String(a.CRT_DT || ''));
        });
        pager.update(items);
    }

    (window.OSH = window.OSH || {}).emergency = { render: render };
})();
