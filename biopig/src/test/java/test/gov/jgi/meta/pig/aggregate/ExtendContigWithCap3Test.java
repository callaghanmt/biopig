/*
 * Copyright (c) 2010, The Regents of the University of California, through Lawrence Berkeley
 * National Laboratory (subject to receipt of any required approvals from the U.S. Dept. of Energy).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * (1) Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * (2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * (3) Neither the name of the University of California, Lawrence Berkeley National Laboratory, U.S. Dept.
 * of Energy, nor the names of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * You are under no obligation whatsoever to provide any bug fixes, patches, or upgrades to the
 * features, functionality or performance of the source code ("Enhancements") to anyone; however,
 * if you choose to make your Enhancements available either publicly, or directly to Lawrence Berkeley
 * National Laboratory, without imposing a separate written license agreement for such Enhancements,
 * then you hereby grant the following license: a  non-exclusive, royalty-free perpetual license to install,
 * use, modify, prepare derivative works, incorporate into other computer software, distribute, and
 * sublicense such enhancements or derivative works thereof, in binary and source code form.
 */

package test.gov.jgi.meta.pig.aggregate;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.Tuple;
import test.gov.jgi.meta.Util;

import java.io.IOException;
import java.util.Iterator;

/**
 * ExtendContigWithCap3 Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>03/07/2011</pre>
 */
public class ExtendContigWithCap3Test extends TestCase {
   public ExtendContigWithCap3Test(String name) {
      super(name);
   }

   public void setUp() throws Exception {
      super.setUp();
   }

   public void tearDown() throws Exception {
      super.tearDown();
   }

   /**
    * Method: exec(Tuple input)
    */
   public void testExec() throws Exception {
      PigServer ps = new PigServer(ExecType.LOCAL);
      String script = "a = load 'target/test-classes/one.txt' using PigStorage() as (one: int);\n" +
                      "b = foreach a generate 'aaaacc', { ('1', 0, 'aaacccc'), ('2', 0, 'acccccc') };\n" +
                      "d = foreach b generate gov.jgi.meta.pig.aggregate.ExtendContigWithCap3($0, $1);\n";

      Util.registerMultiLineQuery(ps, script);
      Iterator<Tuple> it = ps.openIterator("d");

      assertEquals(Util.createTuple(new String[] { "aaaacccccc" }), it.next().get(0));
   }

   public void testContigExtensionWithLargerDataset() throws Exception {
       PigServer ps = new PigServer(ExecType.LOCAL);
       String script = "reads = load '/scratch/karan/1M.fas' using gov.jgi.meta.pig.storage.FastaStorage as (id: chararray, d: int, seq: bytearray, header: chararray);\n" +
               "readindex = foreach reads generate\n" +
               "            seq,\n" +
               "            FLATTEN(gov.jgi.meta.pig.eval.KmerGenerator(seq, 20)) as (kmer:bytearray);\n" +
               "contigs = load '/scratch/karan/all_enzymes_contigs.fas' using gov.jgi.meta.pig.storage.FastaStorage as (id: chararray, d: int, seq: bytearray, header: chararray);\n" +
               "contigindex = foreach contigs generate\n" +
               "              id,\n" +
               "              FLATTEN(gov.jgi.meta.pig.eval.KmerGenerator(seq, 20, 0, 10, 1)) as (kmer:bytearray);\n" +
               "j = join contigindex by kmer,\n" +
               "         readindex by kmer;\n" +
               "k = foreach j generate\n" +
               "         contigindex::id as contigid,\n" +
               "         gov.jgi.meta.pig.eval.UnpackSequence(readindex::seq) as readseq;\n" +
               "l = group k by contigid;\n" +
               "m = foreach l {\n" +
               "         a = $1.$1;\n" +
               "         generate $0, a;\n" +
               "}\n" +
               "n = join contigs by id, m by $0;\n" +
               "complete = foreach n generate gov.jgi.meta.pig.aggregate.ExtendContigWithCap3($2, $5);\n";

       Util.registerMultiLineQuery(ps, script);
       Iterator<Tuple> it = ps.openIterator("complete");

       assertEquals(Util.createTuple(new String[] { "aaaacccccc" }), it.next().get(0));
    }




   public static Test suite() {
      return new TestSuite(ExtendContigWithCap3Test.class);
   }
}
