# CyNDEx-2

The [NDEx](http://www.ndexbio.org/) client App for Cytoscape

## Introduction
This is a hybrid App for Cytoscape.  You can search, import, and save NDEx networks from Cytoscape.

# For Users

## How to Install
Just like other Cytoscape apps, you can install this from the [Cytoscape App Store](http://apps.cytoscape.org/) or directly from the file.  All of the required files will be installed automatically.

### Uninstall CyNDEx-2
To uninstall CyNDEx-2 completely from your machine, you need to follow these steps:

1. Uninstall the app from App menu
1. Open _CytoscapeConfiguration_ directory
1. Remove the following files/directories:
    * _cyndex-2_ directory



## How to Use CyNDEx-2
(TBD)

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
