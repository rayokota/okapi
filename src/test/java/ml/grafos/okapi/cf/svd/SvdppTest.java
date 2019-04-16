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
package ml.grafos.okapi.cf.svd;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;
import ml.grafos.okapi.cf.CfLongIdFloatTextInputFormat;
import ml.grafos.okapi.common.jblas.FloatMatrixWritable;

import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.io.formats.IdWithValueTextOutputFormat;
import org.apache.giraph.utils.InternalVertexRunner;
import org.jblas.FloatMatrix;
import org.junit.Test;

public class SvdppTest {
  
  @Test
  public void testUserUpdate() {
    float lambda = 0.01f;
    float gamma = 0.005f;
    float error = 1f;
    
    //user = (0.1, 0.2, 0.3)
    FloatMatrix user = new FloatMatrix(1, 3, new float[]{0.1f, 0.2f, 0.3f});
    //item = (0.2, 0.1, 0.4)
    FloatMatrix item = new FloatMatrix(1, 3, new float[]{0.2f, 0.1f, 0.4f});
    
    Svdpp.UserComputation comp = new Svdpp.UserComputation();
    comp.updateValue(user, item, error, gamma, lambda);
    
    assertArrayEquals(user.data, new float[] {0.100995f, 0.20049f, 0.301985f}, 
        0.000001f );
  }
  
  @Test
  public void testIncrementValue() {
    float lambda = 0.01f;
    float gamma = 0.005f;
    
    //value = (0.1, 0.2, 0.3)
    FloatMatrix value = new FloatMatrix(1, 3, new float[]{0.1f, 0.2f, 0.3f});
    //step = (0.2, 0.1, 0.4)
    FloatMatrix step = new FloatMatrix(1, 3, new float[]{0.2f, 0.1f, 0.4f});
    
    Svdpp.incrementValue(value, step, gamma, lambda);
    
    assertArrayEquals(value.data, new float[] {0.3f, 0.3f, 0.7f}, 0.0001f);
  }
  
  @Test
  public void testUpdateBaseline() {
    float baseline = 0.5f;
    float predictedRating = 4f;
    float observedRating = 3f; 
    float gamma = 0.005f;
    float lambda = 0.01f;
    
    float newBaseline = Svdpp.computeUpdatedBaseLine(baseline, predictedRating, 
        observedRating, gamma, lambda);
    
    assertEquals(newBaseline, 0.50475, 0.001f);
  }
  
  @Test
  public void testPredictRating() {
    float meanRating = 3;
    float userBaseline = 4;
    float itemBaseline = 2;
    float minRating = 0f;
    float maxRating = 5f;
    int numRatings = 10;
    //user = (0.1, 0.2, 0.3)
    FloatMatrix user = new FloatMatrix(1, 3, new float[]{0.1f, 0.2f, 0.3f});
    //item = (0.2, 0.1, 0.4)
    FloatMatrix item = new FloatMatrix(1, 3, new float[]{0.2f, 0.1f, 0.4f});
    //weights = (0.4, 0.6, 0.8)
    FloatMatrix weights = new FloatMatrix(1, 3, new float[]{0.4f, 0.6f, 0.8f});
    
    float prediction = Svdpp.computePredictedRating(meanRating, userBaseline, 
        itemBaseline, user, item, numRatings, weights, minRating, maxRating);
    
    assertEquals(prediction, 5.0f , 0.000001f);
    
    userBaseline = -2;
    prediction = Svdpp.computePredictedRating(meanRating, userBaseline, 
        itemBaseline, user, item, numRatings, weights, minRating, maxRating);

    assertEquals(prediction, 3.305464f , 0.000001f);
  }
  
  @Test
  public void testValueSerialization() throws IOException {
    float baseline = 0.5f;
    FloatMatrixWritable factors = 
        new FloatMatrixWritable(1, 3, new float[]{0.1f, 0.2f, 0.3f});
    FloatMatrixWritable weight = 
        new FloatMatrixWritable(1, 3, new float[]{0.0f, Float.MAX_VALUE, 0.3f});
    Svdpp.SvdppValue value = new Svdpp.SvdppValue(baseline, factors, weight);
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
    DataOutput output = new DataOutputStream(baos);
    value.write(output);

    Svdpp.SvdppValue valueCopy = new Svdpp.SvdppValue();
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(
        baos.toByteArray()));
    valueCopy.readFields(input);
    
    assertEquals(value.getBaseline(), valueCopy.getBaseline(), 0.000001f);
    assertEquals(value.getFactors(), valueCopy.getFactors());
    assertEquals(value.getWeight(), valueCopy.getWeight());
  }

  @Test
  public void testEndtoEnd() throws Exception {
    String[] graph = {
        "1 1 1.0",
        "1 2 2.0",
        "2 1 3.0",
        "2 2 4.0"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setComputationClass(Svdpp.InitUsersComputation.class);
    conf.setMasterComputeClass(Svdpp.MasterCompute.class);
    conf.setEdgeInputFormatClass(CfLongIdFloatTextInputFormat.class);
    conf.setFloat(Svdpp.BIAS_LAMBDA, 0.005f);
    conf.setFloat(Svdpp.BIAS_GAMMA, 0.01f);
    conf.setFloat(Svdpp.FACTOR_LAMBDA, 0.005f);
    conf.setFloat(Svdpp.FACTOR_GAMMA, 0.01f);
    conf.setFloat(Svdpp.MIN_RATING, 0);
    conf.setFloat(Svdpp.MAX_RATING, 5);
    conf.setInt(Svdpp.VECTOR_SIZE, 2);
    conf.setInt(Svdpp.ITERATIONS, 5);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results = InternalVertexRunner.run(conf, null, graph);
    List<String> res = new LinkedList<String>();
    for (String string : results) {
      res.add(string);
    }
    System.out.println(res);
    Assert.assertEquals(4, res.size());
    
  }

  @Test
  public void testFilmTrust() throws Exception {
    ArrayList<String> result = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader("/tmp/ratings_duped.txt"))) {
      while (br.ready()) {
        result.add(br.readLine());
      }
    }
    String[] graph = result.toArray(new String[result.size()]);

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setComputationClass(Svdpp.InitUsersComputation.class);
    conf.setMasterComputeClass(Svdpp.MasterCompute.class);
    conf.setEdgeInputFormatClass(CfLongIdFloatTextInputFormat.class);
    conf.setFloat(Svdpp.BIAS_LAMBDA, 0.005f);
    conf.setFloat(Svdpp.BIAS_GAMMA, 0.01f);
    conf.setFloat(Svdpp.FACTOR_LAMBDA, 0.005f);
    conf.setFloat(Svdpp.FACTOR_GAMMA, 0.01f);
    conf.setFloat(Svdpp.MIN_RATING, 0);
    conf.setFloat(Svdpp.MAX_RATING, 5);
    conf.setInt(Svdpp.VECTOR_SIZE, 2);
    conf.setInt(Svdpp.ITERATIONS, 2);
    //conf.setInt(Svdpp.ITERATIONS, 5);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results = InternalVertexRunner.run(conf, null, graph);
    Set<String> res = new TreeSet<String>();
    for (String string : results) {
      res.add(string);
    }
    System.out.println(res);
    Assert.assertEquals(3579, res.size());

  }
}
