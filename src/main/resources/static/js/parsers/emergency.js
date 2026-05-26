/**
 * 긴급재난문자 파서
 * 입력 wrapper { emergencyJson: "{ items: [...] }" }
 */
(function () {
    'use strict';
    const H = window.OSH.helpers;

    function stepVariant(step) {
        const s = String(step || '').toLowerCase();
        if (s.includes('경계') || s.includes('주의'))  return 'warning';
        if (s.includes('심각') || s.includes('위기'))  return 'danger';
        if (s.includes('관심')) return 'info';
        return '';
    }

    function render(strJson) {
        const tbody = document.getElementById('emergencyTbody');
        if (!tbody) return;

        const root = H.unwrap(strJson, 'emergencyJson');
        const items = (root && Array.isArray(root.items)) ? root.items : [];

        if (!items.length) {
            tbody.innerHTML = H.emptyRowHtml(5, '현재 진행 중인 긴급재난문자가 없습니다', '5분마다 자동 갱신됩니다');
            return;
        }

        const sorted = items.slice().sort(function (a, b) {
            return String(b.CRT_DT || '').localeCompare(String(a.CRT_DT || ''));
        });
        const max = Math.min(sorted.length, 12);
        let html = '';
        for (let i = 0; i < max; i++) {
            const it = sorted[i] || {};
            const v = stepVariant(it.EMRG_STEP_NM);
            const badgeCls = 'badge' + (v ? ' badge--' + v : '');
            html += '<tr>' +
                '<td>' + H.formatDateTime(it.CRT_DT) + '</td>' +
                '<td>' + H.esc(it.RCPTN_RGN_NM || '-') + '</td>' +
                '<td>' + H.esc(it.MSG_CN || '-') + '</td>' +
                '<td><span class="' + badgeCls + '">' + H.esc(it.EMRG_STEP_NM || '-') + '</span></td>' +
                '<td>' + H.esc(it.DST_SE_NM || '-') + '</td>' +
                '</tr>';
        }
        tbody.innerHTML = html;
    }

    (window.OSH = window.OSH || {}).emergency = { render: render };
})();
