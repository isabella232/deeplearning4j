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

package org.datavec.api.records.reader.impl;

import org.datavec.api.records.Record;
import org.datavec.api.records.SequenceRecord;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataLine;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.regex.RegexLineRecordReader;
import org.datavec.api.records.reader.impl.regex.RegexSequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.common.tests.BaseND4JTest;
import org.nd4j.common.io.ClassPathResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RegexRecordReaderTest  extends BaseND4JTest {

    @Rule
    public TemporaryFolder testDir = new TemporaryFolder();

    @Test
    public void testRegexLineRecordReader() throws Exception {
        String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\d+) ([A-Z]+) (.*)";

        RecordReader rr = new RegexLineRecordReader(regex, 1);
        rr.initialize(new FileSplit(new ClassPathResource("datavec-api/logtestdata/logtestfile0.txt").getFile()));

        List<Writable> exp0 = Arrays.asList((Writable) new Text("2016-01-01 23:59:59.001"), new Text("1"),
                        new Text("DEBUG"), new Text("First entry message!"));
        List<Writable> exp1 = Arrays.asList((Writable) new Text("2016-01-01 23:59:59.002"), new Text("2"),
                        new Text("INFO"), new Text("Second entry message!"));
        List<Writable> exp2 = Arrays.asList((Writable) new Text("2016-01-01 23:59:59.003"), new Text("3"),
                        new Text("WARN"), new Text("Third entry message!"));
        assertEquals(exp0, rr.next());
        assertEquals(exp1, rr.next());
        assertEquals(exp2, rr.next());
        assertFalse(rr.hasNext());

        //Test reset:
        rr.reset();
        assertEquals(exp0, rr.next());
        assertEquals(exp1, rr.next());
        assertEquals(exp2, rr.next());
        assertFalse(rr.hasNext());
    }

    @Test
    public void testRegexLineRecordReaderMeta() throws Exception {
        String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\d+) ([A-Z]+) (.*)";

        RecordReader rr = new RegexLineRecordReader(regex, 1);
        rr.initialize(new FileSplit(new ClassPathResource("datavec-api/logtestdata/logtestfile0.txt").getFile()));

        List<List<Writable>> list = new ArrayList<>();
        while (rr.hasNext()) {
            list.add(rr.next());
        }
        assertEquals(3, list.size());

        List<Record> list2 = new ArrayList<>();
        List<List<Writable>> list3 = new ArrayList<>();
        List<RecordMetaData> meta = new ArrayList<>();
        rr.reset();
        int count = 1; //Start by skipping 1 line
        while (rr.hasNext()) {
            Record r = rr.nextRecord();
            list2.add(r);
            list3.add(r.getRecord());
            meta.add(r.getMetaData());

            assertEquals(count++, ((RecordMetaDataLine) r.getMetaData()).getLineNumber());
        }

        List<Record> fromMeta = rr.loadFromMetaData(meta);

        assertEquals(list, list3);
        assertEquals(list2, fromMeta);
    }

    @Test
    public void testRegexSequenceRecordReader() throws Exception {
        String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\d+) ([A-Z]+) (.*)";

        ClassPathResource cpr = new ClassPathResource("datavec-api/logtestdata/");
        File f = testDir.newFolder();
        cpr.copyDirectory(f);
        String path = new File(f, "logtestfile%d.txt").getAbsolutePath();

        InputSplit is = new NumberedFileInputSplit(path, 0, 1);

        SequenceRecordReader rr = new RegexSequenceRecordReader(regex, 1);
        rr.initialize(is);

        List<List<Writable>> exp0 = new ArrayList<>();
        exp0.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.001"), new Text("1"), new Text("DEBUG"),
                        new Text("First entry message!")));
        exp0.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.002"), new Text("2"), new Text("INFO"),
                        new Text("Second entry message!")));
        exp0.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.003"), new Text("3"), new Text("WARN"),
                        new Text("Third entry message!")));


        List<List<Writable>> exp1 = new ArrayList<>();
        exp1.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.011"), new Text("11"), new Text("DEBUG"),
                        new Text("First entry message!")));
        exp1.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.012"), new Text("12"), new Text("INFO"),
                        new Text("Second entry message!")));
        exp1.add(Arrays.asList((Writable) new Text("2016-01-01 23:59:59.013"), new Text("13"), new Text("WARN"),
                        new Text("Third entry message!")));

        assertEquals(exp0, rr.sequenceRecord());
        assertEquals(exp1, rr.sequenceRecord());
        assertFalse(rr.hasNext());

        //Test resetting:
        rr.reset();
        assertEquals(exp0, rr.sequenceRecord());
        assertEquals(exp1, rr.sequenceRecord());
        assertFalse(rr.hasNext());
    }

    @Test
    public void testRegexSequenceRecordReaderMeta() throws Exception {
        String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\d+) ([A-Z]+) (.*)";

        ClassPathResource cpr = new ClassPathResource("datavec-api/logtestdata/");
        File f = testDir.newFolder();
        cpr.copyDirectory(f);
        String path = new File(f, "logtestfile%d.txt").getAbsolutePath();

        InputSplit is = new NumberedFileInputSplit(path, 0, 1);

        SequenceRecordReader rr = new RegexSequenceRecordReader(regex, 1);
        rr.initialize(is);

        List<List<List<Writable>>> out = new ArrayList<>();
        while (rr.hasNext()) {
            out.add(rr.sequenceRecord());
        }

        assertEquals(2, out.size());
        List<List<List<Writable>>> out2 = new ArrayList<>();
        List<SequenceRecord> out3 = new ArrayList<>();
        List<RecordMetaData> meta = new ArrayList<>();
        rr.reset();
        while (rr.hasNext()) {
            SequenceRecord seqr = rr.nextSequence();
            out2.add(seqr.getSequenceRecord());
            out3.add(seqr);
            meta.add(seqr.getMetaData());
        }

        List<SequenceRecord> fromMeta = rr.loadSequenceFromMetaData(meta);

        assertEquals(out, out2);
        assertEquals(out3, fromMeta);
    }

}
