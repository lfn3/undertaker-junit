package net.lfn3.undertaker.junit;

import org.junit.Ignore;
import org.junit.Test;

public class NoSourceRuleTest {
    @Test
    @Ignore
    public void showsErrorMessageIfSourceRuleIsMissing() throws Exception {
        Source source = new SourceRule();
        source.nextBool();
    }
}
