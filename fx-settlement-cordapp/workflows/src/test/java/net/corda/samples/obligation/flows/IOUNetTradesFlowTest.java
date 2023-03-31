package net.corda.samples.obligation.flows;

import static org.mockito.Mockito.mock;

import java.security.cert.CertificateParsingException;
import java.util.Currency;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.AnonymousParty;

import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.node.services.statemachine.FlowSessionImpl;
import net.corda.node.services.statemachine.SessionId;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.provider.rainbow.BCRainbowPublicKey;
import org.junit.Ignore;
import org.junit.Test;

public class IOUNetTradesFlowTest {
    /**
     * Method under test: {@link IOUNetTradesFlow.InitiatorFlow#call()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testInitiatorFlowCall() throws FlowException {
        // TODO: Complete this test.
        //   Diffblue AI was unable to find a test

        // Arrange
        // TODO: Populate arranged inputs
        IOUNetTradesFlow.InitiatorFlow initiatorFlow = null;

        // Act
        SignedTransaction actualCallResult = initiatorFlow.call();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link IOUNetTradesFlow.InitiatorFlow#InitiatorFlow(Currency, Currency, Party)}
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

        new IOUNetTradesFlow.InitiatorFlow(null, null, new Party(new X509CertificateObject(mock(Certificate.class))));

    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#NetSpendHolder(Currency, Long, Currency, Long, Party, Party)}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getAmountA()}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getAmountB()}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getCurrencyA()}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getCurrencyB()}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getMyself()}
     *   <li>{@link IOUNetTradesFlow.NetSpendHolder#getNetAgainstParty()}
     * </ul>
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testNetSpendHolderConstructor() {
        // TODO: Complete this test.
        //   Reason: R081 Exception in arrange section.
        //   Diffblue Cover was unable to construct an instance of the class under test using
        //   net.corda.samples.obligation.flows.IOUNetTradesFlow$NetSpendHolder.<init>
        //   A step in the arrange section threw an exception:
        //   CertificateParsingException cannot construct BasicConstraints: java.lang.NullPointerException
        //   More information about the exception is provided in the support log.
        //   See https://diff.blue/R081 for further troubleshooting of this issue.

        // Arrange
        // TODO: Populate arranged inputs
        Currency currencyA = null;
        Long amountA = null;
        Currency currencyB = null;
        Long amountB = null;
        Party myself = null;
        Party netAgainstParty = null;

        // Act
        IOUNetTradesFlow.NetSpendHolder actualNetSpendHolder = new IOUNetTradesFlow.NetSpendHolder(currencyA, amountA,
                currencyB, amountB, myself, netAgainstParty);
        Long actualAmountA = actualNetSpendHolder.getAmountA();
        Long actualAmountB = actualNetSpendHolder.getAmountB();
        Currency actualCurrencyA = actualNetSpendHolder.getCurrencyA();
        Currency actualCurrencyB = actualNetSpendHolder.getCurrencyB();
        Party actualMyself = actualNetSpendHolder.getMyself();
        Party actualNetAgainstParty = actualNetSpendHolder.getNetAgainstParty();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link IOUNetTradesFlow.ResponderFlow#call()}
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
        (new IOUNetTradesFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUNetTradesFlow.ResponderFlow#call()}
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
        (new IOUNetTradesFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)))).call();
    }

    /**
     * Method under test: {@link IOUNetTradesFlow.ResponderFlow#ResponderFlow(FlowSession)}
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
        new IOUNetTradesFlow.ResponderFlow(new FlowSessionImpl(destination, wellKnownParty, new SessionId(1L)));
    }
}

