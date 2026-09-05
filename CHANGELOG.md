# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.7.4] - 2026-09-04

### Added
- New `ndex upload network`, `ndex download network` and `ndex search networks` Cytoscape commands. See the Cytoscape Commands section of the README for arguments and examples. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- New `ndex list profiles` command, listing the CyNDEx-2 sign-in profiles configured in Cytoscape so that scripts and MCP tooling can discover which profiles exist, which one is active, and whether any are configured at all. It reads local configuration only and works even when NDEx is unreachable. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

### Changed
- Saving or loading a single network now uses CX2 over the NDEx v3 API, which is what makes network visibility and folders available. This applies to the File menu items and the `/cyndex2/v1/networks` endpoints as well as the new commands, and raises the minimums to an NDEx v3.0.0 server and CX Support 2.8.0. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- `POST /cyndex2/v1/networks/cx` now accepts both CX1 and CX2 streams, detected from the posted content, instead of assuming CX1. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- `ndex download network` and `ndex search networks` now fall back to the public NDEx server anonymously when no profile is selected and none are configured, rather than failing; they can then reach public networks only. `ndex upload network` still requires a signed-in profile. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- The built-in default NDEx server moved from `http://public.ndexbio.org/v2` to `https://www.ndexbio.org`. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- The error raised when no profile is available now distinguishes "no profiles are defined" from "profiles exist but none is selected", since the two need different remedies. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

## [3.7.3] - 2026-07=-06

### Fixed
- Upgrade to latest java ndex object model to fix a parse error on the UNLISTED network visibilities returned form new v3 NDEX servers. [pull/79](https://github.com/cytoscape/cy-ndex-2/pull/79)

## [3.7.2] - 2026-06-12

### Added
- Profile URL support for user account registration and password reset links, sourced from server admin status endpoint. [pull/72](https://github.com/cytoscape/cy-ndex-2/pull/72)
