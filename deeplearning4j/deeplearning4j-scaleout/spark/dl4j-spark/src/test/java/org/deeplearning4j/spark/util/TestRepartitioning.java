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

package org.deeplearning4j.spark.util;

import com.sun.jna.Platform;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.deeplearning4j.spark.BaseSparkTest;
import org.deeplearning4j.spark.api.Repartition;
import org.deeplearning4j.spark.api.RepartitionStrategy;
import org.deeplearning4j.spark.impl.common.CountPartitionsFunction;
import org.deeplearning4j.spark.impl.repartitioner.DefaultRepartitioner;
import org.junit.Assert;
import org.junit.Test;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRepartitioning extends BaseSparkTest {

    @Override
    public long getTimeoutMilliseconds() {
        return isIntegrationTests() ? 240000 : 60000;
    }

    @Test
    public void testRepartitioning() {
        if(Platform.isWindows()) {
            //Spark tests don't run on windows
            return;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(String.valueOf(i));
        }

        JavaRDD<String> rdd = sc.parallelize(list);
        rdd = rdd.repartition(200);

        JavaRDD<String> rdd2 = SparkUtils.repartitionBalanceIfRequired(rdd, Repartition.Always, 100, 10);
        assertFalse(rdd == rdd2); //Should be different objects due to repartitioning

        assertEquals(10, rdd2.partitions().size());
        for (int i = 0; i < 10; i++) {
            List<String> partition = rdd2.collectPartitions(new int[] {i})[0];
//            System.out.println("Partition " + i + " size: " + partition.size());
            assertEquals(100, partition.size()); //Should be exactly 100, for the util method (but NOT spark .repartition)
        }
    }

    @Test
    public void testRepartitioning2() throws Exception {
        if(Platform.isWindows()) {
            //Spark tests don't run on windows
            return;
        }
        int[] ns;
        if(isIntegrationTests()){
            ns = new int[]{320, 321, 25600, 25601, 25615};
        } else {
            ns = new int[]{320, 2561};
        }

        for (int n : ns) {

            List<String> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(String.valueOf(i));
            }

            JavaRDD<String> rdd = sc.parallelize(list);
            rdd.repartition(65);

            int totalDataSetObjectCount = n;
            int dataSetObjectsPerSplit = 8 * 4 * 10;
            int valuesPerPartition = 10;
            int nPartitions = 32;

            JavaRDD<String>[] splits = org.deeplearning4j.spark.util.SparkUtils.balancedRandomSplit(
                            totalDataSetObjectCount, dataSetObjectsPerSplit, rdd, new Random().nextLong());

            List<Integer> counts = new ArrayList<>();
            List<List<Tuple2<Integer, Integer>>> partitionCountList = new ArrayList<>();
            //            System.out.println("------------------------");
            //            System.out.println("Partitions Counts:");
            for (JavaRDD<String> split : splits) {
                JavaRDD<String> repartitioned = SparkUtils.repartition(split, Repartition.Always,
                                RepartitionStrategy.Balanced, valuesPerPartition, nPartitions);
                List<Tuple2<Integer, Integer>> partitionCounts = repartitioned
                                .mapPartitionsWithIndex(new CountPartitionsFunction<String>(), true).collect();
                //                System.out.println(partitionCounts);
                partitionCountList.add(partitionCounts);
                counts.add((int) split.count());
            }

            //            System.out.println(counts.size());
            //            System.out.println(counts);


            int expNumPartitionsWithMore = totalDataSetObjectCount % nPartitions;
            int actNumPartitionsWithMore = 0;
            for (List<Tuple2<Integer, Integer>> l : partitionCountList) {
                assertEquals(nPartitions, l.size());

                for (Tuple2<Integer, Integer> t2 : l) {
                    int partitionSize = t2._2();
                    if (partitionSize > valuesPerPartition)
                        actNumPartitionsWithMore++;
                }
            }

            assertEquals(expNumPartitionsWithMore, actNumPartitionsWithMore);
        }
    }

    @Test
    public void testRepartitioning3(){
        if(Platform.isWindows()) {
            //Spark tests don't run on windows
            return;
        }
        //Initial partitions (idx, count) - [(0,29), (1,29), (2,29), (3,34), (4,34), (5,35), (6,34)]

        List<Integer> ints = new ArrayList<>();
        for( int i=0; i<224; i++ ){
            ints.add(i);
        }

        JavaRDD<Integer> rdd = sc.parallelize(ints);
        JavaPairRDD<Integer,Integer> pRdd = SparkUtils.indexedRDD(rdd);
        JavaPairRDD<Integer,Integer> initial = pRdd.partitionBy(new Partitioner() {
            @Override
            public int getPartition(Object key) {
                int i = (Integer)key;
                if(i < 29){
                    return 0;
                } else if(i < 29+29){
                    return 1;
                } else if(i < 29+29+29){
                    return 2;
                } else if(i < 29+29+29+34){
                    return 3;
                } else if(i < 29+29+29+34+34){
                    return 4;
                } else if(i < 29+29+29+34+34+35){
                    return 5;
                } else {
                    return 6;
                }
            }
            @Override
            public int numPartitions() {
                return 7;
            }
        });

        List<Tuple2<Integer, Integer>> partitionCounts = initial.values().mapPartitionsWithIndex(new CountPartitionsFunction<Integer>(), true).collect();

//        System.out.println(partitionCounts);

        List<Tuple2<Integer,Integer>> initialExpected = Arrays.asList(
                new Tuple2<>(0,29),
                new Tuple2<>(1,29),
                new Tuple2<>(2,29),
                new Tuple2<>(3,34),
                new Tuple2<>(4,34),
                new Tuple2<>(5,35),
                new Tuple2<>(6,34));
        Assert.assertEquals(initialExpected, partitionCounts);


        JavaRDD<Integer> afterRepartition = SparkUtils.repartitionBalanceIfRequired(initial.values(), Repartition.Always, 2, 112);
        List<Tuple2<Integer, Integer>> partitionCountsAfter = afterRepartition.mapPartitionsWithIndex(new CountPartitionsFunction<Integer>(), true).collect();
//        System.out.println(partitionCountsAfter);

        for(Tuple2<Integer,Integer> t2 : partitionCountsAfter){
            assertEquals(2, (int)t2._2());
        }
    }

    @Test
    public void testRepartitioning4() {
        if(Platform.isWindows()) {
            //Spark tests don't run on windows
            return;
        }
        List<Integer> ints = new ArrayList<>();
        for( int i = 0; i < 7040; i++) {
            ints.add(i);
        }

        JavaRDD<Integer> rdd = sc.parallelize(ints);

        JavaRDD<Integer> afterRepartition = new DefaultRepartitioner().repartition(rdd, 1, 32);
        List<Tuple2<Integer, Integer>> partitionCountsAfter = afterRepartition.mapPartitionsWithIndex(new CountPartitionsFunction<Integer>(), true).collect();

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int minIdx = 0;
        int maxIdx = 0;
        for(Tuple2<Integer,Integer> t2 : partitionCountsAfter){
            min = Math.min(min, t2._2());
            max = Math.max(max, t2._2());
            if(min == t2._2()){
                minIdx = t2._1();
            }
            if(max == t2._2()){
                maxIdx = t2._1();
            }
        }

//        System.out.println("min: " + min + "\t@\t" + minIdx);
//        System.out.println("max: " + max + "\t@\t" + maxIdx);

        assertEquals(1, min);
        assertEquals(2, max);
    }


    @Test
    public void testRepartitioningApprox() {
        if(Platform.isWindows()) {
            //Spark tests don't run on windows
            return;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(String.valueOf(i));
        }

        JavaRDD<String> rdd = sc.parallelize(list);
        rdd = rdd.repartition(200);

        JavaRDD<String> rdd2 = SparkUtils.repartitionApproximateBalance(rdd, Repartition.Always, 10);
        assertFalse(rdd == rdd2); //Should be different objects due to repartitioning

        assertEquals(10, rdd2.partitions().size());

        for (int i = 0; i < 10; i++) {
            List<String> partition = rdd2.collectPartitions(new int[] {i})[0];
//            System.out.println("Partition " + i + " size: " + partition.size());
            assertTrue(partition.size() >= 90 && partition.size() <= 110);
        }
    }

    @Test
    public void testRepartitioningApproxReverse() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(String.valueOf(i));
        }

        // initial # of partitions = cores, probably < 100
        JavaRDD<String> rdd = sc.parallelize(list);

        JavaRDD<String> rdd2 = SparkUtils.repartitionApproximateBalance(rdd, Repartition.Always, 100);
        assertFalse(rdd == rdd2); //Should be different objects due to repartitioning

        assertEquals(100, rdd2.partitions().size());
    }


}
