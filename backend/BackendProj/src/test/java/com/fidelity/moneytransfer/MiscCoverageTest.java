package com.fidelity.moneytransfer;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the trivial exception types and the constants holder's private
 * constructor.
 */
class MiscCoverageTest {

    @Test
    void exceptions_CarryMessage() {
        assertEquals("a", new AccountNotFoundException("a").getMessage());
        assertEquals("b", new AccountNotActiveException("b").getMessage());
        assertEquals("c", new InsufficientBalanceException("c").getMessage());
        assertEquals("d", new DuplicateTransferException("d").getMessage());
    }

    @Test
    void exceptions_AreRuntimeExceptions() {
        assertTrue(RuntimeException.class.isAssignableFrom(AccountNotFoundException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(DuplicateTransferException.class));
    }

    @Test
    void rewardConstants_HasExpectedValues_AndPrivateCtor() throws Exception {
        assertEquals(9_999_999_999L, RewardConstants.CASHBACK_ACCOUNT_ID);
        assertEquals("CASHBACK", RewardConstants.CASHBACK_ACCOUNT_NAME);
        assertEquals(30L, RewardConstants.INACTIVITY_DOWNGRADE_DAYS);
        assertEquals(10L, RewardConstants.DOWNGRADE_WARNING_LEAD_DAYS);

        Constructor<RewardConstants> ctor =
                RewardConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
