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

package com.exactpro.th2.converter.config

import com.exactpro.th2.infrarepo.GitConfig

class LocalGitConfig : GitConfig {
    private val remoteRepository: String = ""

    private val localRepositoryRoot: String = ""

    private val sshDir: String = ""

    private val httpAuthUsername: String = ""

    private val httpAuthPassword: String = ""

    override fun getRemoteRepository(): String {
        return remoteRepository
    }

    override fun getHttpAuthUsername(): String {
        return httpAuthUsername
    }

    override fun getHttpAuthPassword(): String {
        return httpAuthPassword
    }

    override fun getLocalRepositoryRoot(): String {
        return localRepositoryRoot
    }

    override fun getSshDir(): String {
        return sshDir
    }

    override fun getPrivateKey(): ByteArray {
        return ByteArray(0)
    }
}
