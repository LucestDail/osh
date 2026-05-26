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

    /**
     * \ub2e8\uc77c EventSource \uc5f0\uacb0\uc73c\ub85c \uc5ec\ub7ec event type\uc744 \uad6c\ub3c5\ud55c\ub2e4.
     * \ube0c\ub77c\uc6b0\uc800 HTTP/1.1 origin-per-host \ud55c\ub3c4(6\uac1c)\ub97c \uc808\uc57d\ud558\uae30 \uc704\ud55c \ud575\uc2ec.
     *
     * @param {string} url
     * @param {{
     *   handlers: Record<string, (data: string) => void>,
     *   onStatus?: (state: 'connecting'|'open'|'reconnecting'|'closed') => void,
     *   reconnectMs?: number
     * }} opts
     * @returns {{ close: () => void }}
     */
    function subscribeMulti(url, opts) {
        const handlers = opts.handlers || {};
        const onStatus = opts.onStatus || function () {};
        const delay    = opts.reconnectMs || 5000;

        let es = null;
        let closed = false;

        function open() {
            onStatus('connecting');
            es = new EventSource(url);
            es.onopen = function () { onStatus('open'); };

            Object.keys(handlers).forEach(function (type) {
                es.addEventListener(type, function (ev) {
                    try { handlers[type](ev.data); }
                    catch (e) { console.error('[sse:' + url + ':' + type + '] handler error', e); }
                });
            });

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

    OSH.sse = { subscribe, subscribeMulti };
})();
