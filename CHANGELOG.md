# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.7.5] - 2026-09-08

### Fixed
- Downloading a network from the CyNDEx-2 search window did nothing when no sign-in profile was configured. With no profile the app falls back to the public NDEx server, and that server URL was passed to the NDEx client without normalization, producing requests against `www.ndexbio.orgv2` instead of `www.ndexbio.org/v2`. Every NDEx client is now built through a single helper that normalizes the URL, so the anonymous fallback works from the search window and from `ndex download network` alike. [issue/92](https://github.com/cytoscape/cy-ndex-2/issues/92)
- A failed download from the search window now reports the reason instead of doing nothing: the HTTP response from Cytoscape's own NDEx endpoint was discarded, so a server error produced no dialog and no log entry. [issue/92](https://github.com/cytoscape/cy-ndex-2/issues/92)
- The search, sign-in and error dialogs are now owned by the Cytoscape main window rather than merely positioned over it, so they can no longer open behind the application. [issue/92](https://github.com/cytoscape/cy-ndex-2/issues/92)
- `POST /cyndex2/v1/networks` could wait forever for a task that had already finished. The endpoint waited on a notification that the task manager may deliver from another thread before the wait begins, in which case it was lost and the request thread never resumed. The wait is now a latch, and is bounded. [issue/92](https://github.com/cytoscape/cy-ndex-2/issues/92)
- A server URL ending in `/v3` no longer has a second version segment appended to it, and one ending in `/v2/` no longer produces a doubled `/v2/v2/` request path. [issue/92](https://github.com/cytoscape/cy-ndex-2/issues/92)

## [3.7.4] - 2026-09-04

### Added
- New `ndex create network`, `ndex update network`, `ndex download network` and `ndex search networks` Cytoscape commands. Saving to NDEx is deliberately two commands: `create` always makes a new network and `update` always replaces the existing network named by its required `networkId`, so the choice has to be made rather than defaulted. See the Cytoscape Commands section of the README for arguments and examples. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- New `ndex list profiles` command, listing the CyNDEx-2 sign-in profiles configured in Cytoscape so that scripts and MCP tooling can discover which profiles exist, which one is active, and whether any are configured at all. It reads local configuration only and works even when NDEx is unreachable. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

### Changed
- Saving or loading a single network now uses CX2 over the NDEx v3 API, which is what makes network visibility and folders available. This applies to the File menu items and the `/cyndex2/v1/networks` endpoints as well as the new commands, and raises the minimums to an NDEx v3.0.0 server and CX Support 2.8.0. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- `POST /cyndex2/v1/networks/cx` now accepts both CX1 and CX2 streams, detected from the posted content, instead of assuming CX1. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- `ndex download network` and `ndex search networks` now fall back to the public NDEx server anonymously when no profile is selected and none are configured, rather than failing; they can then reach public networks only. `ndex create network` and `ndex update network` still require a signed-in profile. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- The built-in default NDEx server moved from `http://public.ndexbio.org/v2` to `https://www.ndexbio.org`. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)
- The error raised when no profile is available now distinguishes "no profiles are defined" from "profiles exist but none is selected", since the two need different remedies. [issue/77](https://github.com/cytoscape/cy-ndex-2/issues/77)

## [3.7.3] - 2026-07=-06

### Fixed
- Upgrade to latest java ndex object model to fix a parse error on the UNLISTED network visibilities returned form new v3 NDEX servers. [pull/79](https://github.com/cytoscape/cy-ndex-2/pull/79)

## [3.7.2] - 2026-06-12

### Added
- Profile URL support for user account registration and password reset links, sourced from server admin status endpoint. [pull/72](https://github.com/cytoscape/cy-ndex-2/pull/72)
