package io.nuls.ledger.event;

import io.nuls.ledger.entity.tx.UnlockCoinTransaction;

/**
 *
 * @author Niels
 * @date 2017/11/20
 */
public class UnlockCoinEvent<T extends UnlockCoinTransaction> extends AbstractCoinTransactionEvent<T> {

    public UnlockCoinEvent() {
        super((short) 5);
    }
}
