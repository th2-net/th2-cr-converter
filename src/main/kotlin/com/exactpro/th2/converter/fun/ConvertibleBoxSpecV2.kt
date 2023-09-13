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

package com.exactpro.th2.converter.`fun`

import com.exactpro.th2.model.latest.box.Spec
import com.exactpro.th2.model.v2.SpecV2

class ConvertibleBoxSpecV2(val spec: SpecV2) : Convertible {
    override fun toNextVersion(): Convertible {
        return ConvertibleBoxSpecV2x2(
            Spec(
                spec.imageName,
                spec.imageVersion,
                spec.type,
                spec.versionRange,
                spec.customConfig,
                spec.extendedSettings?.toExtendedSettings(),
                spec.pins,
                spec.prometheus,
                spec.loggingConfig,
                spec.mqRouter,
                spec.grpcRouter,
                spec.cradleManager,
                spec.disabled,
                spec.bookName,
                spec.imagePullSecrets,
            )
        )
    }

    override fun getSpecObject(): Any {
        return spec
    }
}
