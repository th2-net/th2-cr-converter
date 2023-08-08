/*
 * Copyright 2020-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.converter.model

import com.exactpro.th2.converter.`fun`.Convertible
import com.exactpro.th2.converter.util.SchemaVersion
import com.exactpro.th2.infrarepo.repo.RepositoryResource
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.fabric8.kubernetes.api.model.ObjectMeta

data class Th2Resource(
    @JsonIgnore val schemaVersion: SchemaVersion,
    val kind: String,
    val metadata: ObjectMeta,
    @JsonIgnore var specWrapper: Convertible,
) {
    @JsonProperty
    val apiVersion = schemaVersion.apiVersion

    @JsonProperty
    val spec = specWrapper.getSpecObject()

    fun toNextVersion(): Th2Resource {
        return Th2Resource(
            schemaVersion.nextVersion!!,
            kind,
            metadata,
            specWrapper.toNextVersion()
        )
    }

    fun toRepositoryResource() = RepositoryResource(apiVersion, kind, metadata, spec)
}
