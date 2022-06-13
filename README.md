### Conversion API

__GET/test__

__Returns:__
"Conversion API is working !"

__PUT/convert/{schemaName}/{targetVersion}__

Get files for given schema from gits. Convert to given version and pushes back converted files if the conversion resulted in no errors

__Path variables:__

*schemaName* - Name of the schema, same as the branch name.

*targetVersion* - To what version schema should be converted

Throws exception if conversion to the specified version is not supported, and also if requested schema doesn't exist, or is the same as master.

Important: Changes won’t be committed unless the conversion of every resource is successful, i.e. there are no errors.

__Returns:__

`ConverterControllerResponse` object.

```kotlin
class ConverterControllerResponse(
    val convertedResources: MutableList<String> = ArrayList(),
    val errorMessages: MutableList<ErrorMessage> = ArrayList(),
    var commitRef: String? = null
)
```
__response will contain:__ 

list of resources that were converted (if any)

list of errors encountered during conversion (if any)

commit reference hash (if there were any changes, and push was successful)

__Response body example:__

```json
{
  "convertedResources": [
    "fix-client",
    "rpt-data-viewer",
    "rpt-data-provider",
    "mstore",
    "fix-server",
    "script",
    "cradle-admin",
    "estore",
    "links"
  ],
  "errorMessages": [],
  "commitRef": "ad8f4ccf521c1192cc55608c40b9dd390f0d325d"
}
```

__POST/convert/{sourceSchemaName}/{newSchemaName}/{targetVersion}__

Get files for given source schema from Git, convert them to given version. If the conversion resulted in no errors: create a new schema and push converted files to the new schema; if `k8s-propagation` for sourceSchemaName is not set to deny, set it to deny; Set `k8s-propagation` for  newSchemaName to rule.


__Path variables:__

*sourceSchemaName* - Name of the schema where the files are currently; Same as the branch name

*newSchemaName* - Name of the schema where the files should be pushed after the successful conversion to new version; Same as branch name

*targetVersion* - To what version schema should be converted


Throws exception if conversion to the specified version is not supported, and also if requested schema doesn't exist, or is the same as master.

Important: New schema/branch won’t be made, changes won’t be committed and k8s-propagation will be unchanged unless the conversion of every resource is successful, i.e. there are no errors.

__Returns__:

`ConverterControllerResponse` object.

__response will contain:__

list of resources that were converted (if any)

list of errors encountered during conversion (if any)

commit reference hash (if there were any changes, and push was successful)

__Response body example:__
```json
{
    "convertedResources": [
        "recon",
        "mstore",
        "script",
        "check1",
        "util",
        "rpt-data-provider",
        "codec-fix",
        "fix-server",
        "rpt-data-viewer",
        "fix-client",
        "estore",
        "act",
        "links",
        "dictionary-links",
        "grpc-links"
    ],
    "errorMessages": [],
    "commitRef": "572d49d9dbd37e8f40c20741ae9da17fb0351f91"
}
```

__GET/convert/{targetVersion} [contains request body]__

Convert resources present in request body to the target version. No interaction with Git.

__Path variables:__

*targetVersion* - To what version the provided resources should be converted

Throws exception if conversion to the specified version is not supported


__Request body:__

Set of `RepositoryResource` objects

Example:

```json
[
  {
    "apiVersion": "th2.exactpro.com/v1",
    "kind": "Th2Box",
    "metadata": {
      "name": "script"
    },
    "spec": {
      "image-name": "dev-script",
      "image-version": "dev-script",
      "type": "th2-script",
      "pins": [
        {
          "name": "to_act",
          "connection-type": "grpc-client",
          "service-class": "com.exactpro.th2.act.grpc.ActService"
        },
        {
          "name": "to_check1",
          "connection-type": "grpc-client",
          "service-class": "com.exactpro.th2.check1.grpc.Check1Service"
        }
      ],
      "extended-settings": {
        "externalBox": {
          "enabled": true
        },
        "service": {
          "enabled": false
        }
      }
    }
  }
]
```

__Returns:__

List of converted `Th2Resource` objects.

*Note:* You can convert JSON format to YAML with [this online tool](https://onlineyamltools.com/convert-json-to-yaml), and make it more readable with [linter](http://www.yamllint.com/) if necessary.  

__Response body example:__

```json
[
    {
        "apiVersion": "th2.exactpro.com/v2",
        "kind": "Th2Box",
        "metadata": {
            "name": "script"
        },
        "spec": {
            "imageName": "dev-script",
            "imageVersion": "dev-script",
            "type": "th2-script",
            "extendedSettings": {
                "externalBox": {
                    "enabled": true
                },
                "service": {
                    "enabled": false
                }
            },
            "pins": {
                "grpc": {
                    "client": [
                        {
                            "name": "to_act",
                            "serviceClass": "com.exactpro.th2.act.grpc.ActService"
                        },
                        {
                            "name": "to_check1",
                            "serviceClass": "com.exactpro.th2.check1.grpc.Check1Service"
                        }
                    ]
                }
            }
        }
    }
]
```



