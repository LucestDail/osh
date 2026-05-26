/**
 * SSE 구독 helper — 자동 재연결 + onMessage hook + 연결 상태 콜백.
 */
(function () {
    'use strict';
    const OSH = (window.OSH = window.OSH || {});

    /**
     * @param {string} url
     * @param {{
     *   onMessage: (data: string) => void,
     *   onStatus?: (state: 'connecting'|'open'|'reconnecting'|'closed') => void,
     *   reconnectMs?: number
     * }} opts
     * @returns {{ close: () => void }}
     */
    function subscribe(url, opts) {
        const handler  = opts.onMessage;
        const onStatus = opts.onStatus || function () {};
        const delay    = opts.reconnectMs || 5000;

        let es = null;
        let closed = false;

        function open() {
            onStatus('connecting');
            es = new EventSource(url);
            es.onopen = function () { onStatus('open'); };
            es.onmessage = function (ev) {
                try { handler(ev.data); } catch (e) { console.error('[sse:' + url + '] handler error', e); }
            };
            es.onerror = function () {
                if (closed) return;
                onStatus('reconnecting');
                try { es.close(); } catch (e) { /* noop */ }
                setTimeout(open, delay);
            };
        }
        open();

        return {
            close: function () {
                closed = true;
                onStatus('closed');
                if (es) { try { es.close(); } catch (e) { /* noop */ } }
            }
        };
    }

    OSH.sse = { subscribe };
})();
