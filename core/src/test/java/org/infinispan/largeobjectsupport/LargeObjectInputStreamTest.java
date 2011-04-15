package org.infinispan.largeobjectsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.Test;

/**
 * Test {@link LargeObjectInputStreamTest}.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
public class LargeObjectInputStreamTest {

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void largeObjectInputStreamConstructorShouldRejectMaxChunkSizeInBytesGreaterThanAllowed() {
      LargeObjectMetadata illegalMetadata = LargeObjectMetadata.newBuilder()
               .withLargeObjectKey(new Object()).withMaxChunkSizeInBytes(Long.MAX_VALUE)
               .addChunk(new Object(), 0L).build();
      new LargeObjectInputStream(illegalMetadata, Collections.emptyMap());
   }

   @Test
   public void readShouldReturnMinus1AfterAllBytesHaveBeenRead() throws IOException {
      int largeObjectSize = 1000;
      int maxChunkSize = 50;
      TestData testdata = newTestData(largeObjectSize, maxChunkSize);
      LargeObjectInputStream objectUnderTest = new LargeObjectInputStream(testdata.metadata,
               testdata.chunkCache);

      for (int i = 0; i < largeObjectSize; i++) {
         int read = objectUnderTest.read();
         assert read != -1 : "read() returned -1 BEFORE all bytes had been read";
      }

      int after = objectUnderTest.read();
      assert after == -1 : "read() did NOT return -1 after all bytes have been read. Instead, it returned "
               + after;
   }

   @Test
   public void readingAllBytesShouldReturnTheWholeLargeObjectInCaseOfEqualChunks()
            throws IOException {
      int largeObjectSize = 1000;
      int maxChunkSize = 50;
      TestData testdata = newTestData(largeObjectSize, maxChunkSize);
      LargeObjectInputStream objectUnderTest = new LargeObjectInputStream(testdata.metadata,
               testdata.chunkCache);

      ByteArrayOutputStream largeObject = new ByteArrayOutputStream();
      int currentByte = -1;
      while ((currentByte = objectUnderTest.read()) != -1) {
         largeObject.write(currentByte);
      }

      assert Arrays.equals(testdata.largeObject, largeObject.toByteArray()) : "read() did not produce the large object passed in";
   }

   @Test
   public void readingAllBytesShouldReturnTheWholeLargeObjectInCaseOfUnequalChunks()
            throws IOException {
      int largeObjectSize = 3;
      int maxChunkSize = 2;
      TestData testdata = newTestData(largeObjectSize, maxChunkSize);
      LargeObjectInputStream objectUnderTest = new LargeObjectInputStream(testdata.metadata,
               testdata.chunkCache);

      ByteArrayOutputStream largeObject = new ByteArrayOutputStream();
      int currentByte = -1;
      while ((currentByte = objectUnderTest.read()) != -1) {
         largeObject.write(currentByte);
      }

      assert Arrays.equals(testdata.largeObject, largeObject.toByteArray()) : "read() did not produce the large object passed in";
   }

   public TestData newTestData(int largeObjectSize, int maxChunkSize) {
      Map<String, byte[]> chunkCache = new HashMap<String, byte[]>();
      int currentSize = 0;
      byte[] largeObject = new byte[largeObjectSize];
      List<String> chunkKeys = new ArrayList<String>();
      LargeObjectMetadata.Builder metadataBuilder = LargeObjectMetadata.newBuilder()
               .withLargeObjectKey(UUID.randomUUID().toString())
               .withMaxChunkSizeInBytes(maxChunkSize);
      do {
         int currentChunkSize = (largeObjectSize - currentSize < maxChunkSize) ? (int) (largeObjectSize - currentSize)
                  : maxChunkSize;
         byte[] currentChunk = new byte[currentChunkSize];
         for (int currentChunkIdx = 0; currentChunkIdx < currentChunkSize; currentChunkIdx++) {
            byte currentByte = (byte) (currentSize % 255);
            currentChunk[currentChunkIdx] = currentByte;
            largeObject[currentSize++] = currentByte;
         }
         String currentChunkKey = UUID.randomUUID().toString();
         chunkKeys.add(currentChunkKey);
         chunkCache.put(currentChunkKey, currentChunk);
         metadataBuilder.addChunk(currentChunkKey, currentChunkSize);
      } while (currentSize < largeObjectSize);
      LargeObjectMetadata metadata = metadataBuilder.build();
      return new TestData(chunkCache, metadata, largeObject);
   }

   private static final class TestData {

      final Map<String, byte[]> chunkCache;

      final LargeObjectMetadata metadata;

      final byte[] largeObject;

      TestData(Map<String, byte[]> chunkCache, LargeObjectMetadata metadata, byte[] largeObject) {
         this.chunkCache = chunkCache;
         this.metadata = metadata;
         this.largeObject = largeObject;
      }
   }
}
