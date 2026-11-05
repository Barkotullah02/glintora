/* Glintora storefront interactions — vanilla, no dependencies */
(function () {
  'use strict';

  function on(root, selector, event, handler) {
    root.querySelectorAll(selector).forEach(function (el) {
      el.addEventListener(event, handler);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {

    /* ---------- mobile nav drawer ---------- */
    var drawer = document.getElementById('navDrawer');
    var backdrop = document.getElementById('drawerBackdrop');

    function closeDrawers() {
      if (drawer) drawer.classList.remove('open');
      document.querySelectorAll('.filter-panel.open').forEach(function (p) { p.classList.remove('open'); });
      if (backdrop) backdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
    function openDrawer(panel) {
      if (panel) panel.classList.add('open');
      if (backdrop) backdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }

    on(document, '[data-nav-toggle]', 'click', function () {
      drawer && drawer.classList.contains('open') ? closeDrawers() : openDrawer(drawer);
    });
    on(document, '[data-filter-toggle]', 'click', function () {
      var panel = document.querySelector('.filter-panel');
      panel && panel.classList.contains('open') ? closeDrawers() : openDrawer(panel);
    });
    on(document, '[data-drawer-close]', 'click', closeDrawers);
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeDrawers(); });

    /* ---------- search panel ---------- */
    var searchPanel = document.getElementById('searchPanel');
    on(document, '[data-search-toggle]', 'click', function () {
      if (!searchPanel) return;
      searchPanel.classList.toggle('open');
      if (searchPanel.classList.contains('open')) {
        var input = searchPanel.querySelector('input');
        input && input.focus();
      }
    });

    /* ---------- qty steppers ---------- */
    on(document, '[data-qty-minus]', 'click', function () {
      var wrap = this.closest('[data-qty]');
      var input = wrap && wrap.querySelector('input');
      if (input) input.value = Math.max(parseInt(input.min || '1', 10), (parseInt(input.value, 10) || 1) - 1);
    });
    on(document, '[data-qty-plus]', 'click', function () {
      var wrap = this.closest('[data-qty]');
      var input = wrap && wrap.querySelector('input');
      if (input) input.value = Math.min(parseInt(input.max || '20', 10), (parseInt(input.value, 10) || 0) + 1);
    });

    /* ---------- product gallery ---------- */
    var mainImg = document.getElementById('galleryMain');
    on(document, '[data-gallery-thumb]', 'click', function () {
      var src = this.getAttribute('data-gallery-thumb');
      if (mainImg && src) {
        mainImg.src = src;
        document.querySelectorAll('[data-gallery-thumb]').forEach(function (b) {
          b.setAttribute('aria-current', b === this ? 'true' : 'false');
        }, this);
      }
    });

    /* ---------- PDP variant chips (size + color) ---------- */
    var pdp = document.getElementById('pdpRoot');
    if (pdp) {
      var chips = Array.prototype.slice.call(pdp.querySelectorAll('[data-variant]'));
      var colorBtns = Array.prototype.slice.call(pdp.querySelectorAll('[data-color]'));
      var variantIdInput = pdp.querySelector('input[name="variantId"]');
      var stickyVariant = document.querySelector('#stickyBuy input[name="variantId"]');
      var priceEl = document.getElementById('pdpPrice');
      var selectionEl = document.getElementById('pdpSelection');
      var stickyPrice = document.querySelector('#stickyBuy .price');
      var atc = document.getElementById('pdpAddToCart');

      var selectedColor = (chips.find(function (c) { return c.getAttribute('data-available') === 'true'; }) || chips[0] || {}).dataset;
      selectedColor = selectedColor || {};
      var selectedSize = null;

      function sizeOf(chip) { return chip.getAttribute('data-size-id') || ''; }

      function render() {
        // hide sizes that don't belong to the selected color
        chips.forEach(function (chip) {
          var match = !selectedColor.id || chip.getAttribute('data-color-id') === selectedColor.id;
          chip.style.display = match ? '' : 'none';
        });
        // pick the first available size of this color if current is invalid/hidden
        var visible = chips.filter(function (c) { return c.style.display !== 'none'; });
        if (!selectedSize || !visible.some(function (c) { return c === selectedSize && c.getAttribute('data-available') === 'true'; })) {
          selectedSize = visible.find(function (c) { return c.getAttribute('data-available') === 'true'; }) || visible[0] || null;
        }
        chips.forEach(function (chip) {
          chip.setAttribute('aria-pressed', chip === selectedSize ? 'true' : 'false');
        });
        colorBtns.forEach(function (b) {
          b.setAttribute('aria-pressed', b.getAttribute('data-color') === selectedColor.id ? 'true' : 'false');
        });

        var id = selectedSize ? selectedSize.getAttribute('data-variant') : '';
        if (variantIdInput) variantIdInput.value = id;
        if (stickyVariant) stickyVariant.value = id;
        var price = selectedSize ? selectedSize.getAttribute('data-price') : '';
        if (priceEl && price) priceEl.textContent = '৳' + price;
        if (stickyPrice && price) stickyPrice.textContent = '৳' + price;
        if (selectionEl) {
          var parts = [];
          if (selectedSize) {
            var sName = selectedSize.getAttribute('data-size-name');
            var cName = selectedSize.getAttribute('data-color-name');
            if (sName) parts.push(sName);
            if (cName) parts.push(cName);
          }
          selectionEl.textContent = parts.join(' / ');
        }
        var sellable = selectedSize && selectedSize.getAttribute('data-available') === 'true';
        if (atc) atc.disabled = !sellable;
        var stickyBtn = document.querySelector('#stickyBuy button[type="submit"]');
        if (stickyBtn) stickyBtn.disabled = !sellable;
      }

      colorBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
          selectedColor = { id: btn.getAttribute('data-color') };
          render();
        });
      });
      chips.forEach(function (chip) {
        chip.addEventListener('click', function () {
          if (chip.getAttribute('data-available') !== 'true') return;
          selectedColor = { id: chip.getAttribute('data-color-id') };
          selectedSize = chip;
          render();
        });
      });
      render();
    }

    /* ---------- size guide disclosure ---------- */
    on(document, '[data-size-guide]', 'click', function () {
      var panel = document.getElementById(this.getAttribute('data-size-guide'));
      if (!panel) return;
      var open = panel.classList.toggle('open');
      this.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  });
})();
