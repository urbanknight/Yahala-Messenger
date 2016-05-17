package com.yahala.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.privacy.PrivacyListListener;
import org.jivesoftware.smackx.privacy.PrivacyListManager;
import org.jivesoftware.smackx.privacy.packet.PrivacyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 6/16/2014.
 */
public class Privacy implements PrivacyListListener {
    private static volatile Privacy Instance = null;

    public Privacy() {

    }

    public static Privacy getInstance() {
        Privacy localInstance = Instance;
        if (localInstance == null) {
            synchronized (XMPPManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new Privacy();
                }
            }
        }
        return localInstance;
    }

    public void addlistitem() {
        // Set the name of the list
        String listName = "newList";

        // Create the list of PrivacyItem that will allow or deny some privacy aspect
        String user = "tybalt@example.com";
        String groupName = "enemies";
        ArrayList privacyItems = new ArrayList();

        PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid, user, true, 1);
        privacyItems.add(item);

        item = new PrivacyItem(PrivacyItem.Type.subscription, PrivacyItem.SUBSCRIPTION_BOTH, true, 2);
        privacyItems.add(item);

        item = new PrivacyItem(PrivacyItem.Type.group, groupName, false, 3);
        item.setFilterMessage(true);
        privacyItems.add(item);

        // Get the privacy manager for the current connection.
        PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(XMPPManager.getInstance().connection);

        // Create the new list.
        try {

            privacyManager.createPrivacyList(listName, privacyItems);


        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPrivacyList(String listName, List<PrivacyItem> listItem) {

    }

    @Override
    public void updatedPrivacyList(String listName) {

    }
}
