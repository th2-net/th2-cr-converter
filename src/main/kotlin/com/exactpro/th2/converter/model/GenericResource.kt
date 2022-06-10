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

import com.exactpro.th2.converter.model.latest.Th2Metadata
import com.exactpro.th2.converter.util.ProjectConstants.API_VERSION_V2

data class GenericResource<T : Convertible>(
    private val apiVersion: String,
    private val kind: String,
    private val metadata: Th2Metadata,
    private val spec: T,
) : Th2Resource {
    override fun toNextVersion(): Th2Resource {
        return GenericResource(
            API_VERSION_V2,
            kind,
            metadata,
            spec.toNextVersion()
        )
    }

    override fun getApiVersion(): String {
        return this.apiVersion
    }

    override fun getKind(): String {
        return this.kind
    }

    override fun getMetadata(): Th2Metadata {
        return this.metadata
    }

    override fun getSpec(): Convertible {
        return this.spec
    }
}
