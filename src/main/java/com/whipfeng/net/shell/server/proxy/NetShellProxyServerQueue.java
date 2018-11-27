package com.whipfeng.net.shell.server.proxy;

import com.whipfeng.net.shell.ContextRouter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyServerQueue {

    private List<ContextRouter> nsList = new LinkedList<ContextRouter>();
    private List<ContextRouter> outList = new LinkedList<ContextRouter>();

    synchronized ContextRouter matchNetShell(ContextRouter outRouter) {
        Iterator<ContextRouter> nsItr = nsList.iterator();
        while (nsItr.hasNext()) {
            ContextRouter nsRouter = nsItr.next();
            if (ContextRouter.isMatchFrom(nsRouter, outRouter)) {
                nsItr.remove();
                return nsRouter;
            }
        }
        outList.add(outRouter);
        return null;
    }

    synchronized ContextRouter matchNetOut(ContextRouter nsRouter) {
        Iterator<ContextRouter> outItr = outList.iterator();
        while (outItr.hasNext()) {
            ContextRouter outRouter = outItr.next();
            if (ContextRouter.isMatchTo(outRouter, nsRouter)) {
                outItr.remove();
                return outRouter;
            }
        }
        nsList.add(nsRouter);
        return null;
    }
}
