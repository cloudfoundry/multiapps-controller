# MultiApps documentation site

This is the `gh-pages` branch serving the published landing page and REST API
documentation for the [MultiApps Controller](https://github.com/cloudfoundry/multiapps-controller)
at <https://cloudfoundry.github.io/multiapps-controller/>.

The site is static and served as-is (`.nojekyll` disables Jekyll processing):

- `index.html` — the landing page
- `css/`, `js/`, `img/`, `vendor/` — its assets
- `api/` — versioned REST API reference (generated and published by the
  `publish-docs.yml` workflow on the `master` branch; do not edit by hand)

Edit `index.html` and `css/landing-page.min.css` directly to change the landing page.
