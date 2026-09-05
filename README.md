# CyNDEx-2

The [NDEx](http://www.ndexbio.org/) client App for Cytoscape

## Introduction
This is a hybrid App for Cytoscape.  You can search, import, and save NDEx networks from Cytoscape.

> **Note:** Versions 2.x of CyNDEx-2 were built as an [Electron](https://electron.atom.io/)-based application and used [JxBrowser](http://www.teamdev.com/jxbrowser) to render the search window. Starting with version 3.0, Electron and JxBrowser were removed. If you are using CyNDEx-2 v2.x, see the [v2.x uninstall instructions](#uninstall-cyndex-2-v2x-electron-based) below and note that JxBrowser is a proprietary software governed by the [JxBrowser Product License Agreement](http://www.teamdev.com/jxbrowser-licence-agreement).

# For Users

## How to Install
Just like other Cytoscape apps, you can install this from the [Cytoscape App Store](http://apps.cytoscape.org/) or directly from the file.  All of the required files will be installed automatically.

### Uninstall CyNDEx-2
To uninstall CyNDEx-2 completely from your machine, you need to follow these steps:

1. Uninstall the app from App menu
1. Open _CytoscapeConfiguration_ directory
1. Remove the following files/directories:
    * _cyndex-2_ directory

### Uninstall CyNDEx-2 v2.x (Electron-based)
If you are uninstalling a v2.x release, additional Electron and JxBrowser artifacts need to be removed:

1. Uninstall the app from App menu
1. Open _CytoscapeConfiguration_ directory
1. Remove the following files/directories:
    * _cyndex-2_ directory
    * _ndex-electron-2.x.x_ directory
    * _ndex-installed-2.x.x.txt_ file
1.
    * Mac users - remove ~/Library/Application Support/CyNDEx-2 directory
    * Windows users - remove %AppData%/Roaming/CyNDEx-2 directory



## How to Use CyNDEx-2
(TBD)

## Cytoscape Commands

CyNDEx-2 publishes five commands under the `ndex` namespace, so NDEx can be driven from Cytoscape's
command line, from scripts, and from MCP tooling — using the sign-in profiles you already have.

Open **Tools &rarr; Command Line Dialog** in Cytoscape Desktop, then:

```
help ndex
```

lists the commands, and

```
help ndex create network
```

prints that command's arguments and description.

Saving a network to NDEx is two separate commands, not one command with a switch: `ndex create network`
always makes a **new** network, and `ndex update network` always **replaces** the content of the existing
network you name. Which one you want depends on whether you mean to keep the network you started from, so
there is no default — pick the one that matches your intent.

All of them take a `profile` argument naming a CyNDEx-2 sign-in profile as `username@serverUrl` — the same
spelling shown in the sign-in UI. Omit it and the currently selected profile is used; run `ndex list profiles`
to see what is configured.

If no profile is selected and none are configured, `download` and `search` fall back to the public NDEx server
at `https://www.ndexbio.org` anonymously, and so can only reach public networks. `create` and `update` always
need a signed-in profile and report an error if there is none.

The four network commands require an NDEx server running **v3.0.0 or newer**, and CX Support **2.8.0 or
newer**. `ndex list profiles` reads local configuration only and works even when NDEx is unreachable.

### ndex create network

Saves the network currently selected in Cytoscape to NDEx as a **new** network, and Cytoscape then tracks
that new network. It never changes an existing one — so if the current network came from NDEx and you run
this, you end up with two networks on NDEx.

A **single network only** — not a network collection, which CX2 cannot represent. Use
**File &rarr; Export &rarr; Collection to NDEx...** for those.

| Argument | Description |
|---|---|
| `profile` | Profile to save as, as `username@serverUrl`. Defaults to the selected profile. |
| `visibility` | `PRIVATE` (default), `PUBLIC`, or `UNLISTED`. |
| `folder` | NDEx folder to place the network in, as a folder name or UUID. |

```
ndex create network profile="alice@https://www.ndexbio.org/v2" visibility=PRIVATE folder="My Project"
```

```json
{
  "uuid": "12345678-abcd-1234-abcd-1234567890ab",
  "url": "https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab",
  "visibility": "PRIVATE",
  "folderId": "87654321-dcba-4321-dcba-0987654321ba"
}
```

### ndex update network

Replaces the content of the NDEx network you name with the network currently selected in Cytoscape. This
overwrites that network, so make sure `networkId` is the one you mean. It never creates a network, and it is
the command to use for saving edits back to a network you downloaded.

A **single network only**, as above.

| Argument | Description |
|---|---|
| `networkId` | UUID of the NDEx network to replace. **Required.** |
| `profile` | Profile to save as, as `username@serverUrl`. Defaults to the selected profile. |
| `visibility` | `PRIVATE` (default), `PUBLIC`, or `UNLISTED`. |
| `folder` | NDEx folder to place the network in, as a folder name or UUID. |

```
ndex update network networkId=12345678-abcd-1234-abcd-1234567890ab visibility=PRIVATE
```

```json
{
  "uuid": "12345678-abcd-1234-abcd-1234567890ab",
  "url": "https://www.ndexbio.org/viewer/networks/12345678-abcd-1234-abcd-1234567890ab",
  "visibility": "PRIVATE",
  "folderId": null
}
```

### ndex download network

| Argument | Description |
|---|---|
| `networkId` | UUID of the NDEx network to download. Required. |
| `profile` | Profile to download as. Defaults to the selected profile. |
| `accessKey` | Access key, for a network shared by link. |
| `createView` | Whether to build a network view. Unset uses the CX reader's default. |

```
ndex download network networkId=12345678-abcd-1234-abcd-1234567890ab
```

```json
{"suid": 52, "uuid": "12345678-abcd-1234-abcd-1234567890ab", "name": "My network"}
```

### ndex search networks

| Argument | Description |
|---|---|
| `searchTerm` | Text matched against network name, description and owner. |
| `profile` | Profile to search as. Defaults to the selected profile. |
| `visibility` | `PUBLIC` (default) or `PRIVATE`. `PRIVATE` searches your own networks and needs a signed-in profile. |
| `maxResults` | Maximum results to return, a whole number. Defaults to 100. |
| `startIndex` | Zero-based index of the first result, for paging. A whole number. |

NDEx treats this as a choice of corpus rather than a filter, so there is no option to search public and
private together. `UNLISTED` is not an option either: unlisted networks are reachable by link but are
deliberately excluded from NDEx search results.

```
ndex search networks searchTerm="cancer signaling" visibility=PUBLIC maxResults=25
```

```json
{
  "numFound": 1,
  "start": 0,
  "networks": [
    {
      "uuid": "12345678-abcd-1234-abcd-1234567890ab",
      "name": "My network",
      "owner": "alice",
      "visibility": "PUBLIC",
      "edges": 42,
      "modificationTime": "2026-01-01 00:00:00.0"
    }
  ]
}
```

### ndex list profiles

Lists the sign-in profiles configured in CyNDEx-2. Takes no arguments and contacts no server.

```
ndex list profiles
```

```json
{
  "count": 2,
  "profiles": [
    {"name": "alice@https://www.ndexbio.org", "username": "alice",
     "serverUrl": "https://www.ndexbio.org", "isCurrent": true},
    {"name": "@https://www.ndexbio.org", "username": null,
     "serverUrl": "https://www.ndexbio.org", "isCurrent": false}
  ]
}
```

`name` is exactly the string the `profile` argument of the other commands accepts. A `null` username marks an
anonymous profile. An empty list means `create` and `update` will not work until you add a profile, while
`download` and `search` will still run anonymously against public NDEx.

### From CyREST

The same commands are reachable over CyREST, which is how MCP tooling discovers them:

```
GET  http://localhost:1234/v1/commands/ndex
POST http://localhost:1234/v1/commands/ndex/create%20network
```

### A note on NDEx UUIDs

Saving a single network — by command or through **File &rarr; Export &rarr; Network to NDEx...** — records
the NDEx UUID against that network. Saving a *collection* records it against the collection instead. A
network put through both paths therefore ends up associated with two different NDEx networks, and which
one an update targets depends on which path you use.

# For Developers
This app provides user interface for NDEx, and it uses REST API provided via CyREST.  You can use CyREST-2 endpoints from tools of your choice, including [Jupyter Notebook](http://jupyter.org/).

## REST API Document

![](https://raw.githubusercontent.com/idekerlab/cy-ndex-2/master/notebooks/images/swagger.png)

The REST endpoints are provided via CyREST 3.5.0 or newer.  From CyREST version 3.5, it provides complete API documentation a using Swagger.  Please select ***Help&rarr;Automation&rarr;CyREST API*** to open the Swagger API document.

----
(TBD)
## How to Build the App

This application consists of two parts:

1. NDEx Client for Java
1. Cytoscape Java App

### NDEx Client for Java
This is an official Java client maintained by the NDEx team.

### Cytoscape Java App
This is the code for the actual Cytoscape app.

```bash
mvn clean install
```

## Install
(TBD)

## New: CyREST API
This app adds new endpoints to Cytoscape and you can use them to programmatically access some of the features of this application.

> **Single-network transfers use CX2 over the NDEx v3 API.** Saving or loading one network — through these
> endpoints, through the File menu, or through the `ndex` commands — requires an NDEx server at v3.0.0 or
> newer and CX Support 2.8.0 or newer. Saving a *collection* is unchanged and still uses CX1 over v2, since
> CX2 cannot carry sibling sub-networks. `POST /networks/cx` accepts either CX1 or CX2: the format is
> detected from the stream you post.

### Endpoints

Root of this API is ```http://localhost:1234/ndex/v1```.

Port number depends on your _rest.port_ Cytoscape property setting.

#### GET /
Show status of the App


##### Sample response
```json
{
  "apiVersion": "v1",
  "appName": "CyNDEx-2",
  "appVersion": "2.0.0"
}
```

#### GET /networks/current
Returns summary of the current network.

##### Sample response
```json
{
  "currentNetworkSuid": 25230,
  "currentRootNetwork": {
    "suid": 36,
    "name": "MyCollection1",
    "props": {
      "name": "MyCollection1",
      "SUID": 36,
      "selected": false
    }
  },
  "members": [
    {
      "suid": 52,
      "name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
      "props": {
        "shared name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
        "__Annotations": [],
        "name": "BIOGRID-ORGANISM-Caenorhabditis_elegans-3.4.129.mitab",
        "SUID": 52,
        "selected": false
      }
    },
    {
      "suid": 25230,
      "name": "galFiltered.sif",
      "props": {
        "shared name": "galFiltered.sif",
        "__Annotations": [],
        "name": "galFiltered.sif",
        "SUID": 25230,
        "selected": true
      }
    }
  ]
}
```

#### POST /networks
Create new network from an NDEx entry.

##### Request body

```json
{
    "uuid": "0268f115-b021-11e6-831a-06603eb7f303",
    "serverUrl": "http://www.ndexbio.org/v2",
    "userId": "myid",
    "password": "mypassword"
}
```

* uuid - UUID of the NDEx network
* serverUrl - URL of the NDEx API server
* userId - (Optional) NDEx user ID for loading private network
* password - (Optional) NDEx password for loading private network

The save endpoints (`POST /networks/{suid}`, `POST /networks/current`, and their `PUT` counterparts)
additionally accept, for single networks only:

* visibility - (Optional) `PUBLIC`, `PRIVATE` or `UNLISTED`. Omit to leave NDEx's own default in place.
* folder - (Optional) NDEx folder to place the network in, as a folder name or UUID.
* networkId - (Optional) UUID of an existing NDEx network to overwrite, on an update.

Supplying `visibility` or `folder` when saving a collection is an error. The long-standing `isPublic`
field still works when set explicitly, and is left alone when omitted.

##### Sample response
```json
{
  "suid":52,
  "uuid":"b5cb599b-ae23-11e6-831a-06603eb7f303"
}
```

#### POST /networks/SUID
Upload a Cytoscape network to NDEx server specified by the caller.

##### Request body
```json
{
  "isPublic": false,
  "userId": "myid",
  "password": "mypw",
  "serverUrl": "http://www.ndexbio.org/v2",
  "metadata": {
    "ndex.description": "Sample description from Cytoscape",
    "name": "Network name updated via rest",
    "ndex.species": "human"
  }
}
```

##### Sample response
```json
{
  "suid":3320,
  "uuid":"4a23aef0-2ba5-11e7-8f50-0ac135e8bacf"
}
```

#### POST /networks/current
Utility function to upload current networks to NDEx.  It actually calls ```POST /networks/SUID``` where SUID is the current network's SUID.

(same as above)


#### PUT /networks/SUID
Update the existing NDEx entry.

(TBD)


## License
MIT
