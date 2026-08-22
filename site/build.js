#!/usr/bin/env node
/**
 * Kisab public landing site build.
 *
 * Zero-dependency Node script. Reads:
 *   - site/data/site.config.json   (domain, repo URLs, support placeholder)
 *   - site/data/releases.json      (release metadata, source: GitHub Releases)
 *   - site/content/{en,ne}.json    (localized copy)
 *   - site/templates/*.html        (page templates)
 *
 * Writes a fully static site to dist/web/ that can be copied directly into
 * the document root for kisab.susankhya.com. No runtime is required on the
 * host after this build.
 *
 * Usage:  node build.js   (or: npm run build)
 */
'use strict';

const fs = require('fs');
const path = require('path');

const SITE_ROOT = __dirname;
const CONTENT_DIR = path.join(SITE_ROOT, 'content');
const DATA_DIR = path.join(SITE_ROOT, 'data');
const TEMPLATE_DIR = path.join(SITE_ROOT, 'templates');
const ASSET_DIR = path.join(SITE_ROOT, 'assets');
const OUTPUT_ROOT = path.join(SITE_ROOT, '..', 'dist', 'web');

const LOCALES = ['en', 'ne'];
const PAGES = ['home', 'features', 'download', 'releases', 'help', 'privacy', 'support'];
const ROUTES = {
  home: '/',
  features: '/features/',
  download: '/download/',
  releases: '/releases/',
  help: '/help/',
  privacy: '/privacy/',
  support: '/support/'
};

const EN_MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const NE_MONTHS = ['जनवरी', 'फेब्रुअरी', 'मार्च', 'अप्रिल', 'मे', 'जुन', 'जुलाई', 'अगस्त', 'सेप्टेम्बर', 'अक्टोबर', 'नोभेम्बर', 'डिसेम्बर'];
const NE_DIGITS = ['०', '१', '२', '३', '४', '५', '६', '७', '८', '९'];

function log(msg) {
  process.stdout.write(msg + '\n');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function toNeDigits(value) {
  return String(value).replace(/\d/g, (d) => NE_DIGITS[Number(d)]);
}

function formatDate(iso, locale) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const day = d.getUTCDate();
  const month = d.getUTCMonth();
  const year = d.getUTCFullYear();
  if (locale === 'ne') {
    return `${toNeDigits(day)} ${NE_MONTHS[month]} ${toNeDigits(year)}`;
  }
  return `${day} ${EN_MONTHS[month]} ${year}`;
}

const CHANNEL_LABEL = {
  en: { pilot: 'Pilot', stable: 'Stable' },
  ne: { pilot: 'पाइलट', stable: 'स्थिर' }
};

/**
 * Tiny template engine.
 * Supported:
 *   {{key}}                    simple substitution
 *   {{.}}                      current item (string items in a list)
 *   {{#key}}...{{/key}}        iterate arrays, or render once for truthy scalars
 */
