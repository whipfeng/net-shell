package com.whipfeng.net.shell.server.proxy;

import com.whipfeng.net.shell.ContextRouter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyServerQueue {

    private List<ContextRouter> dfList = new LinkedList<ContextRouter>();
    private List<ContextRouter> nsList = new LinkedList<ContextRouter>();
    private List<ContextRouter> outList = new LinkedList<ContextRouter>();

    public synchronized ContextRouter matchNetShell(ContextRouter outRouter) {
        Iterator<ContextRouter> nsItr = nsList.iterator();
        ContextRouter nsRouter = matchContextRouter(outRouter, nsItr);
        if (nsRouter != null) {
            return nsRouter;
        }

        Iterator<ContextRouter> dfItr = dfList.iterator();
        ContextRouter dfRouter = matchContextRouter(outRouter, dfItr);
        if (dfRouter != null) {
            return dfRouter;
        }
        outList.add(outRouter);
        return null;
    }

    private ContextRouter matchContextRouter(ContextRouter outRouter, Iterator<ContextRouter> nsItr) {
        while (nsItr.hasNext()) {
            ContextRouter nsRouter = nsItr.next();
            if (ContextRouter.isMatchFrom(nsRouter, outRouter)) {
                nsItr.remove();
                return nsRouter;
            }
        }
        return null;
    }

    public synchronized ContextRouter matchNetOut(ContextRouter nsRouter) {
        Iterator<ContextRouter> outItr = outList.iterator();
        while (outItr.hasNext()) {
            ContextRouter outRouter = outItr.next();
            if (ContextRouter.isMatchTo(outRouter, nsRouter)) {
                outItr.remove();
                return outRouter;
            }
        }
        if (nsRouter.getNetworkCode() == 0 && nsRouter.getSubMaskCode() == 0) {
            dfList.add(nsRouter);
            return null;
        }
        nsList.add(nsRouter);
        return null;
    }
}
