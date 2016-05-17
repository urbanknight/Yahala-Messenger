package com.yahala.xmpp;


import com.nostra13.universalimageloader.utils.L;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.UserConfig;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by user on 8/25/2015.
 */
public class MucManager implements InvitationListener {
    private static volatile MucManager Instance = null;
    private MultiUserChatManager manager;
    private MultiUserChat mchat;
    private String groupChatServer = "conference.yahala";

    public MucManager() {
        // Get the MultiUserChatManager
        manager = MultiUserChatManager.getInstanceFor(XMPPManager.getInstance().getConnection());
        manager.addInvitationListener(this);
    }

    public static MucManager getInstance() {
        MucManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (MucManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MucManager();
                }
            }
        }
        return localInstance;
    }

    public boolean join(String group) throws XmppStringprepException {


        try {
            mchat = manager.getMultiUserChat(JidCreate.entityBareFrom((group + "@" + groupChatServer)));

            if (!mchat.isJoined()) {
                String user = "962797982823@yahala" + "/" + XMPPManager.RESOURCE;
                FileLog.d("CONNECT", "Joining room !! " + group + " and username " + user);
                boolean createNow = false;

                try {

                    mchat.createOrJoin(Resourcepart.from(user));
                    createNow = true;
                } catch (Exception e) {
                    FileLog.d("CONNECT", "Error while creating the room " + group + e.getMessage());
                }

                if (createNow) {
                    try {
                        mchat.sendConfigurationForm(new Form(DataForm.Type.submit)); //this is to create the room immediately after join.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            FileLog.d("CONNECT", "Room created!!");
            return true;

        } catch (SmackException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void createRoomAndSaveToDb(String roomName, ArrayList<String> jidArrayList) {
        String bareJid = roomName + "@group.yahala";
        EntityBareJid roomJid = null;
        try {
            roomJid = JidCreate.entityBareFrom(bareJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        if (roomJid != null) {
            MultiUserChat muc = manager.getMultiUserChat(roomJid);


            try {
                FileLog.e("muc", "962797982823@yahala" + "/" + XMPPManager.RESOURCE);
                String user = "962797982823@yahala" + "/" + XMPPManager.RESOURCE;

                muc.create(Resourcepart.from(user));

                // Get the the room's configuration form
                Form form = muc.getConfigurationForm();
                // Create a new form to submit based on the original form
                Form submitForm = form.createAnswerForm();

               /* for (FormField field : form.getFields())
                {
                    if(!(field.getType() == FormField.Type.hidden) && field.getVariable()!= null){
                        submitForm.setDefaultAnswer(field.getVariable());
                    }
                }*/
                for (Iterator fields = (Iterator) form.getFields(); fields.hasNext(); ) {
                    FormField field = (FormField) fields.next();
                    if (!FormField.Type.hidden.equals(field.getType()) && field.getVariable() != null) {
                        submitForm.setDefaultAnswer(field.getVariable());
                    }
                }
                submitForm.setAnswer("muc#roomconfig_publicroom", false);
                submitForm.setAnswer("muc#roomconfig_persistentroom", true);
                submitForm.setAnswer("muc#roomconfig_whois", Arrays.asList("anyone"));
                List owners = new ArrayList();
                owners.add("962797982823@yahala");
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                muc.sendConfigurationForm(submitForm);
                joinMultiUserChat(user, roomName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (String jidStr : jidArrayList) {
                try {
                    muc.invite(jidStr, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //  DbHelper.addRoomToDb(roomJid, userAppPreference.getUserStringKey(), jidArrayList);
        }

    }

    public MultiUserChat joinMultiUserChat(String user, String roomName) {
        String bareJid = roomName + "@yahala." + XMPPManager.getInstance().getConnection().getServiceName();
        EntityBareJid roomJid = null;

        try {
            FileLog.e("joinMultiUserChat", Resourcepart.from(user).toString());
            roomJid = JidCreate.entityBareFrom(bareJid);
            MultiUserChat muc = manager.getMultiUserChat(roomJid);
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(0);
            //history.setSince(new Date());
            muc.join(Resourcepart.from(user));
            return muc;
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }
  /*  @Override
    public void deleteChatGroupAsync(ChatGroup group) {

        String chatRoomJid = group.getAddress().getAddress();

        if (mMUCs.containsKey(chatRoomJid))
        {
            MultiUserChat muc = mMUCs.get(chatRoomJid);

            try {
                //muc.destroy("", null);

                mMUCs.remove(chatRoomJid);

            } catch (Exception e) {
                debug(TAG,"error destroying MUC",e);
            }

        }

    }*/


    /* Roaster Manager*/
    public List<RosterGroup> getGroups(Roster roster) {
        List<RosterGroup> groupList = new ArrayList<RosterGroup>();
        Collection<RosterGroup> rosterGroups = roster.getGroups();
        Iterator<RosterGroup> i = rosterGroups.iterator();
        while (i.hasNext()) {
            groupList.add(i.next());
        }
        return groupList;
    }

    public boolean CreateGroup(Roster roster, String groupName) {
        try {
            roster.createGroup(groupName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<RosterEntry> getEntriesByGroup(Roster roster, String groupName) {
        List<RosterEntry> entriesList = new ArrayList<RosterEntry>();
        RosterGroup rosterGroup = roster.getGroup(groupName);
        Collection<RosterEntry> rosterEntries = rosterGroup.getEntries();
        Iterator<RosterEntry> i = rosterEntries.iterator();
        while (i.hasNext()) {
            entriesList.add(i.next());
        }
        return entriesList;
    }

    //kick user
    public void kickUserFromRoom(String threadId, String userJid) {
        MultiUserChat multiUserChat = null;
        try {
            multiUserChat = manager.getMultiUserChat(JidCreate.entityBareFrom(threadId));
            List<EntityFullJid> list = multiUserChat.getOccupants();
            Occupant occupant = multiUserChat.getOccupant(JidCreate.entityFullFrom(userJid + "/" + XMPPManager.RESOURCE));
            multiUserChat.kickParticipant(occupant.getNick(), "");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }

    }

    //invite user
    public void inviteUserToRoom(String threadId, String userJid) {
        MultiUserChat multiUserChat = null;
        try {
            multiUserChat = manager.getMultiUserChat(JidCreate.entityBareFrom(threadId));
            multiUserChat.invite(userJid, "");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //get Participants
    public List<String> getRoomParticipants(String threadId) {
        List<EntityFullJid> usersJids = null;
        List<String> usersIds = new ArrayList<>();
        try {
            //RoomInfo roomInfo = manager.getRoomInfo(JidCreate.entityBareFrom(threadId));
            MultiUserChat multiUserChat = manager.getMultiUserChat(JidCreate.entityBareFrom(threadId));
            usersJids = multiUserChat.getOccupants();

            for (EntityFullJid entityFullJid : usersJids) {
                usersIds.add(entityFullJid.toString());
            }
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        return usersIds;
    }


    @Override
    public void invitationReceived(XMPPConnection conn, MultiUserChat room, String inviter, String reason, String password, Message message) {
        final EntityBareJid roomBareJid = room.getRoom();
        if (!manager.getJoinedRooms().contains(room.getRoom())) {
            joinRoom(roomBareJid);
            //List<String> usersJids = getRoomParticipants(roomBareJid.asEntityBareJidString());
            //DbHelper.addRoomToDb(roomBareJid, "", null);
        }
    }

    void joinRoom(EntityBareJid roomJid) {
        MultiUserChat multiUserChat = manager.getMultiUserChat(roomJid);
        try {
            String nickName = (UserConfig.currentUser.first_name + " " + UserConfig.currentUser.last_name/*userAppPreference.getFirstSecondName()*/);
            FileLog.e("joinRoom", UserConfig.currentUser.first_name + " " + UserConfig.currentUser.last_name);
            multiUserChat.join(Resourcepart.from(nickName));
        } catch (Exception e) {
            e.printStackTrace();
            // Log.i(RiaXmppService.TAG, e.getMessage());
        }
    }

}
