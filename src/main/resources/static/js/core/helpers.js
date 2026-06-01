/**
 * 공용 유틸 — SSE 페이로드 파싱, DOM 안전 접근, 포맷터.
 * (브라우저 전역 window.OSH 네임스페이스에 attach)
 */
(function () {
    'use strict';

    const OSH = (window.OSH = window.OSH || {});

    /** safe JSON.parse — 실패 시 null */
    function safeParse(str) {
        if (str == null) return null;
        if (typeof str !== 'string') return str;
        const trimmed = str.trim();
        if (!trimmed) return null;
        try { return JSON.parse(trimmed); } catch (e) { return null; }
    }

    /** SSE 페이로드(외부 wrapper)에서 특정 key 의 값을 꺼내 다시 JSON.parse */
    function unwrap(strJson, key) {
        const root = safeParse(strJson);
        if (!root || typeof root !== 'object') return null;
        if (!(key in root)) return null;
        return safeParse(root[key]);
    }

    /**
     * 서버·DB에서 오는 시각 문자열 → Date (KST 기준).
     * 타임존 없는 "2026-06-01T13:42:00" 은 UTC로 해석되면 +9h 밀림 → +09:00 고정.
     */
    function parseToDate(value) {
        if (value == null || value === '') return null;
        try {
            const s = String(value).trim();
            if (typeof value === 'number') return new Date(value);
            if (/^\d{14}$/.test(s)) {
                return new Date(
                    Number(s.substring(0, 4)),
                    Number(s.substring(4, 6)) - 1,
                    Number(s.substring(6, 8)),
                    Number(s.substring(8, 10)),
                    Number(s.substring(10, 12)),
                    Number(s.substring(12, 14))
                );
            }
            if (/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(s) && !/[Zz]|[+-]\d{2}:?\d{2}$/.test(s)) {
                return new Date(s.replace(' ', 'T') + '+09:00');
            }
            const d = new Date(s);
            return isNaN(d.getTime()) ? null : d;
        } catch (e) { return null; }
    }

    /** YYYYMMDDHHmmss 14자리 또는 ISO 문자열 모두 ko-KR 로컬로 */
    function formatDateTime(value) {
        if (!value) return '-';
        const d = parseToDate(value);
        if (!d) return String(value);
        return d.toLocaleString('ko-KR', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit',
            timeZone: 'Asia/Seoul'
        });
    }

    /** Unix epoch (sec) → HH:mm */
    function formatHm(unixSec) {
        if (!unixSec) return '-';
        try {
            const d = new Date(unixSec * 1000);
            return d.getHours().toString().padStart(2, '0') + ':' +
                   d.getMinutes().toString().padStart(2, '0');
        } catch (e) { return '-'; }
    }

    /** Kelvin → ℃ 한 자리 */
    function kToC(kelvin) {
        if (kelvin == null) return '-';
        const c = Number(kelvin) - 273.15;
        if (!isFinite(c)) return '-';
        return c.toFixed(1);
    }

    /** XSS 방지 텍스트 escape */
    function esc(value) {
        if (value == null) return '';
        return String(value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    /** 빈 상태 row HTML — colspan 자동 */
    function emptyRowHtml(colspan, title, hint, variant) {
        const cls = variant === 'danger' ? 'empty-state empty-state--danger' : 'empty-state';
        return '<tr><td colspan="' + colspan + '">' +
               '<div class="' + cls + '">' +
                 '<div class="empty-state__title">' + esc(title) + '</div>' +
                 (hint ? '<div class="empty-state__hint">' + esc(hint) + '</div>' : '') +
               '</div>' +
               '</td></tr>';
    }

    /** 문자열 truncate */
    function truncate(str, max) {
        if (!str) return '-';
        const s = String(str);
        return s.length > max ? s.substring(0, max) + '…' : s;
    }

    OSH.helpers = { safeParse, unwrap, parseToDate, formatDateTime, formatHm, kToC, esc, emptyRowHtml, truncate };
})();
