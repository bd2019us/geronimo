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
package org.apache.geronimo.deployment.app;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.shared.ModuleType;

/**
 * A TargetModuleID for a module deployed to a single server.
 *
 * @version $Revision: 1.1 $ $Date: 2003/10/19 01:56:14 $
 */
public class ServerTargetModule implements TargetModuleID, Serializable {
    private ServerTarget target;
    private String module;
    private String webURL;
    private ServerTargetModule parent;
    private List children = new ArrayList();
    private transient ModuleType type;

    public ServerTargetModule(ModuleType type, ServerTarget target, String module) {
        this.type = type;
        this.target = target;
        this.module = module;
    }

    public ServerTargetModule(ModuleType type, ServerTarget target, String module, String webURL) {
        this.type = type;
        this.target = target;
        this.module = module;
        this.webURL = webURL;
    }

    public ServerTargetModule(ModuleType type, ServerTargetModule parent, ServerTarget target, String module) {
        this.type = type;
        this.parent = parent;
        parent.addChild(this);
        this.target = target;
        this.module = module;
    }

    public ServerTargetModule(ModuleType type, ServerTargetModule parent, ServerTarget target, String module, String webURL) {
        this.type = type;
        this.parent = parent;
        parent.addChild(this);
        this.target = target;
        this.module = module;
        this.webURL = webURL;
    }

    public Target getTarget() {
        return target;
    }

    public String getModuleID() {
        return module;
    }

    public String getWebURL() {
        return webURL;
    }

    public TargetModuleID getParentTargetModuleID() {
        return parent;
    }

    public void addChild(ServerTargetModule child) {
        children.add(child);
    }

    public TargetModuleID[] getChildTargetModuleID() {
        return (TargetModuleID[])children.toArray(new TargetModuleID[children.size()]);
    }

    public ModuleType getType() {
        return type;
    }

    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof ServerTargetModule)) return false;

        final ServerTargetModule serverTargetModule = (ServerTargetModule)o;

        if(!module.equals(serverTargetModule.module)) return false;
        if(parent != null ? !parent.equals(serverTargetModule.parent) : serverTargetModule.parent != null) return false;
        if(!target.equals(serverTargetModule.target)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = target.hashCode();
        result = 29 * result + module.hashCode();
        result = 29 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public String toString() {
        if(parent != null) {
            return parent+"->"+module+" on "+target.getName();
        } else {
            return module+" on "+target.getName();
        }
    }

    //todo: serialize the ModuleType value
}
