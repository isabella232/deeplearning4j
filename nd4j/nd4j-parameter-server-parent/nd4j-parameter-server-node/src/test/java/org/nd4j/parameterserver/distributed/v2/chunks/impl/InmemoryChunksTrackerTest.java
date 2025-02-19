/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.nd4j.parameterserver.distributed.v2.chunks.impl;

import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.parameterserver.distributed.v2.chunks.VoidChunk;
import org.nd4j.parameterserver.distributed.v2.messages.impl.GradientsUpdateMessage;
import org.nd4j.parameterserver.distributed.v2.util.MessageSplitter;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class InmemoryChunksTrackerTest extends BaseND4JTest {
    @Test
    @Ignore
    public void testTracker_1() throws Exception {
        val array = Nd4j.linspace(1, 100000, 10000).reshape(-1, 1000);
        val splitter = MessageSplitter.getInstance();

        val message = new GradientsUpdateMessage("123", array);
        val messages = new ArrayList<VoidChunk>(splitter.split(message, 16384));

        val tracker = new InmemoryChunksTracker<GradientsUpdateMessage>(messages.get(0));

        assertFalse(tracker.isComplete());

        for (val m:messages)
            tracker.append(m);

        assertTrue(tracker.isComplete());

        val des = tracker.getMessage();
        assertNotNull(des);

        val restored = des.getPayload();
        assertNotNull(restored);

        assertEquals(array, restored);
    }
}