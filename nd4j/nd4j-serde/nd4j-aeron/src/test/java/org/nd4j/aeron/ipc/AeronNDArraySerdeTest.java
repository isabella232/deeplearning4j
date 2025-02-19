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

import org.agrona.concurrent.UnsafeBuffer;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
@NotThreadSafe
@Ignore("Tests are too flaky")

public class AeronNDArraySerdeTest extends BaseND4JTest {

    @Test
    public void testToAndFrom() {
        INDArray arr = Nd4j.scalar(1.0);
        UnsafeBuffer buffer = AeronNDArraySerde.toBuffer(arr);
        INDArray back = AeronNDArraySerde.toArray(buffer);
        assertEquals(arr, back);
    }

    @Test
    public void testToAndFromCompressed() {
        INDArray arr = Nd4j.scalar(1.0);
        INDArray compress = Nd4j.getCompressor().compress(arr, "GZIP");
        assertTrue(compress.isCompressed());
        UnsafeBuffer buffer = AeronNDArraySerde.toBuffer(compress);
        INDArray back = AeronNDArraySerde.toArray(buffer);
        INDArray decompressed = Nd4j.getCompressor().decompress(compress);
        assertEquals(arr, decompressed);
        assertEquals(arr, back);
    }


    @Test
    @Ignore // timeout, skip step ignored
    public void testToAndFromCompressedLarge() {
        skipUnlessIntegrationTests();

        INDArray arr = Nd4j.zeros((int) 1e7);
        INDArray compress = Nd4j.getCompressor().compress(arr, "GZIP");
        assertTrue(compress.isCompressed());
        UnsafeBuffer buffer = AeronNDArraySerde.toBuffer(compress);
        INDArray back = AeronNDArraySerde.toArray(buffer);
        INDArray decompressed = Nd4j.getCompressor().decompress(compress);
        assertEquals(arr, decompressed);
        assertEquals(arr, back);
    }


    @Test
    public void timeOldVsNew() throws Exception {
        int numTrials = 1000;
        long oldTotal = 0;
        long newTotal = 0;
        INDArray arr = Nd4j.create(100000);
        Nd4j.getCompressor().compressi(arr, "GZIP");
        for (int i = 0; i < numTrials; i++) {
            StopWatch oldStopWatch = new StopWatch();
            BufferedOutputStream bos = new BufferedOutputStream(new ByteArrayOutputStream((int) arr.length()));
            DataOutputStream dos = new DataOutputStream(bos);
            oldStopWatch.start();
            Nd4j.write(arr, dos);
            oldStopWatch.stop();
            // System.out.println("Old " + oldStopWatch.getNanoTime());
            oldTotal += oldStopWatch.getNanoTime();
            StopWatch newStopWatch = new StopWatch();
            newStopWatch.start();
            AeronNDArraySerde.toBuffer(arr);
            newStopWatch.stop();
            //  System.out.println("New " + newStopWatch.getNanoTime());
            newTotal += newStopWatch.getNanoTime();

        }

        oldTotal /= numTrials;
        newTotal /= numTrials;
        System.out.println("Old avg " + oldTotal + " New avg " + newTotal);

    }

    @Override
    public long getTimeoutMilliseconds() {
        return Long.MAX_VALUE;
    }
}
