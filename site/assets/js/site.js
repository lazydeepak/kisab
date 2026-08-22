/* Kisab landing site — minimal client behaviour.
   No tracking, no analytics, no third-party requests. */
(function () {
  'use strict';

  var themeToggle = document.querySelector('[data-theme-toggle-btn]');
  if (themeToggle) {
    themeToggle.addEventListener('click', function () {
      var root = document.documentElement;
      var dark = root.getAttribute('data-theme') === 'dark';
      if (dark) {
        root.removeAttribute('data-theme');
        try { localStorage.setItem('kisab-theme', 'light'); } catch (e) {}
      } else {
        root.setAttribute('data-theme', 'dark');
        try { localStorage.setItem('kisab-theme', 'dark'); } catch (e) {}
      }
    });
  }

  var navToggle = document.querySelector('[data-nav-toggle]');
  var navList = document.getElementById('site-nav-list');
  if (navToggle && navList) {
    navToggle.addEventListener('click', function () {
      var open = navToggle.getAttribute('aria-expanded') === 'true';
      navToggle.setAttribute('aria-expanded', String(!open));
      navList.classList.toggle('is-open', !open);
    });
    navList.addEventListener('click', function (e) {
      if (e.target && e.target.tagName === 'A') {
        navToggle.setAttribute('aria-expanded', 'false');
        navList.classList.remove('is-open');
      }
    });
  }
})();