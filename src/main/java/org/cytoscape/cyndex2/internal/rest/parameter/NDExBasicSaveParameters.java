package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Required parameters for updading network(s) to NDEx.")
public class NDExBasicSaveParameters {

	@ApiModelProperty(value = "NDEx username", example = "username", required = true)
	public String username;
	@ApiModelProperty(value = "Password for the NDEx account", example = "password", required = true)
	public String password;
	@ApiModelProperty(value = "URL of NDEx V2 API server", example = "http://ndexbio.org/v2", required = true)
	public String serverUrl;
	@ApiModelProperty(value = "Network metadata", required = true)
	public Map<String, String> metadata;
	@ApiModelProperty(value = "UUID of an existing NDEx network to overwrite. When omitted, an update uses the "
			+ "UUID this network was previously saved with. Ignored when creating.",
			example = "12345678-abcd-1234-abcd-1234567890ab", required = false)
	public String networkId;
	@ApiModelProperty(value = "Visibility to apply on NDEx: PUBLIC, PRIVATE or UNLISTED. "
			+ "When omitted the server's default is left in place. Single networks only; "
			+ "not supported when saving a collection.", example = "PRIVATE", required = false)
	public String visibility;
	@ApiModelProperty(value = "NDEx folder to place the network in, given as a folder name or UUID. "
			+ "Single networks only; not supported when saving a collection.",
			example = "My Project", required = false)
	public String folder;

	public NDExBasicSaveParameters() {
		super();
	}

}