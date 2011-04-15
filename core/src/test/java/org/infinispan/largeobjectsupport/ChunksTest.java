package org.infinispan.largeobjectsupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

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
      new Chunks(new Object(), inputStreamNotSupportingMark, 0L, null);
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testThatChunksInstanceCannotBeIteratedOverMoreThanOnce() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks objectUnderTest = new Chunks(new Object(), largeObject, 2L,
               new UuidBasedKeyGenerator());

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
      Chunks objectUnderTest = new Chunks(new Object(), largeObject, maxChunkSizeInBytes,
               new UuidBasedKeyGenerator());
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
      Chunks objectUnderTest = new Chunks(new Object(), largeObject, 2L,
               new UuidBasedKeyGenerator());
      objectUnderTest.iterator().next(); // Should hold more than one chunk

      objectUnderTest.largeObjectMetadata();
   }

   @Test
   public void testThatChunksInstanceProducesCorrectLargeObjectMetadataAfterIteration() {
      byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6, 7 };
      long maxChunkSizeInBytes = 3L;
      InputStream largeObject = new ByteArrayInputStream(bytes);
      Object largeObjectKey = new Object();
      Chunks objectUnderTest = new Chunks(largeObjectKey, largeObject, maxChunkSizeInBytes,
               new UuidBasedKeyGenerator());
      for (Chunk chunk : objectUnderTest) {
         chunk.getChunkKey(); // Whatever
      }

      LargeObjectMetadata largeObjectMetadata = objectUnderTest.largeObjectMetadata();

      assert largeObjectMetadata.getLargeObjectKey() == largeObjectKey : "Unexpected largeObjectKey in LargeObjectMetadata returned";
      assert largeObjectMetadata.getTotalSizeInBytes() == bytes.length : "Unexpected totalSizeInBytes in LargeObjectMetadata returned: was "
               + largeObjectMetadata.getTotalSizeInBytes() + " - should have been " + bytes.length;
      assert largeObjectMetadata.getChunkMetadata().length == 3 : "Unexpected number of chunk keys in LargeObjectMetadata returned: was "
               + largeObjectMetadata.getChunkMetadata().length + " - should have been 3";
   }

   @Test(expectedExceptions = UnsupportedOperationException.class)
   public void testThatChunkIteratorDoesNotSupportRemove() {
      InputStream largeObject = new ByteArrayInputStream("This is a large object".getBytes());
      Chunks objectUnderTest = new Chunks(new Object(), largeObject, 2L,
               new UuidBasedKeyGenerator());

      objectUnderTest.iterator().remove();
   }
}
