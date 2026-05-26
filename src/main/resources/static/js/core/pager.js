/**
 * 공통 페이저 — 각 정보 패널이 동일 인터페이스로 페이지네이션을 사용.
 *
 * 사용 예:
 *   const p = window.OSH.pager.create({
 *       name: 'weather',         // 페이저 식별자 (data-pager 속성 매칭)
 *       pageSize: 6,
 *       onRender: function (slice, page, totalPages) {
 *           // slice = items.slice(page*pageSize, (page+1)*pageSize)
 *           // DOM 그리기
 *       }
 *   });
 *   p.update(items);   // SSE 갱신 시 호출
 *
 * 페이저 DOM (main.html 기준):
 *   <div class="pager" data-pager="weather">
 *       <button data-act="prev">‹</button>
 *       <span id="pagerLabel-weather"></span>
 *       <button data-act="next">›</button>
 *   </div>
 */
(function () {
    'use strict';
    const OSH = (window.OSH = window.OSH || {});

    function create(opts) {
        const name = opts.name;
        const pageSize = Math.max(1, opts.pageSize || 10);
        const onRender = opts.onRender || function () {};

        let items = [];
        let page = 0;

        const root  = document.querySelector('[data-pager="' + name + '"]');
        const label = document.getElementById('pagerLabel-' + name);
        const btnPrev = root ? root.querySelector('[data-act="prev"]') : null;
        const btnNext = root ? root.querySelector('[data-act="next"]') : null;

        function totalPages() {
            return Math.max(1, Math.ceil(items.length / pageSize));
        }

        function draw() {
            const tp = totalPages();
            if (page >= tp) page = tp - 1;
            if (page < 0) page = 0;
            const start = page * pageSize;
            const slice = items.slice(start, start + pageSize);
            onRender(slice, page, tp);
            if (label) label.textContent = (items.length === 0 ? '0 / 0' : (page + 1) + ' / ' + tp);
            if (btnPrev) btnPrev.disabled = (page <= 0);
            if (btnNext) btnNext.disabled = (page >= tp - 1);
        }

        if (btnPrev) btnPrev.addEventListener('click', function () { if (page > 0) { page--; draw(); }});
        if (btnNext) btnNext.addEventListener('click', function () { if (page < totalPages() - 1) { page++; draw(); }});

        return {
            update: function (next) { items = Array.isArray(next) ? next : []; draw(); },
            setPage: function (p) { page = p; draw(); },
            redraw: draw,
            getItems: function () { return items; }
        };
    }

    OSH.pager = { create: create };
})();
