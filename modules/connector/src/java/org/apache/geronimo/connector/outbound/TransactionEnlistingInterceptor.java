/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.connector.outbound;

import javax.resource.ResourceException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.apache.geronimo.transaction.context.TransactionContext;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.apache.geronimo.transaction.context.InheritableTransactionContext;

/**
 * TransactionEnlistingInterceptor.java
 * <p/>
 * <p/>
 * Created: Fri Sep 26 14:52:24 2003
 *
 * @version 1.0
 */
public class TransactionEnlistingInterceptor implements ConnectionInterceptor {

    private final ConnectionInterceptor next;
    private final TransactionContextManager transactionContextManager;

    public TransactionEnlistingInterceptor(ConnectionInterceptor next, TransactionContextManager transactionContextManager) {
        this.next = next;
        this.transactionContextManager = transactionContextManager;
    }

    public void getConnection(ConnectionInfo connectionInfo) throws ResourceException {
        next.getConnection(connectionInfo);
        try {
            ManagedConnectionInfo mci = connectionInfo.getManagedConnectionInfo();
            TransactionContext transactionContext = transactionContextManager.getContext();
            if ((transactionContext instanceof InheritableTransactionContext)) {
                InheritableTransactionContext inheritableTransactionContext = ((InheritableTransactionContext) transactionContext);
                if (inheritableTransactionContext.isActive()) {
                    XAResource xares = mci.getXAResource();
                    inheritableTransactionContext.getTransaction().enlistResource(xares);
                }
            }

        } catch (SystemException e) {
            throw new ResourceException("Could not get transaction", e);
        } catch (RollbackException e) {
            throw new ResourceException("Could not enlist resource in rolled back transaction",
                    e);
        }

    }

    /**
     * The <code>returnConnection</code> method
     * <p/>
     * todo Probably the logic needs improvement if a connection
     * error occurred and we are destroying the handle.
     *
     * @param connectionInfo         a <code>ConnectionInfo</code> value
     * @param connectionReturnAction a <code>ConnectionReturnAction</code> value
     */
    public void returnConnection(ConnectionInfo connectionInfo,
                                 ConnectionReturnAction connectionReturnAction) {
        try {
            ManagedConnectionInfo mci = connectionInfo.getManagedConnectionInfo();
            TransactionContext transactionContext = transactionContextManager.getContext();
            if ((transactionContext instanceof InheritableTransactionContext)) {
                InheritableTransactionContext inheritableTransactionContext = ((InheritableTransactionContext) transactionContext);
                if (inheritableTransactionContext.isActive()) {
                    XAResource xares = mci.getXAResource();
                    inheritableTransactionContext.getTransaction().delistResource(xares, XAResource.TMSUSPEND);
                }
            }

        } catch (SystemException e) {
            //maybe we should warn???
            connectionReturnAction = ConnectionReturnAction.DESTROY;
        }

        next.returnConnection(connectionInfo, connectionReturnAction);
    }

}