function render(tpl, ctx) {
  const blockRe = /\{\{#([\w.]+)\}\}([\s\S]*?)\{\{\/\1\}\}/;
  let match = blockRe.exec(tpl);
  while (match) {
    const key = match[1];
    const inner = match[2];
    const value = key in ctx ? ctx[key] : undefined;
    let out = '';
    if (Array.isArray(value)) {
      out = value.map((item) => {
        const sub = Object.assign({}, ctx, item, { _item: item });
        return render(inner, sub);
      }).join('');
    } else if (value) {
      out = render(inner, ctx);
    }
    tpl = tpl.slice(0, match.index) + out + tpl.slice(match.index + match[0].length);
    match = blockRe.exec(tpl);
  }
  tpl = tpl.replace(/\{\{![\s\S]*?\}\}/g, '');
  tpl = tpl.replace(/\{\{([\w.]+)\}\}/g, (m, key) => {
    if (key === '.') {
      return ctx._item != null ? String(ctx._item) : '';
    }
    return key in ctx ? String(ctx[key]) : '';
  });
  return tpl;
}

function copyDir(srcDir, destDir) {
  fs.mkdirSync(destDir, { recursive: true });
  for (const entry of fs.readdirSync(srcDir, { withFileTypes: true })) {
    const src = path.join(srcDir, entry.name);
    const dest = path.join(destDir, entry.name);
    if (entry.isDirectory()) {
      copyDir(src, dest);
    } else {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.copyFileSync(src, dest);
    }
  }
}

function writeFile(relPath, content) {
  const out = path.join(OUTPUT_ROOT, relPath);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, content);
  return relPath;
}

function main() {
  const config = readJson(path.join(DATA_DIR, 'site.config.json'));
  const releaseData = readJson(path.join(DATA_DIR, 'releases.json'));
  const contentFiles = {};
  for (const locale of LOCALES) {
    contentFiles[locale] = readJson(path.join(CONTENT_DIR, `${locale}.json`));
  }

  const layoutTpl = fs.readFileSync(path.join(TEMPLATE_DIR, 'layout.html'), 'utf8');
  const pageTpls = {};
  for (const page of PAGES) {
    pageTpls[page] = fs.readFileSync(path.join(TEMPLATE_DIR, `${page}.html`), 'utf8');
  }

  // Prepare localized view of the release list.
  function releasesView(locale) {
    const label = CHANNEL_LABEL[locale];
    return releaseData.releases.map((r) => ({
      versionName: r.versionName,
      versionCode: locale === 'ne' ? toNeDigits(r.versionCode) : String(r.versionCode),
      published: formatDate(r.publishedAt, locale),
      channel: label[r.channel] || r.channel,
      status: r.status,
      is_current: r.tag === releaseData.current,
      sha256: r.sha256,
      apkUrl: r.apkUrl,
      releasePage: r.releasePage,
      apkAsset: r.apkAsset,
      summary: locale === 'ne' ? r.summaryNe : r.summary
    }));
  }

  fs.rmSync(OUTPUT_ROOT, { recursive: true, force: true });
  fs.mkdirSync(OUTPUT_ROOT, { recursive: true });

  copyDir(ASSET_DIR, OUTPUT_ROOT);

  const written = [];
  const sitemapUrls = [];

  for (const locale of LOCALES) {
    const content = contentFiles[locale];
    const common = content.common;
    const releases = releasesView(locale);
    const current = releases.find((r) => r.is_current);
    const langSwitchLang = locale === 'en' ? 'ne' : 'en';

    function pathFor(page) {
      const route = ROUTES[page];
      return locale === 'en' ? route : `/ne${route}`;
    }

    for (const page of PAGES) {
      const pathHere = pathFor(page);
      const meta = content.meta[page];
      const canonical = `${config.urls.base}${pathHere}`;
      const hreflangEn = `${config.urls.base}/`;
      const hreflangNe = `${config.urls.base}/ne/`;

      const pageContent = Object.assign(
        {},
        common,
        content[page] || {},
        {
          lang: locale,
          lang_switch_label: common.lang_switch_label,
          lang_switch_lang: langSwitchLang,
          lang_switch_href: pathFor(page),
          path_home: pathFor('home'),
          path_features: pathFor('features'),
          path_download: pathFor('download'),
          path_releases: pathFor('releases'),
          path_help: pathFor('help'),
          path_privacy: pathFor('privacy'),
          path_support: pathFor('support'),
          canonical,
          hreflang_en: hreflangEn,
          hreflang_ne: hreflangNe,
          og_image: `${config.urls.base}/og-image.png`,
          og_title: meta.title,
          og_description: meta.description,
          title: meta.title,
          description: meta.description,
          year: String(new Date().getUTCFullYear()),
          releases,
          repo_issues: config.urls.repoIssues,
          repo_releases: config.urls.repoReleases,
          repo: config.urls.repo
        }
      );

      // Release-derived facts for the download page.
      if (page === 'download' && current) {
        pageContent.download_current_href = current.apkUrl;
        pageContent.download_current_release_page = current.releasePage;
        pageContent.download_current_asset = current.apkAsset;
        pageContent.version_value = current.versionName;
        pageContent.versioncode_value = current.versionCode;
        pageContent.published_value = current.published;
        pageContent.sha256_value = current.sha256;
      }

      // Support email: use a real address only if approved in config.
      if (page === 'support') {
        const email = (config.support && config.support.support_email || '').trim();
        pageContent.support_email = email;
        pageContent.support_email_defined = email.length > 0;
        pageContent.support_email_text = email
          ? pageContent.email_text || ''
          : pageContent.email_text_placeholder;
      }

      const body = render(pageTpls[page], pageContent);
      const html = render(layoutTpl, Object.assign({}, pageContent, { slot: body }));

      const relPath = locale === 'en'
        ? path.join(ROUTES[page].slice(1), 'index.html')
        : path.join('ne', ROUTES[page].slice(1), 'index.html');
      written.push(writeFile(relPath, html));
      sitemapUrls.push(canonical);
    }

    // 404 pages (one per locale).
    const notFound = content.notfound || {};
    const nfContent = Object.assign(
      {},
      common,
      content.support || {},
      {
        title: `${notFound.title || '404'} — Kisab`,
        description: '',
        lang: locale,
        canonical: `${config.urls.base}${locale === 'en' ? '/404.html' : '/ne/404.html'}`,
        path_home: pathFor('home'),
        path_features: pathFor('features'),
        path_download: pathFor('download'),
        path_releases: pathFor('releases'),
        path_help: pathFor('help'),
        path_privacy: pathFor('privacy'),
        path_support: pathFor('support'),
        lang_switch_lang: langSwitchLang,
        lang_switch_href: locale === 'en' ? '/ne/404.html' : '/404.html',
        og_title: '404',
        og_description: '',
        year: String(new Date().getUTCFullYear()),
        slot: `<section class="section page-head"><div class="container"><h1 class="page-title">${notFound.heading || ''}</h1><p class="page-lead">${notFound.text || ''}</p><p><a class="btn btn-primary btn-lg" href="${pathFor('home')}">${notFound.button || ''}</a></p></div></section>`
      }
    );
    written.push(writeFile(locale === 'en' ? '404.html' : path.join('ne', '404.html'), render(layoutTpl, nfContent)));
  }

  // robots.txt
  writeFile('robots.txt', `User-agent: *\nAllow: /\n\nSitemap: ${config.urls.base}/sitemap.xml\n`);

  // sitemap.xml
  const urlsXml = sitemapUrls
    .map((u) => `  <url><loc>${u}</loc></url>`)
    .join('\n');
  writeFile('sitemap.xml', `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urlsXml}\n</urlset>\n`);

  // ---- Validation summary -------------------------------------------------
  const allFiles = [];
  (function walk(dir) {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      if (e.isDirectory()) walk(p);
      else allFiles.push(path.relative(OUTPUT_ROOT, p));
    }
  })(OUTPUT_ROOT);

  let htmlFiles = allFiles.filter((f) => f.endsWith('.html')).length;
  const leftover = [];
  (function check(dir) {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      if (e.isDirectory()) check(p);
      else if (e.name.endsWith('.html')) {
        const text = fs.readFileSync(p, 'utf8');
        const m = text.match(/\{\{[a-zA-Z0-9_.]+\}\}/g);
        if (m) leftover.push(...m);
      }
    }
  })(OUTPUT_ROOT);

  const apkFiles = allFiles.filter((f) => /\.apk$/i.test(f));
  const secretLeaks = allFiles.filter((f) => /\.(keystore|jks|p12|pk8)$/.test(f));
  const leftoverUnique = [...new Set(leftover)];

  log('\n=== BUILD RESULT ===');
  log(`Output: ${OUTPUT_ROOT}`);
  log(`Files written: ${allFiles.length} (${htmlFiles} HTML pages)`);
  log(`Locales: ${LOCALES.join(', ')}`);
  log(`Releases rendered: ${releasesView('en').length}`);
  log(`Unresolved template tokens: ${leftoverUnique.length ? leftoverUnique.join(', ') : 'none'}`);
  log(`APK files in output: ${apkFiles.length}`);
  log(`Keystore/secret files in output: ${secretLeaks.length}`);
  log('=== DONE ===');

  if (leftoverUnique.length || apkFiles.length || secretLeaks.length) {
    process.exitCode = 1;
  }
}

main();