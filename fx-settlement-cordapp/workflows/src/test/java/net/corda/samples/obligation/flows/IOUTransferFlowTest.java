package net.corda.samples.obligation.flows;

import static org.mockito.Mockito.mock;

import java.security.cert.CertificateParsingException;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.node.services.statemachine.FlowSessionImpl;
import net.corda.node.services.statemachine.SessionId;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.rainbow.BCRainbowPublicKey;
import org.junit.Ignore;
import org.junit.Test;

public class IOUTransferFlowTest {
    /**
     * Method under test: {@link IOUTransferFlow.InitiatorFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall() throws CertificateParsingException, FlowException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        UniqueIdentifier stateLinearId = new UniqueIdentifier();
        (new IOUTransferFlow.InitiatorFlow(stateLinearId, new Party(new X509CertificateObject(mock(Certificate.class)))))
                .call();
    }

    /**
     * Method under test: {@link IOUTransferFlow.InitiatorFlow#InitiatorFlow(UniqueIdentifier, Party)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowConstructor() throws CertificateParsingException {
        // TODO: Complete this test.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.security.cert.CertificateParsingException: cannot construct BasicConstraints: java.lang.NullPointerException
        //       at org.bouncycastle.jce.provider.X509CertificateObject.<init>(null)
        //   See https://diff.blue/R013 to resolve this issue.

        UniqueIdentifier stateLinearId = new UniqueIdentifier();
        new IOUTransferFlow.InitiatorFlow(stateLinearId, new Party(new X509CertificateObject(mock(Certificate.class))));

    }

    /**
     * Method under test: {@link IOUTransferFlow.Responder#call()}
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
        (new IOUTransferFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUTransferFlow.Responder#call()}
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
        (new IOUTransferFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUTransferFlow.Responder#Responder(FlowSession)}
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
        new IOUTransferFlow.Responder(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)));
    }
}

