package org.infinispan.largeobjectsupport;

import org.infinispan.config.Configuration;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.DistributionManagerImpl;
import org.infinispan.distribution.ch.NodeTopologyInfo;
import org.infinispan.distribution.ch.TopologyInfo;
import org.infinispan.remoting.transport.Address;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test {@link Chunks}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "largeobjectsupport.ChunksTest")
public class ChunksTest {

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void testThatChunksConstructorRejectsInputStreamNotSupportingMark() {
      InputStream inputStreamNotSupportingMark = new InputStream() {
         @Override
         public int read() throws IOException {
            return -1;
         }

         @Override
         public boolean markSupported() {
            return false;
         }
      };
      new Chunks<Object>(new Object(), inputStreamNotSupportingMark, 1000L, null, null);
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceCannotBeIteratedOverMoreThanOnce() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(1000), newConfigurationWithNumOwners(1));

      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }
      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }
   }

   @Test
   public void testThatChunksInstanceCorrectlyIteratesOverLargeObject() {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      long maxChunkSizeInBytes = 3L;
      InputStream largeObject = new ByteArrayInputStream(bytes);
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject,
               maxChunkSizeInBytes, newDistributionManagerWithNumNodesInCluster(1000),
               newConfigurationWithNumOwners(1));
      List<Chunk> allChunks = new ArrayList<Chunk>();

      for (Chunk chunk : objectUnderTest) {
         allChunks.add(chunk);
      }

      assert Arrays.equals(allChunks.get(0).getData(), new byte[] { 1, 2, 3 });
      assert Arrays.equals(allChunks.get(1).getData(), new byte[] { 4, 5, 6 });
      assert Arrays.equals(allChunks.get(2).getData(), new byte[] { 7 });
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatLargeObjectMetadataCannotBeCalledIfIterationNotYetFinished() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(1000), newConfigurationWithNumOwners(1));
      objectUnderTest.iterator().next(); // Should hold more than one chunk

      objectUnderTest.largeObjectMetadata();
   }

   @Test
   public void testThatChunksInstanceProducesCorrectLargeObjectMetadataAfterIteration() {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      long maxChunkSizeInBytes = 3L;
      InputStream largeObject = new ByteArrayInputStream(bytes);
      Object largeObjectKey = new Object();
      Chunks<Object> objectUnderTest = new Chunks<Object>(largeObjectKey, largeObject,
               maxChunkSizeInBytes, newDistributionManagerWithNumNodesInCluster(1000),
               newConfigurationWithNumOwners(1));
      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }

      LargeObjectMetadata<Object> largeObjectMetadata = objectUnderTest.largeObjectMetadata();

      assert largeObjectMetadata.getLargeObjectKey() == largeObjectKey : "Unexpected largeObjectKey in LargeObjectMetadat returned";
      assert largeObjectMetadata.getTotalSizeInBytes() == bytes.length : "Unexpected totalSizeInBytes in LargeObjectMetadat returned";
      assert largeObjectMetadata.getChunkKeys().length == 3 : "Unexpected number of chunk keys in LargeObjectMetadat returned";
   }

   @Test(expectedExceptions = UnsupportedOperationException.class)
   public void testThatChunkIteratorDoesNotSupportRemove() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(1000), newConfigurationWithNumOwners(1));

      objectUnderTest.iterator().remove();
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceRejectsNumberOfClusterNodesZero() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(0), newConfigurationWithNumOwners(1));

      objectUnderTest.iterator().next();
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceRejectsNumOwnersZero() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(1000), newConfigurationWithNumOwners(0));

      objectUnderTest.iterator().next();
   }

   @Test(expectedExceptions = LargeObjectExceedsSizeLimitException.class)
   public void testThatIterationFailsAsSoonAsLargeObjectSizeLimitIsExceeded() {
      long maximumChunkSizeInBytes = 8;
      int numberOfClusterNodes = 2;
      int numOwners = 4;
      long largeObjectSizeLimitInBytes = (maximumChunkSizeInBytes * numberOfClusterNodes)
               / numOwners;
      byte[] largeObjectData = new byte[(int) largeObjectSizeLimitInBytes + 1];
      Arrays.fill(largeObjectData, (byte) 16);
      InputStream largeObject = new ByteArrayInputStream(largeObjectData);
      Chunks<Object> objectUnderTest = new Chunks<Object>(new Object(), largeObject, 2L,
               newDistributionManagerWithNumNodesInCluster(numberOfClusterNodes),
               newConfigurationWithNumOwners(numOwners));

      for (Chunk chunk : objectUnderTest)
         chunk.getChunkKey(); // Whatever
   }

   private DistributionManager newDistributionManagerWithNumNodesInCluster(int numNodesInCluster) {
      TopologyInfo ti = new TopologyInfo();

      for (int i = 0; i < numNodesInCluster; i++) {
         Address nodeAddress = new Address() {
         };
         NodeTopologyInfo nti = new NodeTopologyInfo(String.valueOf(i), String.valueOf(i),
                  String.valueOf(i), nodeAddress);
         ti.addNodeTopologyInfo(nodeAddress, nti);
      }

      DistributionManager distributionManager = new DistributionManagerImpl();
      distributionManager.setTopologyInfo(ti);

      return distributionManager;
   }

   private Configuration newConfigurationWithNumOwners(final int numOwners) {
      return new Configuration() {
         @Override
         public int getNumOwners() {
            return numOwners;
         }
      };
   }
}
