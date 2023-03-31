package net.corda.samples.obligation.flows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.cert.CertificateParsingException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import kotlin.Pair;

import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;
import net.corda.node.services.statemachine.FlowSessionImpl;
import net.corda.node.services.statemachine.SessionId;
import net.corda.samples.obligation.states.IOUState;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.rainbow.BCRainbowPublicKey;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class IOUIssueFlowTest {
    /**
     * Method under test: {@link IOUIssueFlow.InitiatorFlow#InitiatorFlow(IOUState)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowConstructor() throws CertificateParsingException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.IllegalArgumentException: Parameter specified as non-null is null: method net.corda.core.contracts.Amount.<init>, parameter token
        //       at net.corda.core.contracts.Amount.<init>(Amount.kt)
        //   See https://diff.blue/R013 to resolve this issue.

        Date tradeTime = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        Date valueDate = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        Amount<Currency> tradedAssetAmount = new Amount<>(1L, null);

        Party tradingParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        Amount<Currency> counterAssetAmount = new Amount<>(1L, null);

        new IOUIssueFlow.InitiatorFlow(
                new IOUState(tradeTime, valueDate, tradedAssetAmount, null, tradingParty, counterAssetAmount, null,
                        new Party(new X509CertificateObject(mock(Certificate.class))), IOUState.TradeStatus.NEW));
    }

    /**
     * Method under test: {@link IOUIssueFlow.InitiatorFlow#InitiatorFlow(IOUState)}
     */
    @Test
    public void testInitiatorFlowConstructor2() {
        ProgressTracker progressTracker = (new IOUIssueFlow.InitiatorFlow(null)).getProgressTracker();
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
     * Method under test: {@link IOUIssueFlow.ResponderFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderFlowCall() throws CertificateParsingException, FlowException {
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
        (new IOUIssueFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUIssueFlow.ResponderFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderFlowCall2() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        AnonymousParty destination = new AnonymousParty(new BCRainbowPublicKey(mock(RainbowPublicKeyParameters.class)));
        Party wellKnownParty = new Party(new X509CertificateObject(mock(Certificate.class)));
        (new IOUIssueFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUIssueFlow.ResponderFlow#ResponderFlow(FlowSession)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testResponderFlowConstructor() throws CertificateParsingException {
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
        new IOUIssueFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)));
    }
}

