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

package org.apache.geronimo.connector.outbound;

import junit.framework.TestCase;


/**
 * PoolDequeTest.java
 *
 *
 * Created: Fri Oct 10 12:00:47 2003
 *
 * @version 1.0
 */
public class PoolDequeTest extends TestCase
{

    private static final int MAX_SIZE = 10;

    public PoolDequeTest(String name)
    {
        super(name);
    } // PoolDequeTest constructor


    public void testFill() throws Exception {
        SinglePoolConnectionInterceptor.PoolDeque pool = new SinglePoolConnectionInterceptor.PoolDeque(MAX_SIZE);
        for (int i = 0; i < MAX_SIZE; i++) {
            pool.addLast(new ManagedConnectionInfo(null, null));
        }
    }

    public void testFillAndEmptyFirst() throws Exception {
        SinglePoolConnectionInterceptor.PoolDeque pool = new SinglePoolConnectionInterceptor.PoolDeque(MAX_SIZE);
        for (int i = 0; i < MAX_SIZE; i++) {
            pool.addLast(new ManagedConnectionInfo(null, null));
        }
        ManagedConnectionInfo[] mcis = new ManagedConnectionInfo[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            mcis[i] = pool.removeFirst();
        }
        assertTrue("Expected pool to be empty!", pool.isEmpty());

        for (int i = 0; i < MAX_SIZE; i++) {
            pool.addFirst(mcis[i]);
        }

    }

    public void testFillAndEmptyLast() throws Exception {
        SinglePoolConnectionInterceptor.PoolDeque pool = new SinglePoolConnectionInterceptor.PoolDeque(MAX_SIZE);
        ManagedConnectionInfo[] mcis = new ManagedConnectionInfo[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            mcis[i] = new ManagedConnectionInfo(null, null);
            pool.addLast(mcis[i]);
        }

        for (int i = MAX_SIZE - 1; i >= 0; i--) {
            assertTrue("Expected to get corresponding MCI from pool", mcis[i] == pool.peekLast());
            assertTrue("Expected to get corresponding MCI from pool", mcis[i] == pool.removeLast());
        }
        assertTrue("Expected pool to be empty!", pool.isEmpty());
    }

    public void testRemove() throws Exception {
        SinglePoolConnectionInterceptor.PoolDeque pool = new SinglePoolConnectionInterceptor.PoolDeque(MAX_SIZE);
        ManagedConnectionInfo[] mcis = new ManagedConnectionInfo[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            mcis[i] = new ManagedConnectionInfo(null, null);
            pool.addLast(mcis[i]);
        }

        for (int i = 0; i < MAX_SIZE; i++) {
            assertTrue("Expected to find MCI in pool", pool.remove(mcis[i]));
        }
        assertTrue("Expected pool to be empty!", pool.isEmpty());
    }

} // PoolDequeTest
