/* Glintora admin — small progressive enhancements (guide §3, §15). */
(function () {
    'use strict';

    /* ---- mobile sidebar drawer ---- */
    var sidebar = document.getElementById('adminSidebar');
    var backdrop = document.getElementById('sidebarBackdrop');

    function openDrawer() {
        if (!sidebar) return;
        sidebar.classList.add('open');
        if (backdrop) backdrop.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    function closeDrawer() {
        if (!sidebar) return;
        sidebar.classList.remove('open');
        if (backdrop) backdrop.classList.remove('show');
        document.body.style.overflow = '';
    }

    document.querySelectorAll('[data-menu-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            sidebar && sidebar.classList.contains('open') ? closeDrawer() : openDrawer();
        });
    });
    if (backdrop) backdrop.addEventListener('click', closeDrawer);
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeDrawer();
    });

    /* ---- collapsible nav groups (persist per session) ---- */
    document.querySelectorAll('.nav-group').forEach(function (group) {
        var label = group.querySelector('.nav-group-label');
        if (!label) return;
        var key = 'glintora.nav.' + (group.dataset.group || label.textContent.trim());
        if (sessionStorage.getItem(key) === 'collapsed') {
            group.dataset.collapsed = 'true';
        }
        label.addEventListener('click', function () {
            var collapsed = group.dataset.collapsed === 'true';
            group.dataset.collapsed = collapsed ? 'false' : 'true';
            sessionStorage.setItem(key, collapsed ? 'open' : 'collapsed');
        });
    });

    /* ---- mobile filter drawer ---- */
    document.querySelectorAll('[data-filter-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var bar = document.querySelector(btn.dataset.filterToggle);
            if (bar) bar.classList.toggle('open');
        });
    });

    /* ---- dismissible flash messages ---- */
    document.querySelectorAll('.flash .flash-close').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var flash = btn.closest('.flash');
            if (flash) flash.remove();
        });
    });

    /* auto-hide success flashes so they don't linger */
    document.querySelectorAll('.flash-success').forEach(function (el) {
        setTimeout(function () {
            el.style.transition = 'opacity 300ms';
            el.style.opacity = '0';
            setTimeout(function () { el.remove(); }, 320);
        }, 5000);
    });

    /* ---- confirmation for destructive actions (guide §15) ---- */
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.dataset.confirm)) {
                e.preventDefault();
            }
        });
    });

    /* ---- "unsaved changes" protection on admin forms ---- */
    document.querySelectorAll('form[data-protect]').forEach(function (form) {
        var dirty = false;
        form.addEventListener('input', function () { dirty = true; });
        form.addEventListener('change', function () { dirty = true; });
        form.addEventListener('submit', function () { dirty = false; });
        window.addEventListener('beforeunload', function (e) {
            if (dirty) {
                e.preventDefault();
                e.returnValue = '';
            }
        });
    });

    /* ---- projected quantity preview on the adjust form (guide §6) ---- */
    var adjVariant = document.getElementById('adjustVariant');
    var adjDelta = document.getElementById('adjustDelta');
    var adjPreview = document.getElementById('adjustPreview');
    if (adjVariant && adjDelta && adjPreview) {
        var updatePreview = function () {
            var opt = adjVariant.options[adjVariant.selectedIndex];
            var current = opt ? parseInt(opt.dataset.onhand || '0', 10) : 0;
            var delta = parseInt(adjDelta.value || '0', 10);
            var projected = current + delta;
            if (isNaN(delta) || adjDelta.value === '') {
                adjPreview.textContent = 'Current on hand: ' + current;
            } else {
                adjPreview.innerHTML = 'Current on hand: <strong>' + current +
                    '</strong> → projected: <strong style="color:' +
                    (projected < 0 ? '#B33A3A' : '#357A52') + '">' + projected + '</strong>';
            }
        };
        adjVariant.addEventListener('change', updatePreview);
        adjDelta.addEventListener('input', updatePreview);
        updatePreview();
    }

    /* ---- dynamic PO line rows (purchase order form) ---- */
    var addLineBtn = document.getElementById('addPoLine');
    var linesBody = document.getElementById('poLines');
    if (addLineBtn && linesBody) {
        var lineIndex = linesBody.querySelectorAll('tr').length;
        addLineBtn.addEventListener('click', function () {
            var proto = linesBody.querySelector('tr[data-proto]');
            if (!proto) return;
            var row = proto.cloneNode(true);
            row.removeAttribute('data-proto');
            row.classList.remove('hidden');
            row.querySelectorAll('[name]').forEach(function (input) {
                input.name = input.name.replace('__IDX__', String(lineIndex));
                input.removeAttribute('disabled');
            });
            row.querySelectorAll('.line-remove').forEach(function (btn) {
                btn.addEventListener('click', function () { row.remove(); });
            });
            linesBody.appendChild(row);
            lineIndex++;
        });
        linesBody.querySelectorAll('tr[data-proto] .line-remove').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var row = btn.closest('tr');
                if (row) {
                    row.querySelectorAll('[name]').forEach(function (i) { i.value = ''; });
                }
            });
        });
    }

    /* ---- receiving form: rows with no quantity are skipped server-side ---- */
})();
