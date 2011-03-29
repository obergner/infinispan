package org.infinispan.commands.write;

import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.container.entries.ReadCommittedEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.impl.NonTxInvocationContext;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * WriteLargeObjectToKeyCommandTest.
 * 
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 * @since 5.1
 */
@Test(groups = "unit", testName = "commands.write.WriteLargeObjectToKeyCommandTest")
public class WriteLargeObjectToKeyCommandTest {

   @Test
   public void testThatGetParametersReturnsCorrectParametersArray() {
      Object expectedKey = new Object();
      InputStream expectedLargeObject = getClass().getResourceAsStream(
               getClass().getSimpleName() + ".class");
      Set<Flag> expectedFlags = Collections.emptySet();
      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(expectedKey,
               expectedLargeObject, null, expectedFlags);

      Object[] actualParameters = objectUnderTest.getParameters();

      assert actualParameters.length == 3 : "Parameters returned from getParameters() should have "
               + "exactly three entries";
      assert actualParameters[0] == expectedKey : "First entry of parameters array returned from "
               + "getParameters() should be the key passed in at construction time";
      assert actualParameters[1] == expectedLargeObject : "Second entry of parameters array returned from "
               + "getParameters() should be the Large Object (InputStream) passed in at construction time";
      assert actualParameters[2] == expectedFlags : "Third entry of parameters array returned from "
               + "getParameters() should be the flags passed in at construction time";
   }

   @Test
   public void testThatIsConditionalAlwaysReturnsFalse() {
      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(new Object(),
               getClass().getResourceAsStream(getClass().getSimpleName() + ".class"), null,
               Collections.<Flag> emptySet());

      boolean actualIsConditionalFlag = objectUnderTest.isConditional();

      assert !actualIsConditionalFlag : "isConditional() should always return false";
   }

   @Test
   public void testThatIsSuccessfulAlwaysReturnsTrue() {
      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(new Object(),
               getClass().getResourceAsStream(getClass().getSimpleName() + ".class"), null,
               Collections.<Flag> emptySet());

      boolean actualIsSuccessfulFlag = objectUnderTest.isSuccessful();

      assert actualIsSuccessfulFlag : "isSuccessful() should always return true";
   }

   @Test
   public void testThatPerformResetsIsRemovedFlagOnCacheEntryStoredInCtx() throws Throwable {
      Object expectedKey = new Object();
      InputStream expectedLargeObject = getClass().getResourceAsStream(
               getClass().getSimpleName() + ".class");
      Set<Flag> expectedFlags = Collections.singleton(Flag.FAIL_SILENTLY);

      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(expectedKey,
               expectedLargeObject, null, expectedFlags);
      MVCCEntry entry = new ReadCommittedEntry(expectedKey, new Object(), 0L);
      entry.setRemoved(true);
      NonTxInvocationContext ctx = new NonTxInvocationContext();
      ctx.putLookedUpEntry(expectedKey, entry);

      objectUnderTest.perform(ctx);

      assert !ctx.lookupEntry(expectedKey).isRemoved() : "perform(ctx) should have set the "
               + "isRemoved flag on the cache entry stored in ctx to false";
   }

   @Test
   public void testThatPerformResetsIsValidFlagOnCacheEntryStoredInCtx() throws Throwable {
      Object expectedKey = new Object();
      InputStream expectedLargeObject = getClass().getResourceAsStream(
               getClass().getSimpleName() + ".class");
      Set<Flag> expectedFlags = Collections.singleton(Flag.FAIL_SILENTLY);

      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(expectedKey,
               expectedLargeObject, null, expectedFlags);
      MVCCEntry entry = new ReadCommittedEntry(expectedKey, new Object(), 0L);
      entry.setRemoved(true);
      entry.setValid(false);
      NonTxInvocationContext ctx = new NonTxInvocationContext();
      ctx.putLookedUpEntry(expectedKey, entry);

      objectUnderTest.perform(ctx);

      assert ctx.lookupEntry(expectedKey).isValid() : "perform(ctx) should have set the "
               + "isValid flag on the cache entry stored in ctx to true";
   }

   @Test
   public void testThatSetParametersCorrectlySetsParameters() {
      Object expectedKey = new Object();
      InputStream expectedLargeObject = getClass().getResourceAsStream(
               getClass().getSimpleName() + ".class");
      Set<Flag> expectedFlags = Collections.singleton(Flag.FAIL_SILENTLY);

      WriteLargeObjectToKeyCommand objectUnderTest = new WriteLargeObjectToKeyCommand(new Object(),
               getClass().getResourceAsStream(getClass().getSimpleName() + ".class"), null,
               Collections.<Flag> emptySet());

      objectUnderTest.setParameters(WriteLargeObjectToKeyCommand.COMMAND_ID, new Object[] {
               expectedKey, expectedLargeObject, expectedFlags });

      assert objectUnderTest.getKey() == expectedKey : "setParameters() should have set key";
      assert objectUnderTest.getLargeObject() == expectedLargeObject : "setParameters() should have set largeObject";
      assert objectUnderTest.getFlags() == expectedFlags : "setParameters() should have set flags";
   }
}
