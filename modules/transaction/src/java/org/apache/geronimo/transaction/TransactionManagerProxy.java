/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.apache.geronimo.transaction;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import javax.resource.spi.XATerminator;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GOperationInfo;
import org.apache.geronimo.gbean.GConstructorInfo;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidImporter;

/**
 * A wrapper for a TransactionManager that wraps all Transactions in a TransactionProxy
 * so that we can add addition metadata to the Transaction. Only begin (and setTransactionTimeout)
 * are delegated to the wrapped TransactionManager; all other operations are delegated to the
 * wrapped Transaction.
 *
 * @version $Revision: 1.3 $ $Date: 2004/02/23 20:28:43 $
 */
public class TransactionManagerProxy implements TransactionManager, XATerminator, XAWork {

    public static final GBeanInfo GBEAN_INFO;

    private final TransactionManager delegate;
    private final XidImporter importer;
    private final ThreadLocal threadTx = new ThreadLocal();
    private final Map importedTransactions = new HashMap();
    private Set activeTransactions = new HashSet();

    /**
     * Constructor taking the TransactionManager to wrap.
     * @param delegate the TransactionManager that should be wrapped
     */
    public TransactionManagerProxy(TransactionManager delegate, XidImporter importer) {
        this.delegate = delegate;
        this.importer = importer;
    }

    /**
     * Possibly temporary constructor to enable deploying this as a geronimo mbean w/o a managed constructor.
     */
    public TransactionManagerProxy() {
        this.delegate = new TransactionManagerImpl();
        this.importer = null;
    }

    public void setTransactionTimeout(int timeout) throws SystemException {
        delegate.setTransactionTimeout(timeout);
    }

    public void begin() throws NotSupportedException, SystemException {
        delegate.begin();
        threadTx.set(new TransactionProxy(delegate.getTransaction()));
    }

    public int getStatus() throws SystemException {
        Transaction tx = getTransaction();
        return (tx != null) ? tx.getStatus() : Status.STATUS_NO_TRANSACTION;
    }

    public Transaction getTransaction() throws SystemException {
        return (Transaction) threadTx.get();
    }

    public Transaction suspend() throws SystemException {
        Transaction tx = getTransaction();
        threadTx.set(null);
        return tx;
    }

    public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
        if (threadTx.get() != null) {
            throw new IllegalStateException("Transaction already associated with current thread");
        }
        if (tx instanceof TransactionProxy == false) {
            throw new InvalidTransactionException("Cannot resume foreign transaction: " + tx);
        }
        threadTx.set(tx);
    }

    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        Transaction tx = getTransaction();
        if (tx == null) {
            throw new IllegalStateException("No transaction associated with current thread");
        }
        try {
            tx.commit();
        } finally {
            threadTx.set(null);
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        Transaction tx = getTransaction();
        if (tx == null) {
            throw new IllegalStateException("No transaction associated with current thread");
        }
        try {
            tx.rollback();
        } finally {
            threadTx.set(null);
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        Transaction tx = getTransaction();
        if (tx == null) {
            throw new IllegalStateException("No transaction associated with current thread");
        }
        tx.setRollbackOnly();
    }

    /**
     * @see javax.resource.spi.XATerminator#commit(javax.transaction.xa.Xid, boolean)
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.remove(xid);
        if (tx == null) {
            throw new XAException("No imported transaction for xid: " + xid);
        }

        try {
            int status = tx.getStatus();
            assert status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARED;
        } catch (SystemException e) {
            throw new XAException();
        }
        importer.commit(tx.getDelegate(), onePhase);
    }

    /**
     * @see javax.resource.spi.XATerminator#forget(javax.transaction.xa.Xid)
     */
    public void forget(Xid xid) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.remove(xid);
        if (tx == null) {
            throw new XAException("No imported transaction for xid: " + xid);
        }

        try {
            int status = tx.getStatus();
            //assert status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARED;
        } catch (SystemException e) {
            throw new XAException();
        }
        importer.forget(tx.getDelegate());
    }

    /**
     * @see javax.resource.spi.XATerminator#prepare(javax.transaction.xa.Xid)
     */
    public int prepare(Xid xid) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.get(xid);
        if (tx == null) {
            throw new XAException("No imported transaction for xid: " + xid);
        }

        try {
            int status = tx.getStatus();
            assert status == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            throw new XAException();
        }
        return importer.prepare(tx.getDelegate());
    }

    /**
     * @see javax.resource.spi.XATerminator#recover(int)
     */
    public Xid[] recover(int arg0) throws XAException {
        throw new XAException("Not implemented.");
    }

    /**
     * @see javax.resource.spi.XATerminator#rollback(javax.transaction.xa.Xid)
     */
    public void rollback(Xid xid) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.remove(xid);
        if (tx == null) {
            throw new XAException("No imported transaction for xid: " + xid);
        }

        try {
            int status = tx.getStatus();
            assert status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARED;
        } catch (SystemException e) {
            throw new XAException();
        }
        importer.rollback(tx.getDelegate());
    }

    public void begin(Xid xid, long txTimeoutMillis) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.get(xid);
        if (tx == null) {
            try {
                tx = new TransactionProxy(importer.importXid(xid));
            } catch (SystemException e) {
                throw (XAException)new XAException("Could not import xid").initCause(e);
            }
            importedTransactions.put(xid, tx);
        }
        if (activeTransactions.contains(tx)) {
            throw new XAException("Xid already active");
        }
        activeTransactions.add(tx);
        threadTx.set(tx);
        importer.setTransactionTimeout(txTimeoutMillis);
    }

    public void end(Xid xid) throws XAException {
        TransactionProxy tx = (TransactionProxy) importedTransactions.get(xid);
        if (tx == null) {
            throw new XAException("No imported transaction for xid: " + xid);
        }
        if (!activeTransactions.remove(tx)) {
            throw new XAException("tx not active for xid: " + xid);
        }
    }

    //for now we use the default constructor.
    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(TransactionManagerProxy.class.getName());

        infoFactory.setConstructor(new GConstructorInfo(
                new String[]{"Delegate"},
                new Class[]{TransactionManager.class}));

        infoFactory.addAttribute(new GAttributeInfo("Delegate", true));

        infoFactory.addOperation(new GOperationInfo("setTransactionTimeout", new String[]{Integer.TYPE.getName()}));
        infoFactory.addOperation(new GOperationInfo("begin"));
        infoFactory.addOperation(new GOperationInfo("getStatus"));
        infoFactory.addOperation(new GOperationInfo("getTransaction"));
        infoFactory.addOperation(new GOperationInfo("suspend"));
        infoFactory.addOperation(new GOperationInfo("resume", new String[]{Transaction.class.getName()}));
        infoFactory.addOperation(new GOperationInfo("commit"));
        infoFactory.addOperation(new GOperationInfo("rollback"));
        infoFactory.addOperation(new GOperationInfo("setRollbackOnly"));
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
