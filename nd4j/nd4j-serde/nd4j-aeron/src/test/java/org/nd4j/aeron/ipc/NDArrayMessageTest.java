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

package org.nd4j.aeron.ipc;

import org.agrona.DirectBuffer;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;

import static org.junit.Assert.assertEquals;

@NotThreadSafe
@Ignore("Tests are too flaky")

public class NDArrayMessageTest extends BaseND4JTest {

    @Test
    public void testNDArrayMessageToAndFrom() {
        NDArrayMessage message = NDArrayMessage.wholeArrayUpdate(Nd4j.scalar(1.0));
        DirectBuffer bufferConvert = NDArrayMessage.toBuffer(message);
        bufferConvert.byteBuffer().rewind();
        NDArrayMessage newMessage = NDArrayMessage.fromBuffer(bufferConvert, 0);
        assertEquals(message, newMessage);

        INDArray compressed = Nd4j.getCompressor().compress(Nd4j.scalar(1.0), "GZIP");
        NDArrayMessage messageCompressed = NDArrayMessage.wholeArrayUpdate(compressed);
        DirectBuffer bufferConvertCompressed = NDArrayMessage.toBuffer(messageCompressed);
        NDArrayMessage newMessageTest = NDArrayMessage.fromBuffer(bufferConvertCompressed, 0);
        assertEquals(messageCompressed, newMessageTest);


    }


}
