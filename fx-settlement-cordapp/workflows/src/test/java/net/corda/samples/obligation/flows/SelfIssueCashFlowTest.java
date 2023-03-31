package net.corda.samples.obligation.flows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.utilities.ProgressTracker;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class SelfIssueCashFlowTest {
    /**
     * Method under test: {@link SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testConstructor() {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        new SelfIssueCashFlow(new Amount<>(1L, null));
    }

    /**
     * Method under test: {@link SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
    @Test
    public void testConstructor2() {
        ProgressTracker progressTracker = (new SelfIssueCashFlow(null)).getProgressTracker();
        assertEquals(2, progressTracker.getAllSteps().size());
        assertEquals(2, progressTracker.getAllStepsLabels().size());
        assertEquals(3, progressTracker.getSteps().length);
        Observable<List<Pair<Integer, String>>> stepsTreeChanges = progressTracker.getStepsTreeChanges();
        assertTrue(stepsTreeChanges instanceof ReplaySubject);
        assertEquals(-1, progressTracker.getStepsTreeIndex());
        assertTrue(progressTracker.getStepsTreeIndexChanges() instanceof ReplaySubject);
        assertFalse(progressTracker.getHasEnded());
        assertEquals(1, ((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).size());
        assertFalse(((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).hasObservers());
        assertEquals(1, ((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).getValues().length);
    }

    /**
     * Method under test: {@link SelfIssueCashFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testCall() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        (new SelfIssueCashFlow(new Amount<>(1L, null))).call();
    }

    /**
     * Method under test: {@link SelfIssueCashFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testCall2() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalStateException: You cannot access the flow's state machine until the flow has been started.
        //       at net.corda.core.flows.FlowLogic.getStateMachine(FlowLogic.kt:471)
        //       at net.corda.core.flows.FlowLogic.getServiceHub(FlowLogic.kt:118)
        //       at net.corda.samples.obligation.flows.SelfIssueCashFlow.call(SelfIssueCashFlow.java:31)
        //   See https://diff.blue/R013 to resolve this issue.

        (new SelfIssueCashFlow(null)).call();
    }
}

