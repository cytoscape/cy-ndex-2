# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.7.4] - 2026-09-03

### Added
- New `ndex upload network`, `ndex download network` and `ndex search networks` Cytoscape commands. See the Cytoscape Commands section of the README for arguments and examples. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

### Changed
- Saving or loading a single network now uses CX2 over the NDEx v3 API, which is what makes network visibility and folders available. This applies to the File menu items and the `/cyndex2/v1/networks` endpoints as well as the new commands, and raises the minimums to an NDEx v3.0.0 server and CX Support 2.8.0.  [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

## [3.7.3] - 2026-07=-06

### Fixed
- Upgrade to latest java ndex object model to fix a parse error on the UNLISTED network visibilities returned form new v3 NDEX servers. [pull/79](https://github.com/cytoscape/cy-ndex-2/pull/79)

## [3.7.2] - 2026-06-12

### Added
- Profile URL support for user account registration and password reset links, sourced from server admin status endpoint. [pull/72](https://github.com/cytoscape/cy-ndex-2/pull/72)
