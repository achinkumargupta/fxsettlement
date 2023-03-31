package net.corda.samples.obligation.flows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.cert.CertificateParsingException;
import java.util.List;

import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;
import net.corda.node.services.statemachine.FlowSessionImpl;
import net.corda.node.services.statemachine.SessionId;
import net.corda.samples.obligation.flows.IOUSettleFlow;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.rainbow.BCRainbowPublicKey;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class IOUSettleFlowTest {
    /**
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalStateException: You cannot access the flow's state machine until the flow has been started.
        //       at net.corda.core.flows.FlowLogic.getStateMachine(FlowLogic.kt:471)
        //       at net.corda.core.flows.FlowLogic.getServiceHub(FlowLogic.kt:118)
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$InitiatorFlow.call(IOUSettleFlow.java:58)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.InitiatorFlow(new UniqueIdentifier())).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall2() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$InitiatorFlow.call(IOUSettleFlow.java:56)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.InitiatorFlow(null)).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#call()}
     */
   /* @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalStateException: You cannot access the flow's state machine until the flow has been started.
        //       at net.corda.core.flows.FlowLogic.getStateMachine(FlowLogic.kt:471)
        //       at net.corda.core.flows.FlowLogic.getServiceHub(FlowLogic.kt:118)
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$InitiatorFlow.call(IOUSettleFlow.java:58)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.InitiatorFlow(new UniqueIdentifier())).call();
    }
*/
    /**
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#call()}
     */
  /*  @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall2() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$InitiatorFlow.call(IOUSettleFlow.java:56)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.InitiatorFlow(null)).call();
    }
*/

    /**
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#InitiatorFlow(UniqueIdentifier)}
     */
    @Test
    public void testInitiatorFlowConstructor() {
        ProgressTracker progressTracker = (new IOUSettleFlow.InitiatorFlow(new UniqueIdentifier())).getProgressTracker();
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
     * Method under test: {@link IOUSettleFlow.InitiatorFlow#InitiatorFlow(UniqueIdentifier)}
     */
    @Test
    public void testInitiatorFlowConstructor2() {
        ProgressTracker progressTracker = (new IOUSettleFlow.InitiatorFlow(new UniqueIdentifier("42")))
                .getProgressTracker();
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
     * Method under test: {@link IOUSettleFlow.Responder#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderCall() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(
                new BCRainbowPublicKey(new RainbowPublicKeyParameters(1, new short[][]{new short[]{1, -1, 1, -1}},
                        new short[][]{new short[]{1, -1, 1, -1}}, new short[]{1, -1, 1, -1})));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        (new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.Responder#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderCall2() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(new BCRainbowPublicKey(mock(RainbowPublicKeyParameters.class)));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        (new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.Responder#Responder(FlowSession)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderConstructor() throws CertificateParsingException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(
                new BCRainbowPublicKey(new RainbowPublicKeyParameters(1, new short[][]{new short[]{1, -1, 1, -1}},
                        new short[][]{new short[]{1, -1, 1, -1}}, new short[]{1, -1, 1, -1})));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)));
    }

    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowCall() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.SelfIssueCashFlow(new Amount<>(1L, null))).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowCall2() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Byte Array must not be empty
        //       at net.corda.core.utilities.OpaqueBytes.<init>(ByteArrays.kt:157)
        //       at net.corda.core.utilities.OpaqueBytes$Companion.of(ByteArrays.kt:153)
        //       at net.corda.core.utilities.OpaqueBytes.of(ByteArrays.kt)
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$SelfIssueCashFlow.call(IOUSettleFlow.java:188)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.SelfIssueCashFlow(null)).call();
    }

    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowConstructor() {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        new IOUSettleFlow.SelfIssueCashFlow(new Amount<>(1L, null));
    }

    /**
     * Method under test: {@link IOUSettleFlow.Responder#call()}
     */
    /*@Test
    @Ignore("TODO: Complete this test")
    public void testResponderCall() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(
                new BCRainbowPublicKey(new RainbowPublicKeyParameters(1, new short[][]{new short[]{1, -1, 1, -1}},
                        new short[][]{new short[]{1, -1, 1, -1}}, new short[]{1, -1, 1, -1})));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        (new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }
*/
    /**
     * Method under test: {@link IOUSettleFlow.Responder#call()}
     */
  /*  @Test
    @Ignore("TODO: Complete this test")
    public void testResponderCall2() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(new BCRainbowPublicKey(mock(RainbowPublicKeyParameters.class)));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        (new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }*/

    /**
     * Method under test: {@link IOUSettleFlow.Responder#Responder(FlowSession)}
     */
    /*@Test
    @Ignore("TODO: Complete this test")
    public void testResponderConstructor() throws CertificateParsingException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(
                new BCRainbowPublicKey(new RainbowPublicKeyParameters(1, new short[][]{new short[]{1, -1, 1, -1}},
                        new short[][]{new short[]{1, -1, 1, -1}}, new short[]{1, -1, 1, -1})));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        new IOUSettleFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)));
    }
*/
    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#call()}
     */
  /*  @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowCall() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.SelfIssueCashFlow(new Amount<>(1L, null))).call();
    }

    *//**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#call()}
     *//*
    @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowCall2() throws FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Byte Array must not be empty
        //       at net.corda.core.utilities.OpaqueBytes.<init>(ByteArrays.kt:157)
        //       at net.corda.core.utilities.OpaqueBytes$Companion.of(ByteArrays.kt:153)
        //       at net.corda.core.utilities.OpaqueBytes.of(ByteArrays.kt)
        //       at net.corda.samples.obligation.flows.IOUSettleFlow$SelfIssueCashFlow.call(IOUSettleFlow.java:188)
        //   See https://diff.blue/R013 to resolve this issue.

        (new IOUSettleFlow.SelfIssueCashFlow(null)).call();
    }
*/
    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
  /*  @Test
    @Ignore("TODO: Complete this test")
    public void testSelfIssueCashFlowConstructor() {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        new IOUSettleFlow.SelfIssueCashFlow(new Amount<>(1L, null));
    }
*/

    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
    @Test
    public void testSelfIssueCashFlowConstructor2() {
        IOUSettleFlow.SelfIssueCashFlow actualSelfIssueCashFlow = new IOUSettleFlow.SelfIssueCashFlow(null);
        //assertNull(actualSelfIssueCashFlow.amount);
        ProgressTracker progressTracker = actualSelfIssueCashFlow.getProgressTracker();
        assertTrue(progressTracker.getStepsTreeIndexChanges() instanceof ReplaySubject);
        assertEquals(-1, progressTracker.getStepsTreeIndex());
        Observable<List<Pair<Integer, String>>> stepsTreeChanges = progressTracker.getStepsTreeChanges();
        assertTrue(stepsTreeChanges instanceof ReplaySubject);
        assertEquals(2, progressTracker.getAllSteps().size());
        assertEquals(3, progressTracker.getSteps().length);
        assertEquals(2, progressTracker.getAllStepsLabels().size());
        assertFalse(progressTracker.getHasEnded());
        assertFalse(((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).hasObservers());
        assertTrue(((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).hasAnyValue());
        assertEquals(1, ((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).getValues().length);
    }

    /**
     * Method under test: {@link IOUSettleFlow.SelfIssueCashFlow#SelfIssueCashFlow(Amount)}
     */
    @Test
    public void testSelfIssueCashFlowConstructor3() {
        IOUSettleFlow.SelfIssueCashFlow actualSelfIssueCashFlow = new IOUSettleFlow.SelfIssueCashFlow(null);
        assertNull(actualSelfIssueCashFlow.amount);
        ProgressTracker progressTracker = actualSelfIssueCashFlow.getProgressTracker();
        assertTrue(progressTracker.getStepsTreeIndexChanges() instanceof ReplaySubject);
        assertEquals(-1, progressTracker.getStepsTreeIndex());
        Observable<List<Pair<Integer, String>>> stepsTreeChanges = progressTracker.getStepsTreeChanges();
        assertTrue(stepsTreeChanges instanceof ReplaySubject);
        assertEquals(2, progressTracker.getAllSteps().size());
        assertEquals(3, progressTracker.getSteps().length);
        assertEquals(2, progressTracker.getAllStepsLabels().size());
        assertFalse(progressTracker.getHasEnded());
        assertFalse(((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).hasObservers());
        assertTrue(((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).hasAnyValue());
        assertEquals(1, ((ReplaySubject<List<Pair<Integer, String>>>) stepsTreeChanges).getValues().length);
    }
}

