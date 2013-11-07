package ml.grafos.okapi.cf.ranking;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import ml.grafos.okapi.cf.eval.DoubleArrayListWritable;
import ml.grafos.okapi.cf.eval.LongDoubleArrayListMessage;

import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.edge.EdgeFactory;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.utils.InternalVertexRunner;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TFMAPRankingComputationTest {
	
	TFMAPRankingComputation tfmap;
	
	@Before
	public void setUp() throws Exception {
		tfmap = new TFMAPRankingComputation();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFull() throws Exception{
		String[] graph = { 
				"1 -1 1",
				"1 -2 1",
				"2 -1 1",
				"3 -2 1",
				"4 -1 1",
				"4 -2 1",
				"5 -3 1",
				"6 -4 1",
				"7 -5 1",
				"8 -6 1",
				"9 -7 1",
				"10 -8 1"
		};

		GiraphConfiguration conf = new GiraphConfiguration();
		conf.setComputationClass(TFMAPRankingComputation.class);
		conf.setEdgeInputFormatClass(LongLongIntInputFormat.class);
		conf.set("minItemId", "-8");
		conf.set("maxItemId", "-1");
		conf.set("iter", "2");
		conf.set("bufferSize", "2");
		conf.setVertexOutputFormatClass(LongDoubleArrayListLongArrayListOutputFormat.class);
		Iterable<String> results = InternalVertexRunner.run(conf, null, graph);
		List<String> res = new LinkedList<String>();
		for (String string : results) {
			res.add(string);
			System.out.println(string);
		}
		Assert.assertEquals(18, res.size());
	}
	
	@Test
	public void testsampleRelevantAndIrrelevantEdges(){
		//lets setup vertex mock with one edge
		
		tfmap.setMaxItemId(-1);
		tfmap.setMinItemId(-4);
		TFMAPRankingComputation tfmapMock = spy(tfmap);
		doNothing().when(tfmapMock).sendMessage(any(LongWritable.class), any(LongDoubleArrayListMessage.class));
		
		Vertex<LongWritable, DoubleArrayListWritable, IntWritable> vertex = mock(Vertex.class);
		List<Edge<LongWritable, IntWritable>> edges = new LinkedList<Edge<LongWritable,IntWritable>>();
		edges.add(EdgeFactory.create(new LongWritable(-3), new IntWritable(1)));
		edges.add(EdgeFactory.create(new LongWritable(-4), new IntWritable(1)));
		
		when(vertex.getId()).thenReturn(new LongWritable(1));
		when(vertex.getNumEdges()).thenReturn(2);
		when(vertex.getEdges()).thenReturn(edges);
		
		tfmapMock.sampleRelevantAndIrrelevantEdges(vertex, 2);
		verify(tfmapMock).sendRequestForFactors(-3, 1, true);
		verify(tfmapMock).sendRequestForFactors(-4, 1, true);
		verify(tfmapMock).sendRequestForFactors(-1, 1, false);
		verify(tfmapMock).sendRequestForFactors(-2, 1, false);

	}

}
