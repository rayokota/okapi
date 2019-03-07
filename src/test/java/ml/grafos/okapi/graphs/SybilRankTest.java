/**
 * Copyright 2014 Grafos.ml
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
package ml.grafos.okapi.graphs;

import junit.framework.Assert;
import ml.grafos.okapi.io.formats.LongDoubleTextEdgeInputFormat;
import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.io.formats.IdWithValueTextOutputFormat;
import org.apache.giraph.utils.InternalVertexRunner;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.fail;

public class SybilRankTest {

  @Test
  public void test() {
    String[] graph = {
            "1 2 5.0",
            "2 1 5.0",
            "2 4 4.0",
            "4 2 4.0",
            "4 5 3.0",
            "5 4 3.0",
            "3 5 3.0",
            "5 3 3.0",
            "1 3 2.0",
            "3 1 2.0",
            "3 7 1.0",
            "7 3 1.0",
            "6 7 3.0",
            "7 6 3.0",
            "6 9 3.0",
            "9 6 3.0",
            "8 9 2.0",
            "9 8 2.0",
            "7 8 3.0",
            "8 7 3.0"
    };

    String[] vertices = {
            "1",
            "2",
            "5"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setComputationClass(SybilRank.TrustAggregation.class);
    conf.setMasterComputeClass(SybilRank.SybilRankMasterCompute.class);
    conf.setEdgeInputFormatClass(LongDoubleTextEdgeInputFormat.class);
    conf.setVertexInputFormatClass(SybilRank.SybilRankVertexValueInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results;
    try {
      results = InternalVertexRunner.run(conf, vertices, graph);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occurred");
      return;
    }
    List<String> res = new LinkedList<String>();
    for (String string : results) {
      res.add(string);
      System.out.println(string);
    }
    Assert.assertEquals(9, res.size());
  }

}
