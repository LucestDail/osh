/**
 * 다크/라이트 모드 토글 — localStorage 영속 + prefers-color-scheme 대응.
 * <button class="theme-toggle" data-theme-toggle>...</button> 가 있으면 자동 바인딩.
 */
(function () {
    'use strict';
    const KEY = 'osh.theme';

    function applied() {
        return document.documentElement.getAttribute('data-theme')
            || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    }

    function apply(theme) {
        if (theme === 'dark' || theme === 'light') {
            document.documentElement.setAttribute('data-theme', theme);
            try { localStorage.setItem(KEY, theme); } catch (e) { /* noop */ }
        }
        const buttons = document.querySelectorAll('[data-theme-toggle]');
        const t = applied();
        const icon = t === 'dark' ? '☀️' : '🌙';
        const label = t === 'dark' ? '라이트 모드' : '다크 모드';
        buttons.forEach(function (btn) {
            btn.innerHTML = '<span class="theme-toggle__icon" aria-hidden="true">' + icon + '</span>' +
                            '<span>' + label + '</span>';
            btn.setAttribute('aria-label', label + ' 전환');
        });
    }

    function toggle() {
        apply(applied() === 'dark' ? 'light' : 'dark');
    }

    function init() {
        let saved = null;
        try { saved = localStorage.getItem(KEY); } catch (e) { /* noop */ }
        if (saved === 'dark' || saved === 'light') {
            document.documentElement.setAttribute('data-theme', saved);
        }
        apply(applied());
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', toggle);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    (window.OSH = window.OSH || {}).theme = { apply: apply, toggle: toggle };
})();
