/*
 * Copyright (C) 2012-2017 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.sample_classes.parser.entity;

import java.util.UUID;

import info.archinnov.achilles.annotations.*;
import info.archinnov.achilles.internals.sample_classes.APUnitTest;

@APUnitTest
@Table
@Immutable
public class TestImmutableEntity {

    @PartitionKey
    public final Long partition;

    @ClusteringColumn
    public final UUID clustering;

    @Column
    @Frozen
    public final TestImmutableUDT udt;

    public TestImmutableEntity(Long partition, UUID clustering, @Frozen TestImmutableUDT udt) {
        this.partition = partition;
        this.clustering = clustering;
        this.udt = udt;
    }
}
