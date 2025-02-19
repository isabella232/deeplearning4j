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

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import org.agrona.CloseHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;

@NotThreadSafe
@Ignore("Tests are too flaky")

public class NdArrayIpcTest extends BaseND4JTest {
    private MediaDriver mediaDriver;
    private static Logger log = LoggerFactory.getLogger(NdArrayIpcTest.class);
    private Aeron.Context ctx;
    private String channel = "aeron:udp?endpoint=localhost:" + (40132 + new java.util.Random().nextInt(3000));
    private int streamId = 10;
    private int length = (int) 1e7;

    @Override
    public long getTimeoutMilliseconds() {
        return 120000L;
    }

    @Before
    public void before() {
        if(isIntegrationTests()) {
            MediaDriver.Context ctx = AeronUtil.getMediaDriverContext(length);
            mediaDriver = MediaDriver.launchEmbedded(ctx);
            System.out.println("Using media driver directory " + mediaDriver.aeronDirectoryName());
            System.out.println("Launched media driver");
        }
    }

    @After
    public void after() {
        if(isIntegrationTests()) {
            CloseHelper.quietClose(mediaDriver);
        }
    }

    @Test
    public void testMultiThreadedIpc() throws Exception {
        skipUnlessIntegrationTests();   //Long-running test - don't run as part of unit tests by default

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        INDArray arr = Nd4j.scalar(1.0);

        final AtomicBoolean running = new AtomicBoolean(true);
        Aeron aeron = Aeron.connect(getContext());
        int numSubscribers = 10;
        AeronNDArraySubscriber[] subscribers = new AeronNDArraySubscriber[numSubscribers];
        for (int i = 0; i < numSubscribers; i++) {
            AeronNDArraySubscriber subscriber = AeronNDArraySubscriber.builder().streamId(streamId).ctx(getContext())
                            .channel(channel).aeron(aeron).running(running).ndArrayCallback(new NDArrayCallback() {
                                /**
                                 * A listener for ndarray message
                                 *
                                 * @param message the message for the callback
                                 */
                                @Override
                                public void onNDArrayMessage(NDArrayMessage message) {
                                    System.out.println("Callback invoked for subscriber on ndarray ipc test");
                                    running.set(false);
                                }

                                @Override
                                public void onNDArrayPartial(INDArray arr, long idx, int... dimensions) {

                            }

                                @Override
                                public void onNDArray(INDArray arr) {
                                    System.out.println("Callback invoked for subscriber on ndarray ipc test");
                                    running.set(false);
                                }
                            }).build();


            Thread t = new Thread(() -> {
                try {
                    subscriber.launch();
                } catch (Exception e) {
                    log.error("",e);
                }

            });

            t.setDaemon(true);
            t.start();


            subscribers[i] = subscriber;
        }

        AeronNDArrayPublisher publisher =
                        AeronNDArrayPublisher.builder().publishRetryTimeOut(30000)
                                .streamId(streamId).channel(channel).aeron(aeron).build();

        Thread.sleep(10000);

        for (int i = 0; i < 10 && running.get(); i++) {
            executorService.execute(() -> {
                try {
                    log.info("About to send array.");
                    publisher.publish(arr);
                    log.info("Sent array");
                } catch (Exception e) {
                    log.error("",e);
                }
            });

        }


        Thread.sleep(30000);

        for (int i = 0; i < numSubscribers; i++)
            CloseHelper.close(subscribers[i]);
        CloseHelper.close(publisher);
        CloseHelper.close(aeron);
        Thread.sleep(10000);
        assertFalse(running.get());
    }

    @Test
    public void testIpc() throws Exception {
        skipUnlessIntegrationTests();   //Long-running test - don't run as part of unit tests by default

        INDArray arr = Nd4j.scalar(1.0);


        final AtomicBoolean running = new AtomicBoolean(true);
        Aeron aeron = Aeron.connect(getContext());


        AeronNDArraySubscriber subscriber = AeronNDArraySubscriber.builder().streamId(streamId).aeron(aeron)
                        .channel(channel).running(running).ndArrayCallback(new NDArrayCallback() {
                            /**
                             * A listener for ndarray message
                             *
                             * @param message the message for the callback
                             */
                            @Override
                            public void onNDArrayMessage(NDArrayMessage message) {
                                System.out.println(arr);
                                running.set(false);
                            }

                            @Override
                            public void onNDArrayPartial(INDArray arr, long idx, int... dimensions) {

                        }

                            @Override
                            public void onNDArray(INDArray arr) {

                        }
                        }).build();


        Thread t = new Thread(() -> {
            try {
                subscriber.launch();
            } catch (Exception e) {
                log.error("",e);
            }

        });

        t.start();

        while (!subscriber.launched())
            Thread.sleep(1000);

        Thread.sleep(10000);

        AeronNDArrayPublisher publisher =
                        AeronNDArrayPublisher.builder().streamId(streamId).aeron(aeron).channel(channel).build();
        for (int i = 0; i < 1 && running.get(); i++) {
            publisher.publish(arr);
        }



        Thread.sleep(30000);

        assertFalse(running.get());


        publisher.close();
        subscriber.close();

    }


    private Aeron.Context getContext() {
        if (ctx == null)
            ctx = new Aeron.Context().driverTimeoutMs(1000000)
                            .availableImageHandler(image -> System.out.println(image))
                            .unavailableImageHandler(AeronUtil::printUnavailableImage)
                            .aeronDirectoryName(mediaDriver.aeronDirectoryName()).keepAliveIntervalNs(1000000)
                            .errorHandler(e -> log.error(e.toString(), e));
        return ctx;
    }
}
