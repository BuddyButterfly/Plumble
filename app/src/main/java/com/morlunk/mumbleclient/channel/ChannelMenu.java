/*
 * Copyright (C) 2015 Andrew Comminos <andrew@comminos.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.morlunk.mumbleclient.channel;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.morlunk.jumble.IJumbleService;
import com.morlunk.jumble.model.IChannel;
import com.morlunk.jumble.model.Server;
import com.morlunk.jumble.net.Permissions;
import com.morlunk.mumbleclient.R;
import com.morlunk.mumbleclient.channel.comment.ChannelDescriptionFragment;
import com.morlunk.mumbleclient.db.PlumbleDatabase;

/**
 * Created by andrew on 22/11/15.
 */
public class ChannelMenu implements PermissionsPopupMenu.IOnMenuPrepareListener, PopupMenu.OnMenuItemClickListener {
    private final Context mContext;
    private final IChannel mChannel;
    private final IJumbleService mService;
    private final PlumbleDatabase mDatabase;
    private final FragmentManager mFragmentManager;

    public ChannelMenu(Context context, IChannel channel, IJumbleService service,
                       PlumbleDatabase database, FragmentManager fragmentManager) {
        mContext = context;
        mChannel = channel;
        mService = service;
        mDatabase = database;
        mFragmentManager = fragmentManager;
    }

    @Override
    public void onMenuPrepare(Menu menu, int permissions) {
        // This breaks uMurmur ACL. Put in a fix based on server version perhaps?
        //menu.getMenu().findItem(R.id.menu_channel_add)
        // .setVisible((permissions & (Permissions.MakeChannel | Permissions.MakeTempChannel)) > 0);
        menu.findItem(R.id.context_channel_edit).setVisible((permissions & Permissions.Write) > 0);
        menu.findItem(R.id.context_channel_remove).setVisible((permissions & Permissions.Write) > 0);
        menu.findItem(R.id.context_channel_view_description)
                .setVisible(mChannel.getDescription() != null ||
                        mChannel.getDescriptionHash() != null);
        Server server = mService.getConnectedServer();
        if(server != null) {
            menu.findItem(R.id.context_channel_pin)
                    .setChecked(mDatabase.isChannelPinned(server.getId(), mChannel.getId()));
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        boolean adding = false;
        switch(item.getItemId()) {
            case R.id.context_channel_join:
                mService.joinChannel(mChannel.getId());
                break;
            case R.id.context_channel_add:
                adding = true;
            case R.id.context_channel_edit:
                ChannelEditFragment addFragment = new ChannelEditFragment();
                Bundle args = new Bundle();
                if (adding) args.putInt("parent", mChannel.getId());
                else args.putInt("channel", mChannel.getId());
                args.putBoolean("adding", adding);
                addFragment.setArguments(args);
                addFragment.show(mFragmentManager, "ChannelAdd");
                break;
            case R.id.context_channel_remove:
                AlertDialog.Builder adb = new AlertDialog.Builder(mContext);
                adb.setTitle(R.string.confirm);
                adb.setMessage(R.string.confirm_delete_channel);
                adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mService.removeChannel(mChannel.getId());
                    }
                });
                adb.setNegativeButton(android.R.string.cancel, null);
                adb.show();
                break;
            case R.id.context_channel_view_description:
                Bundle commentArgs = new Bundle();
                commentArgs.putInt("channel", mChannel.getId());
                commentArgs.putString("comment", mChannel.getDescription());
                commentArgs.putBoolean("editing", false);
                DialogFragment commentFragment = (DialogFragment) Fragment.instantiate(mContext,
                        ChannelDescriptionFragment.class.getName(), commentArgs);
                commentFragment.show(mFragmentManager, ChannelDescriptionFragment.class.getName());
                break;
            case R.id.context_channel_pin:
                long serverId = mService.getConnectedServer().getId();
                boolean pinned = mDatabase.isChannelPinned(serverId, mChannel.getId());
                if(!pinned) mDatabase.addPinnedChannel(serverId, mChannel.getId());
                else mDatabase.removePinnedChannel(serverId, mChannel.getId());
                break;
            default:
                return false;
        }
        return true;
    }

    public void showPopup(View anchor) {
        PermissionsPopupMenu popupMenu = new PermissionsPopupMenu(mContext, anchor,
                R.menu.context_channel, this, this, mChannel, mService);
        popupMenu.show();
    }
}
