### Conversion API

__GET/test__

__Returns:__
"Conversion API is working !"

__PUT/convert/{schemaName}/{targetVersion}__

Get files for given schema from gits. Convert to given version and pushes back converted files

__Path variable:__

*schemaName* - Name of the schema, same as the branch name.

*targetVersion* - To what version schema should be converted

Throws exception if requested schema doesn't exist, or is the same as master.

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

list of error encountered during conversion (if any)

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